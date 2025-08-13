package com.example.clothstock.ui.common

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * デバイス互換性マネージャー
 * 
 * TDD Refactor フェーズ - 完全実装
 * Task 9.3: デバイス対応機能のリファクタリング
 */
@Suppress("TooManyFunctions") // デバイス対応機能の完全実装により必要な関数数増加のため許容
class DeviceCompatibilityManager(private val context: Context) {

    companion object {
        private const val LOG_TAG = "DeviceCompatibility"
        
        // デバイス性能判定用の定数
        private const val LOW_END_MEMORY_THRESHOLD_MB = 1024    // 低性能デバイス判定のメモリ閾値（MB）
        private const val LOW_END_CORES_THRESHOLD = 4           // 低性能デバイス判定のCPUコア数閾値
        private const val BYTES_TO_MB_DIVISOR = 1024 * 1024    // バイトからMB変換の除数
        
        // 画面サイズ判定用の定数
        private const val SMALL_SCREEN_WIDTH_DP = 360           // 小画面判定の幅閾値（dp）
        private const val SMALL_SCREEN_HEIGHT_DP = 640          // 小画面判定の高さ閾値（dp）
        private const val TABLET_MIN_WIDTH_DP = 600             // タブレット判定の最小幅（dp）
        
        // スワイプ閾値調整用の定数
        private const val DEFAULT_SWIPE_THRESHOLD = 50f         // デフォルトスワイプ閾値（ピクセル）
        private const val SMALL_SCREEN_SWIPE_MULTIPLIER = 0.8f  // 小画面用スワイプ閾値倍率
        private const val TABLET_SWIPE_MULTIPLIER = 1.2f        // タブレット用スワイプ閾値倍率
        
        // パネル高さ調整用の定数
        private const val DEFAULT_PANEL_HEIGHT = 200            // デフォルトパネル高さ（ピクセル）
        private const val HIGH_DENSITY_MULTIPLIER = 1.5f        // 高密度画面用パネル高さ倍率
        private const val LOW_DENSITY_MULTIPLIER = 0.8f         // 低密度画面用パネル高さ倍率
        private const val HIGH_DENSITY_THRESHOLD = 2.0f         // 高密度画面判定の閾値
        private const val LOW_DENSITY_THRESHOLD = 1.0f          // 低密度画面判定の閾値
    }

    // デバイス情報キャッシュ（パフォーマンス最適化）
    private var cachedIsLowEndDevice: Boolean? = null
    private var cachedIsSmallScreen: Boolean? = null
    private var cachedIsTablet: Boolean? = null
    private var cachedDisplayMetrics: DisplayMetrics? = null

    /**
     * Task 9.3.1: ローエンドデバイス対応の完全実装
     */
    
