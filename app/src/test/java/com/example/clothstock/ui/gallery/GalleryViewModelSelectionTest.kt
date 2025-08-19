package com.example.clothstock.ui.gallery

import androidx.lifecycle.SavedStateHandle
import com.example.clothstock.data.model.SelectionState
import com.example.clothstock.data.model.DeletionResult
import com.example.clothstock.data.preferences.FilterPreferencesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock

/**
 * GalleryViewModel の選択状態管理機能のテストクラス
 * 
 * 削除機能の基盤となる選択状態管理をテスト
 */
@ExperimentalCoroutinesApi
class GalleryViewModelSelectionTest : GalleryViewModelTestBase() {

    @Mock
    private lateinit var savedStateHandle: SavedStateHandle
    
    @Mock
    private lateinit var filterPreferencesManager: FilterPreferencesManager

    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setUpSelection() {
        super.setUp()
        setupBasicRepositoryMocks()
        initializeViewModel()
    }

    private fun setupBasicRepositoryMocks() {
        // 基本的なリポジトリモックを設定（テストデータありで）
        setupBasicRepositoryMocks(
            items = testClothItems, // emptyList()から変更
            filterOptions = defaultFilterOptions
        )
    }

    private fun initializeViewModel() {
        // ViewModelを初期化
        viewModel = GalleryViewModel(
            clothRepository = clothRepository,
            filterManager = filterManager,
            savedStateHandle = savedStateHandle,
            filterPreferencesManager = filterPreferencesManager
        )
    }

    // ===== 選択状態LiveDataの初期状態テスト =====

    @Test
    fun `selectionState has correct initial state`() = runTest {
        // 初期化処理の完了を待つ
        advanceUntilIdle()
        
        val selectionState = viewModel.selectionState.value
        
        assertNotNull("SelectionState should not be null", selectionState)
        assertFalse("Initial selection mode should be false", selectionState!!.isSelectionMode)
        assertTrue("Initial selectedItemIds should be empty", selectionState.selectedItemIds.isEmpty())
        assertEquals("Initial totalSelectedCount should be 0", 0, selectionState.totalSelectedCount)
        assertFalse("Initial hasSelection should be false", selectionState.hasSelection())
    }

    @Test
    fun `isDeletionInProgress has correct initial state`() = runTest {
        advanceUntilIdle()
        
        val isDeletionInProgress = viewModel.isDeletionInProgress.value
        
        assertNotNull("isDeletionInProgress should not be null", isDeletionInProgress)
        assertFalse("Initial deletion progress should be false", isDeletionInProgress!!)
    }

    @Test
    fun `deletionResult has correct initial state`() = runTest {
        advanceUntilIdle()
        
        val deletionResult = viewModel.deletionResult.value
        
        assertNull("Initial deletion result should be null", deletionResult)
    }

    // ===== LiveData の観察可能性テスト =====

    @Test
    fun `selectionState LiveData is observable`() = runTest {
        advanceUntilIdle()
        
        var observedValue: SelectionState? = null
        val observer: (SelectionState) -> Unit = { observedValue = it }
        
        viewModel.selectionState.observeForever(observer)
        
        // observeForeverが設定されていることを確認
        assertTrue("SelectionState should have observer", viewModel.selectionState.hasObservers())
        
        // 値が観測されていることを確認
        assertNotNull("Observer should receive initial value", observedValue)
        
        // クリーンアップ
        viewModel.selectionState.removeObserver(observer)
    }

    @Test
    fun `isDeletionInProgress LiveData is observable`() = runTest {
        advanceUntilIdle()
        
        var observedValue: Boolean? = null
        val observer: (Boolean) -> Unit = { observedValue = it }
        
        viewModel.isDeletionInProgress.observeForever(observer)
        
        assertTrue("isDeletionInProgress should have observer", viewModel.isDeletionInProgress.hasObservers())
        assertNotNull("Observer should receive initial value", observedValue)
        
        viewModel.isDeletionInProgress.removeObserver(observer)
    }

