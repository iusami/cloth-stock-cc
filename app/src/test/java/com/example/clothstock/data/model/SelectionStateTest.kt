package com.example.clothstock.data.model

import org.junit.Test
import org.junit.Assert.*

class SelectionStateTest {

    @Test
    fun `constructor creates default state with no selection`() {
        val selectionState = SelectionState()
        
        assertFalse(selectionState.isSelectionMode)
        assertTrue(selectionState.selectedItemIds.isEmpty())
        assertEquals(0, selectionState.totalSelectedCount)
    }

    @Test
    fun `constructor creates state with provided values`() {
        val selectedIds = setOf(1L, 2L, 3L)
        val selectionState = SelectionState(
            isSelectionMode = true,
            selectedItemIds = selectedIds,
            totalSelectedCount = 3
        )
        
        assertTrue(selectionState.isSelectionMode)
        assertEquals(selectedIds, selectionState.selectedItemIds)
        assertEquals(3, selectionState.totalSelectedCount)
    }

    @Test
    fun `isItemSelected returns true for selected item`() {
        val selectionState = SelectionState(
            selectedItemIds = setOf(1L, 2L, 3L)
        )
        
        assertTrue(selectionState.isItemSelected(1L))
        assertTrue(selectionState.isItemSelected(2L))
        assertTrue(selectionState.isItemSelected(3L))
    }

    @Test
    fun `isItemSelected returns false for non-selected item`() {
        val selectionState = SelectionState(
            selectedItemIds = setOf(1L, 2L)
        )
        
        assertFalse(selectionState.isItemSelected(3L))
        assertFalse(selectionState.isItemSelected(999L))
    }

    @Test
    fun `hasSelection returns true when items are selected`() {
        val selectionState = SelectionState(
            selectedItemIds = setOf(1L)
        )
        
        assertTrue(selectionState.hasSelection())
    }

    @Test
    fun `hasSelection returns false when no items are selected`() {
        val selectionState = SelectionState()
        
        assertFalse(selectionState.hasSelection())
    }

    @Test
    fun `hasSelection returns false when selectedItemIds is empty`() {
        val selectionState = SelectionState(
            selectedItemIds = emptySet()
        )
        
        assertFalse(selectionState.hasSelection())
    }

    // 状態遷移メソッドのテスト
    @Test
    fun `selectItem adds item to selection and returns new state`() {
        val initialState = SelectionState()
        val newState = initialState.selectItem(1L)
        
        assertTrue(newState.selectedItemIds.contains(1L))
        assertEquals(1, newState.totalSelectedCount)
        assertTrue(newState.isSelectionMode)
        // 元の状態は変更されていないことを確認（immutable）
        assertFalse(initialState.selectedItemIds.contains(1L))
    }

    @Test
    fun `selectItem adds multiple items correctly`() {
        val initialState = SelectionState()
        val state1 = initialState.selectItem(1L)
        val state2 = state1.selectItem(2L)
        val state3 = state2.selectItem(3L)
        
        assertEquals(setOf(1L, 2L, 3L), state3.selectedItemIds)
        assertEquals(3, state3.totalSelectedCount)
        assertTrue(state3.isSelectionMode)
    }

    @Test
    fun `selectItem does not add duplicate items`() {
        val initialState = SelectionState()
        val state1 = initialState.selectItem(1L)
        val state2 = state1.selectItem(1L) // 同じアイテムを再選択
        
        assertEquals(setOf(1L), state2.selectedItemIds)
        assertEquals(1, state2.totalSelectedCount)
    }

    @Test
    fun `deselectItem removes item from selection and returns new state`() {
        val initialState = SelectionState(
            isSelectionMode = true,
            selectedItemIds = setOf(1L, 2L, 3L),
            totalSelectedCount = 3
        )
        val newState = initialState.deselectItem(2L)
        
        assertEquals(setOf(1L, 3L), newState.selectedItemIds)
        assertEquals(2, newState.totalSelectedCount)
        assertTrue(newState.isSelectionMode)
        // 元の状態は変更されていないことを確認
        assertEquals(setOf(1L, 2L, 3L), initialState.selectedItemIds)
    }

    @Test
    fun `deselectItem exits selection mode when no items remain`() {
        val initialState = SelectionState(
            isSelectionMode = true,
            selectedItemIds = setOf(1L),
            totalSelectedCount = 1
        )
        val newState = initialState.deselectItem(1L)
        
        assertTrue(newState.selectedItemIds.isEmpty())
        assertEquals(0, newState.totalSelectedCount)
        assertFalse(newState.isSelectionMode)
    }

