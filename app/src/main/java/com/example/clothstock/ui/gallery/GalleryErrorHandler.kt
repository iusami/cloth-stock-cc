package com.example.clothstock.ui.gallery

import android.content.Context
import android.util.Log
import android.view.View
import com.example.clothstock.R
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference

/**
 * エラーハンドリング管理クラス
 * 
 * Strategy Patternを適用
 * エラー表示とユーザーフィードバックを一元管理
 */
class GalleryErrorHandler(
    context: Context,
    private val rootView: View,
    private val onRetry: () -> Unit
) {
    private val contextRef = WeakReference(context)
    
    companion object {
        private const val TAG = "GalleryErrorHandler"
    }
    
    /**
     * 基本的なエラーメッセージ表示
     */
    fun showBasicError(message: String) {
        try {
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("再試行") { onRetry() }
                    .setActionTextColor(context.getColor(android.R.color.holo_blue_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing basic error", e)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing basic error", e)
        }
    }
    
    /**
     * 検索エラー表示
     */
    fun showSearchError(message: String, error: Exception) {
        Log.e(TAG, "Search error: $message", error)
        
        try {
            val errorDetail = getErrorDetail(error)
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, "$message: $errorDetail", Snackbar.LENGTH_LONG)
                    .setAction("再試行") { onRetry() }
                    .setActionTextColor(context.getColor(android.R.color.holo_orange_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing search error", e)
            showBasicError(message)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing search error", e)
            showBasicError(message)
        }
    }
    
    /**
     * フィルターエラー表示
     */
    fun showFilterError(message: String, error: Exception) {
        Log.e(TAG, "Filter error: $message", error)
        
        try {
            val errorDetail = getErrorDetail(error)
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, "$message: $errorDetail", Snackbar.LENGTH_LONG)
                    .setAction("リセット") { onRetry() }
                    .setActionTextColor(context.getColor(android.R.color.holo_red_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing filter error", e)
            showBasicError(message)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing filter error", e)
            showBasicError(message)
        }
    }
    
    /**
     * バリデーションフィードバック表示
     */
    fun showValidationFeedback(message: String) {
        try {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing validation feedback", e)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing validation feedback", e)
        }
    }
    
    /**
     * エラー詳細の取得
     */
    private fun getErrorDetail(error: Exception): String {
        return when (error) {
            is kotlinx.coroutines.TimeoutCancellationException -> "タイムアウト"
            is IllegalStateException -> "状態エラー"
            is SecurityException -> "権限エラー"
            is UninitializedPropertyAccessException -> "初期化エラー"
            is android.content.res.Resources.NotFoundException -> "リソースエラー"
            else -> "処理エラー"
        }
    }
    
    /**
     * リソースクリーンアップ
     */
    fun cleanup() {
        contextRef.clear()
    }
}
