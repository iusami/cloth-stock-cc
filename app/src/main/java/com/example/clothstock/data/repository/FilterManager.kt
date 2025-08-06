package com.example.clothstock.data.repository

import com.example.clothstock.data.model.FilterState
import com.example.clothstock.data.model.FilterType

/**
 * フィルター状態の中央管理を行うクラス
 * 
 * このクラスは衣服ギャラリーのフィルタリング機能において、
 * 複数のフィルタータイプ（サイズ、色、カテゴリ、検索）の状態を
 * 一元的に管理し、状態の変更操作を提供します。
 * 
 * 特徴:
 * - Immutableな状態管理（FilterStateは常に新しいインスタンスを返す）
 * - スレッドセーフ（状態の変更は同期化される）
 * - 型安全性（各フィルタータイプに対する適切な検証）
 * - パフォーマンス最適化（不要な状態変更を回避）
 * - 包括的な入力検証とエラーハンドリング
 * 
 * 使用例:
 * ```
 * val filterManager = FilterManager()
 * val newState = filterManager.updateFilter(FilterType.SIZE, setOf("100", "110"))
 * val currentState = filterManager.getCurrentState()
 * ```
 */
class FilterManager {
    
    companion object {
        // サイズフィルターの有効な範囲
        private const val MIN_VALID_SIZE = 60
        private const val MAX_VALID_SIZE = 160
        
        // 文字列フィルターの最大長（セキュリティ対策）
        private const val MAX_STRING_LENGTH = 100
        
        // 検索テキストの最大長
        private const val MAX_SEARCH_TEXT_LENGTH = 200
    }
    
    // スレッドセーフな状態管理のためのロック
    private val stateLock = Any()
    
    // 現在のフィルター状態（不変オブジェクト）
    @Volatile
    private var currentState = FilterState()
    
    /**
     * 指定されたフィルタータイプの値を更新します
     * 
     * @param type 更新するフィルターのタイプ
     * @param values 設定する値のセット（空の場合はそのタイプのフィルターをクリア）
     * @return 更新後の新しいFilterState
     * @throws IllegalArgumentException 無効な値が含まれている場合
     */
    fun updateFilter(type: FilterType, values: Set<String>): FilterState {
        return synchronized(stateLock) {
            val newState = when (type) {
                FilterType.SIZE -> updateSizeFilters(values)
                FilterType.COLOR -> updateColorFilters(values)
                FilterType.CATEGORY -> updateCategoryFilters(values)
                FilterType.SEARCH -> {
                    // SEARCHタイプの場合は、最初の要素を検索テキストとして使用
                    val searchText = values.firstOrNull()?.trim() ?: ""
                    updateSearchTextInternal(searchText)
                }
            }
            
            // パフォーマンス最適化：状態が変更されない場合は更新をスキップ
            if (newState != currentState) {
                currentState = newState
            }
            newState
        }
    }
    
