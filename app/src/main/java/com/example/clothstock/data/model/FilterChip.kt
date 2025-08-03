package com.example.clothstock.data.model

data class FilterChip(
    val type: FilterType,
    val value: String,
    val displayText: String,
    val isSelected: Boolean = false
) {
    companion object {
        fun createSizeChip(size: Int, isSelected: Boolean = false) = FilterChip(
            type = FilterType.SIZE,
            value = size.toString(),
            displayText = "サイズ$size",
            isSelected = isSelected
        )
        
        fun createColorChip(color: String, isSelected: Boolean = false) = FilterChip(
            type = FilterType.COLOR,
            value = color,
            displayText = color,
            isSelected = isSelected
        )
        
        fun createCategoryChip(category: String, isSelected: Boolean = false) = FilterChip(
            type = FilterType.CATEGORY,
            value = category,
            displayText = category,
            isSelected = isSelected
        )
        
        fun createSearchChip(searchText: String) = FilterChip(
            type = FilterType.SEARCH,
            value = searchText,
            displayText = "\"$searchText\"",
            isSelected = true
        )
    }
}