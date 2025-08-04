package com.example.clothstock.ui.gallery

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import androidx.lifecycle.MutableLiveData

/**
 * GalleryViewModelの基本機能テスト
 * 
 * 核心的な機能のみをテスト
 * フィルター・検索機能は専用テストクラスに分離
 */
@ExperimentalCoroutinesApi
class GalleryViewModelCoreTest : GalleryViewModelTestBase() {

    // ===== 核心機能テスト =====

    @Test
    fun `初期化_デフォルト状態が設定される`() = runTest {
        // Given: 空のデータを返すモック
        setupBasicRepositoryMocks(items = emptyList(), filterOptions = emptyFilterOptions)
        
        // When: ViewModelを初期化
        val viewModel = createViewModelAndAdvance()
        
        // Then: 初期状態が正しく設定される
        assertEquals(emptyList<com.example.clothstock.data.model.ClothItem>(), viewModel.clothItems.value)
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
        assertEquals(emptyList<com.example.clothstock.data.model.ClothItem>(), viewModel.clothItems.value)
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
        setupBasicRepositoryMocks()
        setupFilterMocks(category = TEST_CATEGORY_TOPS, expectedResult = listOf(testClothItems[0]))
        
        val viewModel = createViewModelAndAdvance()
        
        viewModel.filterByCategory(TEST_CATEGORY_TOPS)
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
        setupBasicRepositoryMocks()
        
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
        setupBasicRepositoryMocks(items = emptyList(), filterOptions = emptyFilterOptions)
        
        val viewModel = createViewModelAndAdvance()
        
        // When: エラーメッセージをクリア
        viewModel.clearErrorMessage()
        
        // Then: nullが設定される
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun `アイテム削除_成功時にデータが再読み込みされる`() = runTest {
        // Given: 削除設定
        setupBasicRepositoryMocks()
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
        setupBasicRepositoryMocks()
        
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
        setupBasicRepositoryMocks()
        
        val viewModel = createViewModelAndAdvance()
        
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

}
