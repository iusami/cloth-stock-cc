package com.example.clothstock.data.model

import org.junit.Test
import org.junit.Assert.*

/**
 * TagData UIモデルのユニットテスト
 * 
 * サイズバリデーション (60-160) を含む包括的なテスト
 */
class TagDataTest {

    // ===== 正常なTagData作成テスト =====

    @Test
    fun `TagData作成_正常なデータ_成功する`() {
        // Given & When
        val tagData = TagData(
            size = 100,
            color = "青",
            category = "シャツ"
        )

        // Then
        assertEquals(100, tagData.size)
        assertEquals("青", tagData.color)
        assertEquals("シャツ", tagData.category)
    }

    // ===== サイズバリデーションテスト (60-160の範囲) =====

    @Test
    fun `TagData作成_サイズ60_有効`() {
        // Given & When
        val tagData = TagData(
            size = 60,
            color = "赤",
            category = "パンツ"
        )

        // Then
        assertEquals(60, tagData.size)
        assertTrue("サイズ60は有効範囲", tagData.isValidSize())
    }

    @Test
    fun `TagData作成_サイズ160_有効`() {
        // Given & When
        val tagData = TagData(
            size = 160,
            color = "緑",
            category = "ジャケット"
        )

        // Then
        assertEquals(160, tagData.size)
        assertTrue("サイズ160は有効範囲", tagData.isValidSize())
    }

    @Test
    fun `TagData作成_サイズ59_無効`() {
        // Given & When
        val tagData = TagData(
            size = 59,
            color = "黄",
            category = "シャツ"
        )

        // Then
        assertEquals(59, tagData.size)
        assertFalse("サイズ59は無効範囲", tagData.isValidSize())
    }

    @Test
    fun `TagData作成_サイズ161_無効`() {
        // Given & When
        val tagData = TagData(
            size = 161,
            color = "紫",
            category = "コート"
        )

        // Then
        assertEquals(161, tagData.size)
        assertFalse("サイズ161は無効範囲", tagData.isValidSize())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `TagData作成_サイズ0_例外が発生する`() {
        // Given & When & Then
        TagData(
            size = 0,
            color = "白",
            category = "シャツ"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `TagData作成_負のサイズ_例外が発生する`() {
        // Given & When & Then
        TagData(
            size = -10,
            color = "黒",
            category = "シャツ"
        )
    }

    // ===== バリデーションメソッドテスト =====

    @Test
    fun `isValidSize_範囲内のサイズ_trueを返す`() {
        // Given
        val tagData = TagData(80, "青", "シャツ")

        // When & Then
        assertTrue(tagData.isValidSize())
    }

    @Test
    fun `isValidSize_範囲外のサイズ_falseを返す`() {
        // Given
        val tagData = TagData(200, "青", "シャツ") // バリデーションをバイパスして作成

        // When & Then
        assertFalse(tagData.isValidSize())
    }

    @Test
    fun `isValid_すべてのフィールドが有効_trueを返す`() {
        // Given
        val tagData = TagData(
            size = 120,
            color = "青",
            category = "シャツ"
        )

        // When & Then
        assertTrue("すべてのフィールドが有効", tagData.isValid())
    }

    @Test
    fun `isValid_サイズが無効_falseを返す`() {
        // Given
        val tagData = TagData(
            size = 50, // 無効なサイズ
            color = "青",
            category = "シャツ"
        )

        // When & Then
        assertFalse("無効なサイズ", tagData.isValid())
    }

    @Test
    fun `isValid_空の色_falseを返す`() {
        // Given
        val tagData = TagData(
            size = 100,
            color = "", // 空の色
            category = "シャツ"
        )

        // When & Then
        assertFalse("空の色は無効", tagData.isValid())
    }

    @Test
    fun `isValid_空のカテゴリ_falseを返す`() {
        // Given
        val tagData = TagData(
            size = 100,
            color = "青",
            category = "" // 空のカテゴリ
        )

        // When & Then
        assertFalse("空のカテゴリは無効", tagData.isValid())
    }

    // ===== エラーメッセージテスト =====

    @Test
    fun `getValidationError_有効なデータ_nullを返す`() {
        // Given
        val tagData = TagData(100, "青", "シャツ")

        // When & Then
        assertNull(tagData.getValidationError())
    }

    @Test
    fun `getValidationError_無効なサイズ_適切なエラーメッセージを返す`() {
        // Given
        val tagData = TagData(50, "青", "シャツ")

        // When
        val error = tagData.getValidationError()

        // Then
        assertNotNull(error)
        assertTrue("サイズエラーメッセージを含む", error!!.contains("サイズ"))
        assertTrue("範囲情報を含む", error.contains("60") && error.contains("160"))
    }

    @Test
    fun `getValidationError_空の色_適切なエラーメッセージを返す`() {
        // Given
        val tagData = TagData(100, "", "シャツ")

        // When
        val error = tagData.getValidationError()

        // Then
        assertNotNull(error)
        assertTrue("色エラーメッセージを含む", error!!.contains("色"))
    }

    // ===== デフォルト値とファクトリーテスト =====

    @Test
    fun `createDefault_デフォルトTagDataを作成_正常な値`() {
        // When
        val defaultTagData = TagData.createDefault()

        // Then
        assertTrue("デフォルトサイズは有効範囲", defaultTagData.isValidSize())
        assertNotNull("デフォルト色が設定されている", defaultTagData.color)
        assertNotNull("デフォルトカテゴリが設定されている", defaultTagData.category)
        assertTrue("デフォルトデータは有効", defaultTagData.isValid())
    }

    // ===== コピーとイミュータブルテスト =====

    @Test
    fun `copy_サイズのみ変更_他のフィールドは保持される`() {
        // Given
        val original = TagData(100, "青", "シャツ")

        // When
        val copied = original.copy(size = 120)

        // Then
        assertEquals(120, copied.size)
        assertEquals("青", copied.color)
        assertEquals("シャツ", copied.category)
        assertEquals(100, original.size) // 元のオブジェクトは変更されない
    }

    // ===== 定数テスト =====

    @Test
    fun `定数_サイズ範囲が正しく定義されている`() {
        // When & Then
        assertEquals(60, TagData.MIN_SIZE)
        assertEquals(160, TagData.MAX_SIZE)
    }

    @Test
    fun `定数_デフォルト値が正しく定義されている`() {
        // When & Then
        assertEquals(100, TagData.DEFAULT_SIZE)
        assertEquals("未設定", TagData.DEFAULT_COLOR)
        assertEquals("その他", TagData.DEFAULT_CATEGORY)
    }
}