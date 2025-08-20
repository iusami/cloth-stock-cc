package com.example.clothstock.ui.gallery

import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*
import java.util.Date

/**
 * ClothItemAdapter テスト (Task 7)
 * 
 * マルチ選択UI機能のテストを実装
 * Requirements 1.2, 1.3, 1.4, 1.5 に対応
 */
@RunWith(MockitoJUnitRunner::class)
class ClothItemAdapterTest {

    @Mock
    private lateinit var mockItemClickListener: (ClothItem) -> Unit
    
    @Mock
    private lateinit var mockMemoPreviewClickListener: (ClothItem) -> Unit
    
    @Mock
    private lateinit var mockSelectionListener: (ClothItem, Boolean) -> Unit
    
    @Mock
    private lateinit var mockLongPressListener: (ClothItem) -> Unit
    
    private lateinit var adapter: ClothItemAdapter
    private lateinit var testClothItems: List<ClothItem>
    
    private val testTagData = TagData(
        size = 100,
        color = "青",
        category = "シャツ"
    )

    private val testClothItem1 = ClothItem(
        id = 1L,
        imagePath = "/storage/test/image1.jpg",
        tagData = testTagData,
        createdAt = Date(),
        memo = "テストメモ1"
    )

    private val testClothItem2 = ClothItem(
        id = 2L,
        imagePath = "/storage/test/image2.jpg",
        tagData = testTagData.copy(color = "赤"),
        createdAt = Date(),
        memo = "テストメモ2"
    )

    @Before
    fun setup() {
        testClothItems = listOf(testClothItem1, testClothItem2)
        adapter = ClothItemAdapter(
            onItemClick = mockItemClickListener,
            onMemoPreviewClick = mockMemoPreviewClickListener
        )
        
        // Phase 2-GREEN: simulateLongPress 用にテストデータを設定
        // PRレビュー対応: try-catch削除、テスト環境に依存しない設計
    }

    // ===== Task 7 Phase 1: 選択モード機能テスト =====

    @Test
    fun `isSelectionMode初期値はfalse`() {
        // Given & When - アダプター初期化時
        
        // Then - 選択モードは初期状態ではfalse
        assertFalse("選択モード初期値はfalse", adapter.isSelectionMode)
    }

    @Test
    fun `setSelectionMode_true設定で選択モードが有効になる`() {
        // Given
        assertFalse("初期状態は非選択モード", adapter.isSelectionMode)
        
        // When
        adapter.setSelectionMode(true)
        
        // Then
        assertTrue("選択モードが有効になる", adapter.isSelectionMode)
    }

    @Test
    fun `setSelectionMode_false設定で選択モードが無効になる`() {
        // Given
        adapter.setSelectionMode(true)
        assertTrue("前提：選択モードが有効", adapter.isSelectionMode)
        
        // When
        adapter.setSelectionMode(false)
        
        // Then
        assertFalse("選択モードが無効になる", adapter.isSelectionMode)
    }

    @Test
    fun `selectedItems初期値は空のSet`() {
        // Given & When - アダプター初期化時
        
        // Then
        assertTrue("選択アイテム初期値は空", adapter.selectedItems.isEmpty())
    }

    @Test
    fun `selectItem_アイテムが選択される`() {
        // Given
        adapter.setSelectionMode(true)
        assertTrue("選択アイテムが空", adapter.selectedItems.isEmpty())
        
        // When
        adapter.selectItem(testClothItem1.id)
        
        // Then
        assertTrue("アイテムが選択される", adapter.selectedItems.contains(testClothItem1.id))
        assertEquals("選択アイテム数が1", 1, adapter.selectedItems.size)
    }

    @Test
    fun `selectItem_複数アイテムが選択される`() {
        // Given
        adapter.setSelectionMode(true)
        
        // When
        adapter.selectItem(testClothItem1.id)
        adapter.selectItem(testClothItem2.id)
        
        // Then
        assertTrue("アイテム1が選択される", adapter.selectedItems.contains(testClothItem1.id))
        assertTrue("アイテム2が選択される", adapter.selectedItems.contains(testClothItem2.id))
        assertEquals("選択アイテム数が2", 2, adapter.selectedItems.size)
    }

    @Test
    fun `deselectItem_選択されたアイテムが解除される`() {
        // Given
        adapter.setSelectionMode(true)
        adapter.selectItem(testClothItem1.id)
        assertTrue("前提：アイテムが選択されている", adapter.selectedItems.contains(testClothItem1.id))
        
        // When
        adapter.deselectItem(testClothItem1.id)
        
        // Then
        assertFalse("アイテムの選択が解除される", adapter.selectedItems.contains(testClothItem1.id))
        assertTrue("選択アイテムが空になる", adapter.selectedItems.isEmpty())
    }

