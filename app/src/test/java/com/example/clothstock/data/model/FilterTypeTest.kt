package com.example.clothstock.data.model

import org.junit.Test
import org.junit.Assert.*

class FilterTypeTest {

    @Test
    fun `FilterType enum has correct values`() {
        val values = FilterType.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(FilterType.SIZE))
        assertTrue(values.contains(FilterType.COLOR))
        assertTrue(values.contains(FilterType.CATEGORY))
        assertTrue(values.contains(FilterType.SEARCH))
    }

    @Test
    fun `FilterType valueOf works correctly`() {
        assertEquals(FilterType.SIZE, FilterType.valueOf("SIZE"))
        assertEquals(FilterType.COLOR, FilterType.valueOf("COLOR"))
        assertEquals(FilterType.CATEGORY, FilterType.valueOf("CATEGORY"))
        assertEquals(FilterType.SEARCH, FilterType.valueOf("SEARCH"))
    }
}