    @Test
    fun `deletionResult LiveData is observable`() = runTest {
        advanceUntilIdle()
        
        var observedValue: DeletionResult? = null
        val observer: (DeletionResult?) -> Unit = { observedValue = it }
        
        viewModel.deletionResult.observeForever(observer)
        
        assertTrue("deletionResult should have observer", viewModel.deletionResult.hasObservers())
        // 初期状態はnullなので、observedValueがnullであることを確認
        assertNull("Initial deletion result should be null", observedValue)
        
        viewModel.deletionResult.removeObserver(observer)
    }

    // ===== 選択状態のデータ型テスト =====

    @Test
    fun `selectionState returns SelectionState type`() = runTest {
        advanceUntilIdle()
        
        val selectionState = viewModel.selectionState.value
        
        assertTrue("selectionState should be SelectionState type", 
                  selectionState is SelectionState)
    }

    @Test
    fun `isDeletionInProgress returns Boolean type`() = runTest {
        advanceUntilIdle()
        
        val isDeletionInProgress = viewModel.isDeletionInProgress.value
        
        assertTrue("isDeletionInProgress should be Boolean type", 
                  isDeletionInProgress is Boolean)
    }

    @Test
    fun `deletionResult returns DeletionResult nullable type`() = runTest {
        advanceUntilIdle()
        
        val deletionResult = viewModel.deletionResult.value
        
        // 初期状態ではnullだが、型自体はDeletionResult?であることを確認
        assertTrue("deletionResult should accept DeletionResult type", 
                  deletionResult == null || deletionResult is DeletionResult)
    }

    // ===== 選択操作メソッドテスト（RED段階） =====

    @Test
    fun `enterSelectionMode should set selection mode to true and add initial item`() = runTest {
        advanceUntilIdle()
        
        val testItemId = 1L
        
        // 選択モードに入る
        viewModel.enterSelectionMode(testItemId)
        advanceUntilIdle()
        
        val selectionState = viewModel.selectionState.value
        
        assertNotNull("SelectionState should not be null", selectionState)
        assertTrue("Selection mode should be true", selectionState!!.isSelectionMode)
        assertTrue("Initial item should be selected", selectionState.selectedItemIds.contains(testItemId))
        assertEquals("Selected count should be 1", 1, selectionState.totalSelectedCount)
        assertTrue("Should have selection", selectionState.hasSelection())
    }

    @Test
    fun `toggleItemSelection should add item when not selected`() = runTest {
        advanceUntilIdle()
        
        val testItemId = 2L
        
        // 選択モードに入り、別のアイテムを選択
        viewModel.enterSelectionMode(1L)
        advanceUntilIdle()
        
        // アイテムを追加選択
        viewModel.toggleItemSelection(testItemId)
        advanceUntilIdle()
        
        val selectionState = viewModel.selectionState.value
        
        assertNotNull("SelectionState should not be null", selectionState)
        assertTrue("Item should be selected", selectionState!!.selectedItemIds.contains(testItemId))
        assertEquals("Selected count should be 2", 2, selectionState.totalSelectedCount)
    }

    @Test
    fun `toggleItemSelection should remove item when already selected`() = runTest {
        advanceUntilIdle()
        
        val testItemId = 1L
        
        // 選択モードに入る
        viewModel.enterSelectionMode(testItemId)
        advanceUntilIdle()
        
        // 同じアイテムを再度選択（削除）
        viewModel.toggleItemSelection(testItemId)
        advanceUntilIdle()
        
        val selectionState = viewModel.selectionState.value
        
        assertNotNull("SelectionState should not be null", selectionState)
        assertFalse("Item should not be selected", selectionState!!.selectedItemIds.contains(testItemId))
        assertEquals("Selected count should be 0", 0, selectionState.totalSelectedCount)
        assertFalse("Should not have selection", selectionState.hasSelection())
    }

