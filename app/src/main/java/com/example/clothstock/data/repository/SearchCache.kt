package com.example.clothstock.data.repository

import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.FilterState
import com.example.clothstock.ui.gallery.MemoryPressureLevel
import java.util.LinkedHashMap

/**
 * 検索結果キャッシュクラス
 * 
 * LRUキャッシュベースで頻繁に使用されるフィルター組み合わせの
 * 検索結果をキャッシュし、パフォーマンス向上を図る
 */
class SearchCache(private val maxSize: Int = 10) {
    
    companion object {
        private const val MEMORY_PRESSURE_SIZE_REDUCTION_FACTOR = 3
        private const val INITIAL_CAPACITY = 16
        private const val LOAD_FACTOR = 0.75f
    }
    
    // LRUキャッシュ実装
    private val cache = object : LinkedHashMap<String, List<ClothItem>>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<ClothItem>>?): Boolean {
            return size > maxSize
        }
    }
    
    // 統計情報
    private var hitCount = 0
    private var missCount = 0
    
    /**
     * FilterStateに基づいてユニークなキーを生成
     * フィルターの順序に関係なく同じキーを生成する
     */
    fun generateKey(filterState: FilterState): String {
        val sizeFiltersStr = filterState.sizeFilters.sorted().joinToString(",")
        val colorFiltersStr = filterState.colorFilters.sorted().joinToString(",")
        val categoryFiltersStr = filterState.categoryFilters.sorted().joinToString(",")
        val searchText = filterState.searchText.trim().lowercase()
        
        return "s:$sizeFiltersStr|c:$colorFiltersStr|cat:$categoryFiltersStr|txt:$searchText"
    }
    
    /**
     * 検索結果をキャッシュに保存
     */
    fun put(filterState: FilterState, items: List<ClothItem>) {
        val key = generateKey(filterState)
        synchronized(cache) {
            cache[key] = items
        }
    }
    
    /**
     * キャッシュから検索結果を取得
     * @return キャッシュにある場合は結果、ない場合はnull
     */
    fun get(filterState: FilterState): List<ClothItem>? {
        val key = generateKey(filterState)
        synchronized(cache) {
            val result = cache[key]
            if (result != null) {
                hitCount++
            } else {
                missCount++
            }
            return result
        }
    }
    
    /**
     * キャッシュサイズを取得
     */
    fun size(): Int = synchronized(cache) { cache.size }
    
    /**
     * キャッシュヒット数を取得（統計用）
     */
    fun getHitCount(): Int = hitCount
    
    /**
     * キャッシュミス数を取得（統計用）
     */
    fun getMissCount(): Int = missCount
    
    /**
     * キャッシュヒット率を取得（統計用）
     * @return 0.0-1.0の範囲でのヒット率
     */
    fun getHitRate(): Float {
        val total = hitCount + missCount
        return if (total == 0) 0f else hitCount.toFloat() / total
    }
    
    /**
     * キャッシュをクリア
     */
    fun clear() {
        synchronized(cache) {
            cache.clear()
        }
    }
    
    /**
     * メモリプレッシャー時の処理
     * キャッシュサイズを削減してメモリ使用量を抑制
     */
    fun onMemoryPressure(level: MemoryPressureLevel = MemoryPressureLevel.HIGH) {
        synchronized(cache) {
            when (level) {
                MemoryPressureLevel.HIGH -> {
                    // 高負荷時：キャッシュサイズを1/3に削減
                    val targetSize = maxSize / MEMORY_PRESSURE_SIZE_REDUCTION_FACTOR
                    while (cache.size > targetSize && cache.isNotEmpty()) {
                        cache.remove(cache.keys.first())
                    }
                }
                MemoryPressureLevel.MEDIUM -> {
                    // 中程度負荷時：最古のエントリを半分削除
                    val targetSize = cache.size / 2
                    while (cache.size > targetSize && cache.isNotEmpty()) {
                        cache.remove(cache.keys.first())
                    }
                }
                MemoryPressureLevel.LOW -> {
                    // 低負荷時：特に処理しない
                }
            }
        }
    }
    
    /**
     * 統計情報をリセット
     */
    fun resetStats() {
        hitCount = 0
        missCount = 0
    }
}
