package com.example.clothstock.data.database

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.system.measureTimeMillis

/**
 * データベースパフォーマンス監視ツール
 * 
 * クエリの実行時間とデータベース操作の統計を収集
 */
object DatabasePerformanceMonitor {
    
    private const val TAG = "DatabasePerformance"
    private const val SLOW_QUERY_THRESHOLD_MS = 100L
    
    private val queryStatistics = mutableMapOf<String, QueryStats>()
    
    /**
     * クエリ統計情報
     */
    data class QueryStats(
        var executionCount: Int = 0,
        var totalExecutionTime: Long = 0L,
        var minExecutionTime: Long = Long.MAX_VALUE,
        var maxExecutionTime: Long = 0L,
        var slowQueryCount: Int = 0
    ) {
        val averageExecutionTime: Double
            get() = if (executionCount > 0) totalExecutionTime.toDouble() / executionCount else 0.0
    }
    
    /**
     * suspend関数の実行時間を測定
     */
    suspend fun <T> measureSuspend(
        operationName: String,
        operation: suspend () -> T
    ): T {
        lateinit var result: T
        val executionTime = measureTimeMillis {
            result = operation()
        }
        
        recordQueryExecution(operationName, executionTime)
        
        if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
            Log.w(TAG, "遅いクエリ検出: $operationName (${executionTime}ms)")
        }
        
        return result
    }
    
    /**
     * Flowの各要素に対してログ出力のみ実行
     * 注意: Flowは非同期でコールドストリームのため、正確な実行時間測定は困難
     */
    fun <T> measureFlow(
        operationName: String,
        flow: Flow<T>
    ): Flow<T> {
        return flow
            .onEach { 
                Log.d(TAG, "Flow データ取得: $operationName")
            }
            // 実行時間の記録は削除（不正確な統計データを避けるため）
    }
    
    /**
     * クエリ実行統計を記録
     */
    private fun recordQueryExecution(operationName: String, executionTime: Long) {
        val stats = queryStatistics.getOrPut(operationName) { QueryStats() }
        
        stats.executionCount++
        stats.totalExecutionTime += executionTime
        stats.minExecutionTime = minOf(stats.minExecutionTime, executionTime)
        stats.maxExecutionTime = maxOf(stats.maxExecutionTime, executionTime)
        
        if (executionTime > SLOW_QUERY_THRESHOLD_MS) {
            stats.slowQueryCount++
        }
        
        Log.d(TAG, "クエリ実行: $operationName (${executionTime}ms, 平均: ${String.format("%.2f", stats.averageExecutionTime)}ms)")
    }
    
    /**
     * 統計情報を取得
     */
    fun getStatistics(): Map<String, QueryStats> {
        return queryStatistics.toMap()
    }
    
    /**
     * 統計情報をリセット
     */
    fun resetStatistics() {
        queryStatistics.clear()
    }
    
    /**
     * パフォーマンスレポートを生成
     */
    fun generatePerformanceReport(): String {
        val report = StringBuilder()
        report.appendLine("=== データベースパフォーマンスレポート ===")
        
        if (queryStatistics.isEmpty()) {
            report.appendLine("統計データがありません")
            return report.toString()
        }
        
        queryStatistics.entries
            .sortedByDescending { it.value.averageExecutionTime }
            .forEach { (operation, stats) ->
                report.appendLine("操作: $operation")
                report.appendLine("  実行回数: ${stats.executionCount}")
                report.appendLine("  平均実行時間: ${String.format("%.2f", stats.averageExecutionTime)}ms")
                report.appendLine("  最小実行時間: ${stats.minExecutionTime}ms")
                report.appendLine("  最大実行時間: ${stats.maxExecutionTime}ms")
                report.appendLine("  遅いクエリ数: ${stats.slowQueryCount}")
                report.appendLine("  総実行時間: ${stats.totalExecutionTime}ms")
                report.appendLine("")
            }
        
        return report.toString()
    }
    
    /**
     * 遅いクエリを検出してログ出力
     */
    fun detectSlowQueries(): List<String> {
        return queryStatistics.entries
            .filter { it.value.slowQueryCount > 0 }
            .map { "${it.key}: ${it.value.slowQueryCount}回の遅いクエリ" }
    }
    
    /**
     * メモリ使用量を監視
     */
    fun logMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        Log.d(TAG, "メモリ使用量: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB")
    }
}