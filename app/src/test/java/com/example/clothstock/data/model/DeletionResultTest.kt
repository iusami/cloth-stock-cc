package com.example.clothstock.data.model

import org.junit.Test
import org.junit.Assert.*

class DeletionResultTest {

    @Test
    fun `DeletionResult constructor creates correct instance with all parameters`() {
        val failedItems = listOf(
            DeletionFailure(1L, "File not found", null),
            DeletionFailure(2L, "Permission denied", RuntimeException("Access denied"))
        )
        
        val result = DeletionResult(
            totalRequested = 5,
            successfulDeletions = 3,
            failedDeletions = 2,
            failedItems = failedItems
        )
        
        assertEquals(5, result.totalRequested)
        assertEquals(3, result.successfulDeletions)
        assertEquals(2, result.failedDeletions)
        assertEquals(failedItems, result.failedItems)
    }

    @Test
    fun `DeletionResult constructor creates instance with default empty failedItems`() {
        val result = DeletionResult(
            totalRequested = 3,
            successfulDeletions = 3,
            failedDeletions = 0
        )
        
        assertEquals(3, result.totalRequested)
        assertEquals(3, result.successfulDeletions)
        assertEquals(0, result.failedDeletions)
        assertTrue(result.failedItems.isEmpty())
    }

    @Test
    fun `DeletionFailure constructor creates correct instance with exception`() {
        val exception = RuntimeException("Test exception")
        val failure = DeletionFailure(
            itemId = 123L,
            reason = "Database error",
            exception = exception
        )
        
        assertEquals(123L, failure.itemId)
        assertEquals("Database error", failure.reason)
        assertEquals(exception, failure.exception)
    }

    @Test
    fun `DeletionFailure constructor creates correct instance without exception`() {
        val failure = DeletionFailure(
            itemId = 456L,
            reason = "File not found",
            exception = null
        )
        
        assertEquals(456L, failure.itemId)
        assertEquals("File not found", failure.reason)
        assertNull(failure.exception)
    }

    @Test
    fun `DeletionFailure handles negative itemId correctly`() {
        val failure = DeletionFailure(
            itemId = -1L,
            reason = "Invalid ID",
            exception = null
        )
        
        assertEquals(-1L, failure.itemId)
        assertEquals("Invalid ID", failure.reason)
    }

    @Test
    fun `DeletionFailure handles empty reason string correctly`() {
        val failure = DeletionFailure(
            itemId = 1L,
            reason = "",
            exception = null
        )
        
        assertEquals(1L, failure.itemId)
        assertEquals("", failure.reason)
    }

    // Computed properties のテスト
    @Test
    fun `isCompleteSuccess returns true when all deletions succeeded`() {
        val result = DeletionResult(
            totalRequested = 5,
            successfulDeletions = 5,
            failedDeletions = 0
        )
        
        assertTrue(result.isCompleteSuccess)
    }

    @Test
    fun `isCompleteSuccess returns false when some deletions failed`() {
        val result = DeletionResult(
            totalRequested = 5,
            successfulDeletions = 3,
            failedDeletions = 2
        )
        
        assertFalse(result.isCompleteSuccess)
    }

    @Test
    fun `isPartialSuccess returns true when some succeed and some fail`() {
        val result = DeletionResult(
            totalRequested = 5,
            successfulDeletions = 3,
            failedDeletions = 2
        )
        
        assertTrue(result.isPartialSuccess)
    }

    @Test
    fun `isPartialSuccess returns false when all succeed`() {
        val result = DeletionResult(
            totalRequested = 3,
            successfulDeletions = 3,
            failedDeletions = 0
        )
        
        assertFalse(result.isPartialSuccess)
    }

    @Test
    fun `isPartialSuccess returns false when all fail`() {
        val result = DeletionResult(
            totalRequested = 3,
            successfulDeletions = 0,
            failedDeletions = 3
        )
        
        assertFalse(result.isPartialSuccess)
    }

    @Test
    fun `isCompleteFailure returns true when all deletions failed`() {
        val result = DeletionResult(
            totalRequested = 3,
            successfulDeletions = 0,
            failedDeletions = 3
        )
        
        assertTrue(result.isCompleteFailure)
    }

    @Test
    fun `isCompleteFailure returns false when some deletions succeeded`() {
        val result = DeletionResult(
            totalRequested = 5,
            successfulDeletions = 2,
            failedDeletions = 3
        )
        
        assertFalse(result.isCompleteFailure)
    }

    @Test
    fun `isCompleteFailure returns false when all deletions succeeded`() {
        val result = DeletionResult(
            totalRequested = 3,
            successfulDeletions = 3,
            failedDeletions = 0
        )
        
        assertFalse(result.isCompleteFailure)
    }

    // エッジケースのテスト
    @Test
    fun `computed properties work correctly with zero requested deletions`() {
        val result = DeletionResult(
            totalRequested = 0,
            successfulDeletions = 0,
            failedDeletions = 0
        )
        
        assertTrue(result.isCompleteSuccess)
        assertFalse(result.isPartialSuccess)
        assertFalse(result.isCompleteFailure)
    }

