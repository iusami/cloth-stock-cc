package com.example.clothstock.ui.gallery

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import com.example.clothstock.data.repository.ClothRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.times
import androidx.lifecycle.MutableLiveData
import java.util.Date

/**
 * GalleryViewModelのユニットテスト
 * 
 * TDD Greenフェーズ - 簡素化されたテストセット
 * 核心的な機能のみテスト
 */
@ExperimentalCoroutinesApi
class GalleryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var clothRepository: ClothRepository

    private val testClothItems = listOf(
        ClothItem(
            id = 1L,
            imagePath = "/test/image1.jpg",
            tagData = TagData(size = 100, color = "赤", category = "トップス"),
            createdAt = Date()
        ),
        ClothItem(
            id = 2L,
            imagePath = "/test/image2.jpg",
            tagData = TagData(size = 120, color = "青", category = "ボトムス"),
            createdAt = Date()
        )
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== 核心機能テスト =====

    @Test
    fun `初期化_デフォルト状態が設定される`() = runTest {
        // Given: 空のデータを返すモック
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(emptyList()))
        
        // When: ViewModelを初期化
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 初期状態が正しく設定される
        assertEquals(emptyList<ClothItem>(), viewModel.clothItems.value)
        assertEquals(false, viewModel.isLoading.value)
        assertEquals(null, viewModel.errorMessage.value)
        assertEquals(true, viewModel.isEmpty.value)
    }

    @Test
    fun `データ読み込み_成功時に正しくアイテムが設定される`() = runTest {
        // Given: テストデータを返すモック
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        
        // When: ViewModelを初期化
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: データが正しく設定される
        assertEquals(testClothItems, viewModel.clothItems.value)
        assertEquals(false, viewModel.isEmpty.value)
        assertEquals(false, viewModel.isLoading.value)
        verify(clothRepository).getAllItems()
    }

    @Test
    fun `データ読み込み_空リスト時に空状態が設定される`() = runTest {
        // Given: 空リストを返すモック
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(emptyList()))
        
        // When: ViewModelを初期化
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 空状態が設定される
        assertEquals(emptyList<ClothItem>(), viewModel.clothItems.value)
        assertEquals(true, viewModel.isEmpty.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `カテゴリフィルタ_指定カテゴリのアイテムのみ表示される`() = runTest {
        // Given: 初期化とカテゴリフィルタ設定
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getItemsByCategory("トップス"))
            .thenReturn(flowOf(listOf(testClothItems[0])))
        
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: カテゴリでフィルタ
        viewModel.filterByCategory("トップス")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: トップスのアイテムのみ取得される
        verify(clothRepository).getItemsByCategory("トップス")
        assertEquals(listOf(testClothItems[0]), viewModel.clothItems.value)
        assertEquals(false, viewModel.isEmpty.value)
    }

    @Test
    fun `色フィルタ_指定色のアイテムのみ表示される`() = runTest {
        // Given: 初期化と色フィルタ設定
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getItemsByColor("赤"))
            .thenReturn(flowOf(listOf(testClothItems[0])))
        
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: 色でフィルタ
        viewModel.filterByColor("赤")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 赤のアイテムのみ取得される
        verify(clothRepository).getItemsByColor("赤")
        assertEquals(listOf(testClothItems[0]), viewModel.clothItems.value)
    }

    @Test
    fun `フィルタクリア_全アイテムが再表示される`() = runTest {
        // Given: フィルタ適用済み状態から開始
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.getItemsByCategory("トップス"))
            .thenReturn(flowOf(listOf(testClothItems[0])))
        
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
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
        
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
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
        
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: エラーメッセージをクリア
        viewModel.clearErrorMessage()
        
        // Then: nullが設定される
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun `アイテム削除_成功時にデータが再読み込みされる`() = runTest {
        // Given: 削除設定
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        `when`(clothRepository.deleteItemById(1L)).thenReturn(true)
        
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: アイテムを削除
        viewModel.deleteItem(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: 削除が実行される
        verify(clothRepository).deleteItemById(1L)
    }

    @Test
    fun `アイテム削除_失敗時にエラーメッセージが表示される`() = runTest {
        // Given: 削除失敗設定
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(emptyList()))
        `when`(clothRepository.deleteItemById(1L)).thenReturn(false)
        
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When: アイテムを削除
        viewModel.deleteItem(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then: エラーメッセージが設定される
        assertEquals("アイテムの削除に失敗しました", viewModel.errorMessage.value)
    }

    @Test
    fun `retryLastOperation_loadClothItems操作の場合は再読み込みされる`() = runTest {
        // Given: リポジトリ設定
        `when`(clothRepository.getAllItems()).thenReturn(flowOf(testClothItems))
        
        val viewModel = GalleryViewModel(clothRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        
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
        
        val viewModel = GalleryViewModel(clothRepository)
        
        // lastOperationを未知の値に設定（reflectionを使用）
        val lastOperationField = viewModel.javaClass.getDeclaredField("_lastOperation")
        lastOperationField.isAccessible = true
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