package com.example.clothstock.data.model

/**
 * フィルター状態の一元管理を行うクラス
 * フィルターの適用、削除、クリアなどの操作を提供する
 * 
 * このクラスは不変性を保ちながら、フィルター状態の変更を管理する
 * すべての操作は新しいFilterStateインスタンスを返し、元の状態は変更しない
 */
class FilterManager {
    
    private var currentState = FilterState()
    
    /**
     * 指定されたタイプのフィルターを更新する
     * 
     * @param type フィルタータイプ
     * @param values フィルター値のセット（文字列形式）
     * @return 更新されたFilterState
     */
    fun updateFilter(type: FilterType, values: Set<String>): FilterState {
        currentState = when (type) {
            FilterType.SIZE -> {
                // 無効な値を除外し、有効な整数値のみを保持
                val sizeValues = values
                    .mapNotNull { value -> 
                        value.toIntOrNull()?.takeIf { it > 0 } 
                    }
                    .toSet()
                currentState.copy(sizeFilters = sizeValues)
            }
            FilterType.COLOR -> {
                // 空文字列やブランク文字列を除外
                val colorValues = values.filter { it.isNotBlank() }.toSet()
                currentState.copy(colorFilters = colorValues)
            }
            FilterType.CATEGORY -> {
                // 空文字列やブランク文字列を除外
                val categoryValues = values.filter { it.isNotBlank() }.toSet()
                currentState.copy(categoryFilters = categoryValues)
            }
            FilterType.SEARCH -> {
                // SEARCH type is handled by updateSearchText method
                currentState
            }
        }
        return currentState
    }
    
    /**
     * 指定されたタイプの特定のフィルター値を削除する
     * 
     * @param type フィルタータイプ
     * @param value 削除するフィルター値（文字列形式）
     * @return 更新されたFilterState
     */
    fun removeFilter(type: FilterType, value: String): FilterState {
        currentState = when (type) {
            FilterType.SIZE -> {
                val sizeValue = value.toIntOrNull()?.takeIf { it > 0 }
                if (sizeValue != null && sizeValue in currentState.sizeFilters) {
                    currentState.copy(sizeFilters = currentState.sizeFilters - sizeValue)
                } else {
                    currentState
                }
            }
            FilterType.COLOR -> {
                if (value in currentState.colorFilters) {
                    currentState.copy(colorFilters = currentState.colorFilters - value)
                } else {
                    currentState
                }
            }
            FilterType.CATEGORY -> {
                if (value in currentState.categoryFilters) {
                    currentState.copy(categoryFilters = currentState.categoryFilters - value)
                } else {
                    currentState
                }
            }
            FilterType.SEARCH -> {
                // SEARCH type is handled by updateSearchText method
                currentState
            }
        }
        return currentState
    }
    
    /**
     * 指定されたタイプのすべてのフィルターをクリアする
     * 
     * @param type クリアするフィルタータイプ
     * @return 更新されたFilterState
     */
    fun clearFilter(type: FilterType): FilterState {
        currentState = when (type) {
            FilterType.SIZE -> {
                currentState.copy(sizeFilters = emptySet())
            }
            FilterType.COLOR -> {
                currentState.copy(colorFilters = emptySet())
            }
            FilterType.CATEGORY -> {
                currentState.copy(categoryFilters = emptySet())
            }
            FilterType.SEARCH -> {
                currentState.copy(searchText = "")
            }
        }
        return currentState
    }
    
    /**
     * すべてのフィルターと検索テキストをクリアする
     * 
     * @return 初期状態のFilterState
     */
    fun clearAllFilters(): FilterState {
        currentState = FilterState()
        return currentState
    }
    
    /**
     * 検索テキストを更新する
     * 
     * @param text 検索テキスト（空文字列や空白文字列も許可）
     * @return 更新されたFilterState
     */
    fun updateSearchText(text: String): FilterState {
        // 検索テキストはそのまま保持（空白文字列も有効な検索条件として扱う）
        currentState = currentState.copy(searchText = text)
        return currentState
    }
    
    /**
     * 現在のフィルター状態を取得する
     * 
     * @return 現在のFilterStateのコピー
     */
    fun getCurrentState(): FilterState {
        return currentState.copy() // 不変性を保つためにコピーを返す
    }
    
    /**
     * フィルター状態をリセットして新しい状態を設定する
     * 主にテストや状態復元時に使用
     * 
     * @param newState 設定する新しいFilterState
     * @return 設定されたFilterState
     */
    fun setState(newState: FilterState): FilterState {
        currentState = newState.copy()
        return currentState
    }
}