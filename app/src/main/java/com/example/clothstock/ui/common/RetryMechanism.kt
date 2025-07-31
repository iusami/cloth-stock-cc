package com.example.clothstock.ui.common

import kotlinx.coroutines.*
import kotlin.math.pow

/**
 * リトライメカニズムの実装
 * 
 * 指数バックオフとジッター付きの再試行機能を提供
 * TDDアプローチに従って実装され、テスト可能な設計
 */
class RetryMechanism {

    /**
     * リトライ設定
     */
    data class RetryConfig(
        val maxAttempts: Int = 3,
        val initialDelayMs: Long = 1000L,
        val maxDelayMs: Long = 30000L,
        val backoffMultiplier: Double = 2.0,
        val jitterFactor: Double = 0.1
    ) {
        companion object {
            /**
             * データベース操作用のデフォルト設定
             */
            val DATABASE_DEFAULT = RetryConfig(
                maxAttempts = 5,
                initialDelayMs = 500L,
                maxDelayMs = 10000L,
                backoffMultiplier = 1.5,
                jitterFactor = 0.2
            )

            /**
             * ファイルI/O操作用のデフォルト設定
             */
            val FILE_IO_DEFAULT = RetryConfig(
                maxAttempts = 3,
                initialDelayMs = 1000L,
                maxDelayMs = 15000L,
                backoffMultiplier = 2.0,
                jitterFactor = 0.1
            )

            /**
             * ネットワーク操作用のデフォルト設定
             */
            val NETWORK_DEFAULT = RetryConfig(
                maxAttempts = 3,
                initialDelayMs = 2000L,
                maxDelayMs = 30000L,
                backoffMultiplier = 2.0,
                jitterFactor = 0.15
            )
        }
    }

    /**
     * リトライの結果
     */
    sealed class RetryResult<T> {
        data class Success<T>(val result: T, val attemptCount: Int) : RetryResult<T>()
        data class Failure<T>(
            val lastException: Exception,
            val attemptCount: Int,
            val allExceptions: List<Exception>
        ) : RetryResult<T>()
    }

    /**
     * リトライ可能な例外かどうかを判定するインターフェース
     */
    interface RetryPolicy {
        fun shouldRetry(exception: Exception, attemptCount: Int): Boolean
    }

    /**
     * デフォルトのリトライポリシー
     */
    class DefaultRetryPolicy : RetryPolicy {
        override fun shouldRetry(exception: Exception, attemptCount: Int): Boolean {
            return when {
                // SQLite関連のエラーはリトライ可能
                exception.message?.contains("database", ignoreCase = true) == true -> true
                exception.message?.contains("sqlite", ignoreCase = true) == true -> true
                
                // ファイルI/O関連のエラーはリトライ可能
                exception is java.io.IOException -> true
                
                // ネットワーク関連のエラーはリトライ可能
                exception is java.net.UnknownHostException -> true
                exception is java.net.SocketTimeoutException -> true
                
                // バリデーションエラーはリトライ不可
                exception is IllegalArgumentException -> false
                
                // その他は最大3回までリトライ
                else -> attemptCount < 3
            }
        }
    }

    /**
     * データベース操作専用のリトライポリシー
     */
    class DatabaseRetryPolicy : RetryPolicy {
        override fun shouldRetry(exception: Exception, attemptCount: Int): Boolean {
            return when {
                // データベース関連の例外のみリトライ
                exception.message?.contains("database", ignoreCase = true) == true -> true
                exception.message?.contains("sqlite", ignoreCase = true) == true -> true
                exception is java.sql.SQLException -> true
                
                // その他はリトライしない
                else -> false
            }
        }
    }

    companion object {
        private val defaultRetryPolicy = DefaultRetryPolicy()

        /**
         * リトライ実行（suspend function版）
         * 
         * @param config リトライ設定
         * @param retryPolicy リトライポリシー
         * @param operation 実行する操作
         * @return リトライ結果
         */
        suspend fun <T> execute(
            config: RetryConfig = RetryConfig(),
            retryPolicy: RetryPolicy = defaultRetryPolicy,
            operation: suspend () -> T
        ): RetryResult<T> {
            var attempt = 0
            val exceptions = mutableListOf<Exception>()

            while (attempt < config.maxAttempts) {
                attempt++
                
                try {
                    val result = operation()
                    return RetryResult.Success(result, attempt)
                } catch (e: Exception) {
                    exceptions.add(e)
                    
                    // 最後の試行の場合、または、リトライポリシーがリトライを許可しない場合
                    if (attempt >= config.maxAttempts || !retryPolicy.shouldRetry(e, attempt)) {
                        return RetryResult.Failure(e, attempt, exceptions.toList())
                    }
                    
                    // 指数バックオフ + ジッターで待機
                    val delayMs = calculateDelay(attempt, config)
                    delay(delayMs)
                }
            }

            // ここには到達しないはずだが、念のため
            return RetryResult.Failure(
                RuntimeException("Unexpected retry failure"),
                attempt,
                exceptions.toList()
            )
        }

        /**
         * リトライ実行（コールバック版、メインスレッド実行）
         * 
         * @param config リトライ設定
         * @param retryPolicy リトライポリシー
         * @param operation 実行する操作
         * @param onResult 結果コールバック
         */
        fun <T> executeAsync(
            config: RetryConfig = RetryConfig(),
            retryPolicy: RetryPolicy = defaultRetryPolicy,
            operation: suspend () -> T,
            onResult: (RetryResult<T>) -> Unit
        ) {
            CoroutineScope(Dispatchers.Main).launch {
                val result = execute(config, retryPolicy, operation)
                onResult(result)
            }
        }

        /**
         * データベース操作専用のリトライ実行
         */
        suspend fun <T> executeForDatabase(
            operation: suspend () -> T
        ): RetryResult<T> {
            return execute(
                config = RetryConfig.DATABASE_DEFAULT,
                retryPolicy = DatabaseRetryPolicy(),
                operation = operation
            )
        }

        /**
         * ファイルI/O操作専用のリトライ実行
         */
        suspend fun <T> executeForFileIO(
            operation: suspend () -> T
        ): RetryResult<T> {
            return execute(
                config = RetryConfig.FILE_IO_DEFAULT,
                retryPolicy = defaultRetryPolicy,
                operation = operation
            )
        }

        /**
         * ネットワーク操作専用のリトライ実行
         */
        suspend fun <T> executeForNetwork(
            operation: suspend () -> T
        ): RetryResult<T> {
            return execute(
                config = RetryConfig.NETWORK_DEFAULT,
                retryPolicy = defaultRetryPolicy,
                operation = operation
            )
        }

        /**
         * 指数バックオフ + ジッターによる遅延時間の計算
         * 
         * @param attempt 試行回数（1ベース）
         * @param config リトライ設定
         * @return 遅延時間（ミリ秒）
         */
        private fun calculateDelay(attempt: Int, config: RetryConfig): Long {
            // 指数バックオフ
            val exponentialDelay = (config.initialDelayMs * config.backoffMultiplier.pow(attempt - 1)).toLong()
            
            // 最大遅延時間で制限
            val clampedDelay = minOf(exponentialDelay, config.maxDelayMs)
            
            // ジッター追加（遅延時間の±jitterFactor%の範囲でランダム化）
            val jitterRange = (clampedDelay * config.jitterFactor).toLong()
            val jitter = (-jitterRange..jitterRange).random()
            
            return maxOf(0L, clampedDelay + jitter)
        }
    }
}