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

    companion object {
        private const val LOG_TAG = "SwipeGestureError"
        
        // エラー検出用の定数（Task 8.2: 最小実装版）
        private const val ANIMATION_TIMEOUT_MS = 5000L      // アニメーションタイムアウト時間（ミリ秒）
        private const val MEMORY_PRESSURE_THRESHOLD = 0.8f  // メモリ圧迫の閾値（使用率）
        private const val GESTURE_CONFLICT_TIMEOUT = 100L   // ジェスチャー競合タイムアウト（ミリ秒）
        private const val PERCENTAGE_MULTIPLIER = 100f      // パーセンテージ変換用の乗数
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
        } catch (e: Exception) {
            logError("Failed to check memory status: ${e.message}")
        }
        
        return false
    }

    /**
     * Task 8.2.2: ジェスチャー競合解決（最小実装版）
     */
    
    /**
     * ジェスチャー競合を検出
     * 
     * @return 競合が検出された場合true
     */
    fun detectGestureConflicts(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // 短時間内に複数のジェスチャー処理要求があった場合は競合とみなす
        if (isProcessingGesture) {
            val timeSinceLastError = currentTime - lastErrorHandlingTime
            if (timeSinceLastError < GESTURE_CONFLICT_TIMEOUT) {
                logWarning("Gesture conflict detected")
                return true
            }
        }
        
        return false
    }
    
    /**
     * ジェスチャー競合を解決
     * 
     * @return 解決に成功した場合true
     */
    fun resolveGestureConflicts(): Boolean {
        if (isProcessingGesture) {
            // 現在のジェスチャー処理を中断
            isProcessingGesture = false
            
            // 進行中のアニメーションがあればキャンセル
            if (swipeablePanel.getPanelState() == SwipeableDetailPanel.PanelState.ANIMATING) {
                swipeablePanel.cancelAnimation()
            }
            
            logInfo("Gesture conflict resolved")
            return true
        }
        
        return false
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
     * 不正なパネル状態を検出
     * 
     * @return 不正な状態が検出された場合true
     */
    fun detectInvalidPanelStates(): Boolean {
        val currentState = swipeablePanel.getPanelState()
        
        // null状態や予期しない状態を検出（最小実装では基本チェックのみ）
        if (currentState == null) {
            logError("Invalid panel state detected: null")
            return true
        }
        
        return false
    }
    
    /**
     * 一貫性のない状態を修正
     * 
     * @return 修正に成功した場合true
     */
    fun fixInconsistentStates(): Boolean {
        val currentState = swipeablePanel.getPanelState()
        
        // null状態の場合は安全な状態に復元
        if (currentState == null) {
            swipeablePanel.setPanelState(SwipeableDetailPanel.PanelState.SHOWN)
            logInfo("Inconsistent state fixed: null -> SHOWN")
            return true
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
     * 設定変更エラーをハンドリング
     * 
     * @return ハンドリングに成功した場合true
     */
    fun handleConfigurationChangeErrors(): Boolean {
        // 基本実装：現在の状態を保持するだけ
        val currentState = swipeablePanel.getPanelState()
        logInfo("Configuration change handled: current state = $currentState")
        return true
    }

    /**
     * ヘルパーメソッド群
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
     * ジェスチャー処理開始を記録
     */
    fun startGestureProcessing() {
        isProcessingGesture = true
        lastErrorHandlingTime = System.currentTimeMillis()
    }
    
    /**
     * ジェスチャー処理終了を記録
     */
    fun endGestureProcessing() {
        isProcessingGesture = false
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
    private fun logError(message: String) {
        if (android.util.Log.isLoggable(LOG_TAG, android.util.Log.ERROR) && !isTestEnvironment()) {
            android.util.Log.e(LOG_TAG, message)
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
}
