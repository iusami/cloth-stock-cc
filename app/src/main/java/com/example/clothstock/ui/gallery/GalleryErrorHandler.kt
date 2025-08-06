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
 * パフォーマンス最適化と包括的ログ記録を実装
 */
class GalleryErrorHandler(
    context: Context,
    private val rootView: View,
    private val onRetry: () -> Unit
) {
    private val contextRef = WeakReference(context)
    
    // パフォーマンス最適化: エラー頻度制限
    private var lastErrorTime = 0L
    private var errorCount = 0
    private val errorThrottleMs = 1000L // 1秒間に1回まで
    private val maxErrorsPerMinute = 10
    
    // 包括的ログ記録
    private val errorMetrics = mutableMapOf<String, ErrorMetric>()
    
    companion object {
        private const val TAG = "GalleryErrorHandler"
    }
    
    /**
     * エラーメトリクス追跡用データクラス
     */
    private data class ErrorMetric(
        var count: Int = 0,
        var lastOccurrence: Long = 0L,
        var totalDuration: Long = 0L
    )
    
    /**
     * 基本的なエラーメッセージ表示（パフォーマンス最適化付き）
     */
    fun showBasicError(message: String) {
        if (!shouldShowError()) {
            Log.d(TAG, "Error throttled: $message")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("再試行") { onRetry() }
                    .setActionTextColor(context.getColor(android.R.color.holo_blue_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            // メトリクス記録
            recordErrorMetric("BASIC_ERROR", startTime)
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing basic error", e)
            recordErrorMetric("BASIC_ERROR_EXCEPTION", startTime)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing basic error", e)
            recordErrorMetric("BASIC_ERROR_EXCEPTION", startTime)
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
     * フィルター読み込みエラー表示
     */
    fun showFilterLoadingError(message: String, error: Exception) {
        Log.e(TAG, "Filter loading error: $message", error)
        
        try {
            val errorDetail = getErrorDetail(error)
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, "$message: $errorDetail", Snackbar.LENGTH_LONG)
                    .setAction("再試行") { onRetry() }
                    .setActionTextColor(context.getColor(android.R.color.holo_red_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing filter loading error", e)
            showBasicError(message)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing filter loading error", e)
            showBasicError(message)
        }
    }
    
    /**
     * 検索タイムアウトエラー表示
     */
    fun showSearchTimeoutError(message: String, error: Exception) {
        Log.e(TAG, "Search timeout error: $message", error)
        
        try {
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("再試行") { onRetry() }
                    .setActionTextColor(context.getColor(android.R.color.holo_orange_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing search timeout error", e)
            showBasicError(message)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing search timeout error", e)
            showBasicError(message)
        }
    }
    
    /**
     * 検索キャンセル処理
     */
    fun handleSearchCancellation(message: String) {
        Log.d(TAG, "Search cancelled: $message")
        
        try {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException handling search cancellation", e)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException handling search cancellation", e)
        }
    }
    
    /**
     * リトライダイアログ表示
     */
    fun showRetryDialog(message: String, error: Exception, retryCallback: () -> Unit) {
        Log.e(TAG, "Showing retry dialog: $message", error)
        
        try {
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, message, Snackbar.LENGTH_INDEFINITE)
                    .setAction("再試行") { retryCallback() }
                    .setActionTextColor(context.getColor(android.R.color.holo_blue_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing retry dialog", e)
            showBasicError(message)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing retry dialog", e)
            showBasicError(message)
        }
    }
    
    /**
     * グレースフルデグラデーション処理
     */
    fun handleGracefulDegradation(message: String, error: Exception) {
        Log.w(TAG, "Graceful degradation: $message", error)
        
        try {
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setActionTextColor(context.getColor(android.R.color.holo_orange_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException handling graceful degradation", e)
            showBasicError(message)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException handling graceful degradation", e)
            showBasicError(message)
        }
    }
    
    /**
     * バックオフ付きリトライ表示
     */
    fun showRetryWithBackoff(message: String, error: Exception, retryCount: Int, retryCallback: () -> Unit) {
        Log.e(TAG, "Showing retry with backoff (attempt $retryCount): $message", error)
        
        try {
            val backoffMessage = "$message (試行回数: $retryCount)"
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, backoffMessage, Snackbar.LENGTH_INDEFINITE)
                    .setAction("再試行") { retryCallback() }
                    .setActionTextColor(context.getColor(android.R.color.holo_red_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing retry with backoff", e)
            showBasicError(message)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing retry with backoff", e)
            showBasicError(message)
        }
    }
    
    /**
     * コンテキスト付きエラーログ記録
     */
    fun logErrorWithContext(error: Exception, context: Map<String, Any>) {
        val contextString = context.entries.joinToString(", ") { "${it.key}=${it.value}" }
        Log.e(TAG, "Error with context [$contextString]", error)
    }
    
    /**
     * エラーメトリクス追跡
     */
    fun trackErrorMetrics(errorType: String, errorCount: Int, errorDuration: Long) {
        Log.i(TAG, "Error metrics - Type: $errorType, Count: $errorCount, Duration: ${errorDuration}ms")
    }
    
    /**
     * エラー表示頻度制限チェック
     */
    private fun shouldShowError(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // 1秒以内の連続エラーを制限
        if (currentTime - lastErrorTime < errorThrottleMs) {
            return false
        }
        
        // 1分間のエラー回数制限
        if (errorCount >= maxErrorsPerMinute) {
            val oneMinuteAgo = currentTime - 60000L
            if (lastErrorTime > oneMinuteAgo) {
                return false
            } else {
                errorCount = 0 // リセット
            }
        }
        
        lastErrorTime = currentTime
        errorCount++
        return true
    }
    
    /**
     * エラーメトリクス記録
     */
    private fun recordErrorMetric(errorType: String, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        val metric = errorMetrics.getOrPut(errorType) { ErrorMetric() }
        
        metric.count++
        metric.lastOccurrence = System.currentTimeMillis()
        metric.totalDuration += duration
        
        // 定期的にメトリクスをログ出力
        if (metric.count % 5 == 0) {
            Log.i(TAG, "Error metrics - Type: $errorType, Count: ${metric.count}, " +
                    "Avg Duration: ${metric.totalDuration / metric.count}ms")
        }
    }
    
    /**
     * エラーメトリクス取得（テスト用）
     */
    fun getErrorMetrics(): Map<String, Int> {
        return errorMetrics.mapValues { it.value.count }
    }
    
    /**
     * リソースクリーンアップ
     */
    fun cleanup() {
        contextRef.clear()
        errorMetrics.clear()
        errorCount = 0
        lastErrorTime = 0L
    }
}
