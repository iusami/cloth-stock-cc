package com.example.clothstock.data.preferences

import android.content.SharedPreferences
import com.example.clothstock.data.model.FilterState
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * FilterPreferencesManagerのテスト
 * 
 * TDD Red フェーズ: 失敗するテストを書く
 * Task 10: SharedPreferencesでのフィルター設定永続化
 */
class FilterPreferencesManagerTest {

    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var preferencesManager: FilterPreferencesManager

    @Before
    fun setup() {
        mockSharedPreferences = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.apply() } returns Unit
        
        preferencesManager = FilterPreferencesManager(mockSharedPreferences)
    }

    // ===== RED: フィルター設定保存テスト =====

    @Test
    fun `saveFilterState should save all filter types to SharedPreferences`() {
        // Given: フィルター状態がある
        val filterState = FilterState(
            sizeFilters = setOf(100, 110, 120),
            colorFilters = setOf("赤", "青", "緑"),
            categoryFilters = setOf("トップス", "ボトムス"),
            searchText = "テスト検索"
        )

        // When: フィルター状態を保存
        preferencesManager.saveFilterState(filterState)

        // Then: SharedPreferencesに保存される
        verify { mockEditor.putStringSet("filter_sizes", setOf("100", "110", "120")) }
        verify { mockEditor.putStringSet("filter_colors", setOf("赤", "青", "緑")) }
        verify { mockEditor.putStringSet("filter_categories", setOf("トップス", "ボトムス")) }
        verify { mockEditor.putString("filter_search_text", "テスト検索") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `saveFilterState should save empty state correctly`() {
        // Given: 空のフィルター状態
        val emptyState = FilterState()

        // When: 空の状態を保存
        preferencesManager.saveFilterState(emptyState)

        // Then: 空のセットと空文字列が保存される
        verify { mockEditor.putStringSet("filter_sizes", emptySet()) }
        verify { mockEditor.putStringSet("filter_colors", emptySet()) }
        verify { mockEditor.putStringSet("filter_categories", emptySet()) }
        verify { mockEditor.putString("filter_search_text", "") }
        verify { mockEditor.apply() }
    }

    @Test
    fun `saveFilterState should handle null values gracefully`() {
        // Given: 一部がnullの状態（実際にはFilterStateではnullにならないが、防御的プログラミング）
        val filterState = FilterState(
            sizeFilters = emptySet(),
            colorFilters = emptySet(),
            categoryFilters = emptySet(),
            searchText = ""
        )

        // When: 状態を保存
        preferencesManager.saveFilterState(filterState)

        // Then: エラーが発生せずに保存される
        verify { mockEditor.apply() }
    }

    // ===== RED: フィルター設定読み込みテスト =====

    @Test
    fun `loadFilterState should restore all filter types from SharedPreferences`() {
        // Given: SharedPreferencesに保存されたデータがある
        every { mockSharedPreferences.getStringSet("filter_sizes", emptySet()) } returns setOf("100", "110")
        every { mockSharedPreferences.getStringSet("filter_colors", emptySet()) } returns setOf("赤", "青")
        every { mockSharedPreferences.getStringSet("filter_categories", emptySet()) } returns setOf("トップス")
        every { mockSharedPreferences.getString("filter_search_text", "") } returns "保存された検索"

        // When: フィルター状態を読み込み
        val loadedState = preferencesManager.loadFilterState()

        // Then: 正しく復元される
        assertEquals("サイズフィルターが復元されること", setOf(100, 110), loadedState.sizeFilters)
        assertEquals("色フィルターが復元されること", setOf("赤", "青"), loadedState.colorFilters)
        assertEquals("カテゴリフィルターが復元されること", setOf("トップス"), loadedState.categoryFilters)
        assertEquals("検索テキストが復元されること", "保存された検索", loadedState.searchText)
    }

    @Test
    fun `loadFilterState should return empty state when no data saved`() {
        // Given: SharedPreferencesに何も保存されていない
        every { mockSharedPreferences.getStringSet("filter_sizes", emptySet()) } returns emptySet()
        every { mockSharedPreferences.getStringSet("filter_colors", emptySet()) } returns emptySet()
        every { mockSharedPreferences.getStringSet("filter_categories", emptySet()) } returns emptySet()
        every { mockSharedPreferences.getString("filter_search_text", "") } returns ""

        // When: フィルター状態を読み込み
        val loadedState = preferencesManager.loadFilterState()

        // Then: 空の状態が返される
        assertTrue("サイズフィルターが空であること", loadedState.sizeFilters.isEmpty())
        assertTrue("色フィルターが空であること", loadedState.colorFilters.isEmpty())
        assertTrue("カテゴリフィルターが空であること", loadedState.categoryFilters.isEmpty())
        assertEquals("検索テキストが空であること", "", loadedState.searchText)
        assertFalse("アクティブフィルターがないこと", loadedState.hasActiveFilters())
    }

    @Test
    fun `loadFilterState should handle invalid size values gracefully`() {
        // Given: 無効なサイズ値が保存されている
        every { mockSharedPreferences.getStringSet("filter_sizes", emptySet()) } returns setOf(
            "100", "invalid", "110", ""
        )
        every { mockSharedPreferences.getStringSet("filter_colors", emptySet()) } returns emptySet()
        every { mockSharedPreferences.getStringSet("filter_categories", emptySet()) } returns emptySet()
        every { mockSharedPreferences.getString("filter_search_text", "") } returns ""

        // When: フィルター状態を読み込み
        val loadedState = preferencesManager.loadFilterState()

        // Then: 有効な値のみが復元される
        assertEquals("有効なサイズのみが復元されること", setOf(100, 110), loadedState.sizeFilters)
    }

    @Test
    fun `loadFilterState should handle null SharedPreferences values`() {
        // Given: SharedPreferencesがnullを返す
        every { mockSharedPreferences.getStringSet("filter_sizes", emptySet()) } returns null
        every { mockSharedPreferences.getStringSet("filter_colors", emptySet()) } returns null
        every { mockSharedPreferences.getStringSet("filter_categories", emptySet()) } returns null
        every { mockSharedPreferences.getString("filter_search_text", "") } returns null

        // When: フィルター状態を読み込み
        val loadedState = preferencesManager.loadFilterState()

        // Then: 空の状態が返される（エラーが発生しない）
        assertTrue("サイズフィルターが空であること", loadedState.sizeFilters.isEmpty())
        assertTrue("色フィルターが空であること", loadedState.colorFilters.isEmpty())
        assertTrue("カテゴリフィルターが空であること", loadedState.categoryFilters.isEmpty())
        assertEquals("検索テキストが空であること", "", loadedState.searchText)
    }

    // ===== RED: フィルター設定クリアテスト =====

    @Test
    fun `clearFilterState should remove all filter preferences`() {
        // When: フィルター設定をクリア
        preferencesManager.clearFilterState()

        // Then: 全ての設定が削除される
        verify { mockEditor.remove("filter_sizes") }
        verify { mockEditor.remove("filter_colors") }
        verify { mockEditor.remove("filter_categories") }
        verify { mockEditor.remove("filter_search_text") }
        verify { mockEditor.apply() }
    }

    // ===== RED: 設定存在確認テスト =====

    @Test
    fun `hasFilterPreferences should return true when preferences exist`() {
        // Given: 設定が存在する
        every { mockSharedPreferences.contains("filter_sizes") } returns true

        // When: 設定の存在を確認
        val hasPreferences = preferencesManager.hasFilterPreferences()

        // Then: trueが返される
        assertTrue("設定が存在することが確認されること", hasPreferences)
    }

    @Test
    fun `hasFilterPreferences should return false when no preferences exist`() {
        // Given: 設定が存在しない
        every { mockSharedPreferences.contains("filter_sizes") } returns false
        every { mockSharedPreferences.contains("filter_colors") } returns false
        every { mockSharedPreferences.contains("filter_categories") } returns false
        every { mockSharedPreferences.contains("filter_search_text") } returns false

        // When: 設定の存在を確認
        val hasPreferences = preferencesManager.hasFilterPreferences()

        // Then: falseが返される
        assertFalse("設定が存在しないことが確認されること", hasPreferences)
    }

    // ===== RED: バージョン管理テスト =====

    @Test
    fun `saveFilterVersion should save current version`() {
        // Given: 現在のバージョン
        val currentVersion = 1

        // When: バージョンを保存
        preferencesManager.saveFilterVersion(currentVersion)

        // Then: バージョンが保存される
        verify { mockEditor.putInt("filter_version", currentVersion) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `getFilterVersion should return saved version`() {
        // Given: 保存されたバージョンがある
        val savedVersion = 2
        every { mockSharedPreferences.getInt("filter_version", 0) } returns savedVersion

        // When: バージョンを取得
        val version = preferencesManager.getFilterVersion()

        // Then: 保存されたバージョンが返される
        assertEquals("保存されたバージョンが返されること", savedVersion, version)
    }

    @Test
    fun `getFilterVersion should return default version when not saved`() {
        // Given: バージョンが保存されていない
        every { mockSharedPreferences.getInt("filter_version", 0) } returns 0

        // When: バージョンを取得
        val version = preferencesManager.getFilterVersion()

        // Then: デフォルトバージョンが返される
        assertEquals("デフォルトバージョンが返されること", 0, version)
    }
}
