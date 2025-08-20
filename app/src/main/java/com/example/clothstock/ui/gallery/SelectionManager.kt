package com.example.clothstock.ui.gallery

import com.example.clothstock.data.model.ClothItem

/**
 * アイテム選択状態管理クラス
 * 
 * ClothItemAdapterの選択ロジックを分離して、関心の分離を実現
 * Task 7 PRレビュー対応: 保守性とテスタビリティの向上
 */
class SelectionManager {
    
    /**
     * 選択モードフラグ
     */
    var isSelectionMode: Boolean = false
        private set

    /**
     * 選択されたアイテムのIDセット
     */
    private val _selectedItems = mutableSetOf<Long>()
    val selectedItems: Set<Long> get() = _selectedItems.toSet()

    /**
     * 選択状態変更リスナー
     */
    var selectionListener: ((ClothItem, Boolean) -> Unit)? = null

    /**
     * 長押しリスナー
     */
    var longPressListener: ((ClothItem) -> Unit)? = null

    /**
     * 選択モード設定
     */
    fun setSelectionMode(enabled: Boolean): Boolean {
        val changed = isSelectionMode != enabled
        isSelectionMode = enabled
        if (!enabled) {
            clearSelection()
        }
        return changed
    }

    /**
     * アイテム選択
     */
    fun selectItem(itemId: Long): Boolean {
        return _selectedItems.add(itemId)
    }

    /**
     * アイテム選択解除
     */
    fun deselectItem(itemId: Long): Boolean {
        return _selectedItems.remove(itemId)
    }

    /**
     * 全選択クリア
     */
    fun clearSelection(): Set<Long> {
        val previouslySelected = _selectedItems.toSet()
        _selectedItems.clear()
        return previouslySelected
    }

    /**
     * アイテム選択状態確認
     */
    fun isItemSelected(itemId: Long): Boolean {
        return _selectedItems.contains(itemId)
    }

    /**
     * アイテム選択状態トグル
     */
    fun toggleItemSelection(itemId: Long): Boolean {
        return if (isItemSelected(itemId)) {
            deselectItem(itemId)
            false
        } else {
            selectItem(itemId)
            true
        }
    }

    /**
     * 長押しコールバック処理
     */
    fun handleLongPress(clothItem: ClothItem) {
        if (!isSelectionMode) {
            // 選択モード開始
            setSelectionMode(true)
            selectItem(clothItem.id)
            longPressListener?.invoke(clothItem)
        } else {
            // 選択状態切り替え
            val isSelected = toggleItemSelection(clothItem.id)
            selectionListener?.invoke(clothItem, isSelected)
        }
    }
}
