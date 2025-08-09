package com.example.clothstock.util

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import android.util.Log
import android.view.View

/**
 * IdlingResource管理ヘルパー
 * Thread.sleepの代替として、適切な待機処理を提供
 */
object IdlingResourceHelper {
    
    private const val TAG = "IdlingResourceHelper"
    private val registeredResources = mutableListOf<IdlingResource>()
    
    /**
     * RecyclerViewのデータ読み込み完了を待機
     * 
     * @param recyclerViewId RecyclerViewのリソースID
     * @param minItemCount 最小アイテム数（デフォルト1）
     * @param timeout タイムアウト時間（ミリ秒、デフォルト5000）
     */
    fun waitForRecyclerView(
        recyclerViewId: Int,
        minItemCount: Int = 1,
        timeout: Long = 5000
    ) {
        Log.d(TAG, "Waiting for RecyclerView with minItemCount=$minItemCount")
        
        try {
            // RecyclerViewの参照を取得
            val recyclerView = getRecyclerView(recyclerViewId)
            
            // IdlingResourceを作成・登録
            val idlingResource = RecyclerViewIdlingResource(recyclerView, minItemCount)
            registerIdlingResource(idlingResource)
            
            // タイムアウト処理（フォールバック）
            val startTime = System.currentTimeMillis()
            while (!idlingResource.isIdleNow() && 
                   (System.currentTimeMillis() - startTime) < timeout) {
                Thread.sleep(50) // 短時間の待機
            }
            
            Log.d(TAG, "RecyclerView wait completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error waiting for RecyclerView", e)
            // フォールバックとして短時間待機
            Thread.sleep(500)
        }
    }
    
    /**
     * RecyclerViewが空状態になることを待機
     * 
     * @param recyclerViewId RecyclerViewのリソースID
     * @param timeout タイムアウト時間（ミリ秒、デフォルト3000）
     */
    fun waitForEmptyRecyclerView(
        recyclerViewId: Int,
        timeout: Long = 3000
    ) {
        Log.d(TAG, "Waiting for empty RecyclerView")
        
        try {
            // RecyclerViewの参照を取得
            val recyclerView = getRecyclerView(recyclerViewId)
            
            // IdlingResourceを作成・登録
            val idlingResource = EmptyRecyclerViewIdlingResource(recyclerView)
            registerIdlingResource(idlingResource)
            
            // タイムアウト処理（フォールバック）
            val startTime = System.currentTimeMillis()
            while (!idlingResource.isIdleNow() && 
                   (System.currentTimeMillis() - startTime) < timeout) {
                Thread.sleep(50)
            }
            
            Log.d(TAG, "Empty RecyclerView wait completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error waiting for empty RecyclerView", e)
            // フォールバックとして短時間待機
            Thread.sleep(300)
        }
    }
    
    /**
     * データ読み込み・UI更新処理の汎用待機
     * 軽微な処理に対するフォールバック待機
     * 
     * @param duration 待機時間（ミリ秒、デフォルト500）
     */
    fun waitForUiUpdate(duration: Long = 500) {
        Log.d(TAG, "Waiting for UI update: ${duration}ms")
        Thread.sleep(duration)
    }
    
    /**
     * RecyclerViewの参照を安全に取得
     */
    private fun getRecyclerView(recyclerViewId: Int): RecyclerView {
        var recyclerView: RecyclerView? = null
        
        onView(withId(recyclerViewId)).check { view, _ ->
            if (view is RecyclerView) {
                recyclerView = view
            } else {
                throw AssertionError("View is not a RecyclerView: ${view.javaClass.simpleName}")
            }
        }
        
        return recyclerView ?: throw IllegalStateException("Failed to get RecyclerView")
    }
    
    /**
     * IdlingResourceを登録
     */
    private fun registerIdlingResource(resource: IdlingResource) {
        IdlingRegistry.getInstance().register(resource)
        registeredResources.add(resource)
        Log.d(TAG, "Registered IdlingResource: ${resource.name}")
    }
    
    /**
     * 全てのIdlingResourceの登録を解除
     * テスト終了時に呼び出す
     */
    fun unregisterAllIdlingResources() {
        registeredResources.forEach { resource ->
            try {
                IdlingRegistry.getInstance().unregister(resource)
                Log.d(TAG, "Unregistered IdlingResource: ${resource.name}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister IdlingResource: ${resource.name}", e)
            }
        }
        registeredResources.clear()
        Log.d(TAG, "All IdlingResources unregistered")
    }
}