    /**
     * ローエンドデバイスかどうかを判定（完全実装版）
     * @return ローエンドデバイスの場合true
     */
    @Suppress("TooGenericExceptionCaught") // システム情報取得は多様な例外が発生するため汎用的な例外処理が必要
    fun isLowEndDevice(): Boolean {
        // キャッシュされた結果があれば使用
        cachedIsLowEndDevice?.let { return it }
        
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            // メモリベースの判定
            val totalMemoryMB = memoryInfo.totalMem / BYTES_TO_MB_DIVISOR
            val isMemoryLow = totalMemoryMB < LOW_END_MEMORY_THRESHOLD_MB
            
            // CPUコア数ベースの判定
            val cpuCores = Runtime.getRuntime().availableProcessors()
            val isCpuLow = cpuCores < LOW_END_CORES_THRESHOLD
            
            // 総合判定（メモリまたはCPUのいずれかが低性能）
            val result = isMemoryLow || isCpuLow
            cachedIsLowEndDevice = result
            
            logDeviceInfo("Device detected as ${if (result) "low-end" else "high-end"}: " +
                         "Memory=${totalMemoryMB}MB, CPU cores=$cpuCores")
            
            result
        } catch (e: SecurityException) {
            logError("Failed to detect device performance due to security: ${e.message}", e)
            false // セキュリティエラーの場合は高性能として扱う
        } catch (e: RuntimeException) {
            logError("Failed to detect device performance: ${e.message}", e)
            false // 例外の場合は高性能として扱う
        }
    }
    
    /**
     * アニメーションを無効化すべきかを判定（完全実装版）
     * @return アニメーション無効化が必要な場合true
     */
    fun shouldDisableAnimations(): Boolean {
        return isLowEndDevice() || isReduceMotionEnabled()
    }
    
    /**
     * 簡略化アニメーションを使用すべきかを判定（完全実装版）
     * @return 簡略化アニメーション使用が必要な場合true
     */
    fun shouldUseSimplifiedAnimations(): Boolean {
        return isLowEndDevice() && !shouldDisableAnimations()
    }
    
    /**
     * メモリ使用量を最適化すべきかを判定（完全実装版）
     * @return メモリ最適化が必要な場合true
     */
    fun shouldOptimizeMemoryUsage(): Boolean {
        return isLowEndDevice()
    }

    /**
     * Task 9.3.2: アクセシビリティ設定対応の完全実装
     */
    
    /**
     * モーション削減設定が有効かを判定（完全実装版）
     * @return モーション削減設定が有効な場合true
     */
    @Suppress("TooGenericExceptionCaught") // システム設定アクセスは多様な例外が発生するため汎用的な例外処理が必要
    fun isReduceMotionEnabled(): Boolean {
        return try {
            val animationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            
            // アニメーション無効または大幅減速の場合
            val result = animationScale == 0f || animationScale >= 2.0f
            logDeviceInfo("Motion reduction detected: $result (animation scale: $animationScale)")
            
            result
        } catch (e: SecurityException) {
            logError("Failed to check animation settings due to security: ${e.message}", e)
            false
        } catch (e: RuntimeException) {
            logError("Failed to check animation settings: ${e.message}", e)
            false
        }
    }
    
    /**
     * ハイコントラスト設定が有効かを判定（完全実装版）
     * @return ハイコントラスト設定が有効な場合true
     */
    @Suppress("TooGenericExceptionCaught") // システム設定アクセスは多様な例外が発生するため汎用的な例外処理が必要
    fun isHighContrastEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 正しい定数名: ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED
                val isHighContrastTextEnabled = Settings.Secure.getInt(
                    context.contentResolver,
                    "accessibility_high_text_contrast_enabled", // 文字列リテラルを使用
                    0
                ) == 1
                
                logDeviceInfo("High contrast detected: $isHighContrastTextEnabled")
                isHighContrastTextEnabled
            } else {
                // Android N未満では設定が利用できないため、falseを返す
                false
            }
        } catch (e: SecurityException) {
            logError("Failed to check contrast settings due to security: ${e.message}", e)
            false
        } catch (e: RuntimeException) {
            logError("Failed to check contrast settings: ${e.message}", e)
            false
        }
    }
    
    /**
     * アクセシビリティ設定に基づいてアニメーションを適応すべきかを判定（完全実装版）
     * @return アニメーション適応が必要な場合true
     */
    fun shouldAdaptAnimationsForAccessibility(): Boolean {
        return isReduceMotionEnabled() || isHighContrastEnabled()
    }
    
    /**
     * タッチターゲットを拡大すべきかを判定（完全実装版）
     * @return タッチターゲット拡大が必要な場合true
     */
    @Suppress("TooGenericExceptionCaught") // システム設定アクセスは多様な例外が発生するため汎用的な例外処理が必要
    fun shouldEnhanceTouchTargets(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                val touchExplorationEnabled = Settings.Secure.getInt(
                    context.contentResolver,
                    "touch_exploration_enabled", // 文字列リテラルを使用
                    0
                ) == 1
                
                logDeviceInfo("Touch target enhancement detected: $touchExplorationEnabled")
                touchExplorationEnabled
            } else {
                false
            }
        } catch (e: SecurityException) {
            logError("Failed to check touch exploration settings due to security: ${e.message}", e)
            false
        } catch (e: RuntimeException) {
            logError("Failed to check touch exploration settings: ${e.message}", e)
            false
        }
    }

    /**
     * Task 9.3.3: 画面サイズ対応の完全実装
     */
    
    /**
     * 小画面デバイスかどうかを判定（完全実装版）
     * @return 小画面デバイスの場合true
     */
    fun isSmallScreen(): Boolean {
        cachedIsSmallScreen?.let { return it }
        
        val displayMetrics = getDisplayMetrics()
        val density = displayMetrics.density
        
        // ピクセルからdpに変換して判定
        val widthDp = displayMetrics.widthPixels / density
        val heightDp = displayMetrics.heightPixels / density
        
        val result = widthDp < SMALL_SCREEN_WIDTH_DP || heightDp < SMALL_SCREEN_HEIGHT_DP
        cachedIsSmallScreen = result
        
        logDeviceInfo("Small screen detected: $result (${widthDp.toInt()}x${heightDp.toInt()} dp)")
        
        return result
    }
    
    /**
     * タブレットデバイスかどうかを判定（完全実装版）
     * @return タブレットデバイスの場合true
     */
    fun isTablet(): Boolean {
        cachedIsTablet?.let { return it }
        
        val configuration = context.resources.configuration
        val result = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= 
            Configuration.SCREENLAYOUT_SIZE_LARGE
        
        // 追加の判定：最小幅がタブレット閾値以上
        val displayMetrics = getDisplayMetrics()
        val smallestWidthDp = minOf(
            displayMetrics.widthPixels / displayMetrics.density,
            displayMetrics.heightPixels / displayMetrics.density
        )
        
        val resultByWidth = smallestWidthDp >= TABLET_MIN_WIDTH_DP
        val finalResult = result || resultByWidth
        
        cachedIsTablet = finalResult
        
        logDeviceInfo("Tablet detected: $finalResult (layout=$result, smallest width=${smallestWidthDp.toInt()}dp)")
        
        return finalResult
    }
    
    /**
     * 画面サイズに適応されたスワイプ閾値を取得（完全実装版）
     * @return スワイプ閾値（ピクセル）
     */
    fun getAdaptedSwipeThreshold(): Float {
        val baseThreshold = DEFAULT_SWIPE_THRESHOLD * getDisplayMetrics().density
        
        return when {
            isTablet() -> baseThreshold * TABLET_SWIPE_MULTIPLIER
            isSmallScreen() -> baseThreshold * SMALL_SCREEN_SWIPE_MULTIPLIER
            else -> baseThreshold
        }
    }
    
    /**
     * 画面密度に適応されたパネル高さを取得（完全実装版）
     * @return パネル高さ（ピクセル）
     */
    fun getAdaptedPanelHeight(): Int {
        val displayMetrics = getDisplayMetrics()
        val basePanelHeight = (DEFAULT_PANEL_HEIGHT * displayMetrics.density).toInt()
        
        return when {
            displayMetrics.density >= HIGH_DENSITY_THRESHOLD -> 
                (basePanelHeight * HIGH_DENSITY_MULTIPLIER).toInt()
            displayMetrics.density <= LOW_DENSITY_THRESHOLD -> 
                (basePanelHeight * LOW_DENSITY_MULTIPLIER).toInt()
            else -> basePanelHeight
        }
    }
    
    /**
     * 横向き画面での調整が必要かを判定（完全実装版）
     * @return 横向き調整が必要な場合true
     */
    fun shouldAdjustForLandscape(): Boolean {
        val orientation = context.resources.configuration.orientation
        val result = orientation == Configuration.ORIENTATION_LANDSCAPE
        
        logDeviceInfo("Landscape adjustment needed: $result (orientation=$orientation)")
        
        return result
    }

    /**
     * ヘルパーメソッド群
     */
    
    /**
     * DisplayMetricsを取得（キャッシュ付き）
     */
    @Suppress("TooGenericExceptionCaught") // システム情報取得は多様な例外が発生するため汎用的な例外処理が必要
    private fun getDisplayMetrics(): DisplayMetrics {
        cachedDisplayMetrics?.let { return it }
        
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android R以降の新しいAPI
                windowManager.currentWindowMetrics.bounds.let { bounds ->
                    displayMetrics.widthPixels = bounds.width()
                    displayMetrics.heightPixels = bounds.height()
                }
                context.resources.displayMetrics.let { resourceMetrics ->
                    displayMetrics.density = resourceMetrics.density
                    displayMetrics.scaledDensity = resourceMetrics.scaledDensity
                    displayMetrics.densityDpi = resourceMetrics.densityDpi
                }
            } else {
                // Android R未満の従来API
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(displayMetrics)
            }
            
            cachedDisplayMetrics = displayMetrics
            displayMetrics
        } catch (e: RuntimeException) {
            logError("Failed to get display metrics: ${e.message}", e)
            // フォールバックとしてリソースから取得
            context.resources.displayMetrics
        }
    }
    
    /**
     * テスト環境かどうかを判定
     */
    private fun isTestEnvironment(): Boolean {
        return Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
    }
    
    /**
     * デバイス情報ログ出力
     */
    private fun logDeviceInfo(message: String) {
        if (android.util.Log.isLoggable(LOG_TAG, android.util.Log.INFO) && !isTestEnvironment()) {
            android.util.Log.i(LOG_TAG, message)
        }
    }
    
    /**
     * エラーログ出力
     */
    private fun logError(message: String, tr: Throwable? = null) {
        if (android.util.Log.isLoggable(LOG_TAG, android.util.Log.ERROR) && !isTestEnvironment()) {
            android.util.Log.e(LOG_TAG, message, tr)
        }
    }
}
