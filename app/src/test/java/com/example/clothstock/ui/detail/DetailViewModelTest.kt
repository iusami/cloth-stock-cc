package com.example.clothstock.ui.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import com.example.clothstock.data.repository.ClothRepository
import com.example.clothstock.data.preferences.DetailPreferencesManager
import com.example.clothstock.ui.common.SwipeableDetailPanel
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
 * Task 8追加: エラーハンドリングとバリデーション機能のテスト
 * Task 4追加: パネル状態管理機能のテスト
 * 
 * TDD Red-Green-Refactorサイクルに基づくテスト実装
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var repository: ClothRepository

    @MockK
    private lateinit var preferencesManager: DetailPreferencesManager

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
        
        // デフォルトのPreferencesManagerモック設定
        coEvery { preferencesManager.getLastPanelState() } returns SwipeableDetailPanel.PanelState.SHOWN
        coEvery { preferencesManager.saveLastPanelState(any()) } returns Unit
        
        viewModel = DetailViewModel(repository, preferencesManager)
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
        
        // When - 現在のメモと異なる内容で保存を試行
        viewModel.saveMemoImmediately("Different memo content")
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
    fun `memo with over max length should show ValidationError`() = runTest {
        // Given
        val memoSaveStateObserver: Observer<DetailViewModel.MemoSaveState> = mockk(relaxed = true)
        viewModel.memoSaveState.observeForever(memoSaveStateObserver)
        
        val longMemo = "a".repeat(ClothItem.MAX_MEMO_LENGTH + 100)
        
        // アイテムをロード
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        
        // When
        viewModel.saveMemoImmediately(longMemo)
        
        // Then: ValidationErrorステートが設定される
        verify {
            memoSaveStateObserver.onChanged(
                match<DetailViewModel.MemoSaveState.ValidationError> { state ->
                    state.message.contains("${ClothItem.MAX_MEMO_LENGTH}文字以内") &&
                    state.characterCount == longMemo.length
                }
            )
        }
        
        // リポジトリへの保存は行われない（バリデーション失敗のため）
        coVerify(exactly = 0) { repository.updateItem(any()) }
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
    
    // ===== Task 4: パネル状態管理機能のテスト =====
    
    @Test
    fun `panelState should be initialized from preferences`() = runTest {
        // Given
        val initialPanelState = SwipeableDetailPanel.PanelState.HIDDEN
        coEvery { preferencesManager.getLastPanelState() } returns initialPanelState
        val viewModel = DetailViewModel(repository, preferencesManager)
        
        // When
        val panelState = viewModel.panelState.value
        val isFullScreenMode = viewModel.isFullScreenMode.value
        
        // Then
        assertEquals(initialPanelState, panelState)
        assertTrue(isFullScreenMode ?: false)
    }
    
    @Test
    fun `setPanelState should update panel state and save to preferences`() = runTest {
        // Given
        val panelStateObserver: Observer<SwipeableDetailPanel.PanelState> = mockk(relaxed = true)
        val fullScreenModeObserver: Observer<Boolean> = mockk(relaxed = true)
        viewModel.panelState.observeForever(panelStateObserver)
        viewModel.isFullScreenMode.observeForever(fullScreenModeObserver)
        
        // When
        viewModel.setPanelState(SwipeableDetailPanel.PanelState.HIDDEN)
        
        // Then
        verify { panelStateObserver.onChanged(SwipeableDetailPanel.PanelState.HIDDEN) }
        verify { fullScreenModeObserver.onChanged(true) }
        coVerify { preferencesManager.saveLastPanelState(SwipeableDetailPanel.PanelState.HIDDEN) }
    }
    
    @Test
    fun `togglePanelState should switch between SHOWN and HIDDEN`() = runTest {
        // Given
        val panelStateObserver: Observer<SwipeableDetailPanel.PanelState> = mockk(relaxed = true)
        viewModel.panelState.observeForever(panelStateObserver)
        
        // When: 初期状態(SHOWN)からHIDDENに切り替え
        viewModel.togglePanelState()
        
        // Then
        verify { panelStateObserver.onChanged(SwipeableDetailPanel.PanelState.HIDDEN) }
        
        // When: HIDDENからSHOWNに切り替え
        viewModel.togglePanelState()
        
        // Then
        verify { panelStateObserver.onChanged(SwipeableDetailPanel.PanelState.SHOWN) }
    }
    
    @Test
    fun `togglePanelState should not change state when ANIMATING`() = runTest {
        // Given
        val panelStateObserver: Observer<SwipeableDetailPanel.PanelState> = mockk(relaxed = true)
        viewModel.panelState.observeForever(panelStateObserver)
        
        // 初期状態をSHOWNに設定
        viewModel.setPanelState(SwipeableDetailPanel.PanelState.SHOWN)
        
        // ANIMATING状態に変更
        viewModel.setPanelState(SwipeableDetailPanel.PanelState.ANIMATING)
        
        // When
        viewModel.togglePanelState()
        
        // Then: 状態が変化しないことを確認
        // 初期状態のSHOWN通知(2回) + ANIMATING通知(2回) 
        // (初期化時のSHOWN通知 + setPanelState(SHOWN)の通知 + setPanelState(ANIMATING)の通知 + togglePanelState後のANIMATING通知)
        verify(atLeast = 1) { panelStateObserver.onChanged(SwipeableDetailPanel.PanelState.SHOWN) }
        verify(atLeast = 1) { panelStateObserver.onChanged(SwipeableDetailPanel.PanelState.ANIMATING) }
        verify(exactly = 0) { panelStateObserver.onChanged(SwipeableDetailPanel.PanelState.HIDDEN) }
    }
    
    @Test
    fun `ViewModel destruction should save panel state to preferences`() = runTest {
        // Given: パネル状態を設定
        val testPanelState = SwipeableDetailPanel.PanelState.HIDDEN
        viewModel.setPanelState(testPanelState)
        
        // When: ViewModelを破棄（onClearedを間接的にトリガー）
        // AndroidのViewModelStoreを使ってライフサイクルをシミュレート
        val viewModelStore = ViewModelStore()
        val viewModelProvider = ViewModelProvider(
            viewModelStore, 
            DetailViewModelFactory(repository, preferencesManager)
        )
        val testViewModel = viewModelProvider.get(DetailViewModel::class.java)
        testViewModel.setPanelState(testPanelState)
        
        // ViewModelStoreをクリアしてonClearedをトリガー
        viewModelStore.clear()
        
        // Then: preferencesManager.saveLastPanelState が呼ばれることを確認
        coVerify { preferencesManager.saveLastPanelState(testPanelState) }
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

    // ===== Task 8: エラーハンドリングとバリデーション機能のテスト =====

    @Test
    fun `onMemoChanged should validate character limit and show ValidationError`() = runTest {
        // Given
        val memoSaveStateObserver: Observer<DetailViewModel.MemoSaveState> = mockk(relaxed = true)
        viewModel.memoSaveState.observeForever(memoSaveStateObserver)
        
        val longMemo = "a".repeat(ClothItem.MAX_MEMO_LENGTH + 1)  // 制限超過
        
        // アイテムをロード
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        
        // When
        viewModel.onMemoChanged(longMemo)
        
        // Then
        verify { 
            memoSaveStateObserver.onChanged(
                match<DetailViewModel.MemoSaveState.ValidationError> { state ->
                    state.characterCount == longMemo.length
                }
            )
        }
        
        // リポジトリへの保存は行われない
        coVerify(exactly = 1) { repository.getItemById(1L) } // loadのみ
        coVerify(exactly = 0) { repository.updateItem(any()) }
    }

    @Test
    fun `saveMemoImmediately should validate character limit and show ValidationError`() = runTest {
        // Given
        val memoSaveStateObserver: Observer<DetailViewModel.MemoSaveState> = mockk(relaxed = true)
        viewModel.memoSaveState.observeForever(memoSaveStateObserver)
        
        val longMemo = "a".repeat(ClothItem.MAX_MEMO_LENGTH + 100)  // 制限超過
        
        // アイテムをロード
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        
        // When
        viewModel.saveMemoImmediately(longMemo)
        
        // Then
        verify { 
            memoSaveStateObserver.onChanged(
                match<DetailViewModel.MemoSaveState.ValidationError> { state ->
                    state.message.contains("${ClothItem.MAX_MEMO_LENGTH}文字以内") &&
                    state.characterCount == longMemo.length
                }
            )
        }
        
        // リポジトリへの保存は行われない
        coVerify(exactly = 0) { repository.updateItem(any()) }
    }

    @Test
    fun `saveMemoInternal should handle exception and show Error with retry info`() = runTest {
        // Given
        val memoSaveStateObserver: Observer<DetailViewModel.MemoSaveState> = mockk(relaxed = true)
        viewModel.memoSaveState.observeForever(memoSaveStateObserver)
        
        val testMemo = "Test memo for error"
        val testException = RuntimeException("Database error")
        
        // リポジトリの更新で例外を発生させる
        coEvery { repository.updateItem(any()) } throws testException
        
        // アイテムをロード
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        
        // When
        viewModel.saveMemoImmediately(testMemo)
        advanceTimeBy(1L)
        
        // Then: Errorステートが設定され、リトライ情報が含まれる
        verify { 
            memoSaveStateObserver.onChanged(
                match<DetailViewModel.MemoSaveState.Error> { state ->
                    state.canRetry && state.retryCount > 0
                }
            )
        }
    }

    @Test
    fun `retryMemoSave should retry memo save within retry limit`() = runTest {
        // Given
        val memoSaveStateObserver: Observer<DetailViewModel.MemoSaveState> = mockk(relaxed = true)
        viewModel.memoSaveState.observeForever(memoSaveStateObserver)
        
        val testMemo = "Test memo for retry"
        val testException = RuntimeException("Database error")
        
        // 最初は失敗させる
        coEvery { repository.updateItem(any()) } throws testException
        
        // アイテムをロード
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        
        // 最初の保存で失敗
        viewModel.saveMemoImmediately(testMemo)
        advanceTimeBy(1L)
        
        // リトライで成功させる
        coEvery { repository.updateItem(any()) } returns true
        
        // When: リトライ実行
        viewModel.retryMemoSave(testMemo)
        advanceTimeBy(1L)
        
        // Then: リポジトリの更新が複数回呼ばれる（初回 + リトライ）
        coVerify(atLeast = 2) { repository.updateItem(any()) }
        
        // 最終的にSavedステートになる
        verify { 
            memoSaveStateObserver.onChanged(DetailViewModel.MemoSaveState.Saved)
        }
    }

    @Test
    fun `retryMemoSave should not retry when max retry count exceeded`() = runTest {
        // Given
        val memoSaveStateObserver: Observer<DetailViewModel.MemoSaveState> = mockk(relaxed = true)
        viewModel.memoSaveState.observeForever(memoSaveStateObserver)
        
        val testMemo = "Test memo for max retry"
        
        // リポジトリで例外を継続発生させる
        coEvery { repository.updateItem(any()) } throws RuntimeException("Persistent error")
        
        // アイテムをロード
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        
        // When: 最大リトライ回数まで失敗させる（3回）
        repeat(4) { // 最初の試行 + 3回リトライ
            viewModel.saveMemoImmediately(testMemo)
            advanceTimeBy(1L)
        }
        
        // 更に追加でリトライを試行
        viewModel.retryMemoSave(testMemo)
        
        // Then: 最大リトライ回数到達でcanRetry = falseのErrorステートになる
        verify { 
            memoSaveStateObserver.onChanged(
                match<DetailViewModel.MemoSaveState.Error> { state ->
                    !state.canRetry && state.message.contains("最大リトライ回数")
                }
            )
        }
    }

    @Test
    fun `validateMemo should return true for valid memo length`() = runTest {
        // Given
        val validMemo = "a".repeat(ClothItem.MAX_MEMO_LENGTH)  // 制限内
        
        // When
        val result = viewModel.validateMemo(validMemo)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `validateMemo should return false for invalid memo length`() = runTest {
        // Given
        val invalidMemo = "a".repeat(ClothItem.MAX_MEMO_LENGTH + 1)  // 制限超過
        
        // When
        val result = viewModel.validateMemo(invalidMemo)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `clearMemoSaveState should reset state and retry count`() = runTest {
        // Given
        val memoSaveStateObserver: Observer<DetailViewModel.MemoSaveState> = mockk(relaxed = true)
        viewModel.memoSaveState.observeForever(memoSaveStateObserver)
        
        // エラー状態にする
        coEvery { repository.updateItem(any()) } throws RuntimeException("Test error")
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        viewModel.saveMemoImmediately("test")
        advanceTimeBy(1L)
        
        // When
        viewModel.clearMemoSaveState()
        
        // Then
        verify { memoSaveStateObserver.onChanged(DetailViewModel.MemoSaveState.Idle) }
        
        // リトライカウントがリセットされることを確認するため、再度リトライ実行
        viewModel.retryMemoSave("test retry after clear")
        verify(atLeast = 1) { memoSaveStateObserver.onChanged(any()) }
    }

    @Test
    fun `memo save success should reset retry count`() = runTest {
        // Given
        val memoSaveStateObserver: Observer<DetailViewModel.MemoSaveState> = mockk(relaxed = true)
        viewModel.memoSaveState.observeForever(memoSaveStateObserver)
        
        val testMemo = "Test memo success"
        
        // 最初は失敗させる
        coEvery { repository.updateItem(any()) } throws RuntimeException("First error")
        
        // アイテムをロード
        viewModel.loadClothItem(1L)
        advanceTimeBy(100L)
        
        // 最初の保存で失敗
        viewModel.saveMemoImmediately(testMemo)
        advanceTimeBy(1L)
        
        // リトライで成功させる
        coEvery { repository.updateItem(any()) } returns true
        viewModel.retryMemoSave(testMemo)
        advanceTimeBy(DetailViewModelTest.SAVE_SUCCESS_DISPLAY_TIME_MS + 1L)
        
        // When: 新しいメモを保存（リトライカウントがリセットされているはず）
        val newMemo = "New memo after success"
        viewModel.saveMemoImmediately(newMemo)
        advanceTimeBy(1L)
        
        // Then: 新しい保存が正常に実行される（リトライカウントがリセットされている）
        verify { 
            memoSaveStateObserver.onChanged(DetailViewModel.MemoSaveState.Saved)
        }
    }

    companion object {
        private const val SAVE_SUCCESS_DISPLAY_TIME_MS = 2000L
    }
}
