package com.example.clothstock.data.model

import org.junit.Test
import org.junit.Assert.*

class FilterStateTest {

    companion object {
        // Test data constants for better maintainability
        private val SAMPLE_SIZES = setOf(100, 110, 120)
        private val SAMPLE_COLORS = setOf("赤", "青", "緑")
        private val SAMPLE_CATEGORIES = setOf("トップス", "ボトムス")
        private const val SAMPLE_SEARCH_TEXT = "シャツ"
    }

    @Test
    fun `hasActiveFilters returns false when no filters are active`() {
        val filterState = FilterState()
        assertFalse(filterState.hasActiveFilters())
    }

    @Test
    fun `hasActiveFilters returns true when size filters are active`() {
        val filterState = FilterState(sizeFilters = setOf(100, 110))
        assertTrue(filterState.hasActiveFilters())
    }

    @Test
    fun `hasActiveFilters returns true when color filters are active`() {
        val filterState = FilterState(colorFilters = setOf("赤", "青"))
        assertTrue(filterState.hasActiveFilters())
    }

    @Test
    fun `hasActiveFilters returns true when category filters are active`() {
        val filterState = FilterState(categoryFilters = setOf("トップス", "ボトムス"))
        assertTrue(filterState.hasActiveFilters())
    }

    @Test
    fun `hasActiveFilters returns true when search text is active`() {
        val filterState = FilterState(searchText = "シャツ")
        assertTrue(filterState.hasActiveFilters())
    }

    @Test
    fun `getActiveFilterCount returns 0 when no filters are active`() {
        val filterState = FilterState()
        assertEquals(0, filterState.getActiveFilterCount())
    }

    @Test
    fun `getActiveFilterCount returns correct count for size filters only`() {
        val filterState = FilterState(sizeFilters = setOf(100, 110, 120))
        assertEquals(3, filterState.getActiveFilterCount())
    }

    @Test
    fun `getActiveFilterCount returns correct count for color filters only`() {
        val filterState = FilterState(colorFilters = setOf("赤", "青"))
        assertEquals(2, filterState.getActiveFilterCount())
    }

    @Test
    fun `getActiveFilterCount returns correct count for category filters only`() {
        val filterState = FilterState(categoryFilters = setOf("トップス"))
        assertEquals(1, filterState.getActiveFilterCount())
    }

    @Test
    fun `getActiveFilterCount returns 1 for search text`() {
        val filterState = FilterState(searchText = "シャツ")
        assertEquals(1, filterState.getActiveFilterCount())
    }

    @Test
    fun `getActiveFilterCount returns correct total count for multiple filter types`() {
        val filterState = FilterState(
            sizeFilters = setOf(100, 110),
            colorFilters = setOf("赤"),
            categoryFilters = setOf("トップス", "ボトムス"),
            searchText = "シャツ"
        )
        assertEquals(6, filterState.getActiveFilterCount()) // 2 + 1 + 2 + 1
    }

    @Test
    fun `toDisplayString returns empty string when no filters are active`() {
        val filterState = FilterState()
        assertEquals("", filterState.toDisplayString())
    }

    @Test
    fun `toDisplayString returns correct format for size filters only`() {
        val filterState = FilterState(sizeFilters = setOf(110, 100, 120))
        assertEquals("サイズ: 100, 110, 120", filterState.toDisplayString())
    }

    @Test
    fun `toDisplayString returns correct format for color filters only`() {
        val filterState = FilterState(colorFilters = setOf("青", "赤"))
        assertEquals("色: 青, 赤", filterState.toDisplayString()) // Sorted alphabetically
    }

    @Test
    fun `toDisplayString returns correct format for category filters only`() {
        val filterState = FilterState(categoryFilters = setOf("ボトムス", "トップス"))
        assertEquals("カテゴリ: ボトムス, トップス", filterState.toDisplayString()) // Sorted alphabetically
    }

    @Test
    fun `toDisplayString returns correct format for search text only`() {
        val filterState = FilterState(searchText = "シャツ")
        assertEquals("検索: \"シャツ\"", filterState.toDisplayString())
    }

    @Test
    fun `toDisplayString returns correct format for multiple filter types`() {
        val filterState = FilterState(
            sizeFilters = setOf(100, 110),
            colorFilters = setOf("赤"),
            categoryFilters = setOf("トップス"),
            searchText = "シャツ"
        )
        val expected = "サイズ: 100, 110 | 色: 赤 | カテゴリ: トップス | 検索: \"シャツ\""
        assertEquals(expected, filterState.toDisplayString())
    }

    // Edge cases for better test coverage
    @Test
    fun `hasActiveFilters returns false when search text is blank`() {
        val filterState = FilterState(searchText = "   ")
        assertFalse(filterState.hasActiveFilters())
    }

    @Test
    fun `hasActiveFilters returns false when search text is empty`() {
        val filterState = FilterState(searchText = "")
        assertFalse(filterState.hasActiveFilters())
    }

    @Test
    fun `getActiveFilterCount returns 0 when search text is blank`() {
        val filterState = FilterState(searchText = "   ")
        assertEquals(0, filterState.getActiveFilterCount())
    }

    @Test
    fun `toDisplayString handles empty sets correctly`() {
        val filterState = FilterState(
            sizeFilters = emptySet(),
            colorFilters = emptySet(),
            categoryFilters = emptySet(),
            searchText = ""
        )
        assertEquals("", filterState.toDisplayString())
    }

    @Test
    fun `toDisplayString handles single item sets correctly`() {
        val filterState = FilterState(
            sizeFilters = setOf(100),
            colorFilters = setOf("赤"),
            categoryFilters = setOf("トップス")
        )
        val expected = "サイズ: 100 | 色: 赤 | カテゴリ: トップス"
        assertEquals(expected, filterState.toDisplayString())
    }

    @Test
    fun `toDisplayString handles special characters in search text`() {
        val filterState = FilterState(searchText = "シャツ \"特別\"")
        assertEquals("検索: \"シャツ \\\"特別\\\"\"", filterState.toDisplayString())
    }
}