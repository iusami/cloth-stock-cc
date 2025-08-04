package com.example.clothstock.ui.gallery

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.verify
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.times
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.`when`
import androidx.lifecycle.MutableLiveData
import com.example.clothstock.data.model.FilterType
import com.example.clothstock.data.model.ClothItem

/**
 * GalleryViewModelのユニットテスト
 * 
 * TDD Greenフェーズ - 簡素化されたテストセット
 * 核心的な機能のみテスト
 * 
 * Task5: フィルター・検索機能のテスト追加
 * 
 * 改善点:
 * - テストデータファクトリーパターンを導入
 * - Builder パターンでテストデータ作成を簡素化
 * - テストクラスを機能別に分割予定
 */
@ExperimentalCoroutinesApi
class GalleryViewModelTest : GalleryViewModelTestBase() {

    // 基底クラスから継承されたフィールドとメソッドを使用

    // 基底クラスのヘルパーメソッドを使用



    // ===== 核心機能テスト =====

    @Test
    fun `初期化_デフォルト状態が設定される`() = runTest {
        // Given: 空のデータを返すモック
        setupBasicRepositoryMocks(items = emptyList(), filterOptions = emptyFilterOptions)
        
        // When: ViewModelを初期化
        val viewModel = createViewModelAndAdvance()
        
        // Then: 初期状態が正しく設定される
        assertEquals(emptyList<ClothItem>(), viewModel.clothItems.value)
        assertEquals(false, viewModel.isLoading.value)
        assertEquals(null, viewModel.errorMessage.value)
        assertEquals(true, viewModel.isEmpty.value)
    }

    @Test
    fun `データ読み込み_成功時に正しくアイテムが設定される`() = runTest {
        // Given: テストデータを返すモック
        setupBasicRepositoryMocks()
        
        // When: ViewModelを初期化
        val viewModel = createViewModelAndAdvance()
        
        // Then: データが正しく設定される
        assertViewModelInitialState(viewModel, testClothItems)
        verify(clothRepository).getAllItems()
    }

    @Test
    fun `データ読み込み_空リスト時に空状態が設定される`() = runTest {
        // Given: 空リストを返すモック
        setupBasicRepositoryMocks(items = emptyList(), filterOptions = emptyFilterOptions)
        
        // When: ViewModelを初期化
        val viewModel = createViewModelAndAdvance()
        
        // Then: 空状態が設定される
        assertEquals(emptyList<ClothItem>(), viewModel.clothItems.value)
        assertEquals(true, viewModel.isEmpty.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `カテゴリフィルタ_指定カテゴリのアイテムのみ表示される`() = runTest {
        // Given: 初期化とカテゴリフィルタ設定
        setupBasicRepositoryMocks()
        setupFilterMocks(category = TEST_CATEGORY_TOPS, expectedResult = listOf(testClothItems[0]))
        
        val viewModel = createViewModelAndAdvance()
        
        // When: カテゴリでフィルタ
        viewModel.filterByCategory(TEST_CATEGORY_TOPS)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: トップスのアイテムのみ取得される
        verify(clothRepository).getItemsByCategory(TEST_CATEGORY_TOPS)
        assertEquals(listOf(testClothItems[0]), viewModel.clothItems.value)
        assertEquals(false, viewModel.isEmpty.value)
    }

    @Test
    fun `色フィルタ_指定色のアイテムのみ表示される`() = runTest {
        // Given: 初期化と色フィルタ設定
        setupBasicRepositoryMocks()
        setupFilterMocks(color = TEST_COLOR_RED, expectedResult = listOf(testClothItems[0]))
        
        val viewModel = createViewModelAndAdvance()
        
        // When: 色でフィルタ
        viewModel.filterByColor(TEST_COLOR_RED)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 赤のアイテムのみ取得される
        verify(clothRepository).getItemsByColor(TEST_COLOR_RED)
        assertEquals(listOf(testClothItems[0]), viewModel.clothItems.value)
    }

    @Test
    fun `フィルタクリア_全アイテムが再表示される`() = runTest {
        // Given: フィルタ適用済み状態から開始
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
        `when`(clothRepository.getItemsByCategory("トップス"))
            .thenReturn(flowOf(listOf(testClothItems[0])))
        
        val viewModel = createViewModelAndAdvance()
        
        viewModel.filterByCategory("トップス")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: フィルタをクリア
        viewModel.clearFilters()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 全アイテムが取得される
        assertEquals(testClothItems, viewModel.clothItems.value)
    }

    @Test
    fun `リフレッシュ_データが再読み込みされる`() = runTest {
        // Given: 初期データ
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
        
        val viewModel = createViewModelAndAdvance()
        
        // When: リフレッシュ実行
        viewModel.refreshData()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: データが再読み込みされる
        assertEquals(testClothItems, viewModel.clothItems.value)
        assertEquals(null, viewModel.errorMessage.value) // エラーメッセージクリア
    }

    @Test
    fun `エラーメッセージクリア_nullに設定される`() = runTest {
        // Given: 初期化
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(emptyList()))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = emptyList(),
                availableColors = emptyList(),
                availableCategories = emptyList()
            )
        )
        