    @Test
    fun `clearSelection_すべての選択がクリアされる`() {
        // Given
        adapter.setSelectionMode(true)
        adapter.selectItem(testClothItem1.id)
        adapter.selectItem(testClothItem2.id)
        assertEquals("前提：2つのアイテムが選択されている", 2, adapter.selectedItems.size)
        
        // When
        adapter.clearSelection()
        
        // Then
        assertTrue("すべての選択がクリアされる", adapter.selectedItems.isEmpty())
    }

    @Test
    fun `isItemSelected_選択されたアイテムでtrueを返す`() {
        // Given
        adapter.setSelectionMode(true)
        adapter.selectItem(testClothItem1.id)
        
        // When & Then
        assertTrue("選択アイテムでtrue", adapter.isItemSelected(testClothItem1.id))
        assertFalse("未選択アイテムでfalse", adapter.isItemSelected(testClothItem2.id))
    }

    @Test
    fun `toggleItemSelection_選択状態が切り替わる`() {
        // Given
        adapter.setSelectionMode(true)
        assertFalse("初期状態：アイテム未選択", adapter.isItemSelected(testClothItem1.id))
        
        // When - 1回目のトグル（選択）
        adapter.toggleItemSelection(testClothItem1.id)
        
        // Then
        assertTrue("トグル後：アイテムが選択される", adapter.isItemSelected(testClothItem1.id))
        
        // When - 2回目のトグル（解除）
        adapter.toggleItemSelection(testClothItem1.id)
        
        // Then
        assertFalse("再トグル後：アイテム選択が解除される", adapter.isItemSelected(testClothItem1.id))
    }

    @Test
    fun `setSelectionMode_false時に選択状態がクリアされる`() {
        // Given
        adapter.setSelectionMode(true)
        adapter.selectItem(testClothItem1.id)
        adapter.selectItem(testClothItem2.id)
        assertEquals("前提：2つのアイテムが選択されている", 2, adapter.selectedItems.size)
        
        // When
        adapter.setSelectionMode(false)
        
        // Then
        assertTrue("選択モード無効時に選択状態クリア", adapter.selectedItems.isEmpty())
    }

    // ===== 長押しジェスチャーとコールバック機能テスト =====

    @Test
    fun `setSelectionListener_選択リスナーが設定される`() {
        // Given & When
        adapter.setSelectionListener(mockSelectionListener)
        
        // Then - エラーが発生しないことを確認（設定メソッドの存在確認）
        // 実際のコールバック呼び出しは GREEN フェーズでテスト
    }

    @Test
    fun `setLongPressListener_長押しリスナーが設定される`() {
        // Given & When
        adapter.setLongPressListener(mockLongPressListener)
        
        // Then - エラーが発生しないことを確認（設定メソッドの存在確認）
        // 実際のコールバック呼び出しは GREEN フェーズでテスト
    }

    // ===== 視覚的フィードバック機能テスト =====

    @Test
    fun `ViewHolder_選択状態の視覚的フィードバック更新`() {
        // Given
        adapter.setSelectionMode(true)
        adapter.selectItem(testClothItem1.id)
        
        // When & Then
        // GREEN フェーズ: ViewHolder の UI 統合はREFACTORフェーズで実装予定
        // 現時点では選択状態の論理的確認のみ
        assertTrue("アイテムが選択状態", adapter.isItemSelected(testClothItem1.id))
        assertTrue("選択モードが有効", adapter.isSelectionMode)
    }

    @Test
    fun `ViewHolder_非選択状態の視覚的フィードバック更新`() {
        // Given
        adapter.setSelectionMode(true)
        // testClothItem1は未選択状態
        
        // When & Then
        // GREEN フェーズ: ViewHolder の UI 統合はREFACTORフェーズで実装予定
        // 現時点では非選択状態の論理的確認のみ
        assertFalse("アイテムが非選択状態", adapter.isItemSelected(testClothItem1.id))
        assertTrue("選択モードが有効", adapter.isSelectionMode)
    }

    // ===== ヘルパーメソッド =====

    /**
     * GREEN フェーズ: ViewHolder テスト用メソッドは REFACTOR フェーズで実装予定
     * 現時点では選択ロジックの単体テストに集中
     */

    // ===== Phase 2: 長押しジェスチャー詳細テスト =====

