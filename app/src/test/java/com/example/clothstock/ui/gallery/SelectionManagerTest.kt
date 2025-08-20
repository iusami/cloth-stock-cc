package com.example.clothstock.ui.gallery

import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.junit.runner.RunWith
import java.util.Date

/**
 * SelectionManager テスト
 * 
 * PRレビュー対応: 関心の分離により、選択ロジックを独立してテスト
 */
@RunWith(MockitoJUnitRunner::class)
class SelectionManagerTest {

    @Mock
    private lateinit var mockSelectionListener: (ClothItem, Boolean) -> Unit
    
    @Mock
    private lateinit var mockLongPressListener: (ClothItem) -> Unit

    private lateinit var selectionManager: SelectionManager
    
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
        selectionManager = SelectionManager()
    }

    // ===== 基本選択機能テスト =====

    @Test
    fun `初期状態_選択モードfalse_選択アイテム空`() {
        assertFalse("選択モード初期値はfalse", selectionManager.isSelectionMode)
        assertTrue("選択アイテム初期値は空", selectionManager.selectedItems.isEmpty())
    }

    @Test
    fun `setSelectionMode_true設定で選択モード有効`() {
        val changed = selectionManager.setSelectionMode(true)
        
        assertTrue("選択モードが有効になる", selectionManager.isSelectionMode)
        assertTrue("変更フラグがtrueを返す", changed)
    }

    @Test
    fun `setSelectionMode_false設定で選択状態クリア`() {
        selectionManager.setSelectionMode(true)
        selectionManager.selectItem(testClothItem1.id)
        
        val changed = selectionManager.setSelectionMode(false)
        
        assertFalse("選択モードが無効になる", selectionManager.isSelectionMode)
        assertTrue("選択状態がクリアされる", selectionManager.selectedItems.isEmpty())
        assertTrue("変更フラグがtrueを返す", changed)
    }

    @Test
    fun `selectItem_アイテムが選択される`() {
        selectionManager.setSelectionMode(true)
        
        val added = selectionManager.selectItem(testClothItem1.id)
        
        assertTrue("アイテムが追加される", added)
        assertTrue("アイテムが選択される", selectionManager.isItemSelected(testClothItem1.id))
        assertEquals("選択アイテム数が1", 1, selectionManager.selectedItems.size)
    }

    @Test
    fun `selectItem_重複選択時false返却`() {
        selectionManager.setSelectionMode(true)
        selectionManager.selectItem(testClothItem1.id)
        
        val added = selectionManager.selectItem(testClothItem1.id)
        
        assertFalse("重複選択時falseを返す", added)
        assertEquals("選択数は変わらない", 1, selectionManager.selectedItems.size)
    }

    @Test
    fun `deselectItem_選択アイテムが解除される`() {
        selectionManager.setSelectionMode(true)
        selectionManager.selectItem(testClothItem1.id)
        
        val removed = selectionManager.deselectItem(testClothItem1.id)
        
        assertTrue("アイテムが削除される", removed)
        assertFalse("アイテム選択が解除される", selectionManager.isItemSelected(testClothItem1.id))
        assertTrue("選択アイテムが空になる", selectionManager.selectedItems.isEmpty())
    }

    @Test
    fun `toggleItemSelection_未選択から選択へ`() {
        selectionManager.setSelectionMode(true)
        
        val isSelected = selectionManager.toggleItemSelection(testClothItem1.id)
        
        assertTrue("選択状態になる", isSelected)
        assertTrue("アイテムが選択される", selectionManager.isItemSelected(testClothItem1.id))
    }

    @Test
    fun `toggleItemSelection_選択から未選択へ`() {
        selectionManager.setSelectionMode(true)
        selectionManager.selectItem(testClothItem1.id)
        
        val isSelected = selectionManager.toggleItemSelection(testClothItem1.id)
        
        assertFalse("未選択状態になる", isSelected)
        assertFalse("アイテム選択が解除される", selectionManager.isItemSelected(testClothItem1.id))
    }

    @Test
    fun `clearSelection_すべての選択がクリア`() {
        selectionManager.setSelectionMode(true)
        selectionManager.selectItem(testClothItem1.id)
        selectionManager.selectItem(testClothItem2.id)
        
        val previouslySelected = selectionManager.clearSelection()
        
        assertTrue("すべての選択がクリアされる", selectionManager.selectedItems.isEmpty())
        assertEquals("以前の選択状態を返す", 2, previouslySelected.size)
        assertTrue("以前の選択アイテムを含む", previouslySelected.containsAll(setOf(testClothItem1.id, testClothItem2.id)))
    }

    // ===== 長押し処理テスト =====

    @Test
    fun `handleLongPress_選択モード無効時_選択モード開始`() {
        selectionManager.longPressListener = mockLongPressListener
        
        selectionManager.handleLongPress(testClothItem1)
        
        assertTrue("選択モードが有効になる", selectionManager.isSelectionMode)
        assertTrue("対象アイテムが選択される", selectionManager.isItemSelected(testClothItem1.id))
        verify(mockLongPressListener).invoke(testClothItem1)
    }

    @Test
    fun `handleLongPress_選択モード有効時_選択状態切り替え`() {
        selectionManager.setSelectionMode(true)
        selectionManager.selectionListener = mockSelectionListener
        
        // 未選択から選択へ
        selectionManager.handleLongPress(testClothItem1)
        
        assertTrue("アイテムが選択される", selectionManager.isItemSelected(testClothItem1.id))
        verify(mockSelectionListener).invoke(testClothItem1, true)
    }

    @Test
    fun `handleLongPress_選択済みアイテム_選択解除`() {
        selectionManager.setSelectionMode(true)
        selectionManager.selectItem(testClothItem1.id)
        selectionManager.selectionListener = mockSelectionListener
        
        selectionManager.handleLongPress(testClothItem1)
        
        assertFalse("アイテム選択が解除される", selectionManager.isItemSelected(testClothItem1.id))
        verify(mockSelectionListener).invoke(testClothItem1, false)
    }

    // ===== リスナー設定テスト =====

    @Test
    fun `selectionListener設定確認`() {
        selectionManager.selectionListener = mockSelectionListener
        
        assertNotNull("selectionListenerが設定される", selectionManager.selectionListener)
    }

    @Test
    fun `longPressListener設定確認`() {
        selectionManager.longPressListener = mockLongPressListener
        
        assertNotNull("longPressListenerが設定される", selectionManager.longPressListener)
    }
}