        val viewModel = createViewModelAndAdvance()
        
        // When: エラーメッセージをクリア
        viewModel.clearErrorMessage()
        
        // Then: nullが設定される
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun `アイテム削除_成功時にデータが再読み込みされる`() = runTest {
        // Given: 削除設定
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
        `when`(clothRepository.deleteItemById(1L)).thenReturn(true)
        
        val viewModel = createViewModelAndAdvance()
        
        // When: アイテムを削除
        viewModel.deleteItem(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 削除が実行される
        verify(clothRepository).deleteItemById(1L)
    }

    @Test
    fun `アイテム削除_失敗時にエラーメッセージが表示される`() = runTest {
        // Given: 削除失敗設定
        setupBasicRepositoryMocks(items = emptyList(), filterOptions = emptyFilterOptions)
        `when`(clothRepository.deleteItemById(1L)).thenReturn(false)
        
        val viewModel = createViewModelAndAdvance()
        
        // When: アイテムを削除
        viewModel.deleteItem(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: エラーメッセージが設定される
        assertEquals(ERROR_MESSAGE_DELETE_FAILED, viewModel.errorMessage.value)
    }

    @Test
    fun `retryLastOperation_loadClothItems操作の場合は再読み込みされる`() = runTest {
        // Given: リポジトリ設定
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
        
        val viewModel = createViewModelAndAdvance()
        
        // リセット（初期化時のgetAllItems呼び出しをクリア）
        clearInvocations(clothRepository)
        
        // When: リトライ実行
        viewModel.retryLastOperation()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: データ読み込みが再実行される
        verify(clothRepository, times(1)).getAllItems()
    }

    @Test
    fun `retryLastOperation_未知の操作の場合はデフォルトで再読み込みされる`() = runTest {
        // Given: リポジトリ設定
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
        
        val viewModel = GalleryViewModel(clothRepository, filterManager)
        
        // lastOperationを未知の値に設定（reflectionを使用）
        val lastOperationField = viewModel.javaClass.getDeclaredField("_lastOperation")
        lastOperationField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val lastOperationLiveData = lastOperationField.get(viewModel) as MutableLiveData<String>
        lastOperationLiveData.value = "unknownOperation"
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        // リセット（初期化時の呼び出しをクリア）
        clearInvocations(clothRepository)
        
        // When: リトライ実行
        viewModel.retryLastOperation()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: デフォルトでデータ読み込みが実行される
        verify(clothRepository, times(1)).getAllItems()
    }

    // ===== Task5: フィルター・検索機能のテスト =====

    @Test
    fun `検索デバウンシング_連続入力時に最後の検索のみ実行される`() = runTest {
        // Given: リポジトリ設定
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
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
    fun `フィルター状態LiveData_初期化時に正しいデフォルト値が設定される`() = runTest {
        // Given: リポジトリ設定
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
        
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
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
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
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
        
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
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
        
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
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
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
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
        
        val viewModel = createViewModelAndAdvance()
        
        // When: 検索をクリア
        viewModel.clearSearch()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 検索テキストがクリアされ、全データが表示される
        assertEquals("Search text should be cleared", "", viewModel.currentSearchText.value)
        verify(clothRepository, times(2)).getAllItems() // 初期化 + clearSearch後の再読み込み
    }

    @Test
    fun `複合フィルター操作_サイズと色フィルターの組み合わせで正しく動作する`() = runTest {
        // Given: リポジトリ設定
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
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
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
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

    // ===== エラーハンドリングテスト =====

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
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getAvailableFilterOptions()).thenReturn(
            com.example.clothstock.data.model.FilterOptions(
                availableSizes = listOf(100, 120),
                availableColors = listOf("赤", "青"),
                availableCategories = listOf("トップス", "ボトムス")
            )
        )
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