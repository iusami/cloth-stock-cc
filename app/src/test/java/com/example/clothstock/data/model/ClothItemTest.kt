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

    @Test
    fun `ClothItem_空のimagePath_バリデーションで無効`() {
        // Given
        val clothItem = ClothItem(
            id = 1,
            imagePath = "", // 空文字列
            tagData = validTagData,
            createdAt = testDate
        )
        
        // When
        val validationResult = clothItem.validate()
        
        // Then
        assertFalse("空のimagePathはバリデーションで無効", validationResult.isSuccess())
        assertEquals("画像パスが設定されていません", validationResult.getErrorMessage())
    }

    @Test
    fun `ClothItem_空白のimagePath_バリデーションで無効`() {
        // Given
        val clothItem = ClothItem(
            id = 1,
            imagePath = "   ", // 空白文字列
            tagData = validTagData,
            createdAt = testDate
        )
        
        // When
        val validationResult = clothItem.validate()
        
        // Then
        assertFalse("空白のimagePathはバリデーションで無効", validationResult.isSuccess())
        assertEquals("画像パスが設定されていません", validationResult.getErrorMessage())
    }

    // ===== Room アノテーションテスト =====

    // Room アノテーションはコンパイル時に検証されるため、リフレクションテストは不要

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

    // ===== 🔴 TDD Red: メモ機能テスト（失敗するテスト） =====

    @Test
    fun `withUpdatedMemo_正常なメモ_メモが更新される`() {
        // Given
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate
        )
        val testMemo = "お気に入りのシャツ"

        // When
        val updatedItem = clothItem.withUpdatedMemo(testMemo)

        // Then
        assertEquals(testMemo, updatedItem.memo)
        assertEquals(clothItem.id, updatedItem.id) // 他のフィールドは変更されない
        assertEquals(clothItem.imagePath, updatedItem.imagePath)
        assertEquals(clothItem.tagData, updatedItem.tagData)
    }

    @Test
    fun `withUpdatedMemo_長いメモ_制限文字数でトリミングされる`() {
        // Given
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate
        )
        val longMemo = "a".repeat(ClothItem.MAX_MEMO_LENGTH + 100) // 制限を超える長さ

        // When
        val updatedItem = clothItem.withUpdatedMemo(longMemo)

        // Then
        assertEquals(ClothItem.MAX_MEMO_LENGTH, updatedItem.memo.length)
        assertTrue("メモは制限文字数でトリミングされる", updatedItem.memo.length <= ClothItem.MAX_MEMO_LENGTH)
    }

    @Test
    fun `hasMemo_メモがある場合_trueを返す`() {
        // Given
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate,
            memo = "テストメモ"
        )

        // When & Then
        assertTrue("メモがある場合はtrueを返す", clothItem.hasMemo())
    }

    @Test
    fun `hasMemo_メモが空文字列の場合_falseを返す`() {
        // Given
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate,
            memo = ""
        )

        // When & Then
        assertFalse("空文字列の場合はfalseを返す", clothItem.hasMemo())
    }

    @Test
    fun `hasMemo_メモが空白文字列の場合_falseを返す`() {
        // Given
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate,
            memo = "   "
        )

        // When & Then
        assertFalse("空白文字列の場合はfalseを返す", clothItem.hasMemo())
    }

    @Test
    fun `getMemoPreview_短いメモ_そのまま返される`() {
        // Given
        val shortMemo = "短いメモ"
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate,
            memo = shortMemo
        )

        // When
        val preview = clothItem.getMemoPreview()

        // Then
        assertEquals(shortMemo, preview)
    }

    @Test
    fun `getMemoPreview_長いメモ_省略記号付きでトリミングされる`() {
        // Given
        val longMemo = "a".repeat(80) // デフォルトの50文字を超える長さ
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate,
            memo = longMemo
        )

        // When
        val preview = clothItem.getMemoPreview()

        // Then
        assertTrue("プレビューは50文字 + ... より短い", preview.length <= 53)
        assertTrue("省略記号が末尾に付く", preview.endsWith("..."))
    }

    @Test
    fun `getMemoPreview_カスタム長さ指定_指定文字数でトリミングされる`() {
        // Given
        val memo = "a".repeat(100)
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate,
            memo = memo
        )
        val customLength = 20

        // When
        val preview = clothItem.getMemoPreview(customLength)

        // Then
        assertTrue("カスタム長さ + ... より短い", preview.length <= customLength + 3)
        assertTrue("省略記号が末尾に付く", preview.endsWith("..."))
    }

    @Test
    fun `validate_メモが制限文字数を超える_バリデーションで無効`() {
        // Given
        val longMemo = "a".repeat(ClothItem.MAX_MEMO_LENGTH + 1)
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate,
            memo = longMemo
        )

        // When
        val validationResult = clothItem.validate()

        // Then
        assertFalse("制限文字数を超えるメモはバリデーションで無効", validationResult.isSuccess())
        assertTrue("エラーメッセージにメモに関する内容が含まれる", 
                   validationResult.getErrorMessage()!!.contains("メモ"))
    }

    @Test
    fun `validate_メモが制限文字数以内_バリデーション成功`() {
        // Given
        val validMemo = "a".repeat(ClothItem.MAX_MEMO_LENGTH)
        val clothItem = ClothItem(
            id = 1,
            imagePath = testImagePath,
            tagData = validTagData,
            createdAt = testDate,
            memo = validMemo
        )

        // When
        val validationResult = clothItem.validate()

        // Then
        assertTrue("制限文字数以内のメモはバリデーション成功", validationResult.isSuccess())
    }

    @Test
    fun `MAX_MEMO_LENGTH定数_1000文字に設定されている`() {
        // When & Then
        assertEquals("メモ最大文字数は1000文字", 1000, ClothItem.MAX_MEMO_LENGTH)
    }
}