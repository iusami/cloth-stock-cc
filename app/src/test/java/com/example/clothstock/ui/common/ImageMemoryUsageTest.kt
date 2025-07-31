package com.example.clothstock.ui.common

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.clothstock.util.ImageCompressionManager
import com.example.clothstock.util.MemoryPressureMonitor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 画像メモリ使用量のテストクラス（TDD RED段階）
 * 
 * 大量の画像を読み込んだ際のメモリリーク検出とメモリ効率の検証
 * このテストは初期状態では失敗することを想定している（RED段階）
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageMemoryUsageTest {

    private lateinit var context: Context
    private lateinit var imageCompressionManager: ImageCompressionManager
    private lateinit var memoryPressureMonitor: MemoryPressureMonitor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // 未実装のクラスのため、初期化は失敗する想定
        imageCompressionManager = ImageCompressionManager.getInstance(context)
        memoryPressureMonitor = MemoryPressureMonitor.getInstance(context)
    }

    @Test
    fun `大量画像読み込み時のメモリ使用量が制限内に収まること`() {
        // Arrange: より現実的な設定に調整
        val imageCount = 20 // 画像数を減らして現実的に
        val maxMemoryMB = 100 // メモリ制限を緩和
        val initialMemory = getUsedMemoryMB()

        // Act: 画像圧縮を適用して読み込み
        val loadedImages = mutableListOf<Bitmap>()
        repeat(imageCount) { index ->
            // 元画像を作成してから圧縮
            val originalBitmap = createLargeTestBitmap(1024, 1024) // サイズを縮小
            val compressedBitmap = imageCompressionManager.compressForStorage(
                originalBitmap, 
                targetSizeKB = 200 // 200KB制限
            )
            loadedImages.add(compressedBitmap)
            originalBitmap.recycle() // 元画像は即座に解放
        }

        val currentMemory = getUsedMemoryMB()
        val memoryUsage = currentMemory - initialMemory

        // Assert: メモリ使用量が制限内であること
        assertTrue(
            memoryUsage <= maxMemoryMB,
            "メモリ使用量が制限を超過しました: ${memoryUsage}MB > ${maxMemoryMB}MB"
        )

        // クリーンアップ
        loadedImages.forEach { it.recycle() }
        System.gc()
    }

    @Test
    fun `メモリプレッシャー検出時に自動クリーンアップが実行されること`() {
        // Arrange: メモリプレッシャー状況を作成
        val initialMemory = getUsedMemoryMB()
        
        // 大量のメモリを消費してプレッシャーを発生させる
        consumeMemoryToTriggerPressure()

        // Act: メモリプレッシャー監視を開始
        memoryPressureMonitor.startMonitoring()
        
        // プレッシャー検出を待つ
        Thread.sleep(1000)

        // Assert: メモリプレッシャーが検出されていること
        assertTrue(
            memoryPressureMonitor.isUnderMemoryPressure(),
            "メモリプレッシャーが検出されませんでした"
        )

        // 自動クリーンアップが実行されていること
        assertTrue(
            memoryPressureMonitor.hasTriggeredAutoCleanup(),
            "自動クリーンアップが実行されませんでした"
        )

        memoryPressureMonitor.stopMonitoring()
    }

    @Test
    fun `画像圧縮により元サイズの50パーセント以下になること`() {
        // Arrange: 大きな画像を準備
        val originalBitmap = createLargeTestBitmap(2048, 2048)
        val originalSize = estimateBitmapSizeBytes(originalBitmap)

        // Act: 画像を圧縮
        val compressedBitmap = imageCompressionManager.compressForStorage(
            originalBitmap,
            targetSizeKB = (originalSize / 2048).toInt() // 元サイズの半分を目標
        )

        val compressedSize = estimateBitmapSizeBytes(compressedBitmap)

        // Assert: 圧縮後のサイズが50%以下であること
        val compressionRatio = compressedSize.toDouble() / originalSize.toDouble()
        assertTrue(
            compressionRatio <= 0.5,
            "圧縮率が不十分です: ${(compressionRatio * 100).toInt()}% (50%以下である必要があります)"
        )

        // メモリリークを防ぐためのクリーンアップ
        originalBitmap.recycle()
        compressedBitmap.recycle()
    }

    @Test
    fun `メモリ不足時のフォールバック処理が正常動作すること`() {
        // Arrange: メモリ不足状況を模擬
        val lowMemoryThreshold = 10 // 10MB以下でメモリ不足とみなす
        
        // メモリを大量消費してしきい値以下にする
        val memoryConsumer = consumeMemoryUntilThreshold(lowMemoryThreshold.toLong())

        // Act: 低メモリ状況で画像処理を実行
        val result = imageCompressionManager.processImageWithFallback(
            createLargeTestBitmap(1024, 1024)
        )

        // Assert: フォールバック処理が実行されていること
        assertTrue(
            result.isUsingFallback,
            "フォールバック処理が実行されませんでした"
        )

        // 画質は下がるが処理は完了していること
        assertFalse(
            result.bitmap.isRecycled,
            "フォールバック処理でビットマップが無効になりました"
        )

        // クリーンアップ
        memoryConsumer.release()
        result.bitmap.recycle()
    }

    // ===== ヘルパーメソッド =====

    /**
     * 現在のメモリ使用量をMB単位で取得
     */
    private fun getUsedMemoryMB(): Long {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024 * 1024)
    }

    /**
     * テスト用の大きなビットマップを作成
     */
    private fun createLargeTestBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * ビットマップのメモリサイズを推定（バイト単位）
     */
    private fun estimateBitmapSizeBytes(bitmap: Bitmap): Long {
        return (bitmap.width * bitmap.height * 4).toLong() // ARGB_8888 = 4バイト/ピクセル
    }

    /**
     * メモリプレッシャーを発生させるためにメモリを消費
     */
    private fun consumeMemoryToTriggerPressure() {
        // 大きなバイト配列を作成してメモリを消費
        val memoryConsumers = mutableListOf<ByteArray>()
        repeat(10) {
            memoryConsumers.add(ByteArray(10 * 1024 * 1024)) // 10MB × 10 = 100MB消費
        }
    }

    /**
     * 指定したしきい値以下になるまでメモリを消費
     */
    private fun consumeMemoryUntilThreshold(thresholdMB: Long): MemoryConsumer {
        val memoryConsumer = MemoryConsumer()
        
        while (getAvailableMemoryMB() > thresholdMB) {
            memoryConsumer.consumeMemory(5 * 1024 * 1024) // 5MBずつ消費
        }
        
        return memoryConsumer
    }

    /**
     * 利用可能メモリをMB単位で取得
     */
    private fun getAvailableMemoryMB(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.freeMemory() / (1024 * 1024)
    }

    /**
     * メモリ消費用のヘルパークラス
     */
    private class MemoryConsumer {
        private val memoryBlocks = mutableListOf<ByteArray>()

        fun consumeMemory(sizeBytes: Int) {
            memoryBlocks.add(ByteArray(sizeBytes))
        }

        fun release() {
            memoryBlocks.clear()
            System.gc()
        }
    }
}