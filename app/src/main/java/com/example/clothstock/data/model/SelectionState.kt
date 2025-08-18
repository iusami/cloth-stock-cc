package com.example.clothstock.data.model

/**
 * 選択状態を管理するためのデータクラス
 * 
 * ギャラリー画面でのアイテム選択状態を追跡し、
 * 選択モードの管理と選択済みアイテムの管理を行う
 * 
 * @property isSelectionMode 選択モードがアクティブかどうか
 * @property selectedItemIds 選択されたアイテムのIDセット
 * @property totalSelectedCount 選択されたアイテムの総数
 */
data class SelectionState(
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    val totalSelectedCount: Int = 0
) {
    
    /**
     * 指定されたアイテムが選択されているかチェック
     * 
     * @param itemId 確認するアイテムのID
     * @return 選択されている場合true、そうでなければfalse
     */
    fun isItemSelected(itemId: Long): Boolean {
        return selectedItemIds.contains(itemId)
    }
    
    /**
     * 何らかのアイテムが選択されているかチェック
     * 
     * @return 1つ以上のアイテムが選択されている場合true、そうでなければfalse
     */
    fun hasSelection(): Boolean {
        return selectedItemIds.isNotEmpty()
    }
    
    /**
     * アイテムを選択状態に追加する
     * 
     * @param itemId 選択するアイテムのID
     * @return 新しいSelectionStateインスタンス（immutable）
     */
    fun selectItem(itemId: Long): SelectionState {
        val newSelectedIds = selectedItemIds + itemId
        return copy(
            isSelectionMode = true,
            selectedItemIds = newSelectedIds,
            totalSelectedCount = newSelectedIds.size
        )
    }
    
    /**
     * アイテムを選択状態から除去する
     * 
     * @param itemId 選択解除するアイテムのID
     * @return 新しいSelectionStateインスタンス（immutable）
     */
    fun deselectItem(itemId: Long): SelectionState {
        val newSelectedIds = selectedItemIds - itemId
        return copy(
            isSelectionMode = newSelectedIds.isNotEmpty(),
            selectedItemIds = newSelectedIds,
            totalSelectedCount = newSelectedIds.size
        )
    }
    
    /**
     * 全ての選択をクリアし、デフォルト状態に戻す
     * 
     * @return デフォルトのSelectionStateインスタンス
     */
    fun clearSelection(): SelectionState {
        return SelectionState()
    }
}
