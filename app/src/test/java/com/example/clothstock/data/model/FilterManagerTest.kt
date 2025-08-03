package com.example.clothstock.data.model

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * FilterManagerクラスのユニットテスト
 * TDD Red-Green-Refactorサイクルに従って実装
 */
class FilterManagerTest {

    private lateinit var filterManager: FilterManager

    @Before
    fun setUp() {
        filterManager = FilterManager()
    }

    // RED: FilterManager.updateFilter method with different filter types
    @Test
    fun `updateFilter should update size filters correctly`() {
        // Given
        val sizeValues = setOf("100", "110", "120")
        
        // When
        val result = filterManager.updateFilter(FilterType.SIZE, sizeValues)
        
        // Then
        assertEquals(setOf(100, 110, 120), result.sizeFilters)
        assertTrue(result.colorFilters.isEmpty())
        assertTrue(result.categoryFilters.isEmpty())
        assertEquals("", result.searchText)
    }

    @Test
    fun `updateFilter should update color filters correctly`() {
        // Given
        val colorValues = setOf("赤", "青", "緑")
        
        // When
        val result = filterManager.updateFilter(FilterType.COLOR, colorValues)
        
        // Then
        assertEquals(setOf("赤", "青", "緑"), result.colorFilters)
        assertTrue(result.sizeFilters.isEmpty())
        assertTrue(result.categoryFilters.isEmpty())
        assertEquals("", result.searchText)
    }

    @Test
    fun `updateFilter should update category filters correctly`() {
        // Given
        val categoryValues = setOf("トップス", "ボトムス")
        
        // When
        val result = filterManager.updateFilter(FilterType.CATEGORY, categoryValues)
        
        // Then
        assertEquals(setOf("トップス", "ボトムス"), result.categoryFilters)
        assertTrue(result.sizeFilters.isEmpty())
        assertTrue(result.colorFilters.isEmpty())
        assertEquals("", result.searchText)
    }

    @Test
    fun `updateFilter should preserve existing filters when updating different type`() {
        // Given
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        
        // When
        val result = filterManager.updateFilter(FilterType.COLOR, setOf("赤"))
        
        // Then
        assertEquals(setOf(100), result.sizeFilters)
        assertEquals(setOf("赤"), result.colorFilters)
    }

    // RED: removeFilter method tests
    @Test
    fun `removeFilter should remove specific size filter`() {
        // Given
        filterManager.updateFilter(FilterType.SIZE, setOf("100", "110", "120"))
        
        // When
        val result = filterManager.removeFilter(FilterType.SIZE, "110")
        
        // Then
        assertEquals(setOf(100, 120), result.sizeFilters)
    }

    @Test
    fun `removeFilter should remove specific color filter`() {
        // Given
        filterManager.updateFilter(FilterType.COLOR, setOf("赤", "青", "緑"))
        
        // When
        val result = filterManager.removeFilter(FilterType.COLOR, "青")
        
        // Then
        assertEquals(setOf("赤", "緑"), result.colorFilters)
    }

    @Test
    fun `removeFilter should remove specific category filter`() {
        // Given
        filterManager.updateFilter(FilterType.CATEGORY, setOf("トップス", "ボトムス", "アウター"))
        
        // When
        val result = filterManager.removeFilter(FilterType.CATEGORY, "ボトムス")
        
        // Then
        assertEquals(setOf("トップス", "アウター"), result.categoryFilters)
    }

    // RED: clearFilter method tests
    @Test
    fun `clearFilter should clear all size filters`() {
        // Given
        filterManager.updateFilter(FilterType.SIZE, setOf("100", "110"))
        filterManager.updateFilter(FilterType.COLOR, setOf("赤"))
        
        // When
        val result = filterManager.clearFilter(FilterType.SIZE)
        
        // Then
        assertTrue(result.sizeFilters.isEmpty())
        assertEquals(setOf("赤"), result.colorFilters)
    }

