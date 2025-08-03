package com.example.clothstock.data.model

import org.junit.Test
import org.junit.Assert.*

class FilterOptionsTest {

    @Test
    fun `isEmpty returns true when all lists are empty`() {
        val filterOptions = FilterOptions()
        assertTrue(filterOptions.isEmpty())
    }

    @Test
    fun `isEmpty returns false when sizes list is not empty`() {
        val filterOptions = FilterOptions(availableSizes = listOf(100, 110))
        assertFalse(filterOptions.isEmpty())
    }

    @Test
    fun `isEmpty returns false when colors list is not empty`() {
        val filterOptions = FilterOptions(availableColors = listOf("赤", "青"))
        assertFalse(filterOptions.isEmpty())
    }

    @Test
    fun `isEmpty returns false when categories list is not empty`() {
        val filterOptions = FilterOptions(availableCategories = listOf("トップス", "ボトムス"))
        assertFalse(filterOptions.isEmpty())
    }

    @Test
    fun `isEmpty returns false when multiple lists are not empty`() {
        val filterOptions = FilterOptions(
            availableSizes = listOf(100),
            availableColors = listOf("赤"),
            availableCategories = listOf("トップス")
        )
        assertFalse(filterOptions.isEmpty())
    }
}