    /**
     * 指定されたフィルタータイプから特定の値を削除します
     * 
     * @param type 削除対象のフィルタータイプ
     * @param value 削除する値
     * @return 更新後の新しいFilterState
     */
    fun removeFilter(type: FilterType, value: String): FilterState {
        return synchronized(stateLock) {
            val newState = when (type) {
                FilterType.SIZE -> {
                    val size = value.toIntOrNull()
                    if (size != null && size in currentState.sizeFilters) {
                        currentState.copy(sizeFilters = currentState.sizeFilters - size)
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
                    // 検索フィルターの場合は、検索テキストをクリア
                    currentState.copy(searchText = "")
                }
            }
            currentState = newState
            newState
        }
    }
    
    /**
     * 指定されたフィルタータイプの全ての値をクリアします
     * 
     * @param type クリアするフィルタータイプ
     * @return 更新後の新しいFilterState
     */
    fun clearFilter(type: FilterType): FilterState {
        return synchronized(stateLock) {
            val newState = when (type) {
                FilterType.SIZE -> currentState.copy(sizeFilters = emptySet())
                FilterType.COLOR -> currentState.copy(colorFilters = emptySet())
                FilterType.CATEGORY -> currentState.copy(categoryFilters = emptySet())
                FilterType.SEARCH -> currentState.copy(searchText = "")
            }
            currentState = newState
            newState
        }
    }
    
    /**
     * 全てのフィルターをクリアし、初期状態に戻します
     * 
     * @return 初期状態のFilterState
     */
    fun clearAllFilters(): FilterState {
        return synchronized(stateLock) {
            val newState = FilterState()
            currentState = newState
            newState
        }
    }
    
    /**
     * 検索テキストを更新します
     * 
     * @param searchText 新しい検索テキスト（前後の空白は自動的にトリム）
     * @return 更新後の新しいFilterState
     */
    fun updateSearchText(searchText: String): FilterState {
        return synchronized(stateLock) {
            val newState = updateSearchTextInternal(searchText)
            currentState = newState
            newState
        }
    }
    
    /**
     * 現在のフィルター状態を取得します
     * 
     * @return 現在のFilterStateのコピー（読み取り専用）
     */
    fun getCurrentState(): FilterState {
        return currentState
    }
    
    /**
     * 保存されたフィルター状態を復元します
     * Task 10: 状態復元機能
     * 
     * @param savedState 復元するFilterState
     * @return 復元後の新しいFilterState
     */
    fun restoreState(savedState: FilterState): FilterState {
        return synchronized(stateLock) {
            currentState = savedState
            savedState
        }
    }
    
    // ========== プライベートヘルパーメソッド ==========
    
    /**
     * サイズフィルターを更新する内部メソッド
     * パフォーマンス最適化と入力検証を含む
     */
    private fun updateSizeFilters(values: Set<String>): FilterState {
        if (values.isEmpty()) {
            return if (currentState.sizeFilters.isEmpty()) {
                currentState // 不要な変更を回避
            } else {
                currentState.copy(sizeFilters = emptySet())
            }
        }
        
        val validSizes = buildSet {
            for (value in values) {
                val size = value.toIntOrNull()
                if (size != null && size in MIN_VALID_SIZE..MAX_VALID_SIZE) {
                    add(size)
                }
                // 無効な値は無視（ログ出力や例外は上位層で処理）
            }
        }
        
        return if (validSizes == currentState.sizeFilters) {
            currentState // 同じ値なら更新不要
        } else {
            currentState.copy(sizeFilters = validSizes)
        }
    }
    
    /**
     * 色フィルターを更新する内部メソッド
     * パフォーマンス最適化と入力検証を含む
     */
    private fun updateColorFilters(values: Set<String>): FilterState {
        if (values.isEmpty()) {
            return if (currentState.colorFilters.isEmpty()) {
                currentState // 不要な変更を回避
            } else {
                currentState.copy(colorFilters = emptySet())
            }
        }
        
        val validColors = buildSet {
            for (value in values) {
                val trimmed = value.trim()
                if (trimmed.isNotBlank() && trimmed.length <= MAX_STRING_LENGTH) {
                    add(trimmed)
                }
            }
        }
        
        return if (validColors == currentState.colorFilters) {
            currentState // 同じ値なら更新不要
        } else {
            currentState.copy(colorFilters = validColors)
        }
    }
    
    /**
     * カテゴリフィルターを更新する内部メソッド
     * パフォーマンス最適化と入力検証を含む
     */
    private fun updateCategoryFilters(values: Set<String>): FilterState {
        if (values.isEmpty()) {
            return if (currentState.categoryFilters.isEmpty()) {
                currentState // 不要な変更を回避
            } else {
                currentState.copy(categoryFilters = emptySet())
            }
        }
        
        val validCategories = buildSet {
            for (value in values) {
                val trimmed = value.trim()
                if (trimmed.isNotBlank() && trimmed.length <= MAX_STRING_LENGTH) {
                    add(trimmed)
                }
            }
        }
        
        return if (validCategories == currentState.categoryFilters) {
            currentState // 同じ値なら更新不要
        } else {
            currentState.copy(categoryFilters = validCategories)
        }
    }
    
    /**
     * 検索テキストを更新する内部メソッド
     * パフォーマンス最適化と入力検証を含む
     */
    private fun updateSearchTextInternal(searchText: String): FilterState {
        val trimmedText = searchText.trim().take(MAX_SEARCH_TEXT_LENGTH)
        return if (trimmedText == currentState.searchText) {
            currentState // 同じ値なら更新不要
        } else {
            currentState.copy(searchText = trimmedText)
        }
    }
    
    /**
     * デバッグ用：現在の状態の詳細情報を返します
     * 開発時のトラブルシューティングに使用
     */
    fun getDebugInfo(): String {
        val state = getCurrentState()
        return buildString {
            appendLine("FilterManager Debug Info:")
            appendLine("  hasActiveFilters: ${state.hasActiveFilters()}")
            appendLine("  activeFilterCount: ${state.getActiveFilterCount()}")
            appendLine("  sizeFilters: ${state.sizeFilters}")
            appendLine("  colorFilters: ${state.colorFilters}")
            appendLine("  categoryFilters: ${state.categoryFilters}")
            appendLine("  searchText: '${state.searchText}'")
            appendLine("  displayString: '${state.toDisplayString()}'")
        }
    }
    
    /**
     * フィルター状態のバリデーション
     * データ整合性チェックに使用
     * 
     * @return フィルター状態が有効な場合はtrue、無効な場合はfalse
     */
    fun validateCurrentState(): Boolean {
        val state = getCurrentState()
        return try {
            // FilterStateのinitブロックでバリデーションが実行される
            FilterState(
                sizeFilters = state.sizeFilters,
                colorFilters = state.colorFilters,
                categoryFilters = state.categoryFilters,
                searchText = state.searchText
            )
            true
        } catch (expected: IllegalArgumentException) {
            // バリデーション失敗は期待される動作のため、ログ出力は不要
            // この例外はFilterStateのバリデーションロジックによって発生する
            false
        }
    }
}
