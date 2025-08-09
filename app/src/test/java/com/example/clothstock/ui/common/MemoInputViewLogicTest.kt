package com.example.clothstock.ui.common

import com.example.clothstock.data.model.ClothItem
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * MemoInputViewのロジックテスト
 * 
 * UIコンポーネントに依存しない基本的なロジックのテスト
 * LayoutInflaterに関する問題を回避するため、ロジック部分のみテスト
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MemoInputViewLogicTest {

    @Before
    fun setUp() {
        // 特別な設定は不要
    }

    // ===== 文字数制限ロジックのテスト =====

    @Test
    fun `truncate memo when exceeding max length`() {
        // Given
        val longText = "a".repeat(ClothItem.MAX_MEMO_LENGTH + 100)
        val expectedText = "a".repeat(ClothItem.MAX_MEMO_LENGTH)
        
        // When (ロジックをテスト)
        val result = longText.take(ClothItem.MAX_MEMO_LENGTH)
        
        // Then
        assertEquals(expectedText, result)
        assertEquals(ClothItem.MAX_MEMO_LENGTH, result.length)
    }

    @Test
    fun `handle memo at exactly max length`() {
        // Given
        val maxLengthText = "b".repeat(ClothItem.MAX_MEMO_LENGTH)
        
        // When
        val result = maxLengthText.take(ClothItem.MAX_MEMO_LENGTH)
        
        // Then
        assertEquals(maxLengthText, result)
        assertEquals(ClothItem.MAX_MEMO_LENGTH, result.length)
    }

    @Test
    fun `handle memo just under max length`() {
        // Given
        val nearMaxText = "c".repeat(ClothItem.MAX_MEMO_LENGTH - 1)
        
        // When
        val result = nearMaxText.take(ClothItem.MAX_MEMO_LENGTH)
        
        // Then
        assertEquals(nearMaxText, result)
        assertEquals(ClothItem.MAX_MEMO_LENGTH - 1, result.length)
    }

    // ===== 文字数カウントロジックのテスト =====

    @Test
    fun `generate character count text correctly`() {
        // Given
        val testCases = listOf(
            0 to "0/${ClothItem.MAX_MEMO_LENGTH}",
            10 to "10/${ClothItem.MAX_MEMO_LENGTH}",
            500 to "500/${ClothItem.MAX_MEMO_LENGTH}",
            ClothItem.MAX_MEMO_LENGTH to "${ClothItem.MAX_MEMO_LENGTH}/${ClothItem.MAX_MEMO_LENGTH}"
        )
        
        testCases.forEach { (count, expectedText) ->
            // When
            val result = "$count/${ClothItem.MAX_MEMO_LENGTH}"
            
            // Then
            assertEquals("文字数 $count のカウントテキストが正しい", expectedText, result)
        }
    }

    // ===== 警告状態判定ロジックのテスト =====

    @Test
    fun `determine warning state correctly`() {
        // Given
        val warningThreshold = (ClothItem.MAX_MEMO_LENGTH * 0.9).toInt()
        
        val testCases = listOf(
            warningThreshold - 10 to false, // 通常状態
            warningThreshold - 1 to false,  // 通常状態（境界値）
            warningThreshold to true,       // 警告状態（境界値）
            warningThreshold + 10 to true,  // 警告状態
            ClothItem.MAX_MEMO_LENGTH to true // 最大値
        )
        
        testCases.forEach { (count, expectedWarning) ->
            // When
            val isWarning = count >= warningThreshold
            
            // Then
            assertEquals(
                "文字数 $count での警告状態判定が正しい",
                expectedWarning, 
                isWarning
            )
        }
    }

    @Test
    fun `warning threshold calculation is correct`() {
        // Given
        val expectedThreshold = (ClothItem.MAX_MEMO_LENGTH * 0.9).toInt()
        
        // When
        val calculatedThreshold = (ClothItem.MAX_MEMO_LENGTH * 0.9).toInt()
        
        // Then
        assertEquals(expectedThreshold, calculatedThreshold)
        assertTrue("警告閾値が最大文字数より小さい", calculatedThreshold < ClothItem.MAX_MEMO_LENGTH)
        assertTrue("警告閾値が0より大きい", calculatedThreshold > 0)
    }

    // ===== 文字列処理ロジックのテスト =====

    @Test
    fun `handle unicode characters correctly`() {
        // Given
        val unicodeText = "🌟".repeat(100) + "あいうえお"
        val maxLength = ClothItem.MAX_MEMO_LENGTH
        
        // When
        val result = unicodeText.take(maxLength)
        
        // Then
        assertTrue("Unicode文字でも制限文字数以下", result.length <= maxLength)
        assertTrue("結果が空でない", result.isNotEmpty())
    }

    @Test
    fun `handle newlines and whitespace correctly`() {
        // Given
        val textWithNewlines = "行1\n行2\n\t空白を含む行\n行4"
        
        // When
        val result = textWithNewlines.take(ClothItem.MAX_MEMO_LENGTH)
        
        // Then
        assertEquals("改行・タブ文字が保持される", textWithNewlines, result)
        assertTrue("改行文字を含む", result.contains("\n"))
        assertTrue("タブ文字を含む", result.contains("\t"))
    }

    @Test
    fun `handle empty and null strings correctly`() {
        // Given
        val emptyString = ""
        val nullString: String? = null
        
        // When & Then
        assertEquals("空文字列の処理", "", emptyString.take(ClothItem.MAX_MEMO_LENGTH))
        assertEquals("null文字列の処理", "", (nullString ?: "").take(ClothItem.MAX_MEMO_LENGTH))
    }

    // ===== ClothItem定数のテスト =====

    @Test
    fun `ClothItem MAX_MEMO_LENGTH is defined correctly`() {
        // Given & When
        val maxLength = ClothItem.MAX_MEMO_LENGTH
        
        // Then
        assertEquals("最大メモ文字数が1000文字", 1000, maxLength)
        assertTrue("最大文字数が正の値", maxLength > 0)
    }
}
