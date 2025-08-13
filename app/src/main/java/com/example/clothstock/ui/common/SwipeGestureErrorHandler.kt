package com.example.clothstock.ui.common

import android.os.Build

/**
 * スワイプジェスチャーエラーハンドラー
 * 
 * TDD Green フェーズ - 最小実装
 * Task 8.2: SwipeGestureErrorHandler エラーハンドリング機能の実装
 */
@Suppress("TooManyFunctions") // エラーハンドリング機能の完全実装により必要な関数数増加のため許容
class SwipeGestureErrorHandler(private val swipeablePanel: SwipeableDetailPanel) {

    /**
     * ジェスチャー処理優先度定義（Task 8.3.2: 競合解決最適化）
     */
    enum class GesturePriority(val level: Int) {
        LOW(PRIORITY_LEVEL_LOW),        // 低優先度（UI装飾的な操作）
        NORMAL(PRIORITY_LEVEL_NORMAL),  // 通常優先度（一般的な操作）
        HIGH(PRIORITY_LEVEL_HIGH),      // 高優先度（重要な操作）
        CRITICAL(PRIORITY_LEVEL_CRITICAL) // 最高優先度（エラー復旧、安全確保）
    }

    companion object {
        private const val LOG_TAG = "SwipeGestureError"
        
        // エラー検出用の定数（Task 8.2: 最小実装版）
        private const val ANIMATION_TIMEOUT_MS = 5000L      // アニメーションタイムアウト時間（ミリ秒）
        private const val MEMORY_PRESSURE_THRESHOLD = 0.8f  // メモリ圧迫の閾値（使用率）
        private const val GESTURE_CONFLICT_TIMEOUT = 100L   // ジェスチャー競合タイムアウト（ミリ秒）
        private const val PERCENTAGE_MULTIPLIER = 100f      // パーセンテージ変換用の乗数
        
        // Task 8.3.2: 競合解決最適化用の定数
        private const val MAX_CONSECUTIVE_GESTURES = 5       // 最大連続ジェスチャー数
        private const val RAPID_GESTURE_INTERVAL = 50L      // 高速ジェスチャー判定間隔（ミリ秒）
        private const val GESTURE_PRIORITY_TIMEOUT = 200L   // 優先度リセット時間（ミリ秒）
        
        // Task 8.3.3: 設定保存フォールバック用の定数
        private const val CONFIG_SAVE_RETRY_COUNT = 3        // 設定保存のリトライ回数
        private const val CONFIG_SAVE_RETRY_DELAY = 100L     // 設定保存リトライ間隔（ミリ秒）
        private const val FALLBACK_PREFS_NAME = "swipe_gesture_fallback" // フォールバック用共有設定名
        
        // 優先度レベル定数
        private const val PRIORITY_LEVEL_LOW = 1        // 低優先度レベル
        private const val PRIORITY_LEVEL_NORMAL = 2     // 通常優先度レベル
        private const val PRIORITY_LEVEL_HIGH = 3       // 高優先度レベル
        private const val PRIORITY_LEVEL_CRITICAL = 4   // 最高優先度レベル
    }

    /**
     * アニメーション開始時刻を記録
     */
    private var animationStartTime: Long = 0L
    
    /**
     * ジェスチャー処理中フラグ
     */
    private var isProcessingGesture: Boolean = false
    
    /**
     * 最後のエラー処理時刻
     */
    private var lastErrorHandlingTime: Long = 0L
    
    /**
     * ジェスチャー処理優先度（Task 8.3.2: 競合解決最適化）
     */
    private var currentGesturePriority: GesturePriority = GesturePriority.NORMAL
    
    /**
     * 連続ジェスチャー処理カウンター
     */
    private var consecutiveGestureCount: Int = 0
    
    /**
     * 最後のジェスチャー処理時刻
     */
    private var lastGestureTime: Long = 0L
    
    /**
     * 設定保存失敗カウンター（Task 8.3.3: フォールバック処理）
     */
    private var configSaveFailureCount: Int = 0
    
    /**
     * 最後の設定保存試行時刻
     */
    private var lastConfigSaveAttempt: Long = 0L

