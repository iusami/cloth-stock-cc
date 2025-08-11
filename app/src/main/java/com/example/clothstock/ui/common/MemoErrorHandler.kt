package com.example.clothstock.ui.common

import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.View
import com.example.clothstock.R
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference

/**
 * メモ関連エラーハンドリング管理クラス
 * 
 * Task 8: エラーハンドリングとバリデーション実装
 * 
 * Strategy Patternを適用
 * メモ保存、バリデーション、リトライ機能の統一エラー処理
 * Requirements 1.3, 1.4, 2.4への対応
 */
@Suppress("TooManyFunctions")
class MemoErrorHandler(
    context: Context,
    private val rootView: View,
    private val onRetry: (String) -> Unit // リトライ時にメモ内容を渡す
) {
    private val contextRef = WeakReference(context)
    
    // パフォーマンス最適化: エラー頻度制限
    private var lastErrorTime = 0L
    private var errorCount = 0
    private var currentRetryMemo: String = "" // リトライ用のメモ保存
    
    // 定数定義
    companion object {
        private const val TAG = "MemoErrorHandler"
        private const val ERROR_THROTTLE_MS = 1000L // 1秒間に1回まで
        private const val MAX_ERRORS_PER_MINUTE = 5
        private const val ONE_MINUTE_MS = 60_000L
        private const val MAGIC_NUMBER_THRESHOLD = 50
        private const val METRICS_LOG_INTERVAL = 2
        private const val RETRY_THRESHOLD = 3
        
        // バリデーション関連定数
        private const val MAX_MEMO_LENGTH = 1000
    }
    
    // エラーメトリクス追跡
    private val errorMetrics = mutableMapOf<String, ErrorMetric>()
    
    /**
     * エラーメトリクス追跡用データクラス
     */
    private data class ErrorMetric(
        var count: Int = 0,
        var lastOccurrence: Long = 0L,
        var totalDuration: Long = 0L
    )
    
    /**
     * Requirements 2.4: メモ保存失敗エラー表示
     * リトライ機能付きで元のメモ内容を保持
     */
    fun showMemoSaveError(message: String, originalMemo: String, error: Exception? = null) {
        if (!shouldShowError()) {
            Log.d(TAG, "Memo save error throttled: $message")
            return
        }
        
        val startTime = System.currentTimeMillis()
        currentRetryMemo = originalMemo
        
        try {
            error?.let { Log.e(TAG, "Memo save error: $message", it) }
            
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("再試行") { 
                        retryMemoSave(currentRetryMemo)
                    }
                    .setActionTextColor(getErrorColor(context))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            // メトリクス記録
            recordErrorMetric("MEMO_SAVE_ERROR", startTime)
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing memo save error", e)
            recordErrorMetric("MEMO_SAVE_ERROR_EXCEPTION", startTime)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing memo save error", e)
            recordErrorMetric("MEMO_SAVE_ERROR_EXCEPTION", startTime)
        }
    }
    
    /**
     * Requirements 1.3, 1.4: 文字数制限バリデーションエラー表示
     */
    fun showMemoValidationError(message: String, currentLength: Int) {
        if (!shouldShowError()) {
            Log.d(TAG, "Memo validation error throttled: $message")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            val detailedMessage = "$message (現在の文字数: $currentLength/$MAX_MEMO_LENGTH)"
            
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, detailedMessage, Snackbar.LENGTH_LONG)
                    .setActionTextColor(context.getColor(android.R.color.holo_orange_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            Log.w(TAG, "Memo validation error: $detailedMessage")
            recordErrorMetric("MEMO_VALIDATION_ERROR", startTime)
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing memo validation error", e)
            recordErrorMetric("MEMO_VALIDATION_ERROR_EXCEPTION", startTime)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing memo validation error", e)
            recordErrorMetric("MEMO_VALIDATION_ERROR_EXCEPTION", startTime)
        }
    }
    
    /**
     * メモ自動保存エラー表示（軽微なフィードバック用）
     */
    fun showMemoAutoSaveError(message: String, memo: String) {
        val startTime = System.currentTimeMillis()
        currentRetryMemo = memo
        
        try {
            Log.w(TAG, "Memo auto-save error: $message")
            
            // 自動保存エラーは控えめに表示
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, "メモの自動保存に失敗しました", Snackbar.LENGTH_SHORT)
                    .setAction("手動保存") { 
                        retryMemoSave(currentRetryMemo)
                    }
                    .setActionTextColor(context.getColor(android.R.color.holo_blue_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            recordErrorMetric("MEMO_AUTO_SAVE_ERROR", startTime)
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing memo auto-save error", e)
            recordErrorMetric("MEMO_AUTO_SAVE_ERROR_EXCEPTION", startTime)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing memo auto-save error", e)
            recordErrorMetric("MEMO_AUTO_SAVE_ERROR_EXCEPTION", startTime)
        }
    }
    
    /**
     * メモ読み込みエラー表示
     */
    fun showMemoLoadError(message: String, error: Exception? = null) {
        if (!shouldShowError()) {
            Log.d(TAG, "Memo load error throttled: $message")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            error?.let { Log.e(TAG, "Memo load error: $message", it) }
            
            val errorDetail = error?.let { getErrorDetail(it) } ?: "不明なエラー"
            val detailedMessage = "$message: $errorDetail"
            
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, detailedMessage, Snackbar.LENGTH_LONG)
                    .setAction("再試行") { 
                        onRetry("")  // 読み込み再試行（空文字列）
                    }
                    .setActionTextColor(context.getColor(android.R.color.holo_orange_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            recordErrorMetric("MEMO_LOAD_ERROR", startTime)
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing memo load error", e)
            recordErrorMetric("MEMO_LOAD_ERROR_EXCEPTION", startTime)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing memo load error", e)
            recordErrorMetric("MEMO_LOAD_ERROR_EXCEPTION", startTime)
        }
    }
    
    /**
     * リトライ機能付きメモ保存エラー表示（回数表示付き）
     */
    fun showMemoSaveErrorWithRetry(message: String, memo: String, retryCount: Int, error: Exception? = null) {
        val startTime = System.currentTimeMillis()
        currentRetryMemo = memo
        
        try {
            error?.let { Log.e(TAG, "Memo save error (attempt $retryCount): $message", it) }
            
            val retryMessage = if (retryCount > 1) {
                "$message (試行回数: $retryCount)"
            } else {
                message
            }
            
            contextRef.get()?.let { context ->
                val actionColor = if (retryCount >= RETRY_THRESHOLD) {
                    getErrorColor(context)  // 3回以上失敗した場合は赤色
                } else {
                    getWarningColor(context)
                }
                
                Snackbar.make(rootView, retryMessage, Snackbar.LENGTH_INDEFINITE)
                    .setAction("再試行") { 
                        retryMemoSave(currentRetryMemo)
                    }
                    .setActionTextColor(actionColor)
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            recordErrorMetric("MEMO_SAVE_ERROR_WITH_RETRY", startTime)
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing memo save error with retry", e)
            recordErrorMetric("MEMO_SAVE_ERROR_WITH_RETRY_EXCEPTION", startTime)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing memo save error with retry", e)
            recordErrorMetric("MEMO_SAVE_ERROR_WITH_RETRY_EXCEPTION", startTime)
        }
    }
    
    /**
     * メモバリデーション成功フィードバック
     */
    fun showMemoValidationSuccess(message: String) {
        try {
            contextRef.get()?.let { context ->
                Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(context.getColor(android.R.color.holo_green_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            Log.d(TAG, "Memo validation success: $message")
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing memo validation success", e)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing memo validation success", e)
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
            is java.io.IOException -> "データ保存エラー"
            is android.database.sqlite.SQLiteException -> "データベースエラー"
            else -> "処理エラー"
        }
    }
    
    /**
     * メモ保存のリトライ実行
     */
    private fun retryMemoSave(memo: String) {
        try {
            onRetry(memo)
            Log.d(TAG, "Retrying memo save with content: ${memo.take(MAGIC_NUMBER_THRESHOLD)}...")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException during retry memo save", e)
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException during retry memo save", e)
        }
    }
    
    /**
     * エラー表示頻度制限チェック
     */
    private fun shouldShowError(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // 短時間での連続エラーとエラー回数上限をチェック
        val shouldLimit = isWithinThrottleTime(currentTime) || isErrorCountExceeded(currentTime)
        
        if (!shouldLimit) {
            updateErrorTracking(currentTime)
        }
        
        return !shouldLimit
    }
    
    private fun isWithinThrottleTime(currentTime: Long): Boolean {
        return currentTime - lastErrorTime < ERROR_THROTTLE_MS
    }
    
    private fun isErrorCountExceeded(currentTime: Long): Boolean {
        if (errorCount < MAX_ERRORS_PER_MINUTE) return false
        
        val oneMinuteAgo = currentTime - ONE_MINUTE_MS
        val shouldReset = lastErrorTime <= oneMinuteAgo
        
        if (shouldReset) {
            errorCount = 0 // リセット
        }
        
        return !shouldReset
    }
    
    private fun updateErrorTracking(currentTime: Long) {
        lastErrorTime = currentTime
        errorCount++
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
        if (metric.count % (MAX_ERRORS_PER_MINUTE / METRICS_LOG_INTERVAL) == 0) {
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
        currentRetryMemo = ""
    }
    
    /**
     * Material Designのエラーカラーを取得
     */
    private fun getErrorColor(context: Context): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(com.google.android.material.R.attr.colorError, typedValue, true)) {
            typedValue.data
        } else {
            // フォールバック: 赤系の色
            context.getColor(android.R.color.holo_red_dark)
        }
    }
    
    /**
     * Material Designの警告カラーを取得
     */
    private fun getWarningColor(context: Context): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)) {
            typedValue.data
        } else {
            // フォールバック: オレンジ系の色
            context.getColor(android.R.color.holo_orange_dark)
        }
    }
}
