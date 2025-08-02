package com.example.clothstock.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 衣服のタグ情報を表すUIモデル
 * 
 * サイズは60-160の範囲で制限される
 * 色とカテゴリは必須項目
 */
@Parcelize
data class TagData(
    val size: Int,
    val color: String,
    val category: String
) : Parcelable, Validatable {

    companion object {
        // サイズ制約定数
        const val MIN_SIZE = 60
        const val MAX_SIZE = 160
        const val SIZE_INCREMENT = 10 // 10単位増分
        
        // デフォルト値定数
        const val DEFAULT_SIZE = 100
        const val DEFAULT_COLOR = "未設定"
        const val DEFAULT_CATEGORY = "その他"
        
        /**
         * デフォルト値でTagDataを作成
         */
        fun createDefault(): TagData {
            return TagData(
                size = DEFAULT_SIZE,
                color = DEFAULT_COLOR,
                category = DEFAULT_CATEGORY
            )
        }
        
        /**
         * 有効なサイズオプションのリストを生成
         * 10単位増分で60から160までの値を返す
         * 
         * @return [60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160]
         */
        fun getValidSizeOptions(): List<Int> {
            return (MIN_SIZE..MAX_SIZE step SIZE_INCREMENT).toList()
        }
    }

    init {
        // サイズが負数や0の場合は例外を投げる
        require(size > 0) { "サイズは正の数である必要があります: $size" }
    }

    /**
     * サイズが有効範囲（60-160）内かつ10単位増分かチェック
     */
    fun isValidSize(): Boolean {
        return size in MIN_SIZE..MAX_SIZE && (size - MIN_SIZE) % SIZE_INCREMENT == 0
    }

    /**
     * バリデーション実行（Validatableインターフェース実装）
     */
    override fun validate(): ValidationResult {
        return when {
            !isValidSize() -> ValidationResult.error(
                "サイズは${MIN_SIZE}～${MAX_SIZE}の範囲で10単位刻み" +
                "（${MIN_SIZE}, ${MIN_SIZE + SIZE_INCREMENT}, ${MIN_SIZE + SIZE_INCREMENT * 2}...）" +
                "で入力してください（現在: $size）",
                "size"
            )
            color.isBlank() -> ValidationResult.error("色を入力してください", "color")
            category.isBlank() -> ValidationResult.error("カテゴリを選択してください", "category")
            else -> ValidationResult.success()
        }
    }

    /**
     * フォーマットされた表示用文字列を生成
     */
    fun getDisplayText(): String {
        return "サイズ: $size, 色: $color, カテゴリ: $category"
    }

    /**
     * サイズのみを変更したコピーを作成
     */
    fun withSize(newSize: Int): TagData {
        return copy(size = newSize)
    }

    /**
     * 色のみを変更したコピーを作成
     */
    fun withColor(newColor: String): TagData {
        return copy(color = newColor)
    }

    /**
     * カテゴリのみを変更したコピーを作成
     */
    fun withCategory(newCategory: String): TagData {
        return copy(category = newCategory)
    }
}