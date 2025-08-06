package com.example.clothstock.data.repository

import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.FilterState
import java.util.LinkedHashMap

/**
 * 検索結果のキャッシュクラス
 * LRU (Least Recently Used) アルゴリズムを使用
 */
class SearchCache(val maxSize: Int) {
    
    private val cache = object : LinkedHashMap<String, List<ClothItem>>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<ClothItem>>?): Boolean {
            return size > maxSize
        }
    }
    
    private var hitCount = 0
    private var missCount = 0
    
    /**
     * キャッシュからアイテムを取得
     */
    fun get(filterState: FilterState): List<ClothItem>? {
        val key = generateKey(filterState)
        val result = cache[key]
        
        if (result != null) {
            hitCount++
        } else {
            missCount++
        }
        
        return result
    }
    
    /**
     * キャッシュにアイテムを保存
     */
    fun put(filterState: FilterState, items: List<ClothItem>) {
        val key = generateKey(filterState)
        cache[key] = items
    }
    
    /**
     * FilterStateからキャッシュキーを生成
     * 順序に依存しない一意なキーを生成
     */
    fun generateKey(filterState: FilterState): String {
        val sortedSizes = filterState.sizeFilters.sorted().joinToString(",")
        val sortedColors = filterState.colorFilters.sorted().joinToString(",")
        val sortedCategories = filterState.categoryFilters.sorted().joinToString(",")
        val searchText = filterState.searchText.trim()
        
        return "sizes:$sortedSizes|colors:$sortedColors|categories:$sortedCategories|search:$searchText"
    }
    
    /**
     * メモリプレッシャー時にキャッシュをクリア
     * メモリプレッシャーレベルに応じて異なる戦略を適用
     */
    fun onMemoryPressure() {
        val targetSize = maxSize / 2
        while (cache.size > targetSize && cache.isNotEmpty()) {
            val oldestKey = cache.keys.first()
            cache.remove(oldestKey)
        }
    }
    
    /**
     * メモリプレッシャーレベルに応じたキャッシュクリア
     * @param pressureLevel メモリプレッシャーレベル
     */
    fun onMemoryPressure(pressureLevel: com.example.clothstock.ui.gallery.MemoryPressureLevel) {
        val targetSize = when (pressureLevel) {
            com.example.clothstock.ui.gallery.MemoryPressureLevel.HIGH -> 0 // 全クリア
            com.example.clothstock.ui.gallery.MemoryPressureLevel.MEDIUM -> maxSize / 3 // 2/3削減
            com.example.clothstock.ui.gallery.MemoryPressureLevel.LOW -> maxSize * 2 / 3 // 1/3削減
        }
        
        while (cache.size > targetSize && cache.isNotEmpty()) {
            val oldestKey = cache.keys.first()
            cache.remove(oldestKey)
        }
    }
    
    /**
     * キャッシュサイズを取得
     */
    fun size(): Int = cache.size
    
    /**
     * ヒット数を取得
     */
    fun getHitCount(): Int = hitCount
    
    /**
     * ミス数を取得
     */
    fun getMissCount(): Int = missCount
    
    /**
     * キャッシュをクリア
     */
    fun clear() {
        cache.clear()
        hitCount = 0
        missCount = 0
    }
}