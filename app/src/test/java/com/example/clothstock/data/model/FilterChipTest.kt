package com.example.clothstock.data.model

import org.junit.Test
import org.junit.Assert.*

class FilterChipTest {

    @Test
    fun `createSizeChip creates correct FilterChip for size`() {
        val chip = FilterChip.createSizeChip(100)
        
        assertEquals(FilterType.SIZE, chip.type)
        assertEquals("100", chip.value)
        assertEquals("サイズ100", chip.displayText)
        assertFalse(chip.isSelected)
    }

    @Test
    fun `createSizeChip creates selected FilterChip when specified`() {
        val chip = FilterChip.createSizeChip(110, isSelected = true)
        
        assertEquals(FilterType.SIZE, chip.type)
        assertEquals("110", chip.value)
        assertEquals("サイズ110", chip.displayText)
        assertTrue(chip.isSelected)
    }

    @Test
    fun `createColorChip creates correct FilterChip for color`() {
        val chip = FilterChip.createColorChip("赤")
        
        assertEquals(FilterType.COLOR, chip.type)
        assertEquals("赤", chip.value)
        assertEquals("赤", chip.displayText)
        assertFalse(chip.isSelected)
    }

    @Test
    fun `createColorChip creates selected FilterChip when specified`() {
        val chip = FilterChip.createColorChip("青", isSelected = true)
        
        assertEquals(FilterType.COLOR, chip.type)
        assertEquals("青", chip.value)
        assertEquals("青", chip.displayText)
        assertTrue(chip.isSelected)
    }

    @Test
    fun `createCategoryChip creates correct FilterChip for category`() {
        val chip = FilterChip.createCategoryChip("トップス")
        
        assertEquals(FilterType.CATEGORY, chip.type)
        assertEquals("トップス", chip.value)
        assertEquals("トップス", chip.displayText)
        assertFalse(chip.isSelected)
    }

    @Test
    fun `createCategoryChip creates selected FilterChip when specified`() {
        val chip = FilterChip.createCategoryChip("ボトムス", isSelected = true)
        
        assertEquals(FilterType.CATEGORY, chip.type)
        assertEquals("ボトムス", chip.value)
        assertEquals("ボトムス", chip.displayText)
        assertTrue(chip.isSelected)
    }

    @Test
    fun `createSearchChip creates correct FilterChip for search`() {
        val chip = FilterChip.createSearchChip("シャツ")
        
        assertEquals(FilterType.SEARCH, chip.type)
        assertEquals("シャツ", chip.value)
        assertEquals("\"シャツ\"", chip.displayText)
        assertTrue(chip.isSelected) // Search chips are always selected
    }

    @Test
    fun `createSearchChip handles empty search text`() {
        val chip = FilterChip.createSearchChip("")
        
        assertEquals(FilterType.SEARCH, chip.type)
        assertEquals("", chip.value)
        assertEquals("\"\"", chip.displayText)
        assertTrue(chip.isSelected)
    }
}