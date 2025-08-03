package com.example.clothstock.data.model

/**
 * Represents a filter chip UI element with its associated data and state.
 * Used to display and manage individual filter options in the UI.
 * 
 * This class follows the immutable data pattern and provides factory methods
 * for creating different types of filter chips with proper validation.
 * 
 * @property type The type of filter this chip represents
 * @property value The raw value used for filtering operations
 * @property displayText The text shown to the user in the UI
 * @property isSelected Whether this chip is currently selected
 * 
 * @see FilterType for available filter types
 * @see FilterChipBuilder for fluent chip creation
 */
data class FilterChip(
    val type: FilterType,
    val value: String,
    val displayText: String,
    val isSelected: Boolean = false
) {
    
    /**
     * Creates a copy of this chip with the selection state toggled.
     */
    fun toggleSelection(): FilterChip = copy(isSelected = !isSelected)
    
    /**
     * Returns whether this chip can be deselected.
     * Search chips cannot be deselected (they are removed instead).
     */
    fun canBeDeselected(): Boolean = type != FilterType.SEARCH
    
    /**
     * Returns a unique identifier for this chip based on type and value.
     * Useful for RecyclerView DiffUtil and UI state management.
     */
    fun getUniqueId(): String = "${type.name}:$value"
    
    companion object {
        // Size range constants for validation
        private const val MIN_SIZE = 60
        private const val MAX_SIZE = 160
        
        /**
         * Validates and trims text input for color and category chips.
         * @param text The input text to validate
         * @param fieldName The name of the field for error messages
         * @return The trimmed text
         * @throws IllegalArgumentException if text is blank
         */
        private fun validateAndTrimText(text: String, fieldName: String): String {
            require(text.isNotBlank()) { "$fieldName cannot be blank" }
            return text.trim()
        }
        
        /**
         * Creates a filter chip for clothing size.
         * @param size The size value (e.g., 100, 110, 120)
         * @param isSelected Whether the chip is initially selected
         * @throws IllegalArgumentException if size is outside valid range
         */
        fun createSizeChip(size: Int, isSelected: Boolean = false): FilterChip {
            require(size in MIN_SIZE..MAX_SIZE) { "Size must be between $MIN_SIZE and $MAX_SIZE, got: $size" }
            return FilterChip(
                type = FilterType.SIZE,
                value = size.toString(),
                displayText = "サイズ$size",
                isSelected = isSelected
            )
        }
        
        /**
         * Creates a filter chip for clothing color.
         * @param color The color name
         * @param isSelected Whether the chip is initially selected
         * @throws IllegalArgumentException if color is blank
         */
        fun createColorChip(color: String, isSelected: Boolean = false): FilterChip {
            val trimmedColor = validateAndTrimText(color, "Color")
            return FilterChip(
                type = FilterType.COLOR,
                value = trimmedColor,
                displayText = trimmedColor,
                isSelected = isSelected
            )
        }
        
        /**
         * Creates a filter chip for clothing category.
         * @param category The category name
         * @param isSelected Whether the chip is initially selected
         * @throws IllegalArgumentException if category is blank
         */
        fun createCategoryChip(category: String, isSelected: Boolean = false): FilterChip {
            val trimmedCategory = validateAndTrimText(category, "Category")
            return FilterChip(
                type = FilterType.CATEGORY,
                value = trimmedCategory,
                displayText = trimmedCategory,
                isSelected = isSelected
            )
        }
        
        /**
         * Creates a filter chip for search text.
         * Search chips are always selected and cannot be deselected.
         * @param searchText The search query text
         */
        fun createSearchChip(searchText: String): FilterChip {
            val trimmedText = searchText.trim()
            return FilterChip(
                type = FilterType.SEARCH,
                value = trimmedText,
                displayText = "\"$trimmedText\"",
                isSelected = true
            )
        }
        
        /**
         * Creates a filter chip from a FilterType and value.
         * Useful for generic chip creation.
         * @throws IllegalArgumentException if the value is invalid for the given type
         */
        fun create(type: FilterType, value: String, isSelected: Boolean = false): FilterChip {
            return when (type) {
                FilterType.SIZE -> {
                    val size = value.toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid size value: $value")
                    createSizeChip(size, isSelected)
                }
                FilterType.COLOR -> createColorChip(value, isSelected)
                FilterType.CATEGORY -> createCategoryChip(value, isSelected)
                FilterType.SEARCH -> createSearchChip(value)
            }
        }
    }
}
