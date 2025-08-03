package com.example.clothstock.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * フィルター状態を表すデータクラス
 * 現在適用されているフィルター条件を保持する
 */
@Parcelize
data class FilterState(
    val sizeFilters: Set<Int> = emptySet(),
    val colorFilters: Set<String> = emptySet(),
    val categoryFilters: Set<String> = emptySet(),
    val searchText: String = ""
) : Parcelable {
    
    /**
     * アクティブなフィルターが存在するかチェック
     */
    fun hasActiveFilters(): Boolean = 
        sizeFilters.isNotEmpty() || colorFilters.isNotEmpty() || 
        categoryFilters.isNotEmpty() || searchText.isNotBlank()
    
    /**
     * アクティブなフィルターの数を取得
     */
    fun getActiveFilterCount(): Int = 
        sizeFilters.size + colorFilters.size + categoryFilters.size + 
        if (searchText.isNotBlank()) 1 else 0
        
    /**
     * 表示用の文字列を生成
     */
    fun toDisplayString(): String {
        val parts = mutableListOf<String>()
        if (sizeFilters.isNotEmpty()) {
            parts.add("サイズ: ${sizeFilters.sorted().joinToString(", ")}")
        }
        if (colorFilters.isNotEmpty()) {
            parts.add("色: ${colorFilters.joinToString(", ")}")
        }
        if (categoryFilters.isNotEmpty()) {
            parts.add("カテゴリ: ${categoryFilters.joinToString(", ")}")
        }
        if (searchText.isNotBlank()) {
            parts.add("検索: \"$searchText\"")
        }
        return parts.joinToString(" | ")
    }
}
