package com.example.clothstock.ui.gallery

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.example.clothstock.data.model.FilterState
import com.example.clothstock.data.model.FilterType
import com.example.clothstock.data.repository.ClothRepository
import com.example.clothstock.data.repository.FilterManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * GalleryViewModelの状態保存・復元機能のテスト
 * 
 * TDD Red フェーズ: 失敗するテストを書く
 * Task 10: フィルター状態の保存と復元
 */
@ExperimentalCoroutinesApi
class GalleryViewModelStatePersistenceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var mockRepository: ClothRepository
    private lateinit var mockFilterManager: FilterManager
    private lateinit var mockSavedStateHandle: SavedStateHandle
    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockRepository = mockk(relaxed = true)
        mockFilterManager = mockk(relaxed = true)
        mockSavedStateHandle = mockk(relaxed = true)
        
        // デフォルトの動作を設定
        every { mockFilterManager.getCurrentState() } returns FilterState()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== RED: ViewModel onCleared での状態保存テスト =====

    @Test
    fun `onCleared should save current filter state to SavedStateHandle`() {
        // Given: フィルター状態が設定されている
        val filterState = FilterState(
            sizeFilters = setOf(100, 110),
            colorFilters = setOf("赤", "青"),
            categoryFilters = setOf("トップス"),
            searchText = "シャツ"
        )
        every { mockFilterManager.getCurrentState() } returns filterState
        
        // ViewModelを作成（SavedStateHandle付き）
        viewModel = GalleryViewModel(mockRepository, mockFilterManager, mockSavedStateHandle, null)
        
        // When: 状態保存メソッドが呼ばれる
        viewModel.saveStateToSavedStateHandle()
        
        // Then: SavedStateHandleに状態が保存される
        verify { mockSavedStateHandle.set("filter_state", filterState) }
    }

    @Test
    fun `onCleared should save search text to SavedStateHandle`() {
        // Given: 検索テキストが設定されている
        val searchText = "検索テキスト"
        val filterState = FilterState(searchText = searchText)
        every { mockFilterManager.getCurrentState() } returns filterState
        
        viewModel = GalleryViewModel(mockRepository, mockFilterManager, mockSavedStateHandle, null)
        
        // When: 状態保存メソッドが呼ばれる
        viewModel.saveStateToSavedStateHandle()
        
        // Then: 検索テキストが保存される
        verify { mockSavedStateHandle.set("search_text", searchText) }
    }

    @Test
    fun `onCleared should save empty state when no filters active`() {
        // Given: フィルターが何も設定されていない
        val emptyState = FilterState()
        every { mockFilterManager.getCurrentState() } returns emptyState
        
        viewModel = GalleryViewModel(mockRepository, mockFilterManager, mockSavedStateHandle, null)
        
        // When: 状態保存メソッドが呼ばれる
        viewModel.saveStateToSavedStateHandle()
        
        // Then: 空の状態が保存される
        verify { mockSavedStateHandle.set("filter_state", emptyState) }
        verify { mockSavedStateHandle.set("search_text", "") }
    }

    // ===== RED: Fragment再作成時の状態復元テスト =====

    @Test
    fun `should restore filter state from SavedStateHandle on initialization`() {
        // Given: SavedStateHandleに保存された状態がある
        val savedFilterState = FilterState(
            sizeFilters = setOf(120, 130),
            colorFilters = setOf("緑"),
            categoryFilters = setOf("ボトムス"),
            searchText = "パンツ"
        )
        every { mockSavedStateHandle.get<FilterState>("filter_state") } returns savedFilterState
        every { mockSavedStateHandle.get<String>("search_text") } returns "パンツ"
        
        // When: ViewModelが初期化される
        viewModel = GalleryViewModel(mockRepository, mockFilterManager, mockSavedStateHandle, null)
        
        // Then: FilterManagerに状態が復元される
        verify { mockFilterManager.restoreState(savedFilterState) }
    }

    @Test
    fun `should restore search text from SavedStateHandle on initialization`() {
        // Given: SavedStateHandleに検索テキストが保存されている
        val savedSearchText = "保存された検索"
        every { mockSavedStateHandle.get<String>("search_text") } returns savedSearchText
        every { mockSavedStateHandle.get<FilterState>("filter_state") } returns null
        
        // When: ViewModelが初期化される
        viewModel = GalleryViewModel(mockRepository, mockFilterManager, mockSavedStateHandle, null)
        
        // Then: 検索テキストが復元される
        verify { mockFilterManager.updateSearchText(savedSearchText) }
    }

    @Test
    fun `should handle null saved state gracefully`() {
        // Given: SavedStateHandleに何も保存されていない
        every { mockSavedStateHandle.get<FilterState>("filter_state") } returns null
        every { mockSavedStateHandle.get<String>("search_text") } returns null
        
        // When: ViewModelが初期化される
        viewModel = GalleryViewModel(mockRepository, mockFilterManager, mockSavedStateHandle, null)
        
        // Then: デフォルト状態で初期化される（エラーが発生しない）
        // FilterManagerの初期化は正常に完了する
        verify(exactly = 0) { mockFilterManager.restoreState(any()) }
        verify(exactly = 0) { mockFilterManager.updateSearchText(any()) }
    }

    // ===== RED: SharedPreferences保存・取得テスト =====

    @Test
    fun `should save filter preferences to SharedPreferences`() {
        // Given: フィルター設定がある
        val filterState = FilterState(
            sizeFilters = setOf(140, 150),
            colorFilters = setOf("黒", "白")
        )
        every { mockFilterManager.getCurrentState() } returns filterState
        
        viewModel = GalleryViewModel(mockRepository, mockFilterManager, mockSavedStateHandle, null)
        
        // When: フィルター設定を永続化する
        viewModel.saveFilterPreferences()
        
        // Then: SharedPreferencesに保存される
        // 注意: この時点では実装されていないため失敗する
        fail("saveFilterPreferences method not implemented yet")
    }

    @Test
    fun `should load filter preferences from SharedPreferences`() {
        // Given: SharedPreferencesに保存された設定がある
        viewModel = GalleryViewModel(mockRepository, mockFilterManager, mockSavedStateHandle, null)
        
        // When: フィルター設定を読み込む
        viewModel.loadFilterPreferences()
        
        // Then: 保存された設定が復元される
        // 注意: この時点では実装されていないため失敗する
        fail("loadFilterPreferences method not implemented yet")
    }

    @Test
    fun `should clear filter preferences from SharedPreferences`() {
        // Given: SharedPreferencesに保存された設定がある
        viewModel = GalleryViewModel(mockRepository, mockFilterManager, mockSavedStateHandle, null)
        
        // When: フィルター設定をクリアする
        viewModel.clearFilterPreferences()
        
        // Then: SharedPreferencesから設定が削除される
        // 注意: この時点では実装されていないため失敗する
        fail("clearFilterPreferences method not implemented yet")
    }
}