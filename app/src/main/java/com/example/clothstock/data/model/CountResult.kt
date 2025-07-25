package com.example.clothstock.data.model

/**
 * カウントクエリの結果を表すデータクラス群
 */

/**
 * カテゴリ別カウント結果
 */
data class CategoryCount(
    val category: String,
    val count: Int
)

/**
 * 色別カウント結果
 */
data class ColorCount(
    val color: String,
    val count: Int
)

/**
 * サイズ別カウント結果
 */
data class SizeCount(
    val size: Int,
    val count: Int
)