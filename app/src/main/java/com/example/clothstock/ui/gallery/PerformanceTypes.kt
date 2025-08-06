package com.example.clothstock.ui.gallery

/**
 * メモリプレッシャーレベル
 */
enum class MemoryPressureLevel {
    LOW,    // 通常状態
    MEDIUM, // 中程度のメモリ使用
    HIGH    // 高メモリ使用状態
}

/**
 * 画像品質設定
 */
enum class ImageQuality {
    HIGH,   // 高品質
    MEDIUM, // 中品質
    LOW     // 低品質（メモリ節約）
}

/**
 * メモリプレッシャー監視インターフェース
 */
interface MemoryPressureMonitor {
    fun getCurrentMemoryUsage(): Float
    fun isMemoryPressureHigh(): Boolean
}

/**
 * データベースクエリ分析結果
 */
data class QueryAnalysisResult(
    val usesIndex: Boolean,
    val estimatedRows: Int,
    val executionTimeMs: Long
)

/**
 * データベースクエリ分析インターフェース
 */
interface DatabaseQueryAnalyzer {
    fun analyzeQuery(query: String): QueryAnalysisResult
}