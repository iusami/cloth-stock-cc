package com.example.clothstock.ui.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.clothstock.util.MemoryPressureMonitor
import com.example.clothstock.util.ImageCompressionManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * メモリプレッシャー検出とシステム対応のテストクラス（TDD RED段階）
 * 
 * システムメモリ状況の監視と自動対応機能の検証
 * このテストは初期状態では失敗することを想定している（RED段階）
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MemoryPressureTest {

    private lateinit var context: Context
    private lateinit var memoryPressureMonitor: MemoryPressureMonitor
    private lateinit var imageCompressionManager: ImageCompressionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // 未実装のクラスのため、初期化は失敗する想定
        memoryPressureMonitor = MemoryPressureMonitor.getInstance(context)
        imageCompressionManager = ImageCompressionManager.getInstance(context)
    }

    @Test
    fun `メモリプレッシャー監視が正常に開始されること`() {
        // Act: 監視を開始
        val started = memoryPressureMonitor.startMonitoring()

        // Assert: 監視が正常に開始されていること
        assertTrue(started, "メモリプレッシャー監視の開始に失敗しました")
        assertTrue(
            memoryPressureMonitor.isMonitoring(),
            "監視状態が正しく設定されていません"
        )

        // クリーンアップ
        memoryPressureMonitor.stopMonitoring()
    }

    @Test
    fun `メモリプレッシャーレベルが適切に分類されること`() {
        // Arrange: 異なるメモリ状況を模擬
        val testCases = listOf(
            TestMemoryState(availableMemoryMB = 500, expectedLevel = MemoryPressureMonitor.PressureLevel.LOW),
            TestMemoryState(availableMemoryMB = 200, expectedLevel = MemoryPressureMonitor.PressureLevel.MODERATE),
            TestMemoryState(availableMemoryMB = 50, expectedLevel = MemoryPressureMonitor.PressureLevel.HIGH),
            TestMemoryState(availableMemoryMB = 10, expectedLevel = MemoryPressureMonitor.PressureLevel.CRITICAL)
        )

        testCases.forEach { testCase ->
            // Act: メモリ状態を設定してプレッシャーレベルを取得
            memoryPressureMonitor.simulateMemoryState(testCase.availableMemoryMB)
            val actualLevel = memoryPressureMonitor.getCurrentPressureLevel()

            // Assert: 期待されるレベルと一致すること
            assertEquals(
                testCase.expectedLevel,
                actualLevel,
                "メモリ${testCase.availableMemoryMB}MBでのプレッシャーレベルが期待値と異なります"
            )
        }
    }

    @Test
    fun `高メモリプレッシャー時に自動キャッシュクリアが実行されること`() {
        // Arrange: 高メモリプレッシャー状況を作成
        memoryPressureMonitor.simulateMemoryState(20) // 20MB利用可能（HIGH状態）
        
        // キャッシュにデータを追加
        val cacheManager = memoryPressureMonitor.getCacheManager()
        cacheManager.addToCache("test_key_1", createTestData(1024)) // 1KB
        cacheManager.addToCache("test_key_2", createTestData(2048)) // 2KB
        cacheManager.addToCache("test_key_3", createTestData(4096)) // 4KB

        val initialCacheSize = cacheManager.getCurrentCacheSize()
        assertTrue(initialCacheSize > 0, "テストデータがキャッシュに追加されていません")

        // Act: メモリプレッシャー監視を開始
        memoryPressureMonitor.startMonitoring()
        Thread.sleep(500) // 自動クリーンアップの実行を待つ

        // Assert: キャッシュが自動的にクリアされていること
        val finalCacheSize = cacheManager.getCurrentCacheSize()
        assertTrue(
            finalCacheSize < initialCacheSize,
            "自動キャッシュクリアが実行されませんでした（初期: ${initialCacheSize}B, 最終: ${finalCacheSize}B）"
        )

        memoryPressureMonitor.stopMonitoring()
    }

    @Test
    fun `クリティカルメモリ状況でGCが強制実行されること`() {
        // Arrange: クリティカルメモリ状況を作成
        memoryPressureMonitor.simulateMemoryState(5) // 5MB利用可能（CRITICAL状態）
        
        val initialMemory = getCurrentMemoryUsageMB()

        // Act: メモリプレッシャー監視を開始
        memoryPressureMonitor.startMonitoring()
        Thread.sleep(1000) // GC実行を待つ

        // Assert: GCが実行されてメモリが解放されていること
        val finalMemory = getCurrentMemoryUsageMB()
        assertTrue(
            memoryPressureMonitor.hasTriggeredGarbageCollection(),
            "クリティカル状況でGCが実行されませんでした"
        )

        // メモリ使用量が減少していること（または少なくとも増加していないこと）
        assertTrue(
            finalMemory <= initialMemory,
            "GC実行後もメモリ使用量が改善されていません（初期: ${initialMemory}MB, 最終: ${finalMemory}MB）"
        )

        memoryPressureMonitor.stopMonitoring()
    }

    @Test
    fun `メモリプレッシャーコールバックが正常に実行されること`() {
        // Arrange: コールバックのモック
        val callbackResults = mutableListOf<MemoryPressureMonitor.PressureLevel>()
        
        val callback = object : MemoryPressureMonitor.PressureCallback {
            override fun onPressureLevelChanged(level: MemoryPressureMonitor.PressureLevel) {
                callbackResults.add(level)
            }
            
            override fun onLowMemoryWarning() {
                callbackResults.add(MemoryPressureMonitor.PressureLevel.HIGH)
            }
            
            override fun onCriticalMemoryError() {
                callbackResults.add(MemoryPressureMonitor.PressureLevel.CRITICAL)
            }
        }

        memoryPressureMonitor.setPressureCallback(callback)

        // Act: 異なるメモリ状況を順次作成
        memoryPressureMonitor.startMonitoring()
        
        memoryPressureMonitor.simulateMemoryState(300) // LOW
        Thread.sleep(100)
        
        memoryPressureMonitor.simulateMemoryState(100) // MODERATE  
        Thread.sleep(100)
        
        memoryPressureMonitor.simulateMemoryState(30) // HIGH
        Thread.sleep(100)
        
        memoryPressureMonitor.simulateMemoryState(5) // CRITICAL
        Thread.sleep(100)

        // Assert: すべてのコールバックが実行されていること
        assertFalse(callbackResults.isEmpty(), "コールバックが実行されませんでした")
        
        assertTrue(
            callbackResults.contains(MemoryPressureMonitor.PressureLevel.HIGH),
            "HIGH レベルのコールバックが実行されませんでした"
        )
        
        assertTrue(
            callbackResults.contains(MemoryPressureMonitor.PressureLevel.CRITICAL),
            "CRITICAL レベルのコールバックが実行されませんでした"
        )

        memoryPressureMonitor.stopMonitoring()
    }

    @Test
    fun `画像処理時のメモリプレッシャー対応が動作すること`() {
        // Arrange: 画像処理中にメモリプレッシャーが発生する状況
        val largeBitmap = createLargeBitmap(2048, 2048)
        
        // 低メモリ状況を作成
        memoryPressureMonitor.simulateMemoryState(30) // HIGH圧状況
        memoryPressureMonitor.startMonitoring()

        // Act: 低メモリ状況での画像処理を実行
        val result = imageCompressionManager.processImageWithMemoryPressureHandling(largeBitmap)

        // Assert: メモリプレッシャー対応が実行されていること
        assertNotNull(result, "画像処理結果がnullです")
        
        assertTrue(
            result.wasOptimizedForLowMemory,
            "低メモリ対応の最適化が実行されませんでした"
        )

        // 圧縮率が通常より高く設定されていること
        assertTrue(
            result.compressionRatio > 0.7, // 70%以上圧縮
            "低メモリ対応の圧縮が不十分です（圧縮率: ${result.compressionRatio}）"
        )

        // クリーンアップ
        largeBitmap.recycle()
        result.bitmap?.recycle()
        memoryPressureMonitor.stopMonitoring()
    }

    // ===== ヘルパーメソッド =====

    /**
     * テストデータの作成
     */
    private fun createTestData(sizeBytes: Int): ByteArray {
        return ByteArray(sizeBytes)
    }

    /**
     * 現在のメモリ使用量をMB単位で取得
     */
    private fun getCurrentMemoryUsageMB(): Long {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024 * 1024)
    }

    /**
     * テスト用の大きなビットマップを作成
     */
    private fun createLargeBitmap(width: Int, height: Int): android.graphics.Bitmap {
        return android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    }

    /**
     * テスト用メモリ状態データクラス
     */
    private data class TestMemoryState(
        val availableMemoryMB: Int,
        val expectedLevel: MemoryPressureMonitor.PressureLevel
    )
}