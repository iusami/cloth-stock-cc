package com.example.clothstock.ui.common

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.clothstock.util.ImageCompressionManager
import com.example.clothstock.util.CompressionStrategy
import com.example.clothstock.util.DeviceMemoryAnalyzer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 画像圧縮機能のテストクラス（TDD RED段階）
 * 
 * 動的圧縮アルゴリズムと画質保持機能の検証
 * このテストは初期状態では失敗することを想定している（RED段階）
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageCompressionTest {

    private lateinit var context: Context
    private lateinit var imageCompressionManager: ImageCompressionManager
    private lateinit var deviceMemoryAnalyzer: DeviceMemoryAnalyzer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // 未実装のクラスのため、初期化は失敗する想定
        imageCompressionManager = ImageCompressionManager.getInstance(context)
        deviceMemoryAnalyzer = DeviceMemoryAnalyzer.getInstance(context)
    }

    @Test
    fun `デバイスメモリに基づく動的圧縮率が適切に計算されること`() {
        // Arrange: 異なるメモリ容量のデバイスを模擬
        val testCases = listOf(
            DeviceMemoryTestCase(totalMemoryGB = 2, expectedCompressionRatio = 0.3f), // 低メモリデバイス
            DeviceMemoryTestCase(totalMemoryGB = 4, expectedCompressionRatio = 0.5f), // 標準デバイス
            DeviceMemoryTestCase(totalMemoryGB = 8, expectedCompressionRatio = 0.7f), // 高メモリデバイス
            DeviceMemoryTestCase(totalMemoryGB = 12, expectedCompressionRatio = 0.8f) // ハイエンドデバイス
        )

        testCases.forEach { testCase ->
            // Act: デバイスメモリを設定して圧縮率を計算
            deviceMemoryAnalyzer.simulateDeviceMemory(testCase.totalMemoryGB)
            val actualRatio = imageCompressionManager.calculateDynamicCompressionRatio()

            // Assert: 期待される圧縮率と一致すること
            assertEquals(
                testCase.expectedCompressionRatio,
                actualRatio,
                0.05f, // 許容誤差5%
                "${testCase.totalMemoryGB}GBデバイスでの圧縮率が期待値と異なります"
            )
        }
    }

    @Test
    fun `用途別圧縮戦略が正しく適用されること`() {
        // Arrange: 異なる用途での圧縮設定をテスト
        val originalBitmap = createTestBitmap(1024, 1024)
        val compressionStrategies = listOf(
            CompressionStrategy.THUMBNAIL, // サムネイル用（高圧縮）
            CompressionStrategy.GALLERY_DISPLAY, // ギャラリー表示用（中圧縮）
            CompressionStrategy.DETAIL_VIEW, // 詳細表示用（低圧縮）
            CompressionStrategy.EDITING // 編集用（圧縮最小）
        )

        val results = mutableListOf<ImageCompressionManager.CompressionResult>()

        compressionStrategies.forEach { strategy ->
            // Act: 各戦略で圧縮を実行
            val result = imageCompressionManager.compressWithStrategy(originalBitmap, strategy)
            results.add(result)

            // Assert: 結果が有効であること
            assertNotNull(result.bitmap, "${strategy}戦略での圧縮結果がnullです")
            assertTrue(
                result.compressionRatio > 0f && result.compressionRatio <= 1f,
                "${strategy}戦略での圧縮率が無効です: ${result.compressionRatio}"
            )
        }

        // Assert: 用途に応じた圧縮率の順序が正しいこと
        // THUMBNAIL < GALLERY_DISPLAY < DETAIL_VIEW < EDITING の順で圧縮率が低くなる
        assertTrue(
            results[0].compressionRatio < results[1].compressionRatio,
            "サムネイル用の圧縮率がギャラリー表示用より低くありません"
        )
        assertTrue(
            results[1].compressionRatio < results[2].compressionRatio,
            "ギャラリー表示用の圧縮率が詳細表示用より低くありません"
        )
        assertTrue(
            results[2].compressionRatio < results[3].compressionRatio,
            "詳細表示用の圧縮率が編集用より低くありません"
        )

        // クリーンアップ
        originalBitmap.recycle()
        results.forEach { it.bitmap?.recycle() }
    }

    @Test
    fun `JPEG品質の動的調整が適切に動作すること`() {
        // Arrange: 異なるファイルサイズ目標での品質調整
        val originalBitmap = createTestBitmap(2048, 2048)
        val targetSizes = listOf(
            100 * 1024, // 100KB目標
            500 * 1024, // 500KB目標
            1024 * 1024 // 1MB目標
        )

        targetSizes.forEach { targetSize ->
            // Act: 目標サイズに合わせてJPEG品質を動的調整
            val result = imageCompressionManager.compressToTargetSize(originalBitmap, targetSize)

            // Assert: 目標サイズ付近に収まっていること（±20%の許容範囲）
            val actualSize = result.fileSizeBytes
            val lowerBound = (targetSize * 0.8).toInt()
            val upperBound = (targetSize * 1.2).toInt()

            assertTrue(
                actualSize in lowerBound..upperBound,
                "目標サイズ${targetSize}Bに対して実際のサイズ${actualSize}Bが範囲外です"
            )

            // JPEG品質が有効な範囲内であること
            assertTrue(
                result.jpegQuality in 10..100,
                "JPEG品質が無効です: ${result.jpegQuality}"
            )
        }

        originalBitmap.recycle()
    }

    @Test
    fun `圧縮後の画質が許容範囲内であること`() {
        // Arrange: 高品質なテスト画像を準備
        val originalBitmap = createDetailedTestBitmap(1024, 1024)
        val compressionRatios = listOf(0.3f, 0.5f, 0.7f, 0.9f)

        compressionRatios.forEach { ratio ->
            // Act: 指定された圧縮率で圧縮
            val result = imageCompressionManager.compressWithQualityRatio(originalBitmap, ratio)

            // Assert: 画質評価が許容範囲内であること
            val qualityScore = calculateImageQualityScore(originalBitmap, result.bitmap)
            val expectedMinQuality = when {
                ratio >= 0.8f -> 0.9f // 高圧縮率では高品質を維持
                ratio >= 0.5f -> 0.7f // 中圧縮率では中品質を維持
                else -> 0.5f // 低圧縮率でも最低限の品質を維持
            }

            assertTrue(
                qualityScore >= expectedMinQuality,
                "圧縮率${ratio}での画質スコア${qualityScore}が期待値${expectedMinQuality}を下回りました"
            )

            result.bitmap?.recycle()
        }

        originalBitmap.recycle()
    }

    @Test
    fun `バックグラウンド圧縮処理が正常に動作すること`() {
        // Arrange: 複数の画像を準備
        val bitmaps = (1..5).map { createTestBitmap(800, 600) }
        val compressionJobs = mutableListOf<ImageCompressionManager.CompressionJob>()

        // Act: バックグラウンドで並列圧縮を開始
        bitmaps.forEachIndexed { index, bitmap ->
            val job = imageCompressionManager.compressInBackground(
                bitmap = bitmap,
                strategy = CompressionStrategy.GALLERY_DISPLAY,
                callback = object : ImageCompressionManager.CompressionCallback {
                    override fun onCompressionComplete(result: ImageCompressionManager.CompressionResult) {
                        // 結果の検証は後で実行
                    }
                    
                    override fun onCompressionError(error: Exception) {
                        throw AssertionError("圧縮処理${index}でエラーが発生: ${error.message}")
                    }
                }
            )
            compressionJobs.add(job)
        }

        // すべてのジョブの完了を待つ
        val allCompleted = imageCompressionManager.waitForAllJobs(compressionJobs, timeoutMs = 5000)

        // Assert: すべての圧縮が正常に完了していること
        assertTrue(allCompleted, "バックグラウンド圧縮の完了がタイムアウトしました")

        compressionJobs.forEach { job ->
            assertTrue(job.isCompleted(), "圧縮ジョブが完了していません: ${job.getId()}")
            assertNotNull(job.getResult(), "圧縮結果がnullです: ${job.getId()}")
        }

        // クリーンアップ
        bitmaps.forEach { it.recycle() }
        compressionJobs.forEach { it.getResult()?.bitmap?.recycle() }
    }

    @Test
    fun `圧縮処理のキャンセルが正常に動作すること`() {
        // Arrange: 大きな画像で長時間の圧縮処理を準備
        val largeBitmap = createTestBitmap(4096, 4096)
        
        // Act: 圧縮処理を開始して即座にキャンセル
        val job = imageCompressionManager.compressInBackground(
            bitmap = largeBitmap,
            strategy = CompressionStrategy.DETAIL_VIEW,
            callback = object : ImageCompressionManager.CompressionCallback {
                override fun onCompressionComplete(result: ImageCompressionManager.CompressionResult) {
                    throw AssertionError("キャンセルされたはずの処理が完了しました")
                }
                
                override fun onCompressionError(error: Exception) {
                    // キャンセルエラーは期待される
                }
            }
        )

        // 処理開始から少し待ってキャンセル
        Thread.sleep(100)
        val cancelled = job.cancel()

        // Assert: キャンセルが正常に実行されていること
        assertTrue(cancelled, "圧縮処理のキャンセルに失敗しました")
        assertTrue(job.isCancelled(), "ジョブのキャンセル状態が正しく設定されていません")

        // クリーンアップ
        largeBitmap.recycle()
    }

    // ===== ヘルパーメソッド =====

    /**
     * テスト用のビットマップを作成
     */
    private fun createTestBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * 詳細なテスト用ビットマップを作成（画質評価用）
     */
    private fun createDetailedTestBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()

        // 複雑なパターンを描画して画質評価を可能にする
        for (i in 0 until width step 20) {
            for (j in 0 until height step 20) {
                paint.color = android.graphics.Color.rgb(i % 256, j % 256, (i + j) % 256)
                canvas.drawRect(i.toFloat(), j.toFloat(), (i + 20).toFloat(), (j + 20).toFloat(), paint)
            }
        }

        return bitmap
    }

    /**
     * 画質スコアを計算（簡易実装）
     */
    private fun calculateImageQualityScore(original: Bitmap, compressed: Bitmap?): Float {
        if (compressed == null) return 0f
        
        // 簡易的なPSNR計算の模擬
        // 実際の実装では適切な画質評価アルゴリズムを使用
        val sizeDifference = kotlin.math.abs(original.byteCount - compressed.byteCount).toFloat()
        val originalSize = original.byteCount.toFloat()
        
        return kotlin.math.max(0f, 1f - (sizeDifference / originalSize))
    }

    /**
     * デバイスメモリテストケース用データクラス
     */
    private data class DeviceMemoryTestCase(
        val totalMemoryGB: Int,
        val expectedCompressionRatio: Float
    )

}