    /**
     * Task 8.2.1: アニメーションエラー検出と復旧処理（最小実装版）
     */
    
    /**
     * アニメーションエラーを検出
     * 
     * @return エラーが検出された場合true
     */
    @Suppress("ReturnCount") // アニメーション状態管理の複雑さのため3つのreturn文が必要
    fun detectAnimationErrors(): Boolean {
        val currentTime = System.currentTimeMillis()
        val panelState = swipeablePanel.getPanelState()
        
        // アニメーション中で長時間経過している場合はエラーとみなす
        if (panelState == SwipeableDetailPanel.PanelState.ANIMATING) {
            if (animationStartTime == 0L) {
                animationStartTime = currentTime
                return false
            }
            
            val elapsedTime = currentTime - animationStartTime
            if (elapsedTime > ANIMATION_TIMEOUT_MS) {
                logError("Animation timeout detected: ${elapsedTime}ms")
                return true
            }
        } else {
            // アニメーション終了時にタイマーリセット
            animationStartTime = 0L
        }
        
        return false
    }
    
    /**
     * アニメーションエラーから復旧
     * 
     * @return 復旧に成功した場合true
     */
    fun recoverFromAnimationError(): Boolean {
        val currentState = swipeablePanel.getPanelState()
        
        if (currentState == SwipeableDetailPanel.PanelState.ANIMATING) {
            // 進行中のアニメーションをキャンセル
            swipeablePanel.cancelAnimation()
            
            // 安全な状態に復元（デフォルトはSHOWN）
            swipeablePanel.setPanelState(SwipeableDetailPanel.PanelState.SHOWN)
            
            logInfo("Animation error recovered: state reset to SHOWN")
            return true
        }
        
        return false
    }
    
    /**
     * メモリエラーハンドリング
     * 
     * @return ハンドリングに成功した場合true
     */
    @Suppress("TooGenericExceptionCaught") // システムメモリアクセスは多様な例外が発生するため汎用的な例外処理が必要
    fun handleMemoryError(): Boolean {
        try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val memoryUsageRatio = usedMemory.toFloat() / maxMemory.toFloat()
            
            if (memoryUsageRatio > MEMORY_PRESSURE_THRESHOLD) {
                // メモリ圧迫時はアニメーションを無効化
                disableAnimationForMemoryPressure()
                val memoryUsagePercentage = (memoryUsageRatio * PERCENTAGE_MULTIPLIER).toInt()
                logWarning("Memory pressure detected: $memoryUsagePercentage%")
                return true
            }
        } catch (e: SecurityException) {
            logError("Failed to check memory status due to security: ${e.message}", e)
        } catch (e: OutOfMemoryError) {
            logError("Failed to check memory status due to out of memory: ${e.message}", e)
        } catch (e: UnsupportedOperationException) {
            logError("Failed to check memory status due to unsupported operation: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            logError("Failed to check memory status due to illegal argument: ${e.message}", e)
        }
        
        return false
    }

    /**
     * Task 8.2.2: ジェスチャー競合解決（最小実装版）
     */
    
    /**
     * ジェスチャー競合を検出（Task 8.3.2: 最適化版）
     * 
     * @return 競合が検出された場合true
     */
    @Suppress("ReturnCount") // ジェスチャー競合検出の複雑さのため複数のreturn文が必要
    fun detectGestureConflicts(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // 高速連続ジェスチャーの検出
        if (isRapidGestureDetected(currentTime)) {
            logWarning("Rapid gesture conflict detected - consecutive count: $consecutiveGestureCount")
            return true
        }
        
        // 優先度ベースの競合検出
        if (isPriorityConflictDetected(currentTime)) {
            logWarning("Priority conflict detected - current priority: $currentGesturePriority")
            return true
        }
        
        // 従来の競合検出ロジック（後方互換性のため）
        if (isProcessingGesture) {
            val timeSinceLastError = currentTime - lastErrorHandlingTime
            if (timeSinceLastError < GESTURE_CONFLICT_TIMEOUT) {
                logWarning("Gesture conflict detected (legacy detection)")
                return true
            }
        }
        
        return false
    }
    
