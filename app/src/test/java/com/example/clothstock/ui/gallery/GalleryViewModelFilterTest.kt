package com.example.clothstock.ui.gallery

import com.example.clothstock.data.model.FilterType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

/**
 * GalleryViewModelのフィルター機能専用テスト
 * 
 * フィルター・検索機能に特化したテストクラス
 * テストクラス分割によりメンテナンス性を向上
 */
@ExperimentalCoroutinesApi
class GalleryViewModelFilterTest : GalleryViewModelTestBase() {

    @Test
    fun `フィルター状態LiveData_初期化時に正しいデフォルト値が設定される`() = runTest {
        // Given: リポジトリ設定
        setupBasicRepositoryMocks()
        
        // When: ViewModelを初期化
        val viewModel = createViewModelAndAdvance()
        
        // Then: フィルター関連LiveDataが適切に初期化される
        assertNotNull("currentFilters should be initialized", viewModel.currentFilters.value)
        assertNotNull("availableFilterOptions should be initialized", viewModel.availableFilterOptions.value)
        assertNotNull("isFiltersActive should be initialized", viewModel.isFiltersActive.value)
        assertEquals("Default search text should be empty", "", viewModel.currentSearchText.value)
    }

    @Test
    fun `applyFilter_サイズフィルター適用時に状態が更新される`() = runTest {
        // Given: リポジトリ設定
        setupBasicRepositoryMocks()
        `when`(clothRepository.searchItemsWithFilters(listOf(100), null, null, null))
            .thenReturn(flowOf(testClothItems.filter { it.tagData.size == 100 }))
        
        val viewModel = createViewModelAndAdvance()
        
        // When: サイズフィルターを適用
        viewModel.applyFilter(FilterType.SIZE, "100")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: フィルター状態が更新される
        assertTrue("Filters should be active", viewModel.isFiltersActive.value == true)
        verify(clothRepository).searchItemsWithFilters(listOf(100), null, null, null)
    }

    @Test
    fun `removeFilter_指定フィルター削除時に状態が更新される`() = runTest {
        // Given: リポジトリ設定とフィルター適用状態
        setupBasicRepositoryMocks()
        
        val viewModel = createViewModelAndAdvance()
        
        // When: フィルターを削除
        viewModel.removeFilter(FilterType.SIZE, "100")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: フィルター状態が更新される
        verify(clothRepository, times(2)).getAllItems() // 初期化 + removeFilter後の再読み込み
    }

    @Test
    fun `clearAllFilters_全フィルタークリア時に全データが表示される`() = runTest {
        // Given: リポジトリ設定
        setupBasicRepositoryMocks()
        
        val viewModel = createViewModelAndAdvance()
        
        // When: 全フィルターをクリア
        viewModel.clearAllFilters()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: フィルターが非アクティブになり全データが表示される
        assertFalse("Filters should be inactive", viewModel.isFiltersActive.value == true)
        verify(clothRepository, times(2)).getAllItems() // 初期化 + clearAllFilters後の再読み込み
    }

    @Test
    fun `performSearch_検索テキスト入力時にデバウンシング付きで検索される`() = runTest {
        // Given: リポジトリ設定
        setupBasicRepositoryMocks()
        `when`(clothRepository.searchItemsWithFilters(null, null, null, "シャツ")).thenReturn(flowOf(testClothItems))
        
        val viewModel = createViewModelAndAdvance()
        
        // When: 検索を実行
        viewModel.performSearch("シャツ")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 検索テキストが更新され、リポジトリの検索メソッドが呼ばれる
        assertEquals("Search text should be updated", "シャツ", viewModel.currentSearchText.value)
        verify(clothRepository).searchItemsWithFilters(null, null, null, "シャツ")
    }

    @Test
    fun `clearSearch_検索クリア時に全データが表示される`() = runTest {
        // Given: リポジトリ設定
        setupBasicRepositoryMocks()
        
        val viewModel = createViewModelAndAdvance()
        
        // When: 検索をクリア
        viewModel.clearSearch()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 検索テキストがクリアされ、全データが表示される
        assertEquals("Search text should be cleared", "", viewModel.currentSearchText.value)
        verify(clothRepository, times(2)).getAllItems() // 初期化 + clearSearch後の再読み込み
    }

