package com.example.clothstock.util

import android.content.Context
import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * メモリプレッシャー監視クラス (Android Q+対応統合版)
 * 
 * システムレベルのメモリ管理コールバックと連携し、
 * pinning非推奨警告の解決に貢献する現代的メモリ管理を提供
 */
class MemoryPressureMonitor private constructor(private val context: Context) {

    companion object {
        private const val TAG = "MemoryPressureMonitor"
        private const val MONITORING_INTERVAL_MS = 100L // 監視間隔100ms
        
        @Volatile
        private var INSTANCE: MemoryPressureMonitor? = null

        fun getInstance(context: Context): MemoryPressureMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MemoryPressureMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var isMonitoring = false
    private var simulatedMemoryMB: Int? = null
    private var pressureCallback: PressureCallback? = null
    private var hasTriggeredAutoCleanup = false
    private var hasTriggeredGC = false
    private val cacheManager = SimpleCacheManager()
    
    // Android Q+対応: システム統合関連
    private var isSystemIntegrationEnabled = false
    private var lastSystemTrimLevel: Int? = null
    private var systemTrimCount = 0

    /**
     * メモリプレッシャーレベル
     */
    enum class PressureLevel {
        LOW,        // 余裕あり
        MODERATE,   // 普通
        HIGH,       // 高負荷
        CRITICAL    // 危険
    }

    /**
     * 監視開始（Phase 2 REFACTOR - 改善実装）
     */
    fun startMonitoring(): Boolean {
        isMonitoring = true
        
        // バックグラウンドでの継続監視を開始
        Thread {
            monitoringLoop()
        }.start()
        
        return true
    }

    /**
     * 監視停止（最小実装）
     */
    fun stopMonitoring() {
        isMonitoring = false
    }

    /**
     * 監視状態の確認
     */
    fun isMonitoring(): Boolean = isMonitoring

    /**
     * 現在のプレッシャーレベルを取得（最小実装）
     */
    fun getCurrentPressureLevel(): PressureLevel {
        val availableMemoryMB = getAvailableMemoryMB()
        
        return when {
            availableMemoryMB > 400 -> PressureLevel.LOW
            availableMemoryMB > 150 -> PressureLevel.MODERATE
            availableMemoryMB > 30 -> PressureLevel.HIGH
            else -> PressureLevel.CRITICAL
        }
    }

    /**
     * メモリプレッシャー状況かチェック
     */
    fun isUnderMemoryPressure(): Boolean {
        val level = getCurrentPressureLevel()
        return level == PressureLevel.HIGH || level == PressureLevel.CRITICAL
    }

    /**
     * 自動クリーンアップが実行されたかチェック
     */
    fun hasTriggeredAutoCleanup(): Boolean = hasTriggeredAutoCleanup

    /**
     * GCが実行されたかチェック
     */
    fun hasTriggeredGarbageCollection(): Boolean = hasTriggeredGC

    /**
     * キャッシュマネージャーを取得
     */
    fun getCacheManager(): SimpleCacheManager = cacheManager

    /**
     * プレッシャーコールバックを設定
     */
    fun setPressureCallback(callback: PressureCallback) {
        this.pressureCallback = callback
    }

    /**
     * テスト用メモリ状態シミュレーション
     */
    fun simulateMemoryState(availableMemoryMB: Int) {
        simulatedMemoryMB = availableMemoryMB
    }

    // ===== 内部メソッド =====

    /**
     * メモリ監視ループの実行
     */
    private fun monitoringLoop() {
        var shouldContinue = true
        
        while (isMonitoring && shouldContinue) {
            try {
                performMonitoringCycle()
                
                // 監視間隔（100ms）
                Thread.sleep(MONITORING_INTERVAL_MS)
                
            } catch (e: Exception) {
                shouldContinue = handleMonitoringException(e)
            }
        }
    }

    /**
     * 監視例外の処理
     * @return 監視を継続する場合はtrue、停止する場合はfalse
     */
    private fun handleMonitoringException(exception: Exception): Boolean {
        return when (exception) {
            is InterruptedException -> {
                // スレッド割り込みを適切に処理
                Thread.currentThread().interrupt()
                Log.d(TAG, "Memory monitoring thread interrupted")
                false // 監視停止
            }
            is SecurityException -> {
                // ActivityManagerへのアクセスが拒否された場合
                Log.w(TAG, "Security exception in memory monitoring", exception)
                pressureCallback?.onCriticalMemoryError()
                false // 監視停止
            }
            is IllegalStateException -> {
                // システム状態の問題（ActivityManagerが無効など）
                Log.w(TAG, "System state exception in memory monitoring", exception)
                pressureCallback?.onCriticalMemoryError()
                false // 監視停止
            }
            is OutOfMemoryError -> {
                // メモリ不足エラー（極端な状況）
                Log.e(TAG, "Out of memory in monitoring thread", exception)
                pressureCallback?.onCriticalMemoryError()
                false // 監視停止
            }
            else -> {
                // その他の予期しない例外は継続可能として扱う
                Log.w(TAG, "Unexpected exception in memory monitoring, continuing", exception)
                true // 監視継続
            }
        }
    }

    /**
     * 単一の監視サイクルを実行
     */
    private fun performMonitoringCycle() {
        val currentLevel = getCurrentPressureLevel()
        
        // レベル変更のコールバック
        pressureCallback?.onPressureLevelChanged(currentLevel)
        
        // レベル別の対応処理
        when (currentLevel) {
            PressureLevel.HIGH -> {
                triggerAutoCleanup()
                pressureCallback?.onLowMemoryWarning()
            }
            PressureLevel.CRITICAL -> {
                triggerAutoCleanup()
                triggerGarbageCollection()
                pressureCallback?.onCriticalMemoryError()
            }
            else -> {
                // LOW, MODERATEは特別な処理なし
            }
        }
    }

    private fun getAvailableMemoryMB(): Int {
        // テスト用シミュレーション値があればそれを返す
        simulatedMemoryMB?.let { return it }
        
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return (memoryInfo.availMem / (1024 * 1024)).toInt()
    }

    private fun triggerAutoCleanup() {
        hasTriggeredAutoCleanup = true
        cacheManager.clearCache()
    }

    private fun triggerGarbageCollection() {
        hasTriggeredGC = true
        System.gc()
    }

    // ===== Android Q+対応: システム統合メソッド =====

    /**
     * システムレベルメモリ管理との統合を有効化
     */
    fun enableSystemIntegration() {
        isSystemIntegrationEnabled = true
        Log.d(TAG, "System integration enabled for Android Q+ memory management")
    }

    /**
     * システムレベルメモリ管理との統合を無効化
     */
    fun disableSystemIntegration() {
        isSystemIntegrationEnabled = false
        Log.d(TAG, "System integration disabled")
    }

    /**
     * システム統合状態の確認
     */
    fun isSystemIntegrationEnabled(): Boolean = isSystemIntegrationEnabled

    /**
     * システムコールバックからのトリム通知を処理
     * 
     * Application/ActivityのonTrimMemoryから呼び出される
     */
    fun handleSystemTrimMemory(level: Int) {
        if (!isSystemIntegrationEnabled) {
            Log.d(TAG, "System integration disabled, ignoring trim memory level: $level")
            return
        }

        Log.d(TAG, "Handling system trim memory level: $level")
        lastSystemTrimLevel = level
        systemTrimCount++

        // システムトリムレベルを内部プレッシャーレベルにマッピング
        val pressureLevel = mapTrimLevelToPressureLevel(level)
        Log.d(TAG, "Mapped trim level $level to pressure level: $pressureLevel")

        // 対応する処理を実行
        when (pressureLevel) {
            PressureLevel.HIGH -> {
                triggerAutoCleanup()
                pressureCallback?.onLowMemoryWarning()
            }
            PressureLevel.CRITICAL -> {
                triggerAutoCleanup()
                triggerGarbageCollection()
                pressureCallback?.onCriticalMemoryError()
            }
            else -> {
                // LOW, MODERATEは軽度のクリーンアップ
                if (cacheManager.getCacheEntryCount() > 0) {
                    cacheManager.clearCache()
                    Log.d(TAG, "Cleared cache due to system trim memory")
                }
            }
        }

        // コールバック通知
        pressureCallback?.onPressureLevelChanged(pressureLevel)
    }

    /**
     * システム低メモリ警告の処理
     * 
     * Application/ActivityのonLowMemoryから呼び出される
     */
    fun handleSystemLowMemory() {
        if (!isSystemIntegrationEnabled) {
            Log.d(TAG, "System integration disabled, ignoring low memory warning")
            return
        }

        Log.w(TAG, "Handling system low memory warning")
        
        // 緊急クリーンアップを実行
        triggerAutoCleanup()
        triggerGarbageCollection()
        
        // 監視を開始（まだ開始していない場合）
        if (!isMonitoring) {
            startMonitoring()
            Log.d(TAG, "Started monitoring due to system low memory warning")
        }
        
        // コールバック通知
        pressureCallback?.onCriticalMemoryError()
    }

    /**
     * システム統計情報の取得
     */
    fun getSystemIntegrationStats(): SystemIntegrationStats {
        return SystemIntegrationStats(
            isEnabled = isSystemIntegrationEnabled,
            lastTrimLevel = lastSystemTrimLevel,
            trimCount = systemTrimCount
        )
    }

    /**
     * トリムレベルをプレッシャーレベルにマッピング
     */
    private fun mapTrimLevelToPressureLevel(trimLevel: Int): PressureLevel {
        return when (trimLevel) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> PressureLevel.CRITICAL
            
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> PressureLevel.HIGH
            
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> PressureLevel.MODERATE
            
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> PressureLevel.LOW
            
            else -> PressureLevel.LOW
        }
    }

    // ===== インターフェース =====

    interface PressureCallback {
        fun onPressureLevelChanged(level: PressureLevel)
        fun onLowMemoryWarning()
        fun onCriticalMemoryError()
    }

    // ===== データクラス =====

    /**
     * システム統合統計情報
     */
    data class SystemIntegrationStats(
        val isEnabled: Boolean,
        val lastTrimLevel: Int?, 
        val trimCount: Int
    )

    // ===== 内部クラス =====

    class SimpleCacheManager {
        private val cache = ConcurrentHashMap<String, ByteArray>()

        fun addToCache(key: String, data: ByteArray) {
            cache[key] = data
        }

        fun getCurrentCacheSize(): Long {
            return cache.values.sumOf { it.size.toLong() }
        }

        fun clearCache() {
            cache.clear()
        }

        fun removeFromCache(key: String) {
            cache.remove(key)
        }

        fun getCacheEntryCount(): Int = cache.size
    }
}