package com.example.clothstock.data.model

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.util.Date

/**
 * ClothItem エンティティのユニットテスト
 * 
 * TDD アプローチに従い、まず失敗するテストを作成してから実装を行う
 */
class ClothItemTest {

    private lateinit var validTagData: TagData
    private val testImagePath = "/storage/emulated/0/Pictures/test_image.jpg"
    private val testDate = Date()

    @Before
    fun setUp() {
        // 有効なTagDataを作成（サイズ範囲: 60-160）
        validTagData = TagData(
            size = 100,
            color = "青",
            category = "シャツ"
        )
    }

    // ===== ClothItem 基本的な作成テスト =====

    @Test
    fun `ClothItem作成_正常なデータ_成功する`() {
        // Given & When
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate
        )

        // Then
        assertEquals(1, clothItem.id)
        assertEquals(testImagePath, clothItem.imagePath)
        assertEquals(validTagData, clothItem.tagData)
        assertEquals(testDate, clothItem.createdAt)
    }

    @Test
    fun `ClothItem作成_IDが0_自動生成される`() {
        // Given & When
        val clothItem = ClothItem(
            id = 0, // Roomでは0はAUTO_GENERATEを意味する
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate
        )

        // Then
        assertEquals(0, clothItem.id) // 保存前は0のまま
        assertNotNull(clothItem.imagePath)
        assertNotNull(clothItem.tagData)
        assertNotNull(clothItem.createdAt)
    }

    // ===== バリデーションテスト =====

    @Test(expected = IllegalArgumentException::class)
    fun `ClothItem作成_空のimagePath_例外が発生する`() {
        // Given & When & Then
        ClothItem(
            id = 1,
            imagePath = "", // 空文字列
            tagData = validTagData,
            createdAt = testDate
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ClothItem作成_空白のimagePath_例外が発生する`() {
        // Given & When & Then
        ClothItem(
            id = 1,
            imagePath = "   ", // 空白文字列
            tagData = validTagData,
            createdAt = testDate
        )
    }

    // ===== Room アノテーションテスト =====

    @Test
    fun `ClothItem_Roomアノテーションが適切に設定されている`() {
        // Given
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate
        )

        // When & Then - リフレクションでアノテーションを確認
        val entityAnnotation = ClothItem::class.java.getAnnotation(androidx.room.Entity::class.java)
        assertNotNull("@Entity アノテーションが必要", entityAnnotation)
        assertEquals("cloth_items", entityAnnotation?.tableName)

        // PrimaryKey のチェック
        val idField = ClothItem::class.java.getDeclaredField("id")
        val primaryKeyAnnotation = idField.getAnnotation(androidx.room.PrimaryKey::class.java)
        assertNotNull("id フィールドには @PrimaryKey が必要", primaryKeyAnnotation)
        assertTrue("autoGenerate = true が必要", primaryKeyAnnotation?.autoGenerate == true)
    }

    // ===== フィールドサポートテスト =====

    @Test
    fun `ClothItem_すべてのフィールドがアクセス可能`() {
        // Given
        val clothItem = ClothItem(
            id = 42,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate
        )

        // When & Then
        assertEquals(42, clothItem.id)
        assertEquals(testImagePath, clothItem.imagePath)
        assertEquals(validTagData, clothItem.tagData)
        assertEquals(testDate, clothItem.createdAt)
    }

    // ===== 定数テスト =====

    @Test
    fun `ClothItem_テーブル名定数が正しい`() {
        // When & Then
        assertEquals("cloth_items", ClothItem.TABLE_NAME)
    }

    // ===== TagData統合テスト =====

    @Test
    fun `ClothItem_TagDataとの統合_正常に動作する`() {
        // Given
        val tagData = TagData(
            size = 120,
            color = "赤",
            category = "パンツ"
        )

        // When
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = tagData,
            createdAt = testDate
        )

        // Then
        assertEquals(120, clothItem.tagData.size)
        assertEquals("赤", clothItem.tagData.color)
        assertEquals("パンツ", clothItem.tagData.category)
    }

    // ===== 空文字列処理テスト =====

    @Test
    fun `TagData_空文字列の色とカテゴリ_オブジェクト作成は可能だがバリデーションで無効`() {
        // Given
        val tagDataWithEmptyStrings = TagData(
            size = 100,
            color = "",
            category = ""
        )

        // When - オブジェクト作成は可能
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = tagDataWithEmptyStrings,
            createdAt = testDate
        )

        // Then - バリデーションでは無効と判定される
        val validationResult = clothItem.validate()
        assertFalse("空文字列はバリデーションで無効", validationResult.isSuccess())
        assertTrue("エラーメッセージが含まれる", validationResult.getErrorMessage()!!.isNotEmpty())
    }
}