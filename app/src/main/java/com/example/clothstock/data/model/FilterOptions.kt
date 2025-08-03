package com.example.clothstock.data.model

data class FilterOptions(
    val availableSizes: List<Int> = emptyList(),
    val availableColors: List<String> = emptyList(),
    val availableCategories: List<String> = emptyList()
) {
    fun isEmpty(): Boolean = 
        availableSizes.isEmpty() && availableColors.isEmpty() && availableCategories.isEmpty()
}