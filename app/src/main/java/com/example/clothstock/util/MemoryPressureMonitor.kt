package com.example.clothstock.util

import android.content.Context
import android.app.ActivityManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * メモリプレッシャー監視クラス（TDD GREEN段階 - 最小実装）
 */
class MemoryPressureMonitor private constructor(private val context: Context) {

    companion object {
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
     * 監視開始（最小実装）
     */
    fun startMonitoring(): Boolean {
        isMonitoring = true
        
        // 現在のプレッシャーレベルをチェックして必要に応じて対応
        val currentLevel = getCurrentPressureLevel()
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
        
        pressureCallback?.onPressureLevelChanged(currentLevel)
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

    // ===== インターフェース =====

    interface PressureCallback {
        fun onPressureLevelChanged(level: PressureLevel)
        fun onLowMemoryWarning()
        fun onCriticalMemoryError()
    }

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