    @Test
    fun `clearSelection should reset selection state`() = runTest {
        advanceUntilIdle()
        
        // 複数アイテムを選択
        viewModel.enterSelectionMode(1L)
        viewModel.toggleItemSelection(2L)
        viewModel.toggleItemSelection(3L)
        advanceUntilIdle()
        
        // 選択をクリア
        viewModel.clearSelection()
        advanceUntilIdle()
        
        val selectionState = viewModel.selectionState.value
        
        assertNotNull("SelectionState should not be null", selectionState)
        assertFalse("Selection mode should be false", selectionState!!.isSelectionMode)
        assertTrue("Selected items should be empty", selectionState.selectedItemIds.isEmpty())
        assertEquals("Selected count should be 0", 0, selectionState.totalSelectedCount)
        assertFalse("Should not have selection", selectionState.hasSelection())
    }

    @Test
    fun `getSelectedItems should return correct ClothItem objects`() = runTest {
        advanceUntilIdle()
        
        val testItemId1 = 1L
        val testItemId2 = 2L
        
        // 選択モードに入り、複数アイテムを選択
        viewModel.enterSelectionMode(testItemId1)
        viewModel.toggleItemSelection(testItemId2)
        advanceUntilIdle()
        
        // 選択されたアイテムを取得
        val selectedItems = viewModel.getSelectedItems()
        
        assertEquals("Selected items count should be 2", 2, selectedItems.size)
        assertTrue("Should contain item 1", selectedItems.any { it.id == testItemId1 })
        assertTrue("Should contain item 2", selectedItems.any { it.id == testItemId2 })
    }

    @Test
    fun `enterSelectionMode with invalid item id should handle gracefully`() = runTest {
        advanceUntilIdle()
        
        val invalidItemId = -1L
        
        // 無効なIDで選択モードに入る
        viewModel.enterSelectionMode(invalidItemId)
        advanceUntilIdle()
        
        val selectionState = viewModel.selectionState.value
        
        assertNotNull("SelectionState should not be null", selectionState)
        // 無効IDでも選択モードには入るが、実際の選択は行われない可能性
        assertTrue("Selection mode should be true", selectionState!!.isSelectionMode)
    }

    // ===== REFACTOR段階: エラーハンドリング・エッジケース・パフォーマンステスト =====

    @Test
    fun `toggleItemSelection with non-existing item should handle gracefully`() = runTest {
        advanceUntilIdle()
        
        val nonExistingItemId = 999L
        
        // 選択モードに入る
        viewModel.enterSelectionMode(1L)
        advanceUntilIdle()
        
        val initialCount = viewModel.selectionState.value?.totalSelectedCount ?: 0
        
        // 存在しないアイテムをトグル
        viewModel.toggleItemSelection(nonExistingItemId)
        advanceUntilIdle()
        
        val selectionState = viewModel.selectionState.value
        
        assertNotNull("SelectionState should not be null", selectionState)
        // 存在しないアイテムでも選択リストに追加される（仕様として想定）
        assertEquals(
            "Selection count should increase by 1", 
            initialCount + 1, 
            selectionState!!.totalSelectedCount
        )
        assertTrue(
            "Non-existing item should be in selection", 
            selectionState.selectedItemIds.contains(nonExistingItemId)
        )
    }

    @Test
    fun `multiple rapid toggleItemSelection should maintain consistency`() = runTest {
        advanceUntilIdle()
        
        val testItemId = 1L
        
        // 選択モードに入る
        viewModel.enterSelectionMode(testItemId)
        advanceUntilIdle()
        
        // 同じアイテムを複数回高速トグル
        viewModel.toggleItemSelection(testItemId) // 削除
        viewModel.toggleItemSelection(testItemId) // 追加
        viewModel.toggleItemSelection(testItemId) // 削除
        advanceUntilIdle()
        
        val selectionState = viewModel.selectionState.value
        
        assertNotNull("SelectionState should not be null", selectionState)
        assertFalse(
            "Item should not be selected after odd number of toggles", 
            selectionState!!.selectedItemIds.contains(testItemId)
        )
        assertEquals("Selection count should be 0", 0, selectionState.totalSelectedCount)
    }