    @Test
    fun `computed properties are mutually exclusive in normal cases`() {
        // 全成功のケース
        val completeSuccess = DeletionResult(3, 3, 0)
        assertTrue(completeSuccess.isCompleteSuccess)
        assertFalse(completeSuccess.isPartialSuccess)
        assertFalse(completeSuccess.isCompleteFailure)
        
        // 部分成功のケース
        val partialSuccess = DeletionResult(5, 3, 2)
        assertFalse(partialSuccess.isCompleteSuccess)
        assertTrue(partialSuccess.isPartialSuccess)
        assertFalse(partialSuccess.isCompleteFailure)
        
        // 全失敗のケース
        val completeFailure = DeletionResult(3, 0, 3)
        assertFalse(completeFailure.isCompleteSuccess)
        assertFalse(completeFailure.isPartialSuccess)
        assertTrue(completeFailure.isCompleteFailure)
    }

    // データ整合性とエラー分類のテスト
    @Test
    fun `DeletionResult maintains data consistency between counts and failedItems`() {
        val failedItems = listOf(
            DeletionFailure(1L, "File not found", null),
            DeletionFailure(2L, "Permission denied", RuntimeException("Test"))
        )
        
        val result = DeletionResult(
            totalRequested = 5,
            successfulDeletions = 3,
            failedDeletions = 2,
            failedItems = failedItems
        )
        
        // 失敗数とfailedItemsのサイズが一致していることを確認
        assertEquals(result.failedDeletions, result.failedItems.size)
        
        // 要求数と成功+失敗の合計が一致していることを確認
        assertEquals(result.totalRequested, result.successfulDeletions + result.failedDeletions)
    }

    @Test
    fun `DeletionResult handles large scale deletion results correctly`() {
        val largeFailedItems = (1L..1000L).map { id ->
            DeletionFailure(id, "Batch deletion error", null)
        }
        
        val result = DeletionResult(
            totalRequested = 5000,
            successfulDeletions = 4000,
            failedDeletions = 1000,
            failedItems = largeFailedItems
        )
        
        assertFalse(result.isCompleteSuccess)
        assertTrue(result.isPartialSuccess)
        assertFalse(result.isCompleteFailure)
        assertEquals(1000, result.failedItems.size)
        assertEquals(5000, result.totalRequested)
    }

    @Test
    fun `DeletionFailure supports different error categories`() {
        val fileNotFound = DeletionFailure(1L, "ファイルが見つかりません", null)
        val permissionDenied = DeletionFailure(2L, "アクセスが拒否されました", SecurityException("Access denied"))
        val databaseError = DeletionFailure(3L, "データベースエラー", RuntimeException("Connection failed"))
        val unknownError = DeletionFailure(4L, "不明なエラー", Exception("Unknown"))
        
        val failures = listOf(fileNotFound, permissionDenied, databaseError, unknownError)
        
        // 全ての失敗が適切に構築されていることを確認
        failures.forEach { failure ->
            assertTrue(failure.itemId > 0)
            assertTrue(failure.reason.isNotBlank())
        }
        
        // 例外の有無を確認
        assertNull(fileNotFound.exception)
        assertNotNull(permissionDenied.exception)
        assertNotNull(databaseError.exception)
        assertNotNull(unknownError.exception)
    }

    @Test
    fun `DeletionResult works correctly with complex real-world scenarios`() {
        // シナリオ1: 完全成功（大量データ）
        val completeSuccess = DeletionResult(10000, 10000, 0, emptyList())
        assertTrue(completeSuccess.isCompleteSuccess)
        assertFalse(completeSuccess.isPartialSuccess)
        assertFalse(completeSuccess.isCompleteFailure)
        
        // シナリオ2: 部分成功（ミックス）
        val partialFailures = listOf(
            DeletionFailure(999L, "ファイルロック中", null),
            DeletionFailure(1001L, "権限不足", SecurityException("Permission denied"))
        )
        val partialSuccess = DeletionResult(1000, 998, 2, partialFailures)
        assertFalse(partialSuccess.isCompleteSuccess)
        assertTrue(partialSuccess.isPartialSuccess)
        assertFalse(partialSuccess.isCompleteFailure)
        
        // シナリオ3: 完全失敗（ネットワークエラーなど）
        val totalFailures = (1L..50L).map { id ->
            DeletionFailure(id, "ネットワーク接続エラー", java.net.ConnectException("Connection timeout"))
        }
        val completeFailure = DeletionResult(50, 0, 50, totalFailures)
        assertFalse(completeFailure.isCompleteSuccess)
        assertFalse(completeFailure.isPartialSuccess)
        assertTrue(completeFailure.isCompleteFailure)
    }

    @Test
    fun `data class equals and hashCode work correctly for DeletionResult and DeletionFailure`() {
        // DeletionResult のequals/hashCode
        val result1 = DeletionResult(5, 3, 2, listOf(DeletionFailure(1L, "error", null)))
        val result2 = DeletionResult(5, 3, 2, listOf(DeletionFailure(1L, "error", null)))
        val result3 = DeletionResult(5, 4, 1, listOf(DeletionFailure(2L, "error", null)))
        
        assertEquals(result1, result2)
        assertEquals(result1.hashCode(), result2.hashCode())
        assertNotEquals(result1, result3)
        
        // DeletionFailure のequals/hashCode
        val failure1 = DeletionFailure(1L, "error", null)
        val failure2 = DeletionFailure(1L, "error", null)
        val failure3 = DeletionFailure(2L, "error", null)
        
        assertEquals(failure1, failure2)
        assertEquals(failure1.hashCode(), failure2.hashCode())
        assertNotEquals(failure1, failure3)
    }
}
