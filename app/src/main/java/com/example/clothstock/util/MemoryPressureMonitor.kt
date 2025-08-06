package com.example.clothstock.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug

/**
 * メモリプレッシャー監視クラス
 * システムのメモリ使用状況を監視し、適切なメモリ管理を支援
 */
class MemoryPressureMonitor(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    companion object {
        private const val HIGH_MEMORY_THRESHOLD = 0.85f // 85%以上で高メモリ使用
        private const val MEDIUM_MEMORY_THRESHOLD = 0.70f // 70%以上で中程度メモリ使用
    }
    
    /**
     * 現在のメモリ使用率を取得
     * @return メモリ使用率（0.0-1.0）
     */
    fun getCurrentMemoryUsage(): Float {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalMemory = memoryInfo.totalMem
        val availableMemory = memoryInfo.availMem
        val usedMemory = totalMemory - availableMemory
        
        return usedMemory.toFloat() / totalMemory.toFloat()
    }
    
    /**
     * メモリプレッシャーが高いかどうかを判定
     * @return 高メモリプレッシャーの場合true
     */
    fun isMemoryPressureHigh(): Boolean {
        return getCurrentMemoryUsage() >= HIGH_MEMORY_THRESHOLD
    }
    
    /**
     * メモリプレッシャーが中程度かどうかを判定
     * @return 中程度メモリプレッシャーの場合true
     */
    fun isMemoryPressureMedium(): Boolean {
        val usage = getCurrentMemoryUsage()
        return usage >= MEDIUM_MEMORY_THRESHOLD && usage < HIGH_MEMORY_THRESHOLD
    }
    
    /**
     * 現在のメモリプレッシャーレベルを取得
     * @return メモリプレッシャーレベル
     */
    fun getCurrentMemoryPressureLevel(): com.example.clothstock.ui.gallery.MemoryPressureLevel {
        val usage = getCurrentMemoryUsage()
        return when {
            usage >= HIGH_MEMORY_THRESHOLD -> com.example.clothstock.ui.gallery.MemoryPressureLevel.HIGH
            usage >= MEDIUM_MEMORY_THRESHOLD -> com.example.clothstock.ui.gallery.MemoryPressureLevel.MEDIUM
            else -> com.example.clothstock.ui.gallery.MemoryPressureLevel.LOW
        }
    }
    
    /**
     * アプリのヒープメモリ使用量を取得
     * @return ヒープメモリ使用量（バイト）
     */
    fun getHeapMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    /**
     * 利用可能なヒープメモリ量を取得
     * @return 利用可能ヒープメモリ量（バイト）
     */
    fun getAvailableHeapMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    }
    
    /**
     * システムの低メモリ状態かどうかを判定
     * @return 低メモリ状態の場合true
     */
    fun isLowMemory(): Boolean {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.lowMemory
    }
    
    /**
     * ガベージコレクションを実行
     * 注意: 通常は自動で実行されるため、緊急時のみ使用
     */
    fun forceGarbageCollection() {
        System.gc()
    }
}