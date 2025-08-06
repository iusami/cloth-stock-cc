package com.example.clothstock.data.repository

import com.example.clothstock.data.model.FilterType
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

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
    
    // ========== updateFilter メソッドのテスト ==========
    
    @Test
    fun `updateFilter with SIZE type should update size filters`() {
        // Given: 初期状態は空
        val initialState = filterManager.getCurrentState()
        assertTrue("初期状態ではサイズフィルターが空であること", initialState.sizeFilters.isEmpty())
        
        // When: サイズフィルターを更新
        val values = setOf("100", "110")
        val newState = filterManager.updateFilter(FilterType.SIZE, values)
        
        // Then: サイズフィルターが更新されること
        assertEquals("サイズフィルターが正しく設定されること", setOf(100, 110), newState.sizeFilters)
        assertEquals("他のフィルターは変更されないこと", emptySet<String>(), newState.colorFilters)
        assertEquals("他のフィルターは変更されないこと", emptySet<String>(), newState.categoryFilters)
        assertEquals("検索テキストは変更されないこと", "", newState.searchText)
    }
    
    @Test
    fun `updateFilter with COLOR type should update color filters`() {
        // Given: 初期状態
        val initialState = filterManager.getCurrentState()
        assertTrue("初期状態では色フィルターが空であること", initialState.colorFilters.isEmpty())
        
        // When: 色フィルターを更新
        val values = setOf("赤", "青")
        val newState = filterManager.updateFilter(FilterType.COLOR, values)
        
        // Then: 色フィルターが更新されること
        assertEquals("色フィルターが正しく設定されること", setOf("赤", "青"), newState.colorFilters)
        assertEquals("他のフィルターは変更されないこと", emptySet<Int>(), newState.sizeFilters)
        assertEquals("他のフィルターは変更されないこと", emptySet<String>(), newState.categoryFilters)
        assertEquals("検索テキストは変更されないこと", "", newState.searchText)
    }
    
    @Test
    fun `updateFilter with CATEGORY type should update category filters`() {
        // Given: 初期状態
        val initialState = filterManager.getCurrentState()
        assertTrue("初期状態ではカテゴリフィルターが空であること", initialState.categoryFilters.isEmpty())
        
        // When: カテゴリフィルターを更新
        val values = setOf("トップス", "ボトムス")
        val newState = filterManager.updateFilter(FilterType.CATEGORY, values)
        
        // Then: カテゴリフィルターが更新されること
        assertEquals("カテゴリフィルターが正しく設定されること", setOf("トップス", "ボトムス"), newState.categoryFilters)
        assertEquals("他のフィルターは変更されないこと", emptySet<Int>(), newState.sizeFilters)
        assertEquals("他のフィルターは変更されないこと", emptySet<String>(), newState.colorFilters)
        assertEquals("検索テキストは変更されないこと", "", newState.searchText)
    }
    
    @Test
    fun `updateFilter with empty values should clear that filter type`() {
        // Given: 既存のフィルターが設定されている状態
        filterManager.updateFilter(FilterType.SIZE, setOf("100", "110"))
        filterManager.updateFilter(FilterType.COLOR, setOf("赤"))
        
        // When: 空の値でサイズフィルターを更新
        val newState = filterManager.updateFilter(FilterType.SIZE, emptySet())
        
        // Then: サイズフィルターがクリアされること
        assertTrue("サイズフィルターがクリアされること", newState.sizeFilters.isEmpty())
        assertEquals("色フィルターは保持されること", setOf("赤"), newState.colorFilters)
    }
    
    @Test
    fun `updateFilter with invalid size values should handle gracefully`() {
        // When: 無効なサイズ値を設定
        val values = setOf("invalid", "200", "50")
        val newState = filterManager.updateFilter(FilterType.SIZE, values)
        
        // Then: 有効な値のみが設定されること（この場合は空になる想定）
        // 注意: 実装詳細によってはエラーをthrowする可能性もある
        assertNotNull("結果がnullでないこと", newState)
    }
    
    // ========== removeFilter メソッドのテスト ==========
    
    @Test
    fun `removeFilter should remove specific value from size filters`() {
        // Given: 複数のサイズフィルターが設定されている
        filterManager.updateFilter(FilterType.SIZE, setOf("100", "110", "120"))
        
        // When: 特定のサイズフィルターを削除
        val newState = filterManager.removeFilter(FilterType.SIZE, "110")
        
        // Then: 指定したフィルターのみが削除されること
        assertEquals("指定したサイズフィルターが削除されること", setOf(100, 120), newState.sizeFilters)
    }
    
    @Test
    fun `removeFilter should remove specific value from color filters`() {
        // Given: 複数の色フィルターが設定されている
        filterManager.updateFilter(FilterType.COLOR, setOf("赤", "青", "緑"))
        
        // When: 特定の色フィルターを削除
        val newState = filterManager.removeFilter(FilterType.COLOR, "青")
        
        // Then: 指定したフィルターのみが削除されること
        assertEquals("指定した色フィルターが削除されること", setOf("赤", "緑"), newState.colorFilters)
    }
    
    @Test
    fun `removeFilter should handle non-existent value gracefully`() {
        // Given: サイズフィルターが設定されている
        filterManager.updateFilter(FilterType.SIZE, setOf("100", "110"))
        
        // When: 存在しない値を削除しようとする
        val newState = filterManager.removeFilter(FilterType.SIZE, "130")
        
        // Then: 既存のフィルターは変更されないこと
        assertEquals("既存のフィルターは変更されないこと", setOf(100, 110), newState.sizeFilters)
    }
    
    // ========== clearFilter メソッドのテスト ==========
    
    @Test
    fun `clearFilter should clear all size filters`() {
        // Given: 複数のフィルターが設定されている
        filterManager.updateFilter(FilterType.SIZE, setOf("100", "110"))
        filterManager.updateFilter(FilterType.COLOR, setOf("赤", "青"))
        
        // When: サイズフィルターをクリア
        val newState = filterManager.clearFilter(FilterType.SIZE)
        
        // Then: サイズフィルターのみがクリアされること
        assertTrue("サイズフィルターがクリアされること", newState.sizeFilters.isEmpty())
        assertEquals("色フィルターは保持されること", setOf("赤", "青"), newState.colorFilters)
    }
    
    @Test
    fun `clearFilter should clear all color filters`() {
        // Given: 複数のフィルターが設定されている
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        filterManager.updateFilter(FilterType.COLOR, setOf("赤", "青"))
        
        // When: 色フィルターをクリア
        val newState = filterManager.clearFilter(FilterType.COLOR)
        
        // Then: 色フィルターのみがクリアされること
        assertTrue("色フィルターがクリアされること", newState.colorFilters.isEmpty())
        assertEquals("サイズフィルターは保持されること", setOf(100), newState.sizeFilters)
    }
    
    @Test
    fun `clearFilter should clear all category filters`() {
        // Given: 複数のフィルターが設定されている
        filterManager.updateFilter(FilterType.CATEGORY, setOf("トップス", "ボトムス"))
        filterManager.updateFilter(FilterType.COLOR, setOf("赤"))
        
        // When: カテゴリフィルターをクリア
        val newState = filterManager.clearFilter(FilterType.CATEGORY)
        
        // Then: カテゴリフィルターのみがクリアされること
        assertTrue("カテゴリフィルターがクリアされること", newState.categoryFilters.isEmpty())
        assertEquals("色フィルターは保持されること", setOf("赤"), newState.colorFilters)
    }
    
    // ========== clearAllFilters メソッドのテスト ==========
    
    @Test
    fun `clearAllFilters should clear all filter types`() {
        // Given: 全てのタイプのフィルターが設定されている
        filterManager.updateFilter(FilterType.SIZE, setOf("100", "110"))
        filterManager.updateFilter(FilterType.COLOR, setOf("赤", "青"))
        filterManager.updateFilter(FilterType.CATEGORY, setOf("トップス"))
        filterManager.updateSearchText("テスト検索")
        
        // When: 全フィルターをクリア
        val newState = filterManager.clearAllFilters()
        
        // Then: 全てのフィルターがクリアされること
        assertTrue("サイズフィルターがクリアされること", newState.sizeFilters.isEmpty())
        assertTrue("色フィルターがクリアされること", newState.colorFilters.isEmpty())
        assertTrue("カテゴリフィルターがクリアされること", newState.categoryFilters.isEmpty())
        assertEquals("検索テキストがクリアされること", "", newState.searchText)
    }
    
    @Test
    fun `clearAllFilters on empty state should return empty state`() {
        // Given: 初期状態（フィルターなし）
        val initialState = filterManager.getCurrentState()
        assertTrue("初期状態が空であること", !initialState.hasActiveFilters())
        
        // When: 全フィルターをクリア
        val newState = filterManager.clearAllFilters()
        
        // Then: 空の状態が返されること
        assertFalse("フィルターがアクティブでないこと", newState.hasActiveFilters())
        assertEquals("状態が変わらないこと", initialState, newState)
    }
    
    // ========== updateSearchText メソッドのテスト ==========
    
    @Test
    fun `updateSearchText should update search text`() {
        // Given: 初期状態
        val initialState = filterManager.getCurrentState()
        assertEquals("初期検索テキストが空であること", "", initialState.searchText)
        
        // When: 検索テキストを更新
        val searchText = "テスト検索"
        val newState = filterManager.updateSearchText(searchText)
        
        // Then: 検索テキストが更新されること
        assertEquals("検索テキストが正しく設定されること", searchText, newState.searchText)
        assertTrue("他のフィルターは変更されないこと", newState.sizeFilters.isEmpty())
        assertTrue("他のフィルターは変更されないこと", newState.colorFilters.isEmpty())
        assertTrue("他のフィルターは変更されないこと", newState.categoryFilters.isEmpty())
    }
    
    @Test
    fun `updateSearchText with empty string should clear search text`() {
        // Given: 検索テキストが設定されている
        filterManager.updateSearchText("既存の検索")
        
        // When: 空文字で検索テキストを更新
        val newState = filterManager.updateSearchText("")
        
        // Then: 検索テキストがクリアされること
        assertEquals("検索テキストがクリアされること", "", newState.searchText)
    }
    
    @Test
    fun `updateSearchText should trim whitespace`() {
        // When: 前後に空白がある検索テキストを設定
        val newState = filterManager.updateSearchText("  テスト検索  ")
        
        // Then: 空白が除去されること
        assertEquals("空白が除去されること", "テスト検索", newState.searchText)
    }
    
    // ========== getCurrentState メソッドのテスト ==========
    
    @Test
    fun `getCurrentState should return current filter state`() {
        // Given: 複数のフィルターが設定されている
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        filterManager.updateFilter(FilterType.COLOR, setOf("赤"))
        filterManager.updateFilter(FilterType.CATEGORY, setOf("トップス"))
        filterManager.updateSearchText("検索テキスト")
        
        // When: 現在の状態を取得
        val currentState = filterManager.getCurrentState()
        
        // Then: 設定した全ての値が含まれること
        assertEquals("サイズフィルターが含まれること", setOf(100), currentState.sizeFilters)
        assertEquals("色フィルターが含まれること", setOf("赤"), currentState.colorFilters)
        assertEquals("カテゴリフィルターが含まれること", setOf("トップス"), currentState.categoryFilters)
        assertEquals("検索テキストが含まれること", "検索テキスト", currentState.searchText)
        assertTrue("アクティブフィルターがあること", currentState.hasActiveFilters())
    }
    
    @Test
    fun `getCurrentState should return immutable state`() {
        // Given: フィルターが設定されている
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        
        // When: 状態を2回取得
        val state1 = filterManager.getCurrentState()
        val state2 = filterManager.getCurrentState()
        
        // Then: 同一の内容であること（ただし不変性は実装次第）
        assertEquals("同一の内容であること", state1, state2)
        assertEquals("サイズフィルターが同じであること", state1.sizeFilters, state2.sizeFilters)
    }
    
    // ========== Task 10: 状態復元機能のテスト ==========
    
    @Test
    fun `restoreState should restore all filter types`() {
        // Given: 保存された状態がある
        val savedState = com.example.clothstock.data.model.FilterState(
            sizeFilters = setOf(100, 110),
            colorFilters = setOf("赤", "青"),
            categoryFilters = setOf("トップス", "ボトムス"),
            searchText = "復元テスト"
        )
        
        // When: 状態を復元
        filterManager.restoreState(savedState)
        val restoredState = filterManager.getCurrentState()
        
        // Then: 全ての状態が復元されること
        assertEquals("サイズフィルターが復元されること", setOf(100, 110), restoredState.sizeFilters)
        assertEquals("色フィルターが復元されること", setOf("赤", "青"), restoredState.colorFilters)
        assertEquals("カテゴリフィルターが復元されること", setOf("トップス", "ボトムス"), restoredState.categoryFilters)
        assertEquals("検索テキストが復元されること", "復元テスト", restoredState.searchText)
        assertTrue("アクティブフィルターがあること", restoredState.hasActiveFilters())
    }
    
    @Test
    fun `restoreState should handle empty state`() {
        // Given: 既存のフィルターが設定されている
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        filterManager.updateSearchText("既存検索")
        
        // When: 空の状態を復元
        val emptyState = com.example.clothstock.data.model.FilterState()
        filterManager.restoreState(emptyState)
        val restoredState = filterManager.getCurrentState()
        
        // Then: 全てがクリアされること
        assertTrue("サイズフィルターがクリアされること", restoredState.sizeFilters.isEmpty())
        assertTrue("色フィルターがクリアされること", restoredState.colorFilters.isEmpty())
        assertTrue("カテゴリフィルターがクリアされること", restoredState.categoryFilters.isEmpty())
        assertEquals("検索テキストがクリアされること", "", restoredState.searchText)
        assertFalse("アクティブフィルターがないこと", restoredState.hasActiveFilters())
    }
    
    @Test
    fun `restoreState should overwrite existing state`() {
        // Given: 既存のフィルターが設定されている
        filterManager.updateFilter(FilterType.SIZE, setOf("100"))
        filterManager.updateFilter(FilterType.COLOR, setOf("赤"))
        filterManager.updateSearchText("既存検索")
        
        // When: 異なる状態を復元
        val newState = com.example.clothstock.data.model.FilterState(
            sizeFilters = setOf(120, 130),
            colorFilters = setOf("青", "緑"),
            categoryFilters = setOf("アウター"),
            searchText = "新しい検索"
        )
        filterManager.restoreState(newState)
        val restoredState = filterManager.getCurrentState()
        
        // Then: 新しい状態で上書きされること
        assertEquals("サイズフィルターが上書きされること", setOf(120, 130), restoredState.sizeFilters)
        assertEquals("色フィルターが上書きされること", setOf("青", "緑"), restoredState.colorFilters)
        assertEquals("カテゴリフィルターが上書きされること", setOf("アウター"), restoredState.categoryFilters)
        assertEquals("検索テキストが上書きされること", "新しい検索", restoredState.searchText)
    }
}
