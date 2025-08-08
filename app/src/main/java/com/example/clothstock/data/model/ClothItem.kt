package com.example.clothstock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import java.util.Date

/**
 * 衣服アイテムを表すRoom Entityクラス
 * 
 * データベースのテーブル構造を定義し、
 * 撮影した衣服の写真、タグ情報、およびメモ情報を保存する
 * 
 * @property id アイテムの一意識別子（自動生成）
 * @property imagePath 衣服画像のファイルパス
 * @property tagData サイズ、色、カテゴリ情報
 * @property createdAt アイテム作成日時
 * @property memo ユーザーが追加するメモ情報（最大${MAX_MEMO_LENGTH}文字）
 */
@Entity(tableName = ClothItem.TABLE_NAME)
data class ClothItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val imagePath: String,
    
    @Embedded
    val tagData: TagData,
    
    val createdAt: Date,
    
    val memo: String = ""
) : Validatable {
    
    companion object {
        const val TABLE_NAME = "cloth_items"
        const val MAX_MEMO_LENGTH = 1000
        
        /**
         * 新規アイテム作成用のファクトリーメソッド
         * 
         * @param imagePath 衣服画像のファイルパス
         * @param tagData タグ情報（サイズ、色、カテゴリ）
         * @param memo メモ情報（省略可能）
         * @param createdAt 作成日時（省略時は現在時刻）
         * @return 新しいClothItemインスタンス
         */
        fun create(
            imagePath: String,
            tagData: TagData,
            memo: String = "",
            createdAt: Date = Date()
        ): ClothItem {
            return ClothItem(
                id = 0, // Room の autoGenerate により自動生成される
                imagePath = imagePath,
                tagData = tagData,
                createdAt = createdAt,
                memo = memo.take(MAX_MEMO_LENGTH) // 文字数制限を適用
            )
        }
    }

    // 注意: 初期化時のバリデーションは行わず、validate()メソッドで実行
    // これによりテスト用の無効なデータでもオブジェクト作成が可能
    
    /**
     * バリデーション実行（Validatableインターフェース実装）
     * 
     * ClothItemの全フィールドの整合性をチェックする
     * - imagePath: 空文字やブランクでないことを確認
     * - memo: 最大文字数（${MAX_MEMO_LENGTH}文字）を超えないことを確認
     * - tagData: 埋め込まれたTagDataのバリデーション結果を使用
     * 
     * @return ValidationResult バリデーション結果（成功時はSuccess、失敗時はError）
     */
    override fun validate(): ValidationResult {
        return when {
            imagePath.isBlank() -> ValidationResult.error("画像パスが設定されていません", "imagePath")
            memo.length > MAX_MEMO_LENGTH -> ValidationResult.error(
                "メモが${MAX_MEMO_LENGTH}文字を超えています（現在: ${memo.length}文字）", 
                "memo"
            )
            else -> {
                val tagValidation = tagData.validate()
                if (tagValidation.isError()) {
                    tagValidation
                } else {
                    ValidationResult.success()
                }
            }
        }
    }
    
    /**
     * 表示用の短いファイル名を取得
     */
    fun getFileName(): String {
        return imagePath.substringAfterLast("/")
    }
    
    /**
     * 作成日の表示用フォーマット
     */
    fun getFormattedDate(): String {
        return java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.JAPAN)
            .format(createdAt)
    }
    
    /**
     * タグ情報を更新した新しいインスタンスを作成
     */
    fun withUpdatedTag(newTagData: TagData): ClothItem {
        // バリデーションは呼び出し側の責任とする
        return copy(tagData = newTagData)
    }
    
    /**
     * アイテムの概要テキストを生成
     */
    fun getSummary(): String {
        return "${tagData.category} (${tagData.color}・サイズ${tagData.size})"
    }
    
    /**
     * メモを更新した新しいインスタンスを作成
     * 
     * 指定されたメモが最大文字数を超える場合、
     * 自動的に制限文字数でトリミングされる
     * 
     * @param newMemo 新しいメモテキスト
     * @return メモが更新された新しいClothItemインスタンス
     */
    fun withUpdatedMemo(newMemo: String): ClothItem {
        return copy(memo = newMemo.take(MAX_MEMO_LENGTH))
    }
    
    /**
     * メモが存在するかどうかを判定
     * 
     * 空文字列または空白文字のみの場合はfalseを返す
     * 
     * @return メモが存在するtrue、存在しない場合false
     */
    fun hasMemo(): Boolean {
        return memo.isNotBlank()
    }
    
    /**
     * メモのプレビューテキストを取得
     * 
     * 指定された最大長を超える場合、
     * 指定文字数でトリミングし、末尾に"..."を付加する
     * 
     * @param maxLength プレビューの最大文字数（デフォルト: 50文字）
     * @return プレビューテキスト
     */
    fun getMemoPreview(maxLength: Int = 50): String {
        return if (memo.length <= maxLength) {
            memo
        } else {
            "${memo.take(maxLength)}..."
        }
    }
}