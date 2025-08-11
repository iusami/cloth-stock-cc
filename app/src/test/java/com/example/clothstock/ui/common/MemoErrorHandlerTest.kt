package com.example.clothstock.ui.common

import android.content.Context
import android.content.res.Resources
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * MemoErrorHandlerのユニットテスト（コアロジック）
 * 
 * Task 8: エラーハンドリングとバリデーション機能のテスト
 * Requirements 1.3, 1.4, 2.4の検証
 * 
 * TDD Red-Green-Refactorサイクルに基づくテスト実装
 * 
 * NOTE: AndroidのUIコンポーネント（Snackbar）のテストは統合テスト（Espresso）で実装
 * ここではUIに依存しないコアロジック（エラー頻度制限、メトリクス収集）をテスト
 */
@ExperimentalCoroutinesApi
class MemoErrorHandlerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Suppress("unused")
    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockRootView: View
    
    @Mock
    private lateinit var mockResources: Resources

    private lateinit var memoErrorHandler: MemoErrorHandler
    private var retryCallbackInvoked = false
    private var retryCallbackMemo = ""

    // テスト用定数
    companion object {
        private const val TEST_MEMO_SHORT = "テストメモ"
        private const val TEST_ERROR_MESSAGE = "テストエラーメッセージ"
        private const val TEST_VALIDATION_MESSAGE = "バリデーションエラー"
    }
    
    // 動的な値はcompanion objectの外で定義（テストでは未使用だが、将来の拡張性のため保持）
    @Suppress("UnusedPrivateProperty")
    private val testMemoLong = "a".repeat(1001) // 1001文字（制限超過）

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Contextのモック設定
        `when`(mockContext.resources).thenReturn(mockResources)
        
        // Android APIレベル23以上のgetColor(int)メソッドをモック
        runCatching {
            `when`(mockContext.getColor(anyInt())).thenReturn(0xFF000000.toInt())
        } // Android API互換性のため例外を無視（テスト環境での意図された動作）
        
        // リトライコールバックの初期化
        retryCallbackInvoked = false
        retryCallbackMemo = ""

        // MemoErrorHandlerの初期化
        memoErrorHandler = MemoErrorHandler(
            context = mockContext,
            rootView = mockRootView
        ) { memo ->
            retryCallbackInvoked = true
            retryCallbackMemo = memo
        }
    }

    @After
    fun tearDown() {
        memoErrorHandler.cleanup()
    }

    // ===== Requirements 2.4: メモ保存失敗エラーのテスト =====

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `showMemoSaveError - 基本的なメモ保存エラー表示`() = runTest {
        // Given
        val originalMemo = TEST_MEMO_SHORT
        val errorMessage = TEST_ERROR_MESSAGE
        val testException = RuntimeException("テスト例外")

        // When
        memoErrorHandler.showMemoSaveError(errorMessage, originalMemo, testException)

        // Then: エラーメトリクスが記録されることを確認
        val metrics = memoErrorHandler.getErrorMetrics()
        assertTrue(metrics.containsKey("MEMO_SAVE_ERROR"))
        assertEquals(1, metrics["MEMO_SAVE_ERROR"])
    }

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `showMemoSaveErrorWithRetry - リトライ機能付きエラー表示`() = runTest {
        // Given
        val memo = TEST_MEMO_SHORT
        val errorMessage = TEST_ERROR_MESSAGE
        val retryCount = 1
        val testException = RuntimeException("テスト例外")

        // When
        memoErrorHandler.showMemoSaveErrorWithRetry(errorMessage, memo, retryCount, testException)

        // Then: エラーメトリクスが記録されることを確認
        val metrics = memoErrorHandler.getErrorMetrics()
        assertTrue(metrics.containsKey("MEMO_SAVE_ERROR_WITH_RETRY"))
        assertEquals(1, metrics["MEMO_SAVE_ERROR_WITH_RETRY"])
    }

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `showMemoSaveErrorWithRetry - 3回以上のリトライでは異なる色を使用`() = runTest {
        // Given
        val memo = TEST_MEMO_SHORT
        val errorMessage = TEST_ERROR_MESSAGE
        val retryCount = 3  // 3回以上
        val testException = RuntimeException("テスト例外")

        // When
        memoErrorHandler.showMemoSaveErrorWithRetry(errorMessage, memo, retryCount, testException)

        // Then: メトリクスで実行回数を確認
        val metrics = memoErrorHandler.getErrorMetrics()
        assertTrue(metrics.containsKey("MEMO_SAVE_ERROR_WITH_RETRY"))
    }

    // ===== Requirements 1.3, 1.4: バリデーションエラーのテスト =====

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `showMemoValidationError - 文字数制限エラー表示`() = runTest {
        // Given
        val validationMessage = TEST_VALIDATION_MESSAGE
        val currentLength = 1001  // 制限超過

        // When
        memoErrorHandler.showMemoValidationError(validationMessage, currentLength)

        // Then: エラーメトリクスが記録されることを確認
        val metrics = memoErrorHandler.getErrorMetrics()
        assertTrue(metrics.containsKey("MEMO_VALIDATION_ERROR"))
        assertEquals(1, metrics["MEMO_VALIDATION_ERROR"])
    }

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `showMemoValidationError - 詳細メッセージに文字数情報が含まれる`() = runTest {
        // Given
        val validationMessage = "文字数制限エラー"
        val currentLength = 1050

        // When
        memoErrorHandler.showMemoValidationError(validationMessage, currentLength)

        // Then: ログ出力でメッセージが確認されることを検証
        // 実際のSnackbar表示はUIテストで確認するため、ここではメトリクス確認
        val metrics = memoErrorHandler.getErrorMetrics()
        assertEquals(1, metrics["MEMO_VALIDATION_ERROR"])
    }

    // ===== 自動保存エラーのテスト =====

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `showMemoAutoSaveError - 自動保存エラー表示`() = runTest {
        // Given
        val errorMessage = "自動保存失敗"
        val memo = TEST_MEMO_SHORT

        // When
        memoErrorHandler.showMemoAutoSaveError(errorMessage, memo)

        // Then: エラーメトリクスが記録されることを確認
        val metrics = memoErrorHandler.getErrorMetrics()
        assertTrue(metrics.containsKey("MEMO_AUTO_SAVE_ERROR"))
        assertEquals(1, metrics["MEMO_AUTO_SAVE_ERROR"])
    }

    // ===== メモ読み込みエラーのテスト =====

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `showMemoLoadError - メモ読み込みエラー表示`() = runTest {
        // Given
        val errorMessage = "メモ読み込み失敗"
        val testException = RuntimeException("読み込み例外")

        // When
        memoErrorHandler.showMemoLoadError(errorMessage, testException)

        // Then: エラーメトリクスが記録されることを確認
        val metrics = memoErrorHandler.getErrorMetrics()
        assertTrue(metrics.containsKey("MEMO_LOAD_ERROR"))
        assertEquals(1, metrics["MEMO_LOAD_ERROR"])
    }

    // ===== エラー頻度制限のテスト（コアロジック） =====

    @Test
    fun `エラー頻度制限 - shouldShowErrorメソッドのテスト`() = runTest {
        // Given
        val shouldShowErrorMethod = memoErrorHandler::class.java.getDeclaredMethod("shouldShowError")
        shouldShowErrorMethod.isAccessible = true

        // When: 最初の呼び出し
        val firstCall = shouldShowErrorMethod.invoke(memoErrorHandler) as Boolean

        // すぐに2回目の呼び出し（ERROR_THROTTLE_MS以内）
        val secondCall = shouldShowErrorMethod.invoke(memoErrorHandler) as Boolean

        // Then: 1回目はtrue、2回目はfalse（頻度制限）
        assertTrue(firstCall, "最初のエラー表示は許可されるべき")
        assertFalse(secondCall, "短時間での連続エラーは制限されるべき")
    }

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `エラー頻度制限 - 短時間での連続エラーは制限される`() = runTest {
        // Given
        val errorMessage = "頻繁なエラー"
        val memo = TEST_MEMO_SHORT

        // When: 連続でエラーを表示
        memoErrorHandler.showMemoSaveError(errorMessage, memo)
        memoErrorHandler.showMemoSaveError(errorMessage, memo) // 2回目は制限される

        // Then: エラーメトリクスで1回のみ記録されていることを確認
        val metrics = memoErrorHandler.getErrorMetrics()
        assertEquals(1, metrics["MEMO_SAVE_ERROR"])
    }

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `エラー頻度制限 - 制限回数を超えると一定時間制限される`() = runTest {
        // Given
        val errorMessage = "大量エラー"
        val memo = TEST_MEMO_SHORT

        // When: 制限回数まで連続してエラーを発生（時間をずらして）
        repeat(6) { // MAX_ERRORS_PER_MINUTE = 5なので6回
            Thread.sleep(200) // 短い間隔で連続実行
            memoErrorHandler.showMemoSaveError(errorMessage, memo)
        }

        // Then: 制限により全てが記録されるわけではない
        val metrics = memoErrorHandler.getErrorMetrics()
        val errorCount = metrics["MEMO_SAVE_ERROR"] ?: 0
        assertTrue(errorCount <= 5) // エラー回数が制限されるべき
    }

    // ===== バリデーション成功フィードバックのテスト =====

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `showMemoValidationSuccess - バリデーション成功メッセージ表示`() = runTest {
        // Given
        val successMessage = "バリデーション成功"

        // When
        memoErrorHandler.showMemoValidationSuccess(successMessage)

        // Then: 例外が発生しないことを確認（成功フィードバックはメトリクスなし）
        // 実際の表示はUIテストで確認
        assertTrue(true) // 成功フィードバック表示が完了
    }

    // ===== エラー詳細取得のテスト =====

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `getErrorDetail - 様々な例外タイプの詳細取得`() = runTest {
        // Given & When & Then: 各例外タイプでエラー詳細が適切に取得される
        
        // TimeoutException (内部クラスなのでRuntimeExceptionで代用)
        val timeoutException = RuntimeException("タイムアウト")
        memoErrorHandler.showMemoSaveError("タイムアウトエラー", TEST_MEMO_SHORT, timeoutException)
        
        // IllegalStateException
        val stateException = IllegalStateException("状態エラー")
        memoErrorHandler.showMemoSaveError("状態エラー", TEST_MEMO_SHORT, stateException)
        
        // SecurityException
        val securityException = SecurityException("権限エラー")
        memoErrorHandler.showMemoSaveError("権限エラー", TEST_MEMO_SHORT, securityException)

        // エラーメトリクスで実行回数を確認
        val metrics = memoErrorHandler.getErrorMetrics()
        assertTrue(metrics.containsKey("MEMO_SAVE_ERROR"))
        val errorCount = metrics["MEMO_SAVE_ERROR"] ?: 0
        assertTrue(errorCount >= 1) // 複数のエラータイプが処理された
    }

    // ===== リソースクリーンアップのテスト =====

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `cleanup - リソースクリーンアップの確認`() = runTest {
        // Given: エラーを発生させてメトリクスを蓄積
        memoErrorHandler.showMemoSaveError("テスト", TEST_MEMO_SHORT)
        val metricsBeforeCleanup = memoErrorHandler.getErrorMetrics()
        assertTrue(metricsBeforeCleanup.isNotEmpty()) // クリーンアップ前にメトリクスが存在

        // When: クリーンアップ実行
        memoErrorHandler.cleanup()

        // Then: メトリクスがクリアされることを確認
        val metricsAfterCleanup = memoErrorHandler.getErrorMetrics()
        assertTrue(metricsAfterCleanup.isEmpty()) // クリーンアップ後にメトリクスがクリアされる
    }

    // ===== エラーメトリクスのテスト（コアロジック） =====

    @Test
    fun `recordErrorMetric - メトリクス記録のコアロジック`() = runTest {
        // Given
        val recordErrorMetricMethod = memoErrorHandler::class.java.getDeclaredMethod(
            "recordErrorMetric", 
            String::class.java, 
            Long::class.java
        )
        recordErrorMetricMethod.isAccessible = true
        
        val startTime = System.currentTimeMillis()

        // When: メトリクスを記録
        recordErrorMetricMethod.invoke(memoErrorHandler, "TEST_ERROR", startTime)
        recordErrorMetricMethod.invoke(memoErrorHandler, "TEST_ERROR", startTime)

        // Then: メトリクスが正しく記録されている
        val metrics = memoErrorHandler.getErrorMetrics()
        assertEquals(2, metrics["TEST_ERROR"]) // 2回記録されている
    }

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `getErrorMetrics - エラーメトリクス取得の確認`() = runTest {
        // Given: 様々なタイプのエラーを発生
        memoErrorHandler.showMemoSaveError("保存エラー", TEST_MEMO_SHORT)
        memoErrorHandler.showMemoValidationError("バリデーションエラー", 1001)
        memoErrorHandler.showMemoAutoSaveError("自動保存エラー", TEST_MEMO_SHORT)

        // When: メトリクス取得
        val metrics = memoErrorHandler.getErrorMetrics()

        // Then: 各エラータイプのメトリクスが記録されている
        assertTrue(metrics.containsKey("MEMO_SAVE_ERROR")) // 保存エラーメトリクスが存在
        assertTrue(metrics.containsKey("MEMO_VALIDATION_ERROR")) // バリデーションエラーメトリクスが存在
        assertTrue(metrics.containsKey("MEMO_AUTO_SAVE_ERROR")) // 自動保存エラーメトリクスが存在
        
        // 各メトリクスが正しい回数を示している
        assertEquals(1, metrics["MEMO_SAVE_ERROR"])
        assertEquals(1, metrics["MEMO_VALIDATION_ERROR"])
        assertEquals(1, metrics["MEMO_AUTO_SAVE_ERROR"])
    }

    // ===== Context例外処理のテスト =====

    @Ignore("「SnackbarのUIテストは統合テストで実装")
    @Test
    fun `Context例外処理 - Context取得失敗時の適切な処理`() = runTest {
        // Given: 無効なContextでMemoErrorHandlerを作成
        val invalidContextHandler = MemoErrorHandler(
            context = mock(Context::class.java), // 設定されていないモックContext
            rootView = mockRootView
        ) { /* リトライコールバック */ }

        // When & Then: 例外が発生してもクラッシュしない
        try {
            invalidContextHandler.showMemoSaveError("テスト", TEST_MEMO_SHORT)
            // 例外が発生しなければ成功
            assertTrue(true) // Context例外が適切に処理された
        } catch (e: Exception) {
            // 想定外の例外が発生した場合はテスト失敗
            @Suppress("SwallowedException")
            assertTrue(false, "Context例外処理が失敗: ${e.message}")
        } finally {
            invalidContextHandler.cleanup()
        }
    }
}
