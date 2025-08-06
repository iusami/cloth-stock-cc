package com.example.clothstock.ui.gallery

import android.content.Context
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DefaultItemAnimator
import com.example.clothstock.databinding.FragmentGalleryBinding
import java.lang.ref.WeakReference

/**
 * ギャラリーアニメーション管理クラス
 * 
 * パフォーマンス最適化とメモリリーク防止を考慮
 * アニメーション処理を一元管理
 */
class GalleryAnimationManager(
    context: Context,
    private val binding: FragmentGalleryBinding
) {
    private val contextRef = WeakReference(context)
    
    companion object {
        private const val TAG = "GalleryAnimationManager"
        private const val FILTER_ANIMATION_DURATION = 250L
        private const val FILTER_TRANSITION_DURATION = 200L
        private const val FILTER_LOADING_ANIMATION_DURATION = 150L
    }
    
    /**
     * RecyclerViewアニメーターの設定
     */
    fun setupRecyclerViewAnimator() {
        binding.recyclerViewGallery.itemAnimator = DefaultItemAnimator().apply {
            addDuration = FILTER_ANIMATION_DURATION
            removeDuration = FILTER_TRANSITION_DURATION
            moveDuration = FILTER_TRANSITION_DURATION
            changeDuration = FILTER_ANIMATION_DURATION
            supportsChangeAnimations = true
        }
    }
    
    /**
     * フィルター結果の状態変更アニメーション
     */
    fun animateFilteredStateChange(isEmpty: Boolean) {
        Log.d(TAG, "animateFilteredStateChange: isEmpty = $isEmpty")
        
        contextRef.get()?.let { context ->
            try {
                val fadeOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
                val fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
                
                fadeOut.duration = FILTER_TRANSITION_DURATION
                fadeIn.duration = FILTER_TRANSITION_DURATION
                
                if (isEmpty) {
                    animateToEmptyState(fadeOut, fadeIn)
                } else {
                    animateToDataState(fadeOut, fadeIn)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error animating filtered state change", e)
                // フォールバック: 即座に状態変更
                setStateWithoutAnimation(isEmpty)
            }
        }
    }
    
    /**
     * 空状態へのアニメーション
     */
    private fun animateToEmptyState(fadeOut: android.view.animation.Animation, fadeIn: android.view.animation.Animation) {
        binding.recyclerViewGallery.startAnimation(fadeOut)
        binding.recyclerViewGallery.visibility = View.GONE
        
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.layoutEmptyState.startAnimation(fadeIn)
    }
    
    /**
     * データ表示状態へのアニメーション
     */
    private fun animateToDataState(fadeOut: android.view.animation.Animation, fadeIn: android.view.animation.Animation) {
        binding.layoutEmptyState.startAnimation(fadeOut)
        binding.layoutEmptyState.visibility = View.GONE
        
        binding.recyclerViewGallery.visibility = View.VISIBLE
        binding.recyclerViewGallery.startAnimation(fadeIn)
    }
    
    /**
     * アニメーションなしの状態変更（フォールバック）
     */
    private fun setStateWithoutAnimation(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewGallery.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerViewGallery.visibility = View.VISIBLE
        }
    }
    
    /**
     * フィルター操作時のローディングアニメーション
     */
    fun animateFilterLoadingState(shouldShow: Boolean) {
        Log.d(TAG, "animateFilterLoadingState: shouldShow = $shouldShow")
        
        contextRef.get()?.let { context ->
            try {
                if (shouldShow) {
                    showLoadingAnimation(context)
                } else {
                    hideLoadingAnimation(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error animating filter loading state", e)
                // フォールバック: 即座に表示/非表示
                binding.layoutLoading.visibility = if (shouldShow) View.VISIBLE else View.GONE
            }
        }
    }
    
    /**
     * ローディング表示アニメーション
     */
    private fun showLoadingAnimation(context: Context) {
        if (binding.layoutLoading.visibility != View.VISIBLE) {
            binding.layoutLoading.visibility = View.VISIBLE
            val fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
            fadeIn.duration = FILTER_LOADING_ANIMATION_DURATION
            binding.layoutLoading.startAnimation(fadeIn)
        }
    }
    
    /**
     * ローディング非表示アニメーション
     */
    private fun hideLoadingAnimation(context: Context) {
        if (binding.layoutLoading.visibility == View.VISIBLE) {
            val fadeOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
            fadeOut.duration = FILTER_LOADING_ANIMATION_DURATION
            fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    binding.layoutLoading.visibility = View.GONE
                }
            })
            binding.layoutLoading.startAnimation(fadeOut)
        }
    }
    
    /**
     * RecyclerView表示アニメーション
     */
    fun animateRecyclerViewEntry() {
        contextRef.get()?.let { context ->
            try {
                binding.recyclerViewGallery.visibility = View.VISIBLE
                val slideIn = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
                slideIn.duration = FILTER_ANIMATION_DURATION
                binding.recyclerViewGallery.startAnimation(slideIn)
            } catch (e: Exception) {
                Log.e(TAG, "Error animating RecyclerView entry", e)
                binding.recyclerViewGallery.visibility = View.VISIBLE
            }
        }
    }
    
    /**
     * リソースクリーンアップ
     */
    fun cleanup() {
        contextRef.clear()
    }
}