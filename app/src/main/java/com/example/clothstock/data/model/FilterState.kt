package com.example.clothstock.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FilterState(
    val sizeFilters: Set<Int> = emptySet(),
    val colorFilters: Set<String> = emptySet(),
    val categoryFilters: Set<String> = emptySet(),
    val searchText: String = ""
) : Parcelable {
    
    init {
        require(sizeFilters.all { it > 0 }) { "All sizes must be positive" }
        require(colorFilters.all { it.isNotBlank() }) { "Color filters cannot be blank" }
        require(categoryFilters.all { it.isNotBlank() }) { "Category filters cannot be blank" }
    }
    
    fun hasActiveFilters(): Boolean = 
        sizeFilters.isNotEmpty() || colorFilters.isNotEmpty() || 
        categoryFilters.isNotEmpty() || searchText.isNotBlank()
    
    fun getActiveFilterCount(): Int = 
        sizeFilters.size + colorFilters.size + categoryFilters.size + 
        if (searchText.isNotBlank()) 1 else 0
        
    fun toDisplayString(): String {
        if (!hasActiveFilters()) return ""
        
        val parts = buildList {
            if (sizeFilters.isNotEmpty()) {
                add("サイズ: ${sizeFilters.sorted().joinToString(", ")}")
            }
            if (colorFilters.isNotEmpty()) {
                add("色: ${colorFilters.sorted().joinToString(", ")}")
            }
            if (categoryFilters.isNotEmpty()) {
                add("カテゴリ: ${categoryFilters.sorted().joinToString(", ")}")
            }
            if (searchText.isNotBlank()) {
                add("検索: \"${searchText.replace("\"", "\\\"")}\"")
            }
        }
        return parts.joinToString(" | ")
    }
}
