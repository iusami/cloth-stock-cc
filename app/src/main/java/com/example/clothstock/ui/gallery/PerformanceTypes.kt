package com.example.clothstock.ui.gallery

/**
 * パフォーマンス関連の型定義
 * 
 * Task 12で実装するパフォーマンス最適化機能で使用する
 * 列挙型とデータクラスを定義
 */

/**
 * メモリプレッシャーレベル
 * システムのメモリ使用状況を表す
 */
enum class MemoryPressureLevel {
    LOW,     // 余裕あり
    MEDIUM,  // 中程度の負荷
    HIGH     // 高負荷
}

/**
 * 画像品質レベル
 * メモリプレッシャーに応じて画像品質を調整する
 */
enum class ImageQuality {
    HIGH,    // 高品質
    MEDIUM,  // 中品質
    LOW      // 低品質（メモリ節約）
}

/**
 * プログレッシブローディングの状態
 */
data class ProgressiveLoadingState(
    val isLoading: Boolean = false,
    val currentOffset: Int = 0,
    val batchSize: Int = 20,
    val hasMoreData: Boolean = true,
    val isPaused: Boolean = false
)

/**
 * キャッシュ統計情報
 */
data class CacheStats(
    val hitCount: Int = 0,
    val missCount: Int = 0,
    val size: Int = 0,
    val maxSize: Int = 0
) {
    val hitRate: Float
        get() {
            val total = hitCount + missCount
            return if (total == 0) 0f else hitCount.toFloat() / total
        }
}

/**
 * パフォーマンスメトリクス
 * 検索とフィルタリングのパフォーマンス指標
 */
data class PerformanceMetrics(
    val searchDuration: Long = 0L,
    val databaseQueryTime: Long = 0L,
    val cacheStats: CacheStats = CacheStats(),
    val memoryPressureLevel: MemoryPressureLevel = MemoryPressureLevel.LOW,
    val imageQuality: ImageQuality = ImageQuality.HIGH
)