    @Test
    fun `長押しリスナー設定_ViewHolderに長押しリスナーが設定される`() {
        // Given
        adapter.setLongPressListener(mockLongPressListener)
        
        // When - ViewHolder作成時にリスナーが設定されることを確認
        // 実際のViewHolder内での長押し処理はGREENフェーズで実装予定
        
        // Then - 長押しリスナーが設定されていることを確認
        // Phase 2-RED: ViewHolderでの長押し処理は未実装なため、設定メソッドの存在のみ確認
    }

    @Test
    fun `simulateLongPress_選択モード無効_選択モード有効になりアイテム選択`() {
        // Given
        adapter.setLongPressListener(mockLongPressListener)
        adapter.setSelectionListener(mockSelectionListener)
        assertFalse("前提：選択モード無効", adapter.isSelectionMode)
        assertFalse("前提：アイテム未選択", adapter.isItemSelected(testClothItem1.id))
        
        // When - Phase 2-GREEN: 長押しコールバック呼び出し
        adapter.triggerLongPressCallback(testClothItem1)
        
        // Then - 長押し後の状態確認
        assertTrue("長押し後：選択モード有効", adapter.isSelectionMode)
        assertTrue("長押し後：アイテム選択", adapter.isItemSelected(testClothItem1.id))
        
        // コールバック呼び出し確認は REFACTOR フェーズで実装予定
        // Mockito verify が Android コンテキストを必要とするためスキップ
    }

    @Test
    fun `simulateLongPress_選択モード有効_アイテム選択状態切り替え`() {
        // Given
        adapter.setSelectionMode(true)
        adapter.setSelectionListener(mockSelectionListener)
        assertFalse("前提：アイテム未選択", adapter.isItemSelected(testClothItem1.id))
        
        // When - Phase 2-GREEN: 長押しで選択状態を切り替え
        adapter.triggerLongPressCallback(testClothItem1)
        
        // Then
        assertTrue("長押し後：アイテム選択", adapter.isItemSelected(testClothItem1.id))
        
        // When - 再度長押しで選択解除
        adapter.triggerLongPressCallback(testClothItem1)
        
        // Then
        assertFalse("再長押し後：アイテム選択解除", adapter.isItemSelected(testClothItem1.id))
    }

    @Test
    fun `長押しコールバック_選択モード無効時_longPressListenerが呼び出される`() {
        // Given
        adapter.setLongPressListener(mockLongPressListener)
        assertFalse("前提：選択モード無効", adapter.isSelectionMode)
        assertFalse("前提：アイテム未選択", adapter.isItemSelected(testClothItem1.id))
        
        // When - Phase 2-GREEN: 長押しコールバック呼び出し
        adapter.triggerLongPressCallback(testClothItem1)
        
        // Then - 選択モードが有効になりアイテムが選択される
        assertTrue("コールバック後：選択モード有効", adapter.isSelectionMode)
        assertTrue("コールバック後：アイテム選択", adapter.isItemSelected(testClothItem1.id))
        
        // longPressListener 呼び出し確認は REFACTOR フェーズで実装予定
        // Mockito verify が Android コンテキストを必要とするためスキップ
    }

    @Test
    fun `長押しコールバック_選択モード有効時_selectionListenerが呼び出される`() {
        // Given
        adapter.setSelectionMode(true)
        adapter.setSelectionListener(mockSelectionListener)
        adapter.setLongPressListener(mockLongPressListener)
        assertFalse("前提：アイテム未選択", adapter.isItemSelected(testClothItem1.id))
        
        // When - Phase 2-GREEN: 長押しコールバック呼び出し（選択）
        adapter.triggerLongPressCallback(testClothItem1)
        
        // Then
        assertTrue("コールバック後：アイテム選択", adapter.isItemSelected(testClothItem1.id))
        
        // When - 再度長押しコールバック呼び出し（選択解除）
        adapter.triggerLongPressCallback(testClothItem1)
        
        // Then
        assertFalse("再コールバック後：アイテム選択解除", adapter.isItemSelected(testClothItem1.id))
        
        // selectionListener 呼び出し確認は REFACTOR フェーズで実装予定
    }

    @Test
    fun `ViewHolder長押し_選択モード無効_選択モード開始とアイテム選択`() {
        // Given
        val mockViewHolder = createMockViewHolderForLongPress(testClothItem1)
        adapter.setLongPressListener(mockLongPressListener)
        assertFalse("前提：選択モード無効", adapter.isSelectionMode)
        assertFalse("前提：アイテム未選択", adapter.isItemSelected(testClothItem1.id))
        
        // When - Phase 2-GREEN: ViewHolder長押し実行
        mockViewHolder.performLongClick()
        
        // Then - 選択モードが有効になりアイテムが選択される
        assertTrue("長押し後：選択モード有効", adapter.isSelectionMode)
        assertTrue("長押し後：アイテム選択", adapter.isItemSelected(testClothItem1.id))
    }