    @Test
    fun `検索デバウンシング_連続入力時に最後の検索のみ実行される`() = runTest {
        // Given: リポジトリ設定
        setupBasicRepositoryMocks()
        `when`(clothRepository.searchItemsWithFilters(null, null, null, "最終検索"))
            .thenReturn(flowOf(testClothItems))
        
        val viewModel = createViewModelAndAdvance()
        
        // When: 連続して検索を実行（デバウンシングテスト）
        viewModel.performSearch("検索1")
        viewModel.performSearch("検索2")
        viewModel.performSearch("最終検索")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 最後の検索のみが実行される
        verify(clothRepository).searchItemsWithFilters(null, null, null, "最終検索")
        assertEquals("最終検索", viewModel.currentSearchText.value)
    }

    @Test
    fun `複合フィルター操作_サイズと色フィルターの組み合わせで正しく動作する`() = runTest {
        // Given: リポジトリ設定
        setupBasicRepositoryMocks()
        // 最初のサイズフィルター適用時の呼び出し
        `when`(clothRepository.searchItemsWithFilters(listOf(100), null, null, null))
            .thenReturn(flowOf(testClothItems.filter { it.tagData.size == 100 }))
        // 2回目の色フィルター追加時の呼び出し
        `when`(clothRepository.searchItemsWithFilters(listOf(100), listOf("赤"), null, null))
            .thenReturn(flowOf(testClothItems.take(1)))
        
        val viewModel = createViewModelAndAdvance()
        
        // When: サイズと色フィルターを適用
        viewModel.applyFilter(FilterType.SIZE, "100")
        viewModel.applyFilter(FilterType.COLOR, "赤")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 複合条件で検索が実行される（最終的な呼び出しを確認）
        // 実際には2回呼ばれる可能性があるので、atLeastOnceを使用
        verify(clothRepository, atLeastOnce()).searchItemsWithFilters(listOf(100), listOf("赤"), null, null)
    }

    @Test
    fun `複合フィルター操作_フィルターと検索テキストの組み合わせで正しく動作する`() = runTest {
        // Given: リポジトリ設定
        setupBasicRepositoryMocks()
        `when`(clothRepository.searchItemsWithFilters(listOf(100), null, null, "シャツ"))
            .thenReturn(flowOf(testClothItems.take(1)))
        
        val viewModel = createViewModelAndAdvance()
        
        // When: サイズフィルターと検索テキストを適用
        viewModel.applyFilter(FilterType.SIZE, "100")
        viewModel.performSearch("シャツ")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 複合条件で検索が実行される
        verify(clothRepository).searchItemsWithFilters(listOf(100), null, null, "シャツ")
    }

    @Test
    fun `フィルターオプション読み込み失敗時_空のオプションが設定される`() = runTest {
        // Given: フィルターオプション読み込みが失敗するモック
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenThrow(RuntimeException("Database error"))
        
        // When: ViewModelを初期化
        val viewModel = createViewModelAndAdvance()
        
        // Then: 空のフィルターオプションが設定される
        val options = viewModel.availableFilterOptions.value
        assertNotNull("Filter options should not be null", options)
        assertTrue("Available sizes should be empty", options!!.availableSizes.isEmpty())
        assertTrue("Available colors should be empty", options.availableColors.isEmpty())
        assertTrue("Available categories should be empty", options.availableCategories.isEmpty())
    }

    @Test
    fun `検索実行中にエラー発生時_適切なエラーメッセージが表示される`() = runTest {
        // Given: 検索でエラーが発生するモック
        setupBasicRepositoryMocks()
        `when`(clothRepository.searchItemsWithFilters(null, null, null, "エラー検索"))
            .thenThrow(RuntimeException("Search failed"))
        
        val viewModel = createViewModelAndAdvance()
        
        // When: エラーが発生する検索を実行
        viewModel.performSearch("エラー検索")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: エラーメッセージが設定される
        assertNotNull("Error message should be set", viewModel.errorMessage.value)
        assertTrue("Error message should contain relevant info", 
            viewModel.errorMessage.value!!.contains("フィルタリング・検索エラー") ||
            viewModel.errorMessage.value!!.contains("Search failed"))
        assertEquals(false, viewModel.isLoading.value)
    }

}
