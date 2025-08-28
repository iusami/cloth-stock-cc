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
    
    // 定数定義
    companion object {
        private const val TAG = "GalleryErrorHandler"
        private const val ERROR_THROTTLE_MS = 1000L // 1秒間に1回まで
        private const val MAX_ERRORS_PER_MINUTE = 10
        private const val TIMEOUT_DURATION = 5000L
        private const val ONE_MINUTE_IN_TIMEOUT_UNITS = 12L
        
        // REFACTOR Phase 2: マジックナンバー定数化
        private const val MAX_FAILED_ITEMS_DISPLAY = 5
        private const val MAX_FAILED_ITEMS_SHORT_DISPLAY = 3
        private const val MAX_ERROR_REASONS_DISPLAY = 2
        private const val BASE_BACKOFF_DELAY = 1000L
        private const val MAX_BACKOFF_DELAY = 16000L
        private const val SHIFT_ONE = 1
    }
    
    private val errorThrottleMs = ERROR_THROTTLE_MS
    private val maxErrorsPerMinute = MAX_ERRORS_PER_MINUTE
    
    // 包括的ログ記録
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
        
        // 短時間での連続エラーとエラー回数上限をチェック
        val shouldLimit = isWithinThrottleTime(currentTime) || isErrorCountExceeded(currentTime)
        
        if (!shouldLimit) {
            updateErrorTracking(currentTime)
        }
        
        return !shouldLimit
    }
    
    private fun isWithinThrottleTime(currentTime: Long): Boolean {
        return currentTime - lastErrorTime < errorThrottleMs
    }
    
    private fun isErrorCountExceeded(currentTime: Long): Boolean {
        if (errorCount < maxErrorsPerMinute) return false
        
        val oneMinuteAgo = currentTime - TIMEOUT_DURATION * ONE_MINUTE_IN_TIMEOUT_UNITS
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
        if (metric.count % MAX_ERRORS_PER_MINUTE / 2 == 0) {
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
     * Task 9: 削除エラータイプ別のエラー表示 (リファクタリング済み)
     */
    fun showDeletionError(
        errorType: DeletionErrorType, 
        itemCount: Int, 
        retryCallback: () -> Unit,
        settingsCallback: (() -> Unit)? = null
    ) {
        if (!shouldShowError()) {
            Log.d(TAG, "Deletion error throttled: $errorType for $itemCount items")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            contextRef.get()?.let { context ->
                val errorHandlingStrategy = getErrorHandlingStrategy(errorType)
                val message = getDeletionErrorMessage(context, errorType, itemCount)
                val guidanceMessage = getErrorGuidanceMessage(context, errorType)
                val fullMessage = if (guidanceMessage.isNotEmpty()) {
                    "$message\n$guidanceMessage"
                } else message
                
                val snackbar = Snackbar.make(rootView, fullMessage, errorHandlingStrategy.duration)
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                
                // リトライ戦略に基づいたアクション設定
                when (errorHandlingStrategy.actionType) {
                    ErrorActionType.RETRY -> {
                        snackbar.setAction(context.getString(R.string.retry)) { retryCallback() }
                    }
                    ErrorActionType.SETTINGS -> {
                        snackbar.setAction(context.getString(R.string.settings)) { 
                            settingsCallback?.invoke() ?: retryCallback() 
                        }
                    }
                    ErrorActionType.CONTACT_SUPPORT -> {
                        snackbar.setAction(context.getString(R.string.contact_support)) { 
                            // サポート連絡処理
                        }
                    }
                    ErrorActionType.NO_ACTION -> {
                        // アクションボタンなし
                    }
                }
                
                snackbar.setActionTextColor(context.getColor(errorHandlingStrategy.actionColor))
                    .show()
            }
            
            // メトリクス記録
            recordErrorMetric(errorType.getMetricsKey(), startTime)
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing deletion error", e)
            recordErrorMetric("DELETION_ERROR_EXCEPTION", startTime)
            // グレースフルデグラデーション: 基本エラーにフォールバック
            showBasicError("アイテムの削除に失敗しました")
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing deletion error", e)
            recordErrorMetric("DELETION_ERROR_EXCEPTION", startTime)
            showBasicError("アイテムの削除に失敗しました")
        }
    }
    
    /**
     * Task 9: ファイル権限エラー表示
     */
    fun showFilePermissionError(filePath: String, retryCallback: () -> Unit) {
        if (!shouldShowError()) {
            Log.d(TAG, "File permission error throttled for: $filePath")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            contextRef.get()?.let { context ->
                val message = context.getString(R.string.file_permission_error_with_path, filePath)
                val guidanceMessage = context.getString(R.string.file_permission_error_guidance)
                val fullMessage = "$message\n$guidanceMessage"
                
                Snackbar.make(rootView, fullMessage, Snackbar.LENGTH_LONG)
                    .setAction(context.getString(R.string.settings)) { retryCallback() }
                    .setActionTextColor(context.getColor(android.R.color.holo_red_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            recordErrorMetric("FILE_PERMISSION_ERROR", startTime)
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing file permission error", e)
            showBasicError("\u30d5\u30a1\u30a4\u30eb\u306e\u524a\u9664\u6a29\u9650\u304c\u3042\u308a\u307e\u305b\u3093")
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing file permission error", e)
            showBasicError("\u30d5\u30a1\u30a4\u30eb\u306e\u524a\u9664\u6a29\u9650\u304c\u3042\u308a\u307e\u305b\u3093")
        }
    }
    
    /**
     * Task 9: データベーストランザクションエラー表示
     */
    fun showDatabaseTransactionError(retryCallback: () -> Unit) {
        if (!shouldShowError()) {
            Log.d(TAG, "Database transaction error throttled")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            contextRef.get()?.let { context ->
                val message = context.getString(R.string.database_transaction_error_message)
                val guidanceMessage = context.getString(R.string.database_transaction_error_guidance)
                val fullMessage = "$message\n$guidanceMessage"
                
                Snackbar.make(rootView, fullMessage, Snackbar.LENGTH_LONG)
                    .setAction(context.getString(R.string.retry)) { retryCallback() }
                    .setActionTextColor(context.getColor(android.R.color.holo_orange_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            recordErrorMetric("DATABASE_TRANSACTION_ERROR", startTime)
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing database transaction error", e)
            showBasicError("\u30c7\u30fc\u30bf\u30d9\u30fc\u30b9\u306e\u524a\u9664\u51e6\u7406\u306b\u5931\u6557\u3057\u307e\u3057\u305f")
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing database transaction error", e)
            showBasicError("\u30c7\u30fc\u30bf\u30d9\u30fc\u30b9\u306e\u524a\u9664\u51e6\u7406\u306b\u5931\u6557\u3057\u307e\u3057\u305f")
        }
    }
    
    /**
     * Task 9: 部分削除結果表示
     */
    fun showPartialDeletionResult(successCount: Int, failedCount: Int, failedItems: List<String>) {
        if (!shouldShowError()) {
            Log.d(TAG, "Partial deletion result throttled: $successCount success, $failedCount failed")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            contextRef.get()?.let { context ->
                val message = context.resources.getQuantityString(
                    R.plurals.partial_deletion_success_message, 
                    successCount, 
                    successCount, 
                    failedCount
                )
                
                val failedItemsText = if (failedItems.isNotEmpty()) {
                    val failedItemsList = failedItems.take(3).joinToString(", ")
                    val additionalCount = if (failedItems.size > 3) " (+${failedItems.size - 3})" else ""
                    "\n" + context.getString(R.string.partial_deletion_failed_items, failedItemsList + additionalCount)
                } else ""
                
                val fullMessage = message + failedItemsText
                
                Snackbar.make(rootView, fullMessage, Snackbar.LENGTH_LONG)
                    .setAction(context.getString(R.string.view_details)) { /* 詳細表示処理 */ }
                    .setActionTextColor(context.getColor(android.R.color.holo_blue_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            recordErrorMetric("PARTIAL_DELETION_RESULT", startTime)
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing partial deletion result", e)
            showBasicError("\u4e00\u90e8\u306e\u30a2\u30a4\u30c6\u30e0\u306e\u307f\u524a\u9664\u3055\u308c\u307e\u3057\u305f")
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing partial deletion result", e)
            showBasicError("\u4e00\u90e8\u306e\u30a2\u30a4\u30c6\u30e0\u306e\u307f\u524a\u9664\u3055\u308c\u307e\u3057\u305f")
        }
    }
    
    /**
     * Task 9: アクセシビリティ準拠削除エラー表示
     */
    fun showAccessibilityCompliantDeletionError(errorType: DeletionErrorType, itemCount: Int) {
        try {
            contextRef.get()?.let { context ->
                val message = getDeletionErrorMessage(context, errorType, itemCount)
                val accessibilityMessage = context.getString(R.string.accessibility_deletion_error_announcement)
                
                // アクセシビリティサービス用のアナウンスメントを行う
                rootView.announceForAccessibility(accessibilityMessage)
                
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing accessibility compliant deletion error", e)
            showBasicError("\u30a2\u30a4\u30c6\u30e0\u306e\u524a\u9664\u306b\u5931\u6557\u3057\u307e\u3057\u305f")
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException showing accessibility compliant deletion error", e)
            showBasicError("\u30a2\u30a4\u30c6\u30e0\u306e\u524a\u9664\u306b\u5931\u6557\u3057\u307e\u3057\u305f")
        }
    }
    
    /**
     * REFACTOR Phase 1: エラーハンドリング戦略データクラス
     */
    private data class ErrorHandlingStrategy(
        val actionType: ErrorActionType,
        val actionColor: Int,
        val duration: Int,
        val requiresGuidance: Boolean = true
    )
    
    private enum class ErrorActionType {
        RETRY,
        SETTINGS,
        CONTACT_SUPPORT,
        NO_ACTION
    }
    
    /**
     * REFACTOR Phase 1: エラータイプ別のハンドリング戦略取得
     */
    private fun getErrorHandlingStrategy(errorType: DeletionErrorType): ErrorHandlingStrategy {
        return when (errorType) {
            DeletionErrorType.FILE_PERMISSION_DENIED -> ErrorHandlingStrategy(
                actionType = ErrorActionType.SETTINGS,
                actionColor = android.R.color.holo_red_light,
                duration = Snackbar.LENGTH_INDEFINITE,
                requiresGuidance = true
            )
            DeletionErrorType.DATABASE_TRANSACTION_FAILED -> ErrorHandlingStrategy(
                actionType = ErrorActionType.RETRY,
                actionColor = android.R.color.holo_orange_light,
                duration = Snackbar.LENGTH_LONG,
                requiresGuidance = true
            )
            DeletionErrorType.NETWORK_STORAGE_UNAVAILABLE -> ErrorHandlingStrategy(
                actionType = ErrorActionType.RETRY,
                actionColor = android.R.color.holo_blue_light,
                duration = Snackbar.LENGTH_LONG,
                requiresGuidance = true
            )
            DeletionErrorType.PARTIAL_DELETION_OCCURRED -> ErrorHandlingStrategy(
                actionType = ErrorActionType.RETRY,
                actionColor = android.R.color.holo_blue_light,
                duration = Snackbar.LENGTH_LONG,
                requiresGuidance = false
            )
            DeletionErrorType.UNKNOWN_DELETION_ERROR -> ErrorHandlingStrategy(
                actionType = ErrorActionType.CONTACT_SUPPORT,
                actionColor = android.R.color.holo_red_light,
                duration = Snackbar.LENGTH_LONG,
                requiresGuidance = true
            )
        }
    }
    
    /**
     * REFACTOR Phase 1: エラーガイダンスメッセージ取得
     */
    private fun getErrorGuidanceMessage(context: Context, errorType: DeletionErrorType): String {
        return when (errorType) {
            DeletionErrorType.FILE_PERMISSION_DENIED -> {
                context.getString(R.string.file_permission_error_guidance)
            }
            DeletionErrorType.DATABASE_TRANSACTION_FAILED -> {
                context.getString(R.string.database_transaction_error_guidance)
            }
            DeletionErrorType.NETWORK_STORAGE_UNAVAILABLE -> {
                context.getString(R.string.network_storage_error_guidance)
            }
            DeletionErrorType.PARTIAL_DELETION_OCCURRED -> {
                context.getString(R.string.partial_deletion_guidance)
            }
            DeletionErrorType.UNKNOWN_DELETION_ERROR -> {
                "アプリを再起動してから再度お試しください。問題が続く場合はサポートにお問い合わせください。"
            }
        }
    }
    
    /**
     * Task 9: 削除エラーメッセージ取得ヘルパー
     */
    private fun getDeletionErrorMessage(context: Context, errorType: DeletionErrorType, itemCount: Int): String {
        return when (errorType) {
            DeletionErrorType.FILE_PERMISSION_DENIED -> {
                context.getString(R.string.file_permission_error_message)
            }
            DeletionErrorType.DATABASE_TRANSACTION_FAILED -> {
                context.getString(R.string.database_transaction_error_message)
            }
            DeletionErrorType.NETWORK_STORAGE_UNAVAILABLE -> {
                context.getString(R.string.network_storage_error_message)
            }
            DeletionErrorType.PARTIAL_DELETION_OCCURRED -> {
                context.getString(R.string.partial_deletion_success_title)
            }
            DeletionErrorType.UNKNOWN_DELETION_ERROR -> {
                context.resources.getQuantityString(R.plurals.deletion_error_message, itemCount, itemCount)
            }
        }
    }
    
    /**
     * REFACTOR Phase 1: 指数バックオフ付きリトライ機能
     */
    fun showDeletionErrorWithBackoff(
        errorType: DeletionErrorType,
        itemCount: Int,
        attemptCount: Int,
        maxAttempts: Int,
        retryCallback: () -> Unit
    ) {
        if (!shouldShowError()) {
            Log.d(TAG, "Deletion error with backoff throttled: $errorType, attempt $attemptCount")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            contextRef.get()?.let { context ->
                val baseMessage = getDeletionErrorMessage(context, errorType, itemCount)
                val backoffMessage = if (attemptCount > 1) {
                    val waitTime = calculateBackoffDelay(attemptCount)
                    "\n再試行 $attemptCount/$maxAttempts (次の試行まで ${waitTime}ms 待機)"
                } else ""
                
                val guidanceMessage = getErrorGuidanceMessage(context, errorType)
                val fullMessage = baseMessage + backoffMessage + if (guidanceMessage.isNotEmpty()) "\n$guidanceMessage" else ""
                
                val isLastAttempt = attemptCount >= maxAttempts
                val actionText = if (isLastAttempt) {
                    context.getString(R.string.contact_support)
                } else {
                    context.getString(R.string.retry)
                }
                
                val actionColor = if (isLastAttempt) {
                    android.R.color.holo_red_light
                } else {
                    android.R.color.holo_orange_light
                }
                
                Snackbar.make(rootView, fullMessage, Snackbar.LENGTH_INDEFINITE)
                    .setAction(actionText) { retryCallback() }
                    .setActionTextColor(context.getColor(actionColor))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            recordErrorMetric("${errorType.getMetricsKey()}_BACKOFF_ATTEMPT_$attemptCount", startTime)
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception showing deletion error with backoff", e)
            showBasicError("削除エラーが発生しました")
        }
    }
    
    /**
     * REFACTOR Phase 2: 指数バックオフ遅延計算（マジックナンバー修正）
     */
    private fun calculateBackoffDelay(attemptCount: Int): Long {
        // 指数バックオフ: 1s, 2s, 4s, 8s, 16s (max)
        val calculatedDelay = BASE_BACKOFF_DELAY * (SHIFT_ONE shl attemptCount - SHIFT_ONE)
        return minOf(calculatedDelay, MAX_BACKOFF_DELAY)
    }
    
    /**
     * REFACTOR Phase 1: コンテキスト情報付きエラーログ記録拡張
     */
    fun logDeletionErrorWithContext(
        error: Exception, 
        errorType: DeletionErrorType,
        itemCount: Int,
        context: Map<String, Any>
    ) {
        val enhancedContext = context.toMutableMap().apply {
            put("error_type", errorType.name)
            put("item_count", itemCount)
            put("is_retryable", errorType.isRetryable())
            put("metrics_key", errorType.getMetricsKey())
            put("timestamp", System.currentTimeMillis())
        }
        
        val contextString = enhancedContext.entries.joinToString(", ") { "${it.key}=${it.value}" }
        Log.e(TAG, "Deletion error with enhanced context [$contextString]", error)
        
        // メトリクス記録
        recordErrorMetric("DELETION_ERROR_WITH_CONTEXT_${errorType.getMetricsKey()}", System.currentTimeMillis())
    }
    
    /**
     * GREEN Phase 2: 拡張部分削除結果表示
     */
    fun showEnhancedPartialDeletionResult(
        successCount: Int,
        failedCount: Int,
        failedItems: List<String>,
        @Suppress("UNUSED_PARAMETER") recoveryOptions: List<String>
    ) {
        if (!shouldShowError()) {
            Log.d(TAG, "Enhanced partial deletion result throttled: $successCount success, $failedCount failed")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            contextRef.get()?.let { context ->
                val message = context.resources.getQuantityString(
                    R.plurals.partial_deletion_success_message,
                    successCount,
                    successCount,
                    failedCount
                )
                
                val failedItemsText = if (failedItems.isNotEmpty()) {
                    val displayItems = failedItems.take(MAX_FAILED_ITEMS_DISPLAY)
                    val remainingCount = if (failedItems.size > MAX_FAILED_ITEMS_DISPLAY) {
                        failedItems.size - MAX_FAILED_ITEMS_DISPLAY
                    } else 0
                    val failedItemsList = displayItems.joinToString(", ")
                    val additionalText = if (remainingCount > 0) " (+$remainingCount 他)" else ""
                    "\n" + context.getString(R.string.partial_deletion_failed_items, failedItemsList + additionalText)
                } else ""
                
                val guidanceText = "\n" + context.getString(R.string.partial_deletion_guidance)
                val fullMessage = message + failedItemsText + guidanceText
                
                Snackbar.make(rootView, fullMessage, Snackbar.LENGTH_INDEFINITE)
                    .setAction(context.getString(R.string.view_details)) { 
                        // 詳細表示処理は呼び出し元で実装する
                    }
                    .setActionTextColor(context.getColor(android.R.color.holo_blue_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            recordErrorMetric("ENHANCED_PARTIAL_DELETION_RESULT", startTime)
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception showing enhanced partial deletion result", e)
            showBasicError("一部のアイテムのみ削除されました")
        }
    }
    
    /**
     * GREEN Phase 2: 削除復旧オプション表示
     */
    fun showDeletionRecoveryOptions(
        failedItems: List<String>,
        errorReasons: List<String>,
        recoveryCallback: (String, String) -> Unit
    ) {
        if (!shouldShowError()) {
            Log.d(TAG, "Deletion recovery options throttled for ${failedItems.size} items")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            contextRef.get()?.let { context ->
                val title = "削除失敗アイテムの復旧"
                val itemsText = failedItems.take(MAX_FAILED_ITEMS_SHORT_DISPLAY).joinToString(", ")
                val remainingCount = if (failedItems.size > MAX_FAILED_ITEMS_SHORT_DISPLAY) {
                    " (+${failedItems.size - MAX_FAILED_ITEMS_SHORT_DISPLAY} 他)"
                } else ""
                val message = "$title\n失敗: $itemsText$remainingCount"
                
                val reasonsText = if (errorReasons.isNotEmpty()) {
                    "\n原因: ${errorReasons.take(MAX_ERROR_REASONS_DISPLAY).joinToString(", ")}"
                } else ""
                
                val fullMessage = message + reasonsText
                
                Snackbar.make(rootView, fullMessage, Snackbar.LENGTH_INDEFINITE)
                    .setAction(context.getString(R.string.retry)) {
                        // 最初の失敗アイテムと理由でコールバック
                        val firstItem = failedItems.firstOrNull() ?: ""
                        val firstReason = errorReasons.firstOrNull() ?: "不明なエラー"
                        recoveryCallback(firstItem, firstReason)
                    }
                    .setActionTextColor(context.getColor(android.R.color.holo_orange_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            recordErrorMetric("DELETION_RECOVERY_OPTIONS", startTime)
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception showing deletion recovery options", e)
            showBasicError("削除失敗アイテムの復旧オプションを表示できません")
        }
    }
    
    /**
     * GREEN Phase 2: 削除機能低下時のグレースフルデグラデーション
     */
    fun showGracefulDegradationForDeletion(
        degradationReason: String,
        alternativeActions: List<String>
    ) {
        if (!shouldShowError()) {
            Log.d(TAG, "Graceful degradation for deletion throttled: $degradationReason")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            contextRef.get()?.let { context ->
                val title = "削除機能が一時的に利用できません"
                val reasonText = "理由: $degradationReason"
                val alternativesText = if (alternativeActions.isNotEmpty()) {
                    "\n代替手段: ${alternativeActions.joinToString(", ")}"
                } else ""
                
                val fullMessage = "$title\n$reasonText$alternativesText"
                
                Snackbar.make(rootView, fullMessage, Snackbar.LENGTH_LONG)
                    .setAction(context.getString(R.string.contact_support)) {
                        // サポート連絡処理
                    }
                    .setActionTextColor(context.getColor(android.R.color.holo_red_light))
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            recordErrorMetric("GRACEFUL_DEGRADATION_DELETION", startTime)
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception showing graceful degradation for deletion", e)
            showBasicError("削除機能が一時的に利用できません")
        }
    }
    
    /**
     * GREEN Phase 2: 削除進行状況とエラーの同時表示
     */
    fun showDeletionProgressWithErrors(
        totalItems: Int,
        processedItems: Int,
        successfulItems: Int,
        failedItems: Int,
        currentErrors: List<String>
    ) {
        if (!shouldShowError()) {
            Log.d(TAG, "Deletion progress with errors throttled: $processedItems/$totalItems processed")
            return
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            contextRef.get()?.let { context ->
                val progressText = "進行状況: $processedItems/$totalItems (成功: $successfulItems, 失敗: $failedItems)"
                
                val errorsText = if (currentErrors.isNotEmpty()) {
                    val displayErrors = currentErrors.take(MAX_ERROR_REASONS_DISPLAY)
                    val remainingErrors = if (currentErrors.size > MAX_ERROR_REASONS_DISPLAY) {
                        " (+${currentErrors.size - MAX_ERROR_REASONS_DISPLAY} 他)"
                    } else ""
                    "\nエラー: ${displayErrors.joinToString(", ")}$remainingErrors"
                } else ""
                
                val fullMessage = progressText + errorsText
                
                Snackbar.make(rootView, fullMessage, Snackbar.LENGTH_SHORT)
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
            recordErrorMetric("DELETION_PROGRESS_WITH_ERRORS", startTime)
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception showing deletion progress with errors", e)
        }
    }
    
    /**
     * GREEN Phase 2: アクセシビリティ対応部分削除結果表示
     */
    fun showAccessibilityCompliantPartialDeletionResult(
        successCount: Int,
        failedCount: Int,
        failedItems: List<String>
    ) {
        try {
            contextRef.get()?.let { context ->
                val message = context.resources.getQuantityString(
                    R.plurals.partial_deletion_success_message,
                    successCount,
                    successCount,
                    failedCount
                )
                
                val accessibilityMessage = context.getString(R.string.accessibility_partial_deletion_announcement)
                
                // アクセシビリティサービス用のアナウンスメント
                rootView.announceForAccessibility(accessibilityMessage)
                
                val failedItemsText = if (failedItems.isNotEmpty()) {
                    val displayItems = failedItems.take(MAX_FAILED_ITEMS_SHORT_DISPLAY)
                    val remainingCount = if (failedItems.size > MAX_FAILED_ITEMS_SHORT_DISPLAY) {
                        " (+${failedItems.size - MAX_FAILED_ITEMS_SHORT_DISPLAY} 他)"
                    } else ""
                    "\n失敗アイテム: ${displayItems.joinToString(", ")}$remainingCount"
                } else ""
                
                val fullMessage = message + failedItemsText
                
                Snackbar.make(rootView, fullMessage, Snackbar.LENGTH_LONG)
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception showing accessibility compliant partial deletion result", e)
            showBasicError("一部のアイテムのみ削除されました")
        }
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