    @Test
    fun `large selection set performance test`() = runTest {
        advanceUntilIdle()
        
        val largeItemCount = 100
        
        // 選択モードに入る
        viewModel.enterSelectionMode(1L)
        advanceUntilIdle()
        
        // 大量のアイテムを選択
        for (i in 2L..largeItemCount) {
            viewModel.toggleItemSelection(i)
        }
        advanceUntilIdle()
        
        val selectionState = viewModel.selectionState.value
        
        assertNotNull("SelectionState should not be null", selectionState)
        assertEquals("Selection count should match large count", largeItemCount, selectionState!!.totalSelectedCount)
        assertTrue("Should have selection", selectionState.hasSelection())
    }

    @Test
    fun `clearSelection after multiple operations should reset completely`() = runTest {
        advanceUntilIdle()
        
        // 複雑な選択操作を実行
        viewModel.enterSelectionMode(1L)
        viewModel.toggleItemSelection(2L)
        viewModel.toggleItemSelection(3L)
        viewModel.toggleItemSelection(1L) // 削除
        viewModel.toggleItemSelection(4L)
        advanceUntilIdle()
        
        // クリア前の状態確認
        val beforeClear = viewModel.selectionState.value
        assertNotNull("State should not be null before clear", beforeClear)
        assertTrue("Should have selection before clear", beforeClear!!.hasSelection())
        
        // クリア実行
        viewModel.clearSelection()
        advanceUntilIdle()
        
        val afterClear = viewModel.selectionState.value
        
        assertNotNull("State should not be null after clear", afterClear)
        assertFalse("Selection mode should be false", afterClear!!.isSelectionMode)
        assertTrue("Selected items should be empty", afterClear.selectedItemIds.isEmpty())
        assertEquals("Selection count should be 0", 0, afterClear.totalSelectedCount)
        assertFalse("Should not have selection", afterClear.hasSelection())
    }

    @Test
    fun `getSelectedItems with empty selection should return empty list`() = runTest {
        advanceUntilIdle()
        
        // 初期状態でgetSelectedItemsを呼び出し
        val selectedItems = viewModel.getSelectedItems()
        
        assertTrue("Selected items should be empty list", selectedItems.isEmpty())
    }

    @Test
    fun `getSelectedItems should maintain order consistency`() = runTest {
        advanceUntilIdle()
        
        val itemIds = listOf(1L, 2L)
        
        // 順番に選択
        viewModel.enterSelectionMode(itemIds[0])
        viewModel.toggleItemSelection(itemIds[1])
        advanceUntilIdle()
        
        val selectedItems = viewModel.getSelectedItems()
        
        assertEquals("Selected items count should be 2", 2, selectedItems.size)
        // アイテムの順序は元のアイテムリストの順序に従う
        assertEquals("First item should have id 1", 1L, selectedItems[0].id)
        assertEquals("Second item should have id 2", 2L, selectedItems[1].id)
    }

    @Test
    fun `concurrent operations should maintain thread safety`() = runTest {
        advanceUntilIdle()
        
        // 並行操作のシミュレーション（コルーチンコンテキスト内で実行）
        viewModel.enterSelectionMode(1L)
        
        // 複数操作を並行実行
        launch { viewModel.toggleItemSelection(2L) }
        launch { viewModel.toggleItemSelection(3L) }
        launch { 
            viewModel.getSelectedItems()
            viewModel.selectionState.value
        }
        
        advanceUntilIdle()
        
        val selectionState = viewModel.selectionState.value
        
        assertNotNull("SelectionState should not be null", selectionState)
        assertTrue("Should have selection", selectionState!!.hasSelection())
        assertTrue("Selection count should be > 0", selectionState.totalSelectedCount > 0)
    }

    @Test
    fun `SelectionState immutability test`() = runTest {
        advanceUntilIdle()
        
        viewModel.enterSelectionMode(1L)
        advanceUntilIdle()
        
        val state1 = viewModel.selectionState.value
        
        viewModel.toggleItemSelection(2L)
        advanceUntilIdle()
        
        val state2 = viewModel.selectionState.value
        
        assertNotNull("State1 should not be null", state1)
        assertNotNull("State2 should not be null", state2)
        assertNotSame("States should be different instances", state1, state2)
        assertNotEquals("States should have different content", state1!!.selectedItemIds, state2!!.selectedItemIds)
    }

