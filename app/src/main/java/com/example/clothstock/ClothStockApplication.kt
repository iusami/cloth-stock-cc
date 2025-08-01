package com.example.clothstock

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log
import com.bumptech.glide.Glide
import com.example.clothstock.util.MemoryPressureMonitor

/**
 * cloth-stock アプリケーションクラス
 * 
 * Android Q+対応のメモリ管理を提供
 * - システムレベルのメモリ圧迫対応
 * - Glideとの連携したメモリクリーンアップ
 * - 既存のMemoryPressureMonitorとの統合
 */
class ClothStockApplication : Application() {

    companion object {
        private const val TAG = "ClothStockApplication"
        
        /**
         * Android Q+かどうかをチェック
         */
        private fun isAndroidQPlus(): Boolean {
            return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
        }
        
        /**
         * API 34 (UPSIDE_DOWN_CAKE) 未満かどうかをチェック
         */
        private fun isBeforeUpsideDownCake(): Boolean {
            return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        }
    }

    private lateinit var memoryPressureMonitor: MemoryPressureMonitor

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ClothStockApplication starting up")
        
        // MemoryPressureMonitorの初期化
        memoryPressureMonitor = MemoryPressureMonitor.getInstance(this)
        
        // Android Q+対応: システム統合を有効化
        memoryPressureMonitor.enableSystemIntegration()
        Log.d(TAG, "MemoryPressureMonitor system integration enabled")
        