    @Test
    fun `clearFilter should clear all color filters`() {
        // Given
        filterManager.updateFilter(FilterType.COLOR, setOf("赤", "青"))
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        
        // When
        val result = filterManager.clearFilter(FilterType.COLOR)
        
        // Then
        assertTrue(result.colorFilters.isEmpty())
        assertEquals(setOf(100), result.sizeFilters)
    }

    @Test
    fun `clearFilter should clear all category filters`() {
        // Given
        filterManager.updateFilter(FilterType.CATEGORY, setOf("トップス", "ボトムス"))
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        
        // When
        val result = filterManager.clearFilter(FilterType.CATEGORY)
        
        // Then
        assertTrue(result.categoryFilters.isEmpty())
        assertEquals(setOf(100), result.sizeFilters)
    }

    // RED: clearAllFilters method tests
    @Test
    fun `clearAllFilters should clear all filters and search text`() {
        // Given
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        filterManager.updateFilter(FilterType.COLOR, setOf("赤"))
        filterManager.updateFilter(FilterType.CATEGORY, setOf("トップス"))
        filterManager.updateSearchText("テスト")
        
        // When
        val result = filterManager.clearAllFilters()
        
        // Then
        assertTrue(result.sizeFilters.isEmpty())
        assertTrue(result.colorFilters.isEmpty())
        assertTrue(result.categoryFilters.isEmpty())
        assertEquals("", result.searchText)
    }

    // RED: updateSearchText method tests
    @Test
    fun `updateSearchText should update search text correctly`() {
        // Given
        val searchText = "シャツ"
        
        // When
        val result = filterManager.updateSearchText(searchText)
        
        // Then
        assertEquals("シャツ", result.searchText)
    }

    @Test
    fun `updateSearchText should preserve existing filters`() {
        // Given
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        
        // When
        val result = filterManager.updateSearchText("テスト")
        
        // Then
        assertEquals(setOf(100), result.sizeFilters)
        assertEquals("テスト", result.searchText)
    }

    @Test
    fun `updateSearchText should handle empty string`() {
        // Given
        filterManager.updateSearchText("テスト")
        
        // When
        val result = filterManager.updateSearchText("")
        
        // Then
        assertEquals("", result.searchText)
    }

    // RED: getCurrentState method tests
    @Test
    fun `getCurrentState should return current filter state`() {
        // Given
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        filterManager.updateFilter(FilterType.COLOR, setOf("赤"))
        filterManager.updateSearchText("テスト")
        
        // When
        val result = filterManager.getCurrentState()
        
        // Then
        assertEquals(setOf(100), result.sizeFilters)
        assertEquals(setOf("赤"), result.colorFilters)
        assertEquals("テスト", result.searchText)
    }

    @Test
    fun `getCurrentState should return empty state initially`() {
        // When
        val result = filterManager.getCurrentState()
        
        // Then
        assertTrue(result.sizeFilters.isEmpty())
        assertTrue(result.colorFilters.isEmpty())
        assertTrue(result.categoryFilters.isEmpty())
        assertEquals("", result.searchText)
    }

    // REFACTOR: Edge case handling tests
    @Test
    fun `updateFilter should handle empty values set`() {
        // Given
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        
        // When
        val result = filterManager.updateFilter(FilterType.SIZE, emptySet())
        
        // Then
        assertTrue(result.sizeFilters.isEmpty())
    }

    @Test
    fun `updateFilter should handle invalid size values`() {
        // Given
        val invalidSizeValues = setOf("invalid", "abc", "100", "110")
        
        // When
        val result = filterManager.updateFilter(FilterType.SIZE, invalidSizeValues)
        
        // Then
        assertEquals(setOf(100, 110), result.sizeFilters)
    }