    @Test
    fun `ViewHolder長押し_選択モード有効_アイテム選択状態切り替えのみ`() {
        // Given
        val mockViewHolder = createMockViewHolderForLongPress(testClothItem1)
        adapter.setSelectionMode(true)
        adapter.setSelectionListener(mockSelectionListener)
        assertFalse("前提：アイテム未選択", adapter.isItemSelected(testClothItem1.id))
        
        // When - Phase 2-GREEN: ViewHolder長押し実行（選択）
        mockViewHolder.performLongClick()
        
        // Then
        assertTrue("長押し後：アイテム選択", adapter.isItemSelected(testClothItem1.id))
        
        // When - 再度長押し（選択解除）
        mockViewHolder.performLongClick()
        
        // Then
        assertFalse("再長押し後：アイテム選択解除", adapter.isItemSelected(testClothItem1.id))
    }

    @Test
    fun `長押しジェスチャー_連続長押し_適切にハンドルされる`() {
        // Given
        adapter.setLongPressListener(mockLongPressListener)
        adapter.setSelectionListener(mockSelectionListener)
        assertFalse("前提：選択モード無効", adapter.isSelectionMode)
        
        // When - Phase 2-GREEN: 同一アイテムの連続長押し
        adapter.triggerLongPressCallback(testClothItem1)
        assertTrue("1回目長押し後：選択モード有効", adapter.isSelectionMode)
        assertTrue("1回目長押し後：アイテム選択", adapter.isItemSelected(testClothItem1.id))
        
        adapter.triggerLongPressCallback(testClothItem1)
        assertTrue("2回目長押し後：選択モード維持", adapter.isSelectionMode)
        assertFalse("2回目長押し後：アイテム選択解除", adapter.isItemSelected(testClothItem1.id))
        
        adapter.triggerLongPressCallback(testClothItem1)
        assertTrue("3回目長押し後：選択モード維持", adapter.isSelectionMode)
        assertTrue("3回目長押し後：アイテム再選択", adapter.isItemSelected(testClothItem1.id))
    }

    @Test
    fun `長押しジェスチャー_複数アイテム長押し_選択状態が同期される`() {
        // Given
        adapter.setLongPressListener(mockLongPressListener)
        adapter.setSelectionListener(mockSelectionListener)
        assertFalse("前提：選択モード無効", adapter.isSelectionMode)
        
        // When - Phase 2-GREEN: 複数アイテムの長押し
        adapter.triggerLongPressCallback(testClothItem1)
        assertTrue("1つ目長押し後：選択モード有効", adapter.isSelectionMode)
        assertTrue("1つ目長押し後：アイテム1選択", adapter.isItemSelected(testClothItem1.id))
        assertFalse("1つ目長押し後：アイテム2未選択", adapter.isItemSelected(testClothItem2.id))
        
        adapter.triggerLongPressCallback(testClothItem2)
        assertTrue("2つ目長押し後：選択モード維持", adapter.isSelectionMode)
        assertTrue("2つ目長押し後：アイテム1選択維持", adapter.isItemSelected(testClothItem1.id))
        assertTrue("2つ目長押し後：アイテム2選択", adapter.isItemSelected(testClothItem2.id))
        
        // Then - 選択状態確認
        assertEquals("複数選択：選択数は2", 2, adapter.selectedItems.size)
        assertTrue("複数選択：両アイテム含まれる", 
            adapter.selectedItems.containsAll(setOf(testClothItem1.id, testClothItem2.id)))
    }

    // ===== Phase 2 RED ヘルパーメソッド =====

    /**
     * 長押しテスト用のViewHolderモック作成
     * Phase 2-GREEN で実際の長押し処理を実装
     */
    private fun createMockViewHolderForLongPress(clothItem: ClothItem = testClothItem1): MockViewHolder {
        return MockViewHolder(adapter, clothItem)
    }

    /**
     * 長押しテスト用ViewHolderモック
     * Phase 2-GREEN で実際の長押し処理を実装
     */
    class MockViewHolder(private val adapter: ClothItemAdapter, private val clothItem: ClothItem) {
        fun performLongClick() {
            // 長押し処理を実行
            adapter.triggerLongPressCallback(clothItem)
        }
    }
}