        Log.d(TAG, "Application initialized successfully")
    }

    /**
     * システムメモリ圧迫時のコールバック (Android Q+対応)
     * 
     * pinning非推奨警告の解決とAPI 34での非推奨レベル対応
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        Log.d(TAG, "onTrimMemory called with level: $level (API ${android.os.Build.VERSION.SDK_INT})")
        
        // MemoryPressureMonitorに通知
        memoryPressureMonitor.handleSystemTrimMemory(level)
        
        when (level) {
            // Android Q+でも有効なレベル
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                Log.d(TAG, "TRIM_MEMORY_BACKGROUND - アプリがバックグラウンドに移行")
                performAntiPinningMemoryCleanup("background")
            }
            
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Log.d(TAG, "TRIM_MEMORY_COMPLETE - 完全なメモリクリーンアップ")
                performAntiPinningMemoryCleanup("complete")
            }
            
            // API 34で非推奨だが、Q+では条件付きで使用可能
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                if (isBeforeUpsideDownCake()) {
                    Log.d(TAG, "TRIM_MEMORY_MODERATE - 中程度のメモリクリーンアップ")
                    performAntiPinningMemoryCleanup("moderate")
                } else {
                    Log.d(TAG, "Skipping deprecated TRIM_MEMORY_MODERATE on API " +
                        "${android.os.Build.VERSION.SDK_INT}")
                    performAntiPinningMemoryCleanup("background") // フォールバック
                }
            }
            
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                if (isBeforeUpsideDownCake()) {
                    Log.d(TAG, "Running trim memory level $level - 実行中クリーンアップ")
                    performAntiPinningMemoryCleanup("running")
                } else {
                    Log.d(TAG, "Skipping deprecated running trim level $level on API " +
                        "${android.os.Build.VERSION.SDK_INT}")
                    performAntiPinningMemoryCleanup("light") // フォールバック
                }
            }
            
            else -> {
                Log.d(TAG, "Unknown trim memory level: $level")
                performAntiPinningMemoryCleanup("light")
            }
        }
    }

    /**
     * 低メモリ警告時のコールバック (Android Q+対応)
     */
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "onLowMemory called - 緊急メモリクリーンアップを実行")
        
        // MemoryPressureMonitorに通知
        memoryPressureMonitor.handleSystemLowMemory()
        
        // 緊急時の包括的メモリクリーンアップ
        performEmergencyMemoryCleanup()
    }

    // ===== Android Q+ pinning非推奨対応メモリクリーンアップメソッド =====

    /**
     * pinning非推奨警告対応のメモリクリーンアップ
     * 
     * @param type クリーンアップのタイプ（light, moderate, background, complete, running）
     */
    private fun performAntiPinningMemoryCleanup(type: String) {
        Log.d(TAG, "Performing anti-pinning memory cleanup: $type")
        
        try {
            when (type) {
                "light" -> performLightAntiPinningCleanup()
                "moderate" -> performModerateAntiPinningCleanup()
                "background" -> performBackgroundAntiPinningCleanup()
                "complete" -> performCompleteAntiPinningCleanup()
                "running" -> performRunningAntiPinningCleanup()
                else -> performLightAntiPinningCleanup()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during anti-pinning memory cleanup: $type", e)
        }
    }

    /**
     * 軽度のpinning非推奨対応クリーンアップ
     */
    private fun performLightAntiPinningCleanup() {
        Log.d(TAG, "Performing light anti-pinning cleanup")
        
        try {
            // pinningを使わないGlideメモリクリア
            Glide.get(this).apply {
                // ハードウェアビットマップをクリア（pinning回避）
                clearMemory()
            }
            
            // MemoryPressureMonitorの軽度クリーンアップ
            memoryPressureMonitor.getCacheManager().clearCache() // キャッシュクリア
            
            Log.d(TAG, "Light anti-pinning cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during light anti-pinning cleanup", e)
        }
    }

    /**
     * 中程度のpinning非推奨対応クリーンアップ
     */
    private fun performModerateAntiPinningCleanup() {
        Log.d(TAG, "Performing moderate anti-pinning cleanup")
        
        try {
            // 軽度クリーンアップも実行
            performLightAntiPinningCleanup()
            
            // より積極的なメモリ管理
            memoryPressureMonitor.getCacheManager().clearCache()
            
            // pinning非推奨対応: native heap管理
            if (isAndroidQPlus()) {
                // Android Q+でのnative heap最適化
                System.gc() // 保守的なGC実行
            }
            
            Log.d(TAG, "Moderate anti-pinning cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during moderate anti-pinning cleanup", e)
        }
    }

    /**
     * バックグラウンド状態でのpinning非推奨対応クリーンアップ
     */
    private fun performBackgroundAntiPinningCleanup() {
        Log.d(TAG, "Performing background anti-pinning cleanup")
        
        try {
            // 中程度クリーンアップを実行
            performModerateAntiPinningCleanup()
            
            // バックグラウンド特有の処理
            Glide.get(this).apply {
                clearMemory()
                // ディスクキャッシュの非同期クリア（pinning回避）
                Thread { 
                    try {
                        clearDiskCache()
                        Log.d(TAG, "Background disk cache cleared")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error clearing background disk cache", e)
                    }
                }.start()
            }
            
            Log.d(TAG, "Background anti-pinning cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during background anti-pinning cleanup", e)
        }
    }

    /**
     * 完全なpinning非推奨対応クリーンアップ
     */
    private fun performCompleteAntiPinningCleanup() {
        Log.d(TAG, "Performing complete anti-pinning cleanup")
        
        try {
            // バックグラウンドクリーンアップを実行
            performBackgroundAntiPinningCleanup()
            
            // より徹底的なクリーンアップ
            memoryPressureMonitor.getCacheManager().clearCache()
            
            // システム統合を再有効化
            memoryPressureMonitor.enableSystemIntegration()
            
            // 緊急時のGC（Android Q+対応）
            if (isAndroidQPlus()) {
                System.gc()
                Log.d(TAG, "Android Q+ GC completed")
            }
            
            Log.d(TAG, "Complete anti-pinning cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during complete anti-pinning cleanup", e)
        }
    }

    /**
     * 実行中のpinning非推奨対応クリーンアップ（UI影響最小限）
     */
    private fun performRunningAntiPinningCleanup() {
        Log.d(TAG, "Performing running anti-pinning cleanup")
        
        try {
            // UIに影響を与えない軽度なクリーンアップ
            memoryPressureMonitor.getCacheManager().clearCache() // キャッシュクリア
            
            // MemoryPressureMonitorの監視を開始
            if (!memoryPressureMonitor.isMonitoring()) {
                memoryPressureMonitor.startMonitoring()
            }
            
            // Glideの軽度クリーンアップ（UIスレッドで安全）
            Glide.get(this).clearMemory()
            
            Log.d(TAG, "Running anti-pinning cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during running anti-pinning cleanup", e)
        }
    }

    /**
     * 緊急メモリクリーンアップ（pinning非推奨対応）
     */
    private fun performEmergencyMemoryCleanup() {
        Log.w(TAG, "Performing emergency anti-pinning memory cleanup")
        
        try {
            // 最も徹底的なクリーンアップを実行
            performCompleteAntiPinningCleanup()
            
            // MemoryPressureMonitorを緊急モードで起動
            if (!memoryPressureMonitor.isMonitoring()) {
                memoryPressureMonitor.startMonitoring()
            }
            
            Log.w(TAG, "Emergency anti-pinning memory cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during emergency anti-pinning memory cleanup", e)
        }
    }
}