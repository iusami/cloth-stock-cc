package com.example.clothstock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded
import java.util.Date

/**
 * 衣服アイテムを表すRoom Entityクラス
 * 
 * データベースのテーブル構造を定義し、
 * 撮影した衣服の写真とタグ情報を保存する
 */
@Entity(tableName = ClothItem.TABLE_NAME)
data class ClothItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val imagePath: String,
    
    @Embedded
    val tagData: TagData,
    
    val createdAt: Date
) : Validatable {
    
    companion object {
        const val TABLE_NAME = "cloth_items"
        
        /**
         * 新規アイテム作成用のファクトリーメソッド
         */
        fun create(
            imagePath: String,
            tagData: TagData,
            createdAt: Date = Date()
        ): ClothItem {
            return ClothItem(
                id = 0, // Room の autoGenerate により自動生成される
                imagePath = imagePath,
                tagData = tagData,
                createdAt = createdAt
            )
        }
    }

    init {
        // imagePathの検証
        require(imagePath.isNotBlank()) { 
            "画像パスは空にできません" 
        }
        
        // TagDataの基本検証（詳細はTagData内で実施）
        val tagValidation = tagData.validate()
        require(tagValidation.isSuccess()) { 
            "無効なタグデータです: ${tagValidation.getErrorMessage()}" 
        }
    }
    
    /**
     * バリデーション実行（Validatableインターフェース実装）
     */
    override fun validate(): ValidationResult {
        return when {
            imagePath.isBlank() -> ValidationResult.error("画像パスが設定されていません", "imagePath")
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
        val tagValidation = newTagData.validate()
        require(tagValidation.isSuccess()) { 
            "無効なタグデータです: ${tagValidation.getErrorMessage()}" 
        }
        
        return copy(tagData = newTagData)
    }
    
    /**
     * アイテムの概要テキストを生成
     */
    fun getSummary(): String {
        return "${tagData.category} (${tagData.color}・サイズ${tagData.size})"
    }
}