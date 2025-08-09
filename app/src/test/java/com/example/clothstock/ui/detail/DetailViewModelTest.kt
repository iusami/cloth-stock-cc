package com.example.clothstock.ui.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import com.example.clothstock.data.repository.ClothRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

/**
 * DetailViewModelのユニットテスト
 * 
 * Task 5実装: メモ機能のテスト
 * 
 * TDD Red-Green-Refactorサイクルに基づくテスト実装
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var repository: ClothRepository

    private lateinit var viewModel: DetailViewModel
    private val testDispatcher = StandardTestDispatcher()

    // テスト用データ
    private val testClothItem = ClothItem(
        id = 1L,
        imagePath = "/test/image.jpg",
        tagData = TagData(
            size = 100,
            color = "Blue",
            category = "トップス"
        ),
        createdAt = Date(),
        memo = "Test memo"
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(testDispatcher)
        
        // デフォルトのリポジトリモック設定
        coEvery { repository.getItemById(any()) } returns testClothItem
        coEvery { repository.updateItem(any()) } returns true
        
        viewModel = DetailViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===== メモ機能テスト =====

    @Test
    fun `onMemoChanged should trigger auto save after debounce`() = runTest {
        // Given
        val newMemo = "Updated memo"
        val memoSaveStateObserver: Observer<DetailViewModel.MemoSaveState> = mockk(relaxed = true)
        viewModel.memoSaveState.observeForever(memoSaveStateObserver)
        
        // アイテムをロード
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        
        // When
        viewModel.onMemoChanged(newMemo)
        
        // debounce時間経過前は保存されない
        advanceTimeBy(500L)
        coVerify(exactly = 0) { repository.updateItem(any()) }
        
        // debounce時間経過後は保存される
        advanceTimeBy(1000L)
        
        // Then
        val updatedItemSlot = slot<ClothItem>()
        coVerify { repository.updateItem(capture(updatedItemSlot)) }
        assertEquals(newMemo, updatedItemSlot.captured.memo)
        
        verify { memoSaveStateObserver.onChanged(DetailViewModel.MemoSaveState.Saving) }
        verify { memoSaveStateObserver.onChanged(DetailViewModel.MemoSaveState.Saved) }
    }

    @Test
    fun `saveMemoImmediately should save without debounce`() = runTest {
        // Given
        val newMemo = "Immediate save memo"
        val memoSaveStateObserver: Observer<DetailViewModel.MemoSaveState> = mockk(relaxed = true)
        viewModel.memoSaveState.observeForever(memoSaveStateObserver)
        
        // アイテムをロード
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        
        // When
        viewModel.saveMemoImmediately(newMemo)
        advanceTimeBy(1L) // 最小時間進行
        
        // Then
        val updatedItemSlot = slot<ClothItem>()
        coVerify { repository.updateItem(capture(updatedItemSlot)) }
        assertEquals(newMemo, updatedItemSlot.captured.memo)
        
        verify { memoSaveStateObserver.onChanged(DetailViewModel.MemoSaveState.Saving) }
        verify { memoSaveStateObserver.onChanged(DetailViewModel.MemoSaveState.Saved) }
    }

    @Test
    fun `memo save error should be handled properly`() = runTest {
        // Given
        val errorMessage = "Database error"
        coEvery { repository.updateItem(any()) } throws RuntimeException(errorMessage)
        
        val memoSaveStateObserver: Observer<DetailViewModel.MemoSaveState> = mockk(relaxed = true)
        viewModel.memoSaveState.observeForever(memoSaveStateObserver)
        
        // アイテムをロード
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        
        // When
        viewModel.saveMemoImmediately("Test memo")
        advanceTimeBy(1L)
        
        // Then
        verify { memoSaveStateObserver.onChanged(DetailViewModel.MemoSaveState.Saving) }
        verify { 
            memoSaveStateObserver.onChanged(
                match<DetailViewModel.MemoSaveState> { 
                    it is DetailViewModel.MemoSaveState.Error && 
                    it.message.contains(errorMessage) 
                }
            ) 
        }
    }

    @Test
    fun `getCurrentMemo should return current item memo`() = runTest {
        // Given
        val testMemo = "Test memo from item"
        val itemWithMemo = testClothItem.copy(memo = testMemo)
        
        // リポジトリのモックを設定
        coEvery { repository.getItemById(1L) } returns itemWithMemo
        
        // When
        viewModel.loadClothItem(1L)
        advanceTimeBy(1L) // 非同期処理の完了を待つ
        
        // Then
        assertEquals(testMemo, viewModel.getCurrentMemo())
    }

    @Test
    fun `getCurrentMemo should return empty string when no item`() {
        // Given - アイテムがロードされていない状態
        
        // When
        val result = viewModel.getCurrentMemo()
        
        // Then
        assertEquals("", result)
    }

    @Test
    fun `clearMemoSaveState should reset state to Idle`() {
        // Given
        val memoSaveStateObserver: Observer<DetailViewModel.MemoSaveState> = mockk(relaxed = true)
        viewModel.memoSaveState.observeForever(memoSaveStateObserver)
        
        // When
        viewModel.clearMemoSaveState()
        
        // Then
        verify { memoSaveStateObserver.onChanged(DetailViewModel.MemoSaveState.Idle) }
    }

    @Test
    fun `memo with over max length should be trimmed`() = runTest {
        // Given
        val longMemo = "a".repeat(ClothItem.MAX_MEMO_LENGTH + 100)
        
        // アイテムをロード
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        
        // When
        viewModel.saveMemoImmediately(longMemo)
        advanceTimeBy(1L)
        
        // Then
        val updatedItemSlot = slot<ClothItem>()
        coVerify { repository.updateItem(capture(updatedItemSlot)) }
        assertEquals(ClothItem.MAX_MEMO_LENGTH, updatedItemSlot.captured.memo.length)
        assertEquals(longMemo.take(ClothItem.MAX_MEMO_LENGTH), updatedItemSlot.captured.memo)
    }

    // ===== 既存機能テスト（メモ機能統合確認） =====

    @Test
    fun `loadClothItem should load item with memo`() = runTest {
        // Given
        val itemObserver: Observer<ClothItem?> = mockk(relaxed = true)
        viewModel.clothItem.observeForever(itemObserver)
        
        // When
        viewModel.loadClothItem(1L)
        advanceTimeBy(1L)
        
        // Then
        coVerify { repository.getItemById(1L) }
        verify { itemObserver.onChanged(testClothItem) }
        assertEquals("Test memo", viewModel.getCurrentMemo())
    }

    @Test
    fun `memo update should update clothItem LiveData`() = runTest {
        // Given
        val newMemo = "Updated memo content"
        val itemObserver: Observer<ClothItem?> = mockk(relaxed = true)
        viewModel.clothItem.observeForever(itemObserver)
        
        // アイテムをロード
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        
        // When
        viewModel.saveMemoImmediately(newMemo)
        advanceTimeBy(1L)
        
        // Then
        verify { 
            itemObserver.onChanged(
                match<ClothItem> { item -> 
                    item?.memo == newMemo 
                }
            ) 
        }
    }
}