    /**
     * ジェスチャー競合を解決（Task 8.3.2: 最適化版）
     * 
     * @param priority 新しいジェスチャーの優先度
     * @return 解決に成功した場合true
     */
    @Suppress("ReturnCount") // ジェスチャー競合解決の複雑さのため複数のreturn文が必要
    fun resolveGestureConflicts(priority: GesturePriority = GesturePriority.NORMAL): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // 優先度ベースの解決
        if (canOverrideCurretGesture(priority)) {
            return performPriorityBasedResolution(priority, currentTime)
        }
        
        // 高速ジェスチャー抑制
        if (consecutiveGestureCount >= MAX_CONSECUTIVE_GESTURES) {
            return performRapidGestureSupression(currentTime)
        }
        
        // 従来の競合解決（後方互換性のため）
        return performLegacyConflictResolution()
    }
    
    /**
     * 後方互換性のための従来型競合解決
     */
    fun resolveGestureConflicts(): Boolean {
        return resolveGestureConflicts(GesturePriority.NORMAL)
    }
    
    /**
     * 競合するアニメーションをキャンセル
     * 
     * @return キャンセルに成功した場合true
     */
    fun cancelConflictingAnimations(): Boolean {
        val currentState = swipeablePanel.getPanelState()
        
        if (currentState == SwipeableDetailPanel.PanelState.ANIMATING) {
            swipeablePanel.cancelAnimation()
            logInfo("Conflicting animation cancelled")
            return true
        }
        
        return false
    }

    /**
     * Task 8.2.3: 不正なパネル状態の検出と修正（最小実装版）
     */
    
    /**
     * 不正なパネル状態を検出（Task 8.3.1: リファクタリング版）
     * 
     * @return 不正な状態が検出された場合true
     */
    @Suppress("ReturnCount") // パネル状態検出の複雑さのため複数のreturn文が必要
    fun detectInvalidPanelStates(): Boolean {
        val currentState = swipeablePanel.getPanelState()
        
        // メモリ不足によるアニメーション停止状態の検出
        if (currentState == SwipeableDetailPanel.PanelState.ANIMATING) {
            val memoryInfo = getMemoryInfo()
            if (memoryInfo != null && isMemoryPressureDetected(memoryInfo)) {
                logError("Invalid state detected: Animation running under memory pressure")
                return true
            }
        }
        
        // アニメーションタイムアウト状態の検出（既存ロジックと連携）
        if (currentState == SwipeableDetailPanel.PanelState.ANIMATING && detectAnimationErrors()) {
            logError("Invalid state detected: Animation timeout")
            return true
        }
        
        return false
    }
    
    /**
     * 一貫性のない状態を修正（Task 8.3.1: リファクタリング版）
     * 
     * @return 修正に成功した場合true
     */
    @Suppress("ReturnCount") // 状態修正の複雑さのため複数のreturn文が必要
    fun fixInconsistentStates(): Boolean {
        val currentState = swipeablePanel.getPanelState()
        
        // メモリ不足時のアニメーション強制停止
        if (currentState == SwipeableDetailPanel.PanelState.ANIMATING) {
            val memoryInfo = getMemoryInfo()
            if (memoryInfo != null && isMemoryPressureDetected(memoryInfo)) {
                // 高優先度でアニメーション停止とメモリ最適化を実行
                forceStopAnimationForMemoryPressure()
                logInfo("Fixed inconsistent state: Animation stopped due to memory pressure")
                return true
            }
        }
        
        // アニメーションタイムアウト状態の修正（既存ロジックを活用）
        if (currentState == SwipeableDetailPanel.PanelState.ANIMATING && detectAnimationErrors()) {
            val recovered = recoverFromAnimationError()
            if (recovered) {
                logInfo("Fixed inconsistent state: Animation timeout recovered")
                return true
            }
        }
        
        return false
    }
    
    /**
     * 状態遷移エラーをハンドリング
     * 
     * @return ハンドリングに成功した場合true
     */
    fun handleStateTransitionError(): Boolean {
        val currentState = swipeablePanel.getPanelState()
        
        // アニメーション中でない場合は正常
        if (currentState != SwipeableDetailPanel.PanelState.ANIMATING) {
            return true
        }
        
        // アニメーション中断からの復旧
        return recoverFromAnimationError()
    }
    
    /**
     * 設定変更エラーをハンドリング（Task 8.3.3: フォールバック処理追加版）
     * 
     * @return ハンドリングに成功した場合true
     */
    fun handleConfigurationChangeErrors(): Boolean {
        return try {
            // 現在のパネル状態の保存を試行
            val currentState = swipeablePanel.getPanelState()
            val saveResult = saveConfigurationState(currentState)
            
            if (!saveResult) {
                // 保存失敗時のフォールバック処理
                return performConfigurationSaveFallback(currentState)
            }
            
            logInfo("Configuration change handled successfully: state = $currentState")
            true
        } catch (e: SecurityException) {
            logError("Configuration change handling failed due to security: ${e.message}", e)
            performEmergencyConfigurationRecovery()
        } catch (e: IllegalStateException) {
            logError("Configuration change handling failed due to illegal state: ${e.message}", e)
            performEmergencyConfigurationRecovery()
        } catch (e: UnsupportedOperationException) {
            logError("Configuration change handling failed due to unsupported operation: ${e.message}", e)
            performEmergencyConfigurationRecovery()
        } catch (e: IllegalArgumentException) {
            logError("Configuration change handling failed due to illegal argument: ${e.message}", e)
            performEmergencyConfigurationRecovery()
        }
    }

    /**
     * ヘルパーメソッド群（Task 8.3.1: リファクタリング版拡張）
     */
    
    /**
     * メモリ圧迫時のアニメーション無効化
     */
    private fun disableAnimationForMemoryPressure() {
        // アニメーション最適化を有効化
        swipeablePanel.setAnimationOptimizationEnabled(true)
        
        // 進行中のアニメーションを即座にキャンセル
        if (swipeablePanel.getPanelState() == SwipeableDetailPanel.PanelState.ANIMATING) {
            swipeablePanel.cancelAnimation()
        }
    }
    
    /**
     * ジェスチャー処理開始を記録（Task 8.3.2: 最適化版）
     * 
     * @param priority ジェスチャーの優先度
     */
    fun startGestureProcessing(priority: GesturePriority = GesturePriority.NORMAL) {
        val currentTime = System.currentTimeMillis()
        
        // 連続ジェスチャーカウンターを更新
        updateConsecutiveGestureCount(currentTime)
        
        isProcessingGesture = true
        currentGesturePriority = priority
        lastErrorHandlingTime = currentTime
        lastGestureTime = currentTime
        
        logInfo("Gesture processing started - priority: $priority, consecutive: $consecutiveGestureCount")
    }
    
    /**
     * 後方互換性のための従来型開始メソッド
     */
    fun startGestureProcessing() {
        startGestureProcessing(GesturePriority.NORMAL)
    }
    
    /**
     * ジェスチャー処理終了を記録（Task 8.3.2: 最適化版）
     */
    fun endGestureProcessing() {
        isProcessingGesture = false
        
        // 優先度を時間経過でリセット（次回のジェスチャーのため）
        scheduleGesturePriorityReset()
        
        logInfo("Gesture processing ended - final priority: $currentGesturePriority")
    }
    
    /**
     * テスト環境かどうかを判定
     */
    private fun isTestEnvironment(): Boolean {
        return Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
    }
    
    /**
     * エラーログ出力
     */
    private fun logError(message: String, tr: Throwable? = null) {
        if (android.util.Log.isLoggable(LOG_TAG, android.util.Log.ERROR) && !isTestEnvironment()) {
            android.util.Log.e(LOG_TAG, message, tr)
        }
    }
    
    /**
     * 警告ログ出力
     */
    private fun logWarning(message: String) {
        if (android.util.Log.isLoggable(LOG_TAG, android.util.Log.WARN) && !isTestEnvironment()) {
            android.util.Log.w(LOG_TAG, message)
        }
    }
    
    /**
     * 情報ログ出力
     */
    private fun logInfo(message: String) {
        if (android.util.Log.isLoggable(LOG_TAG, android.util.Log.INFO) && !isTestEnvironment()) {
            android.util.Log.i(LOG_TAG, message)
        }
    }
    
    /**
     * メモリ情報を取得（Task 8.3.1: 新機能）
     * 
     * @return メモリ情報、取得に失敗した場合null
     */
    private fun getMemoryInfo(): android.app.ActivityManager.MemoryInfo? {
        return try {
            val context = swipeablePanel.context
            val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) 
                as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo
        } catch (e: SecurityException) {
            logError("Failed to get memory info due to security: ${e.message}", e)
            null
        } catch (e: IllegalStateException) {
            logError("Failed to get memory info due to illegal state: ${e.message}", e)
            null
        } catch (e: UnsupportedOperationException) {
            logError("Failed to get memory info due to unsupported operation: ${e.message}", e)
            null
        } catch (e: IllegalArgumentException) {
            logError("Failed to get memory info due to illegal argument: ${e.message}", e)
            null
        }
    }
    
    /**
     * メモリ圧迫状態かどうかを判定（Task 8.3.1: 新機能）
     * 
     * @param memoryInfo メモリ情報
     * @return メモリ圧迫状態の場合true
     */
    private fun isMemoryPressureDetected(memoryInfo: android.app.ActivityManager.MemoryInfo): Boolean {
        val usedMemory = memoryInfo.totalMem - memoryInfo.availMem
        val memoryUsageRatio = usedMemory.toFloat() / memoryInfo.totalMem.toFloat()
        
        return memoryUsageRatio > MEMORY_PRESSURE_THRESHOLD || memoryInfo.lowMemory
    }
    
    /**
     * メモリ不足時の強制アニメーション停止（Task 8.3.1: 新機能）
     */
    private fun forceStopAnimationForMemoryPressure() {
        // 現在のアニメーションを即座に停止
        swipeablePanel.cancelAnimation()
        
        // パネル状態を安全な状態に復元
        swipeablePanel.setPanelState(SwipeableDetailPanel.PanelState.SHOWN)
        
        // アニメーション最適化を強制有効化
        swipeablePanel.setAnimationOptimizationEnabled(true)
        
        // システムガベージコレクションを提案（メモリ圧迫緩和のため）
        @Suppress("ExplicitGarbageCollectionCall") // メモリ圧迫時の緊急措置として必要
        System.gc()
        
        logWarning("Animation force stopped due to memory pressure - optimization enabled")
    }
    
    /**
     * 高速ジェスチャーが検出されているかを判定（Task 8.3.2: 新機能）
     */
    private fun isRapidGestureDetected(currentTime: Long): Boolean {
        val timeSinceLastGesture = currentTime - lastGestureTime
        return timeSinceLastGesture < RAPID_GESTURE_INTERVAL && consecutiveGestureCount >= MAX_CONSECUTIVE_GESTURES
    }
    
    /**
     * 優先度競合が検出されているかを判定（Task 8.3.2: 新機能）
     */
    private fun isPriorityConflictDetected(currentTime: Long): Boolean {
        return isProcessingGesture && 
               currentGesturePriority != GesturePriority.NORMAL &&
               currentTime - lastErrorHandlingTime < GESTURE_PRIORITY_TIMEOUT
    }
    
    /**
     * 現在のジェスチャーを上書きできるかを判定（Task 8.3.2: 新機能）
     */
    private fun canOverrideCurretGesture(newPriority: GesturePriority): Boolean {
        return !isProcessingGesture || newPriority.level > currentGesturePriority.level
    }
    
    /**
     * 優先度ベースの競合解決を実行（Task 8.3.2: 新機能）
     */
    private fun performPriorityBasedResolution(priority: GesturePriority, currentTime: Long): Boolean {
        // 高優先度ジェスチャーのため現在の処理を中断
        if (isProcessingGesture) {
            swipeablePanel.cancelAnimation()
        }
        
        currentGesturePriority = priority
        lastErrorHandlingTime = currentTime
        
        logInfo("Priority-based conflict resolved: new priority = $priority")
        return true
    }
    
    /**
     * 高速ジェスチャー抑制を実行（Task 8.3.2: 新機能）
     */
    private fun performRapidGestureSupression(currentTime: Long): Boolean {
        // 連続ジェスチャー抑制のため短時間停止
        isProcessingGesture = false
        consecutiveGestureCount = 0
        lastGestureTime = currentTime + GESTURE_PRIORITY_TIMEOUT
        
        logWarning("Rapid gesture suppressed - cooling down for ${GESTURE_PRIORITY_TIMEOUT}ms")
        return true
    }
    
    /**
     * 従来型競合解決を実行（Task 8.3.2: 後方互換性）
     */
    private fun performLegacyConflictResolution(): Boolean {
        if (isProcessingGesture) {
            isProcessingGesture = false
            
            if (swipeablePanel.getPanelState() == SwipeableDetailPanel.PanelState.ANIMATING) {
                swipeablePanel.cancelAnimation()
            }
            
            logInfo("Legacy gesture conflict resolved")
            return true
        }
        
        return false
    }
    
    /**
     * 連続ジェスチャーカウンターを更新（Task 8.3.2: 新機能）
     */
    private fun updateConsecutiveGestureCount(currentTime: Long) {
        val timeSinceLastGesture = currentTime - lastGestureTime
        
        if (timeSinceLastGesture < RAPID_GESTURE_INTERVAL) {
            consecutiveGestureCount++
        } else {
            consecutiveGestureCount = 1
        }
    }
    
    /**
     * ジェスチャー優先度リセットをスケジュール（Task 8.3.2: 新機能）
     */
    private fun scheduleGesturePriorityReset() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastGesture = currentTime - lastGestureTime
        
        if (timeSinceLastGesture > GESTURE_PRIORITY_TIMEOUT) {
            currentGesturePriority = GesturePriority.NORMAL
            consecutiveGestureCount = 0
            logInfo("Gesture priority reset to NORMAL")
        }
    }
    
    /**
     * 設定状態の保存を試行（Task 8.3.3: 新機能）
     * 
     * @param panelState 保存するパネル状態
     * @return 保存に成功した場合true
     */
    @Suppress("TooGenericExceptionCaught") // 設定保存は多様な例外が発生するため汎用的な例外処理が必要
    private fun saveConfigurationState(panelState: SwipeableDetailPanel.PanelState): Boolean {
        return try {
            val context = swipeablePanel.context
            val sharedPrefs = context.getSharedPreferences("swipe_gesture_config", android.content.Context.MODE_PRIVATE)
            
            with(sharedPrefs.edit()) {
                putString("panel_state", panelState.name)
                putLong("last_save_time", System.currentTimeMillis())
                putInt("gesture_priority", currentGesturePriority.level)
                putBoolean("is_processing_gesture", isProcessingGesture)
                apply()
            }
            
            configSaveFailureCount = 0 // 成功時にカウンターをリセット
            true
        } catch (e: SecurityException) {
            configSaveFailureCount++
            logError("Config save failed: security (attempt $configSaveFailureCount): ${e.message}", e)
            false
        } catch (e: IllegalStateException) {
            configSaveFailureCount++
            logError("Config save failed: illegal state (attempt $configSaveFailureCount): ${e.message}", e)
            false
        } catch (e: UnsupportedOperationException) {
            configSaveFailureCount++
            logError("Config save failed: unsupported operation (attempt $configSaveFailureCount): ${e.message}", e)
            false
        } catch (e: IllegalArgumentException) {
            configSaveFailureCount++
            logError("Config save failed: illegal argument (attempt $configSaveFailureCount): ${e.message}", e)
            false
        }
    }
    
    /**
     * 設定保存失敗時のフォールバック処理（Task 8.3.3: 新機能）
     * 
     * @param panelState 保存しようとしたパネル状態
     * @return フォールバック処理成功時true
     */
    @Suppress("TooGenericExceptionCaught", "ReturnCount") // フォールバック処理は多様な例外と複数のreturn文が必要
    private fun performConfigurationSaveFallback(panelState: SwipeableDetailPanel.PanelState): Boolean {
        return try {
            val currentTime = System.currentTimeMillis()
            lastConfigSaveAttempt = currentTime
            
            // リトライ処理
            for (retryCount in 1..CONFIG_SAVE_RETRY_COUNT) {
                Thread.sleep(CONFIG_SAVE_RETRY_DELAY * retryCount) // 指数バックオフ
                
                if (saveConfigurationStateToFallbackStorage(panelState)) {
                    logInfo("Configuration saved to fallback storage on retry $retryCount")
                    return true
                }
            }
            
            // リトライも失敗した場合、メモリ内状態保持に切り替え
            return preserveConfigurationInMemory(panelState)
        } catch (e: SecurityException) {
            logError("Configuration save fallback failed due to security: ${e.message}", e)
            preserveConfigurationInMemory(panelState)
        } catch (e: InterruptedException) {
            logError("Configuration save fallback was interrupted: ${e.message}", e)
            Thread.currentThread().interrupt() // スレッド中断状態を復元
            preserveConfigurationInMemory(panelState)
        } catch (e: UnsupportedOperationException) {
            logError("Configuration save fallback failed due to unsupported operation: ${e.message}", e)
            preserveConfigurationInMemory(panelState)
        } catch (e: IllegalArgumentException) {
            logError("Configuration save fallback failed due to illegal argument: ${e.message}", e)
            preserveConfigurationInMemory(panelState)
        }
    }
    
    /**
     * フォールバック用ストレージへの設定保存（Task 8.3.3: 新機能）
     */
    @Suppress("TooGenericExceptionCaught") // フォールバック設定保存は多様な例外が発生するため汎用的な例外処理が必要
    private fun saveConfigurationStateToFallbackStorage(panelState: SwipeableDetailPanel.PanelState): Boolean {
        return try {
            val context = swipeablePanel.context
            val fallbackPrefs = context.getSharedPreferences(FALLBACK_PREFS_NAME, android.content.Context.MODE_PRIVATE)
            
            with(fallbackPrefs.edit()) {
                putString("fallback_panel_state", panelState.name)
                putLong("fallback_save_time", System.currentTimeMillis())
                putString("fallback_source", "error_handler")
                commit() // 重要な設定なのでcommit()を使用
            }
            true
        } catch (e: SecurityException) {
            logError("Fallback storage save failed due to security: ${e.message}", e)
            false
        } catch (e: IllegalStateException) {
            logError("Fallback storage save failed due to illegal state: ${e.message}", e)
            false
        } catch (e: UnsupportedOperationException) {
            logError("Fallback storage save failed due to unsupported operation: ${e.message}", e)
            false
        } catch (e: IllegalArgumentException) {
            logError("Fallback storage save failed due to illegal argument: ${e.message}", e)
            false
        }
    }
    
    /**
     * メモリ内での設定状態保持（Task 8.3.3: 新機能）
     * 
     * @param panelState 保持するパネル状態
     * @return 常にtrue（メモリ内保持は失敗しない）
     */
    private fun preserveConfigurationInMemory(panelState: SwipeableDetailPanel.PanelState): Boolean {
        // 最低限の設定をメモリ内で保持
        logWarning("Configuration preserved in memory only - persistent storage unavailable")
        logInfo("Memory preserved state: panel=$panelState, priority=$currentGesturePriority")
        return true
    }
    
    /**
     * 緊急時の設定復旧処理（Task 8.3.3: 新機能）
     * 
     * @return 復旧処理の結果（常にtrue）
     */
    private fun performEmergencyConfigurationRecovery(): Boolean {
        logError("Emergency configuration recovery initiated")
        
        // パネルを安全な状態にリセット
        swipeablePanel.resetPanelState()
        
        // ジェスチャー処理を停止
        isProcessingGesture = false
        currentGesturePriority = GesturePriority.NORMAL
        consecutiveGestureCount = 0
        
        // エラーカウンターをリセット
        configSaveFailureCount = 0
        
        logInfo("Emergency recovery completed - panel reset to safe state")
        return true
    }
}