    @Test
    fun `removeFilter should handle non-existent value`() {
        // Given
        filterManager.updateFilter(FilterType.COLOR, setOf("赤", "青"))
        
        // When
        val result = filterManager.removeFilter(FilterType.COLOR, "緑")
        
        // Then
        assertEquals(setOf("赤", "青"), result.colorFilters)
    }

    @Test
    fun `removeFilter should handle invalid size value`() {
        // Given
        filterManager.updateFilter(FilterType.SIZE, setOf("100", "110"))
        
        // When
        val result = filterManager.removeFilter(FilterType.SIZE, "invalid")
        
        // Then
        assertEquals(setOf(100, 110), result.sizeFilters)
    }

    @Test
    fun `updateSearchText should handle null-like strings`() {
        // Given
        filterManager.updateSearchText("テスト")
        
        // When
        val result = filterManager.updateSearchText("   ")
        
        // Then
        assertEquals("   ", result.searchText) // Preserve whitespace as-is
    }

    @Test
    fun `multiple operations should maintain state consistency`() {
        // Given & When
        filterManager.updateFilter(FilterType.SIZE, setOf("100", "110"))
        filterManager.updateFilter(FilterType.COLOR, setOf("赤"))
        filterManager.updateSearchText("テスト")
        filterManager.removeFilter(FilterType.SIZE, "100")
        val result = filterManager.getCurrentState()
        
        // Then
        assertEquals(setOf(110), result.sizeFilters)
        assertEquals(setOf("赤"), result.colorFilters)
        assertEquals("テスト", result.searchText)
    }

    @Test
    fun `clearFilter should not affect other filter types`() {
        // Given
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        filterManager.updateFilter(FilterType.COLOR, setOf("赤"))
        filterManager.updateFilter(FilterType.CATEGORY, setOf("トップス"))
        filterManager.updateSearchText("テスト")
        
        // When
        val result = filterManager.clearFilter(FilterType.COLOR)
        
        // Then
        assertEquals(setOf(100), result.sizeFilters)
        assertTrue(result.colorFilters.isEmpty())
        assertEquals(setOf("トップス"), result.categoryFilters)
        assertEquals("テスト", result.searchText)
    }

    // Additional tests for refactored methods
    @Test
    fun `setState should update current state correctly`() {
        // Given
        val newState = FilterState(
            sizeFilters = setOf(100, 110),
            colorFilters = setOf("赤", "青"),
            categoryFilters = setOf("トップス"),
            searchText = "テスト"
        )
        
        // When
        val result = filterManager.setState(newState)
        
        // Then
        assertEquals(newState, result)
        assertEquals(newState, filterManager.getCurrentState())
    }

    @Test
    fun `getCurrentState should return copy to maintain immutability`() {
        // Given
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        val state1 = filterManager.getCurrentState()
        
        // When
        val state2 = filterManager.getCurrentState()
        
        // Then
        assertEquals(state1, state2)
        assertNotSame(state1, state2) // Different instances
    }

    @Test
    fun `updateFilter should filter out blank color values`() {
        // Given
        val colorValues = setOf("赤", "", "  ", "青", "\t")
        
        // When
        val result = filterManager.updateFilter(FilterType.COLOR, colorValues)
        
        // Then
        assertEquals(setOf("赤", "青"), result.colorFilters)
    }

    @Test
    fun `updateFilter should filter out blank category values`() {
        // Given
        val categoryValues = setOf("トップス", "", "  ", "ボトムス", "\n")
        
        // When
        val result = filterManager.updateFilter(FilterType.CATEGORY, categoryValues)
        
        // Then
        assertEquals(setOf("トップス", "ボトムス"), result.categoryFilters)
    }

    @Test
    fun `updateFilter should filter out negative and zero size values`() {
        // Given
        val sizeValues = setOf("-10", "0", "100", "110", "-5")
        
        // When
        val result = filterManager.updateFilter(FilterType.SIZE, sizeValues)
        
        // Then
        assertEquals(setOf(100, 110), result.sizeFilters)
    }
}