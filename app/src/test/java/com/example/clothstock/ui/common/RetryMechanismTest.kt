package com.example.clothstock.ui.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException
import java.net.UnknownHostException
import java.sql.SQLException

/**
 * RetryMechanismのユニットテスト
 * 
 * TDDアプローチに従った包括的リトライ機能のテスト
 */
@ExperimentalCoroutinesApi
class RetryMechanismTest {

    @Test
    fun execute_成功時_Successが返される() = runTest {
        // Given
        val expectedResult = "成功結果"
        val config = RetryMechanism.RetryConfig(maxAttempts = 3)

        // When
        val result = RetryMechanism.execute(config) {
            expectedResult
        }

        // Then
        assertTrue(result is RetryMechanism.RetryResult.Success)
        assertEquals(expectedResult, (result as RetryMechanism.RetryResult.Success).result)
        assertEquals(1, result.attemptCount)
    }

    @Test
    fun execute_最大試行回数まで失敗_Failureが返される() = runTest {
        // Given
        val config = RetryMechanism.RetryConfig(
            maxAttempts = 3,
            initialDelayMs = 10L
        )
        val exception = RuntimeException("テストエラー")

        // When
        val result = RetryMechanism.execute(config) {
            throw exception
        }

        // Then
        assertTrue(result is RetryMechanism.RetryResult.Failure)
        val failure = result as RetryMechanism.RetryResult.Failure
        assertEquals(exception, failure.lastException)
        assertEquals(3, failure.attemptCount)
        assertEquals(3, failure.allExceptions.size)
    }

    @Test
    fun execute_2回目で成功_Successが返される() = runTest {
        // Given
        val config = RetryMechanism.RetryConfig(
            maxAttempts = 3,
            initialDelayMs = 10L
        )
        var attemptCount = 0
        val expectedResult = "2回目で成功"

        // When
        val result = RetryMechanism.execute(config) {
            attemptCount++
            if (attemptCount == 1) {
                throw RuntimeException("1回目は失敗")
            }
            expectedResult
        }

        // Then
        assertTrue(result is RetryMechanism.RetryResult.Success)
        assertEquals(expectedResult, (result as RetryMechanism.RetryResult.Success).result)
        assertEquals(2, result.attemptCount)
    }

    @Test
    fun defaultRetryPolicy_shouldRetry_データベースエラーでtrueが返される() {
        // Given
        val policy = RetryMechanism.DefaultRetryPolicy()
        val databaseException = RuntimeException("database error occurred")

        // When
        val shouldRetry = policy.shouldRetry(databaseException, 1)

        // Then
        assertTrue(shouldRetry)
    }

    @Test
    fun defaultRetryPolicy_shouldRetry_SQLiteエラーでtrueが返される() {
        // Given
        val policy = RetryMechanism.DefaultRetryPolicy()
        val sqliteException = RuntimeException("sqlite constraint failed")

        // When
        val shouldRetry = policy.shouldRetry(sqliteException, 1)

        // Then
        assertTrue(shouldRetry)
    }

    @Test
    fun defaultRetryPolicy_shouldRetry_IOExceptionでtrueが返される() {
        // Given
        val policy = RetryMechanism.DefaultRetryPolicy()
        val ioException = IOException("ファイルアクセスエラー")

        // When
        val shouldRetry = policy.shouldRetry(ioException, 1)

        // Then
        assertTrue(shouldRetry)
    }

    @Test
    fun defaultRetryPolicy_shouldRetry_UnknownHostExceptionでtrueが返される() {
        // Given
        val policy = RetryMechanism.DefaultRetryPolicy()
        val networkException = UnknownHostException("ホストが見つかりません")

        // When
        val shouldRetry = policy.shouldRetry(networkException, 1)

        // Then
        assertTrue(shouldRetry)
    }

    @Test
    fun defaultRetryPolicy_shouldRetry_IllegalArgumentExceptionでfalseが返される() {
        // Given
        val policy = RetryMechanism.DefaultRetryPolicy()
        val validationException = IllegalArgumentException("バリデーションエラー")

        // When
        val shouldRetry = policy.shouldRetry(validationException, 1)

        // Then
        assertFalse(shouldRetry)
    }

    @Test
    fun databaseRetryPolicy_shouldRetry_データベース関連のみリトライする() {
        // Given
        val policy = RetryMechanism.DatabaseRetryPolicy()

        // When & Then
        assertTrue(policy.shouldRetry(RuntimeException("database error"), 1))
        assertTrue(policy.shouldRetry(RuntimeException("sqlite error"), 1))
        assertTrue(policy.shouldRetry(SQLException("SQL error"), 1))
        assertFalse(policy.shouldRetry(IOException("IO error"), 1))
        assertFalse(policy.shouldRetry(IllegalArgumentException("validation error"), 1))
    }

    @Test
    fun retryConfig_DATABASE_DEFAULT_適切な設定値が設定される() {
        // Given
        val config = RetryMechanism.RetryConfig.DATABASE_DEFAULT

        // Then
        assertEquals(5, config.maxAttempts)
        assertEquals(500L, config.initialDelayMs)
        assertEquals(10000L, config.maxDelayMs)
        assertEquals(1.5, config.backoffMultiplier, 0.01)
        assertEquals(0.2, config.jitterFactor, 0.01)
    }

    @Test
    fun retryConfig_FILE_IO_DEFAULT_適切な設定値が設定される() {
        // Given
        val config = RetryMechanism.RetryConfig.FILE_IO_DEFAULT

        // Then
        assertEquals(3, config.maxAttempts)
        assertEquals(1000L, config.initialDelayMs)
        assertEquals(15000L, config.maxDelayMs)
        assertEquals(2.0, config.backoffMultiplier, 0.01)
        assertEquals(0.1, config.jitterFactor, 0.01)
    }

    @Test
    fun retryConfig_NETWORK_DEFAULT_適切な設定値が設定される() {
        // Given
        val config = RetryMechanism.RetryConfig.NETWORK_DEFAULT

        // Then
        assertEquals(3, config.maxAttempts)
        assertEquals(2000L, config.initialDelayMs)
        assertEquals(30000L, config.maxDelayMs)
        assertEquals(2.0, config.backoffMultiplier, 0.01)
        assertEquals(0.15, config.jitterFactor, 0.01)
    }

    @Test
    fun executeForDatabase_データベース専用設定で実行される() = runTest {
        // Given
        val expectedResult = "データベース処理成功"

        // When
        val result = RetryMechanism.executeForDatabase {
            expectedResult
        }

        // Then
        assertTrue(result is RetryMechanism.RetryResult.Success)
        assertEquals(expectedResult, (result as RetryMechanism.RetryResult.Success).result)
    }

    @Test
    fun executeForFileIO_ファイルIO専用設定で実行される() = runTest {
        // Given
        val expectedResult = "ファイル処理成功"

        // When
        val result = RetryMechanism.executeForFileIO {
            expectedResult
        }

        // Then
        assertTrue(result is RetryMechanism.RetryResult.Success)
        assertEquals(expectedResult, (result as RetryMechanism.RetryResult.Success).result)
    }

    @Test
    fun executeForNetwork_ネットワーク専用設定で実行される() = runTest {
        // Given
        val expectedResult = "ネットワーク処理成功"

        // When
        val result = RetryMechanism.executeForNetwork {
            expectedResult
        }

        // Then
        assertTrue(result is RetryMechanism.RetryResult.Success)
        assertEquals(expectedResult, (result as RetryMechanism.RetryResult.Success).result)
    }
}