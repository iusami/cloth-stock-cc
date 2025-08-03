package com.example.clothstock.data.model

/**
 * Enum representing different types of filters available in the gallery.
 * Used to categorize and manage filter operations throughout the application.
 * 
 * Each filter type has specific characteristics:
 * - SIZE: Numeric filters for clothing sizes (60-160)
 * - COLOR: String-based color filters (supports Japanese color names)
 * - CATEGORY: String-based category filters (トップス, ボトムス, etc.)
 * - SEARCH: Text-based search across multiple fields
 */
enum class FilterType {
    /** 
     * Filter by clothing size (e.g., 100, 110, 120)
     * Supports sizes from 60 to 160 in increments of 10
     */
    SIZE,
    
    /** 
     * Filter by clothing color (e.g., 赤, 青, 緑)
     * Supports both Japanese and English color names
     */
    COLOR,
    
    /** 
     * Filter by clothing category (e.g., トップス, ボトムス)
     * Categories are user-defined and can be customized
     */
    CATEGORY,
    
    /** 
     * Filter by search text across multiple fields
     * Searches through color, category, and notes fields
     */
    SEARCH;
    
    /**
     * Returns a localized display name for the filter type.
     * Useful for UI components that need to show filter type names.
     * Cached for performance optimization.
     */
    fun getDisplayName(): String = displayNameCache
    
    private val displayNameCache: String by lazy {
        when (this) {
            SIZE -> "サイズ"
            COLOR -> "色"
            CATEGORY -> "カテゴリ"
            SEARCH -> "検索"
        }
    }
    
    /**
     * Returns whether this filter type supports multiple selections.
     * SEARCH type only supports single value, others support multiple.
     */
    fun supportsMultipleSelection(): Boolean = this != SEARCH
}