    @Test
    fun `deselectItem handles non-existent item gracefully`() {
        val initialState = SelectionState(
            isSelectionMode = true,
            selectedItemIds = setOf(1L, 2L),
            totalSelectedCount = 2
        )
        val newState = initialState.deselectItem(999L) // 存在しないアイテム
        
        assertEquals(setOf(1L, 2L), newState.selectedItemIds)
        assertEquals(2, newState.totalSelectedCount)
        assertTrue(newState.isSelectionMode)
    }

    @Test
    fun `clearSelection returns default state`() {
        val initialState = SelectionState(
            isSelectionMode = true,
            selectedItemIds = setOf(1L, 2L, 3L),
            totalSelectedCount = 3
        )
        val newState = initialState.clearSelection()
        
        assertFalse(newState.isSelectionMode)
        assertTrue(newState.selectedItemIds.isEmpty())
        assertEquals(0, newState.totalSelectedCount)
        // 元の状態は変更されていないことを確認
        assertTrue(initialState.isSelectionMode)
        assertEquals(3, initialState.totalSelectedCount)
    }

    @Test
    fun `clearSelection on empty state returns same default state`() {
        val initialState = SelectionState()
        val newState = initialState.clearSelection()
        
        assertFalse(newState.isSelectionMode)
        assertTrue(newState.selectedItemIds.isEmpty())
        assertEquals(0, newState.totalSelectedCount)
    }

    // エッジケースとデータ整合性のテスト
    @Test
    fun `totalSelectedCount is always consistent with selectedItemIds size`() {
        val state1 = SelectionState()
        assertEquals(state1.selectedItemIds.size, state1.totalSelectedCount)
        
        val state2 = state1.selectItem(1L).selectItem(2L).selectItem(3L)
        assertEquals(state2.selectedItemIds.size, state2.totalSelectedCount)
        
        val state3 = state2.deselectItem(2L)
        assertEquals(state3.selectedItemIds.size, state3.totalSelectedCount)
        
        val state4 = state3.clearSelection()
        assertEquals(state4.selectedItemIds.size, state4.totalSelectedCount)
    }

    @Test
    fun `selectItem with negative ID works correctly`() {
        val initialState = SelectionState()
        val newState = initialState.selectItem(-1L)
        
        assertTrue(newState.selectedItemIds.contains(-1L))
        assertEquals(1, newState.totalSelectedCount)
        assertTrue(newState.isSelectionMode)
    }

    @Test
    fun `selectItem with zero ID works correctly`() {
        val initialState = SelectionState()
        val newState = initialState.selectItem(0L)
        
        assertTrue(newState.selectedItemIds.contains(0L))
        assertEquals(1, newState.totalSelectedCount)
        assertTrue(newState.isSelectionMode)
    }

    @Test
    fun `selectItem with very large ID works correctly`() {
        val initialState = SelectionState()
        val largeId = Long.MAX_VALUE
        val newState = initialState.selectItem(largeId)
        
        assertTrue(newState.selectedItemIds.contains(largeId))
        assertEquals(1, newState.totalSelectedCount)
        assertTrue(newState.isSelectionMode)
    }

    @Test
    fun `complex state transitions maintain immutability`() {
        val initialState = SelectionState()
        
        // 複数の操作を連続して実行
        val finalState = initialState
            .selectItem(1L)
            .selectItem(2L)
            .selectItem(3L)
            .deselectItem(2L)
            .selectItem(4L)
            .deselectItem(1L)
        
        // 初期状態は変更されていない
        assertFalse(initialState.isSelectionMode)
        assertTrue(initialState.selectedItemIds.isEmpty())
        assertEquals(0, initialState.totalSelectedCount)
        
        // 最終状態は期待される値
        assertEquals(setOf(3L, 4L), finalState.selectedItemIds)
        assertEquals(2, finalState.totalSelectedCount)
        assertTrue(finalState.isSelectionMode)
    }

    @Test
    fun `data class equals and hashCode work correctly`() {
        val state1 = SelectionState(
            isSelectionMode = true,
            selectedItemIds = setOf(1L, 2L),
            totalSelectedCount = 2
        )
        val state2 = SelectionState(
            isSelectionMode = true,
            selectedItemIds = setOf(1L, 2L),
            totalSelectedCount = 2
        )
        val state3 = SelectionState(
            isSelectionMode = true,
            selectedItemIds = setOf(1L, 3L),
            totalSelectedCount = 2
        )
        
        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())
        assertNotEquals(state1, state3)
        assertNotEquals(state1.hashCode(), state3.hashCode())
    }
}
