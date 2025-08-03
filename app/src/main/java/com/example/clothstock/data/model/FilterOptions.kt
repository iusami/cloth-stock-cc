package com.example.clothstock.data.model

data class FilterOptions(
    val availableSizes: List<Int> = emptyList(),
    val availableColors: List<String> = emptyList(),
    val availableCategories: List<String> = emptyList()
) {
    
    /**
     * Returns true if all filter option lists are empty.
     * Computed property for better performance than lazy in data class.
     */
    fun isEmpty(): Boolean = 
        availableSizes.isEmpty() && availableColors.isEmpty() && availableCategories.isEmpty()
    
    /**
     * Returns the total number of available filter options.
     * Useful for UI display and analytics.
     */
    fun getTotalOptionsCount(): Int = availableSizes.size + availableColors.size + availableCategories.size
    
    /**
     * Returns whether any filter type has options available.
     * More semantic than !isEmpty().
     */
    fun hasAnyOptions(): Boolean = !isEmpty()
    
    /**
     * Creates a copy with sorted lists for consistent display order.
     */
    fun sorted(): FilterOptions = copy(
        availableSizes = availableSizes.sorted(),
        availableColors = availableColors.sorted(),
        availableCategories = availableCategories.sorted()
    )
}