    // ===== Task4: 一括削除機能テスト（RED段階） =====

    @Test
    fun `deleteSelectedItems should start deletion process`() = runTest {
        advanceUntilIdle()
        
        // アイテムを選択
        viewModel.enterSelectionMode(1L)
        viewModel.toggleItemSelection(2L)
        advanceUntilIdle()
        
        // 削除実行前の状態確認
        assertFalse("Deletion should not be in progress initially", 
                   viewModel.isDeletionInProgress.value == true)
        assertNull("Deletion result should be null initially", 
                  viewModel.deletionResult.value)
        
        // 削除実行
        viewModel.deleteSelectedItems()
        advanceUntilIdle()
        
        // 削除進行中の状態は実装後に確認
        // この時点では実装未完了によりテストは失敗するはず（RED段階）
    }

    @Test
    fun `deleteSelectedItems with empty selection should handle gracefully`() = runTest {
        advanceUntilIdle()
        
        // 選択なしで削除実行
        viewModel.deleteSelectedItems()
        advanceUntilIdle()
        
        // エラー処理または何もしない処理が期待される
        val deletionResult = viewModel.deletionResult.value
        // 実装完了後に適切なアサーションを追加
    }

    @Test
    fun `deleteSelectedItems should update deletion progress LiveData`() = runTest {
        advanceUntilIdle()
        
        // アイテムを選択
        viewModel.enterSelectionMode(1L)
        advanceUntilIdle()
        
        var progressStates = mutableListOf<Boolean>()
        val observer: (Boolean) -> Unit = { progressStates.add(it) }
        
        viewModel.isDeletionInProgress.observeForever(observer)
        
        // 削除実行
        viewModel.deleteSelectedItems()
        advanceUntilIdle()
        
        // 進捗状態の変化を確認（実装完了後）
        // RED段階では実装未完了によりテストは失敗
        
        viewModel.isDeletionInProgress.removeObserver(observer)
    }

    @Test
    fun `deleteSelectedItems should update deletion result LiveData`() = runTest {
        advanceUntilIdle()
        
        // アイテムを選択
        viewModel.enterSelectionMode(1L)
        viewModel.toggleItemSelection(2L)
        advanceUntilIdle()
        
        var resultValues = mutableListOf<DeletionResult?>()
        val observer: (DeletionResult?) -> Unit = { resultValues.add(it) }
        
        viewModel.deletionResult.observeForever(observer)
        
        // 削除実行
        viewModel.deleteSelectedItems()
        advanceUntilIdle()
        
        // 削除結果の更新を確認（実装完了後）
        // RED段階では実装未完了によりテストは失敗
        
        viewModel.deletionResult.removeObserver(observer)
    }

    @Test
    fun `deleteSelectedItems should clear selection after successful deletion`() = runTest {
        advanceUntilIdle()
        
        // アイテムを選択
        viewModel.enterSelectionMode(1L)
        viewModel.toggleItemSelection(2L)
        advanceUntilIdle()
        
        val beforeDeletion = viewModel.selectionState.value
        assertNotNull("Selection should exist before deletion", beforeDeletion)
        assertTrue("Should have selection before deletion", beforeDeletion!!.hasSelection())
        
        // 削除実行
        viewModel.deleteSelectedItems()
        advanceUntilIdle()
        
        // 削除成功後の選択状態クリアを確認（実装完了後）
        // RED段階では実装未完了によりテストは失敗
    }

    @Test
    fun `deleteSelectedItems should handle repository errors gracefully`() = runTest {
        advanceUntilIdle()
        
        // エラーを発生させるモック設定は実装段階で追加
        // アイテムを選択
        viewModel.enterSelectionMode(1L)
        advanceUntilIdle()
        
        // 削除実行
        viewModel.deleteSelectedItems()
        advanceUntilIdle()
        
        // エラーハンドリングの確認（実装完了後）
        // RED段階では実装未完了によりテストは失敗
    }
}
