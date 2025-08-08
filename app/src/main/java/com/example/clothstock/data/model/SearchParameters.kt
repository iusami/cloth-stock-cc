package com.example.clothstock.data.model

/**
 * 衣服アイテム検索用のパラメーター集約クラス
 * Detekt LongParameterList対応
 */
data class SearchParameters(
    val sizeFilters: List<Int>? = null,
    val colorFilters: List<String>? = null,
    val categoryFilters: List<String>? = null,
    val searchText: String? = null
)

/**
 * ページネーション付き検索用のパラメーター集約クラス
 */
data class PaginationSearchParameters(
    val sizeFilters: List<Int>? = null,
    val colorFilters: List<String>? = null,
    val categoryFilters: List<String>? = null,
    val searchText: String? = null,
    val offset: Int,
    val limit: Int
)
