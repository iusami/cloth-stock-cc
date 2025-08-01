package com.example.clothstock.util

import android.app.ActivityManager
import android.content.Context

/**
 * デバイスメモリ分析クラス（TDD GREEN段階 - 最小実装）
 */
class DeviceMemoryAnalyzer private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: DeviceMemoryAnalyzer? = null

        fun getInstance(context: Context): DeviceMemoryAnalyzer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeviceMemoryAnalyzer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var simulatedMemoryGB: Int? = null

    /**
     * 総メモリ量をGB単位で取得（最小実装）
     */
    fun getTotalMemoryGB(): Int {
        // テスト用のシミュレーション値があればそれを返す
        simulatedMemoryGB?.let { return it }
        
        // 実際のデバイスメモリを取得
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        // バイトからGBに変換
        val totalMemoryGB = (memoryInfo.totalMem / (1024 * 1024 * 1024)).toInt()
        
        // 最小2GB、最大16GBの範囲で制限
        return totalMemoryGB.coerceIn(2, 16)
    }

    /**
     * テスト用のメモリシミュレーション（最小実装）
     */
    fun simulateDeviceMemory(memoryGB: Int) {
        simulatedMemoryGB = memoryGB
    }

    /**
     * 利用可能メモリ量をMB単位で取得（最小実装）
     */
    fun getAvailableMemoryMB(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return memoryInfo.availMem / (1024 * 1024)
    }

    /**
     * メモリ使用率を取得（最小実装）
     */
    fun getMemoryUsageRatio(): Float {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val usedMemory = memoryInfo.totalMem - memoryInfo.availMem
        return usedMemory.toFloat() / memoryInfo.totalMem.toFloat()
    }
}