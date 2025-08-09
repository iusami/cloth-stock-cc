package com.example.clothstock.util

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.IdlingResource
import android.util.Log

/**
 * RecyclerView用IdlingResource
 * RecyclerViewのデータ読み込み完了を適切に待機するためのリソース
 * 
 * Thread.sleepの代替として使用し、テストの安定性を向上させる
 */
class RecyclerViewIdlingResource(
    private val recyclerView: RecyclerView,
    private val minItemCount: Int = 1
) : IdlingResource {
    
    companion object {
        private const val TAG = "RecyclerViewIdling"
    }
    
    private var resourceCallback: IdlingResource.ResourceCallback? = null
    private var isIdle = false
    
    override fun getName(): String = "RecyclerViewIdlingResource"
    
    override fun isIdleNow(): Boolean {
        if (isIdle) {
            return true
        }
        
        val adapter = recyclerView.adapter
        val currentItemCount = adapter?.itemCount ?: 0
        
        Log.d(TAG, "Checking idle state: currentItemCount=$currentItemCount, minItemCount=$minItemCount")
        
        // アダプターが設定されており、最小アイテム数以上ある場合
        val hasEnoughItems = adapter != null && currentItemCount >= minItemCount
        
        // レイアウトが完了している場合
        val isLayoutComplete = !recyclerView.isLayoutRequested && !recyclerView.isComputingLayout
        
        isIdle = hasEnoughItems && isLayoutComplete
        
        Log.d(TAG, "Idle state: hasEnoughItems=$hasEnoughItems, isLayoutComplete=$isLayoutComplete, isIdle=$isIdle")
        
        if (isIdle) {
            resourceCallback?.onTransitionToIdle()
        }
        
        return isIdle
    }
    
    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.resourceCallback = callback
        Log.d(TAG, "Registered idle transition callback")
    }
}

/**
 * 空状態チェック用IdlingResource
 * RecyclerViewが空（アイテム数0）であることを待機
 */
class EmptyRecyclerViewIdlingResource(
    private val recyclerView: RecyclerView
) : IdlingResource {
    
    companion object {
        private const val TAG = "EmptyRecyclerIdling"
    }
    
    private var resourceCallback: IdlingResource.ResourceCallback? = null
    private var isIdle = false
    
    override fun getName(): String = "EmptyRecyclerViewIdlingResource"
    
    override fun isIdleNow(): Boolean {
        if (isIdle) {
            return true
        }
        
        val adapter = recyclerView.adapter
        val currentItemCount = adapter?.itemCount ?: 0
        
        // アダプターが設定されており、アイテム数が0の場合
        val isEmpty = adapter != null && currentItemCount == 0
        
        // レイアウトが完了している場合
        val isLayoutComplete = !recyclerView.isLayoutRequested && !recyclerView.isComputingLayout
        
        isIdle = isEmpty && isLayoutComplete
        
        Log.d(TAG, "Empty idle state: isEmpty=$isEmpty, isLayoutComplete=$isLayoutComplete, isIdle=$isIdle")
        
        if (isIdle) {
            resourceCallback?.onTransitionToIdle()
        }
        
        return isIdle
    }
    
    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.resourceCallback = callback
        Log.d(TAG, "Registered empty idle transition callback")
    }
}