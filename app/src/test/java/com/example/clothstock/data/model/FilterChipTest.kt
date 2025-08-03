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
    
    // New tests for enhanced functionality
    
    @Test
    fun `toggleSelection changes selection state`() {
        val chip = FilterChip.createSizeChip(100, isSelected = false)
        val toggledChip = chip.toggleSelection()
        
        assertTrue(toggledChip.isSelected)
        assertEquals(chip.type, toggledChip.type)
        assertEquals(chip.value, toggledChip.value)
        assertEquals(chip.displayText, toggledChip.displayText)
    }
    
    @Test
    fun `canBeDeselected returns true for non-search chips`() {
        assertTrue(FilterChip.createSizeChip(100).canBeDeselected())
        assertTrue(FilterChip.createColorChip("赤").canBeDeselected())
        assertTrue(FilterChip.createCategoryChip("トップス").canBeDeselected())
    }
    
    @Test
    fun `canBeDeselected returns false for search chips`() {
        assertFalse(FilterChip.createSearchChip("シャツ").canBeDeselected())
    }
    
    @Test
    fun `createSizeChip throws exception for invalid size`() {
        assertThrows(IllegalArgumentException::class.java) {
            FilterChip.createSizeChip(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FilterChip.createSizeChip(-1)
        }
    }
    
    @Test
    fun `createColorChip throws exception for blank color`() {
        assertThrows(IllegalArgumentException::class.java) {
            FilterChip.createColorChip("")
        }
        assertThrows(IllegalArgumentException::class.java) {
            FilterChip.createColorChip("   ")
        }
    }
    
    @Test
    fun `createCategoryChip throws exception for blank category`() {
        assertThrows(IllegalArgumentException::class.java) {
            FilterChip.createCategoryChip("")
        }
        assertThrows(IllegalArgumentException::class.java) {
            FilterChip.createCategoryChip("   ")
        }
    }
    
    @Test
    fun `createColorChip trims whitespace`() {
        val chip = FilterChip.createColorChip("  赤  ")
        assertEquals("赤", chip.value)
        assertEquals("赤", chip.displayText)
    }
    
    @Test
    fun `createCategoryChip trims whitespace`() {
        val chip = FilterChip.createCategoryChip("  トップス  ")
        assertEquals("トップス", chip.value)
        assertEquals("トップス", chip.displayText)
    }
    
    @Test
    fun `createSearchChip trims whitespace`() {
        val chip = FilterChip.createSearchChip("  シャツ  ")
        assertEquals("シャツ", chip.value)
        assertEquals("\"シャツ\"", chip.displayText)
    }
    
    @Test
    fun `create factory method works for all types`() {
        val sizeChip = FilterChip.create(FilterType.SIZE, "100")
        assertEquals(FilterType.SIZE, sizeChip.type)
        assertEquals("100", sizeChip.value)
        
        val colorChip = FilterChip.create(FilterType.COLOR, "赤")
        assertEquals(FilterType.COLOR, colorChip.type)
        assertEquals("赤", colorChip.value)
        
        val categoryChip = FilterChip.create(FilterType.CATEGORY, "トップス")
        assertEquals(FilterType.CATEGORY, categoryChip.type)
        assertEquals("トップス", categoryChip.value)
        
        val searchChip = FilterChip.create(FilterType.SEARCH, "シャツ")
        assertEquals(FilterType.SEARCH, searchChip.type)
        assertEquals("シャツ", searchChip.value)
    }
    
    @Test
    fun `getUniqueId returns consistent identifier`() {
        val sizeChip = FilterChip.createSizeChip(100)
        assertEquals("SIZE:100", sizeChip.getUniqueId())
        
        val colorChip = FilterChip.createColorChip("赤")
        assertEquals("COLOR:赤", colorChip.getUniqueId())
        
        val categoryChip = FilterChip.createCategoryChip("トップス")
        assertEquals("CATEGORY:トップス", categoryChip.getUniqueId())
        
        val searchChip = FilterChip.createSearchChip("シャツ")
        assertEquals("SEARCH:シャツ", searchChip.getUniqueId())
    }
    
    @Test
    fun `chips with same type and value have same unique id`() {
        val chip1 = FilterChip.createSizeChip(100, isSelected = true)
        val chip2 = FilterChip.createSizeChip(100, isSelected = false)
        
        assertEquals(chip1.getUniqueId(), chip2.getUniqueId())
    }
}
