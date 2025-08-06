package com.example.clothstock.data.preferences

import android.content.SharedPreferences
import com.example.clothstock.data.model.FilterState

/**
 * フィルター設定の永続化を管理するクラス
 * 
 * TDD Green フェーズ: テストを通す最小限の実装
 * Task 10: SharedPreferencesでのフィルター設定永続化
 */
class FilterPreferencesManager(
    private val sharedPreferences: SharedPreferences
) {
    
    companion object {
        private const val KEY_FILTER_SIZES = "filter_sizes"
        private const val KEY_FILTER_COLORS = "filter_colors"
        private const val KEY_FILTER_CATEGORIES = "filter_categories"
        private const val KEY_FILTER_SEARCH_TEXT = "filter_search_text"
        private const val KEY_FILTER_VERSION = "filter_version"
    }
    
    /**
     * フィルター状態をSharedPreferencesに保存
     */
    fun saveFilterState(filterState: FilterState) {
        val editor = sharedPreferences.edit()
        
        // サイズフィルターを文字列セットとして保存
        val sizeStrings = filterState.sizeFilters.map { it.toString() }.toSet()
        editor.putStringSet(KEY_FILTER_SIZES, sizeStrings)
        
        // 色フィルターを保存
        editor.putStringSet(KEY_FILTER_COLORS, filterState.colorFilters)
        
        // カテゴリフィルターを保存
        editor.putStringSet(KEY_FILTER_CATEGORIES, filterState.categoryFilters)
        
        // 検索テキストを保存
        editor.putString(KEY_FILTER_SEARCH_TEXT, filterState.searchText)
        
        editor.apply()
    }
    
    /**
     * SharedPreferencesからフィルター状態を読み込み
     */
    fun loadFilterState(): FilterState {
        // サイズフィルターを読み込み（文字列から整数に変換）
        val sizeStrings = sharedPreferences.getStringSet(KEY_FILTER_SIZES, emptySet()) ?: emptySet()
        val sizeFilters = sizeStrings.mapNotNull { it.toIntOrNull() }.toSet()
        
        // 色フィルターを読み込み
        val colorFilters = sharedPreferences.getStringSet(KEY_FILTER_COLORS, emptySet()) ?: emptySet()
        
        // カテゴリフィルターを読み込み
        val categoryFilters = sharedPreferences.getStringSet(KEY_FILTER_CATEGORIES, emptySet()) ?: emptySet()
        
        // 検索テキストを読み込み
        val searchText = sharedPreferences.getString(KEY_FILTER_SEARCH_TEXT, "") ?: ""
        
        return FilterState(
            sizeFilters = sizeFilters,
            colorFilters = colorFilters,
            categoryFilters = categoryFilters,
            searchText = searchText
        )
    }
    
    /**
     * フィルター設定をクリア
     */
    fun clearFilterState() {
        val editor = sharedPreferences.edit()
        editor.remove(KEY_FILTER_SIZES)
        editor.remove(KEY_FILTER_COLORS)
        editor.remove(KEY_FILTER_CATEGORIES)
        editor.remove(KEY_FILTER_SEARCH_TEXT)
        editor.apply()
    }
    
    /**
     * フィルター設定が存在するかチェック
     */
    fun hasFilterPreferences(): Boolean {
        return sharedPreferences.contains(KEY_FILTER_SIZES) ||
               sharedPreferences.contains(KEY_FILTER_COLORS) ||
               sharedPreferences.contains(KEY_FILTER_CATEGORIES) ||
               sharedPreferences.contains(KEY_FILTER_SEARCH_TEXT)
    }
    
    /**
     * フィルターバージョンを保存
     */
    fun saveFilterVersion(version: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_FILTER_VERSION, version)
        editor.apply()
    }
    
    /**
     * フィルターバージョンを取得
     */
    fun getFilterVersion(): Int {
        return sharedPreferences.getInt(KEY_FILTER_VERSION, 0)
    }
}
