package com.example.clothstock.ui.common

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import androidx.test.espresso.IdlingResource
import java.lang.ref.WeakReference

/**
 * SwipeableDetailPanelのアニメーション完了を待機するIdlingResource
 * 
 * アニメーション中はidle状態をfalseにして、完了時にtrueにする
 */
class AnimationIdlingResource(
    view: View,
    private val resourceName: String = "AnimationIdlingResource"
) : IdlingResource {

    private val viewRef = WeakReference(view)
    private var callback: IdlingResource.ResourceCallback? = null
    private var isIdle = true

    init {
        // Viewのアニメーター監視を開始
        startMonitoring()
    }

    override fun getName(): String = resourceName

    override fun isIdleNow(): Boolean = isIdle

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
    }

    private fun startMonitoring() {
        val view = viewRef.get() ?: return
        
        // ViewPropertyAnimatorの監視
        view.animate().setListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                setIdle(false)
            }
            
            override fun onAnimationEnd(animation: Animator) {
                setIdle(true)
            }
            
            override fun onAnimationCancel(animation: Animator) {
                setIdle(true)
            }
            
            override fun onAnimationRepeat(animation: Animator) {
                // Do nothing
            }
        })
    }

    private fun setIdle(idle: Boolean) {
        isIdle = idle
        if (idle) {
            callback?.onTransitionToIdle()
        }
    }

    companion object {
        /**
         * ValueAnimatorの監視用IdlingResource
         */
        fun forValueAnimator(animator: ValueAnimator): IdlingResource {
            return object : IdlingResource {
                private var callback: IdlingResource.ResourceCallback? = null
                
                init {
                    animator.addListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {
                            // アニメーション開始 - idle状態ではない
                        }
                        
                        override fun onAnimationEnd(animation: Animator) {
                            callback?.onTransitionToIdle()
                        }
                        
                        override fun onAnimationCancel(animation: Animator) {
                            callback?.onTransitionToIdle()
                        }
                        
                        override fun onAnimationRepeat(animation: Animator) {
                            // Do nothing
                        }
                    })
                }
                
                override fun getName(): String = "ValueAnimatorIdlingResource"
                
                override fun isIdleNow(): Boolean = !animator.isRunning
                
                override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
                    this.callback = callback
                }
            }
        }
        
        /**
         * 単純なタイムアウト待機IdlingResource（最後の手段として使用）
         */
        fun timeout(timeoutMs: Long): IdlingResource {
            return object : IdlingResource {
                private var callback: IdlingResource.ResourceCallback? = null
                private val startTime = System.currentTimeMillis()
                
                override fun getName(): String = "TimeoutIdlingResource"
                
                override fun isIdleNow(): Boolean {
                    val isTimeout = System.currentTimeMillis() - startTime >= timeoutMs
                    if (isTimeout) {
                        callback?.onTransitionToIdle()
                    }
                    return isTimeout
                }
                
                override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
                    this.callback = callback
                }
            }
        }
    }
}