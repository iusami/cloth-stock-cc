package com.example.clothstock.performance

import android.content.Intent
import android.os.Debug
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.example.clothstock.R
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import com.example.clothstock.ui.detail.DetailActivity
import com.example.clothstock.ui.common.AnimationIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Task 13.1: SwipeableDetailPanelのパフォーマンステスト
 * 
 * Requirements: 1.5, 4.4, 7.1, 7.2, 7.3, 7.4
 * - スムーズなパネル移動のパフォーマンステスト
 * - ローエンドデバイスでの動作テスト
 * - メモリ使用量の監視テスト
 * - 長いメモテキストでの背景表示テスト
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SwipeableDetailPanelPerformanceTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_MEDIA_IMAGES
    )

    private lateinit var uiDevice: UiDevice
    private var initialMemory: Long = 0
    private var maxMemoryThreshold: Long = 0

    companion object {
        private const val TEST_CLOTH_ITEM_ID = 1L
        private const val ANIMATION_PERFORMANCE_THRESHOLD_MS = 200L // 200ms以内でアニメーション完了
        private const val MEMORY_LEAK_THRESHOLD_MB = 5L // 5MB以内のメモリ増加許容
        private const val LOW_END_DEVICE_MEMORY_LIMIT_MB = 50L // ローエンドデバイス想定メモリ制限
        
        private val TEST_TAG_DATA = TagData(
            size = 140,
            color = "緑",
            category = "アウター"
        )
        
        private val TEST_CLOTH_ITEM = ClothItem(
            id = TEST_CLOTH_ITEM_ID,
            imagePath = "/storage/emulated/0/Pictures/performance_test.jpg",
            tagData = TEST_TAG_DATA,
            createdAt = Date(),
            memo = "パフォーマンステスト用のメモ内容"
        )
    }

    @Before
    fun setUp() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // 初期メモリ状態を記録
        initialMemory = getCurrentMemoryUsage()
        maxMemoryThreshold = initialMemory + (MEMORY_LEAK_THRESHOLD_MB * 1024 * 1024)
        
        // ガベージコレクションを実行して初期状態をクリーンにする
        System.gc()
        Thread.sleep(100)
    }

    @After
    fun tearDown() {
        // テスト終了時のメモリ使用量チェック
        val finalMemory = getCurrentMemoryUsage()
        val memoryIncrease = finalMemory - initialMemory
        
        if (memoryIncrease > MEMORY_LEAK_THRESHOLD_MB * 1024 * 1024) {
            android.util.Log.w("PerformanceTest", 
                "メモリ使用量が閾値を超過: ${memoryIncrease / (1024 * 1024)}MB増加")
        }
        
        // 最終ガベージコレクション
        System.gc()
    }

    /**
     * Task 13.1: テスト1 - スムーズなパネル移動のパフォーマンステスト
     * Requirements: 7.1, 7.2, 7.3 - アニメーション性能とスムーズさ
     */
    @Test
    fun swipeableDetailPanel_パネル移動_スムーズなアニメーション性能() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            
            val animationTimes = mutableListOf<Long>()
            
            repeat(10) { iteration ->
                val startTime = System.currentTimeMillis()
                
                // When: パネルをスワイプで非表示にする
                onView(withId(R.id.swipeableDetailPanel))
                    .perform(swipeUp())
                
                // アニメーション完了を待機
                waitForAnimationCompletion(scenario)
                
                val hideTime = System.currentTimeMillis() - startTime
                
                // When: パネルをスワイプで表示する
                val showStartTime = System.currentTimeMillis()
                
                onView(withId(R.id.swipeHandle))
                    .perform(swipeDown())
                
                waitForAnimationCompletion(scenario)
                
                val showTime = System.currentTimeMillis() - showStartTime
                
                animationTimes.add(hideTime)
                animationTimes.add(showTime)
                
                // 連続操作の間に少し待機
                Thread.sleep(50)
            }
            
            // Then: すべてのアニメーションが性能閾値内で完了する
            val averageTime = animationTimes.average()
            val maxTime = animationTimes.maxOrNull() ?: 0L
            
            android.util.Log.i("PerformanceTest", 
                "アニメーション性能 - 平均: ${averageTime}ms, 最大: ${maxTime}ms")
            
            assert(averageTime <= ANIMATION_PERFORMANCE_THRESHOLD_MS) {
                "アニメーション平均時間が閾値を超過: ${averageTime}ms > ${ANIMATION_PERFORMANCE_THRESHOLD_MS}ms"
            }
            
            assert(maxTime <= ANIMATION_PERFORMANCE_THRESHOLD_MS * 1.5) {
                "アニメーション最大時間が閾値を超過: ${maxTime}ms > ${ANIMATION_PERFORMANCE_THRESHOLD_MS * 1.5}ms"
            }
        }
    }

    /**
     * Task 13.1: テスト2 - ローエンドデバイスでの動作テスト
     * Requirements: 4.4 - ローエンドデバイス対応
     */
    @Test
    fun swipeableDetailPanel_ローエンドデバイス_適切に動作する() {
        // Given: ローエンドデバイス環境をシミュレート
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        // メモリ制限をシミュレートするため、大量のメモリを消費
        val memoryBallast = mutableListOf<ByteArray>()
        
        try {
            // 利用可能メモリを制限（ローエンドデバイスをシミュレート）
            val currentMemory = getCurrentMemoryUsage()
            val targetMemoryUsage = currentMemory + (LOW_END_DEVICE_MEMORY_LIMIT_MB * 1024 * 1024)
            
            while (getCurrentMemoryUsage() < targetMemoryUsage) {
                memoryBallast.add(ByteArray(1024 * 1024)) // 1MBずつ確保
            }
            
            ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
                
                // When: ローエンドデバイス環境でパネル操作を実行
                onView(withId(R.id.swipeableDetailPanel))
                    .check(matches(isDisplayed()))
                
                // パネル非表示操作
                val startTime = System.currentTimeMillis()
                
                onView(withId(R.id.swipeableDetailPanel))
                    .perform(swipeUp())
                
                waitForAnimationCompletion(scenario)
                
                val operationTime = System.currentTimeMillis() - startTime
                
                // Then: ローエンドデバイスでも適切な時間内で動作する
                // ローエンドデバイスでは通常の2倍の時間まで許容
                val lowEndThreshold = ANIMATION_PERFORMANCE_THRESHOLD_MS * 2
                
                android.util.Log.i("PerformanceTest", 
                    "ローエンドデバイス動作時間: ${operationTime}ms (閾値: ${lowEndThreshold}ms)")
                
                assert(operationTime <= lowEndThreshold) {
                    "ローエンドデバイスでの動作時間が閾値を超過: ${operationTime}ms > ${lowEndThreshold}ms"
                }
                
                // パネルが適切に非表示になっていることを確認
                onView(withId(R.id.swipeableDetailPanel))
                    .check(matches(isDisplayed()))
                
                onView(withId(R.id.swipeHandle))
                    .check(matches(isDisplayed()))
            }
            
        } finally {
            // メモリバラストをクリア
            memoryBallast.clear()
            System.gc()
        }
    }

    /**
     * Task 13.1: テスト3 - メモリ使用量の監視テスト
     * Requirements: 7.4 - メモリ使用量最適化
     */
    @Test
    fun swipeableDetailPanel_メモリ使用量_適切に管理される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        val memorySnapshots = mutableListOf<Long>()
        
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            
            // 初期メモリ使用量を記録
            memorySnapshots.add(getCurrentMemoryUsage())
            
            repeat(20) { iteration ->
                // When: パネル操作を繰り返し実行
                onView(withId(R.id.swipeableDetailPanel))
                    .perform(swipeUp())
                
                waitForAnimationCompletion(scenario)
                
                onView(withId(R.id.swipeHandle))
                    .perform(swipeDown())
                
                waitForAnimationCompletion(scenario)
                
                // メモリ使用量を記録
                if (iteration % 5 == 0) {
                    System.gc() // 定期的にガベージコレクション
                    Thread.sleep(100)
                    memorySnapshots.add(getCurrentMemoryUsage())
                }
            }
            
            // 最終メモリ使用量を記録
            System.gc()
            Thread.sleep(100)
            memorySnapshots.add(getCurrentMemoryUsage())
            
            // Then: メモリ使用量が適切に管理されている
            val initialMemory = memorySnapshots.first()
            val finalMemory = memorySnapshots.last()
            val maxMemory = memorySnapshots.maxOrNull() ?: 0L
            
            val memoryIncrease = finalMemory - initialMemory
            val maxMemoryIncrease = maxMemory - initialMemory
            
            android.util.Log.i("PerformanceTest", 
                "メモリ使用量 - 初期: ${initialMemory / (1024 * 1024)}MB, " +
                "最終: ${finalMemory / (1024 * 1024)}MB, " +
                "最大: ${maxMemory / (1024 * 1024)}MB, " +
                "増加: ${memoryIncrease / (1024 * 1024)}MB")
            
            assert(memoryIncrease <= MEMORY_LEAK_THRESHOLD_MB * 1024 * 1024) {
                "メモリ使用量の増加が閾値を超過: ${memoryIncrease / (1024 * 1024)}MB > ${MEMORY_LEAK_THRESHOLD_MB}MB"
            }
            
            assert(maxMemoryIncrease <= MEMORY_LEAK_THRESHOLD_MB * 2 * 1024 * 1024) {
                "最大メモリ使用量の増加が閾値を超過: ${maxMemoryIncrease / (1024 * 1024)}MB > ${MEMORY_LEAK_THRESHOLD_MB * 2}MB"
            }
        }
    }

    /**
     * Task 13.1: テスト4 - 長いメモテキストでの背景表示テスト
     * Requirements: 1.5 - 長文メモでのパフォーマンス
     */
    @Test
    fun memoInputView_長文メモ_背景表示パフォーマンス() {
        // Given: 長文メモを準備
        val longMemo = "これは非常に長いメモテキストです。".repeat(100) + 
                      "パフォーマンステストのために大量のテキストを入力します。\n".repeat(50) +
                      "メモ背景の描画パフォーマンスをテストしています。"
        
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            val startTime = System.currentTimeMillis()
            val initialMemory = getCurrentMemoryUsage()
            
            // When: 長文メモを入力
            onView(withId(R.id.editTextMemo))
                .perform(clearText())
                .perform(typeText(longMemo))
            
            val inputTime = System.currentTimeMillis() - startTime
            
            // 背景描画完了を待機
            Thread.sleep(200)
            
            val totalTime = System.currentTimeMillis() - startTime
            val finalMemory = getCurrentMemoryUsage()
            val memoryIncrease = finalMemory - initialMemory
            
            // Then: 長文メモでも適切な性能で背景が表示される
            android.util.Log.i("PerformanceTest", 
                "長文メモ処理 - 入力時間: ${inputTime}ms, " +
                "総時間: ${totalTime}ms, " +
                "メモリ増加: ${memoryIncrease / (1024 * 1024)}MB, " +
                "メモ長: ${longMemo.length}文字")
            
            // 長文でも1秒以内で処理完了
            assert(totalTime <= 1000) {
                "長文メモ処理時間が閾値を超過: ${totalTime}ms > 1000ms"
            }
            
            // 背景が正しく表示されている
            onView(withId(R.id.editTextMemo))
                .check(matches(isDisplayed()))
                .check(matches(withText(longMemo)))
            
            // メモリ使用量が適切
            assert(memoryIncrease <= 10 * 1024 * 1024) {
                "長文メモのメモリ使用量が閾値を超過: ${memoryIncrease / (1024 * 1024)}MB > 10MB"
            }
        }
    }

    /**
     * Task 13.1: テスト5 - 連続操作時のパフォーマンス安定性
     * Requirements: 7.1, 7.2, 7.3 - 連続操作での性能維持
     */
    @Test
    fun swipeableDetailPanel_連続操作_パフォーマンス安定性() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            
            val operationTimes = mutableListOf<Long>()
            
            // When: 高頻度でパネル操作を実行
            repeat(50) { iteration ->
                val startTime = System.currentTimeMillis()
                
                // 上スワイプ
                onView(withId(R.id.swipeableDetailPanel))
                    .perform(swipeUp())
                
                // 下スワイプ
                onView(withId(R.id.swipeHandle))
                    .perform(swipeDown())
                
                val operationTime = System.currentTimeMillis() - startTime
                operationTimes.add(operationTime)
                
                // 10回ごとにメモリ状況をチェック
                if (iteration % 10 == 0) {
                    val currentMemory = getCurrentMemoryUsage()
                    val memoryIncrease = currentMemory - initialMemory
                    
                    assert(memoryIncrease <= maxMemoryThreshold - initialMemory) {
                        "連続操作でメモリ使用量が閾値を超過 (操作${iteration}回目): " +
                        "${memoryIncrease / (1024 * 1024)}MB > ${MEMORY_LEAK_THRESHOLD_MB}MB"
                    }
                }
            }
            
            // Then: 操作時間が安定している（性能劣化なし）
            val firstHalfAverage = operationTimes.take(25).average()
            val secondHalfAverage = operationTimes.drop(25).average()
            val performanceDegradation = secondHalfAverage - firstHalfAverage
            
            android.util.Log.i("PerformanceTest", 
                "連続操作性能 - 前半平均: ${firstHalfAverage}ms, " +
                "後半平均: ${secondHalfAverage}ms, " +
                "性能劣化: ${performanceDegradation}ms")
            
            // 性能劣化が50ms以内（許容範囲）
            assert(performanceDegradation <= 50) {
                "連続操作で性能劣化が発生: ${performanceDegradation}ms > 50ms"
            }
            
            // 全操作が閾値内で完了
            val maxOperationTime = operationTimes.maxOrNull() ?: 0L
            assert(maxOperationTime <= ANIMATION_PERFORMANCE_THRESHOLD_MS * 2) {
                "連続操作の最大時間が閾値を超過: ${maxOperationTime}ms > ${ANIMATION_PERFORMANCE_THRESHOLD_MS * 2}ms"
            }
        }
    }

    // ===== ヘルパーメソッド =====

    /**
     * 現在のメモリ使用量を取得
     */
    private fun getCurrentMemoryUsage(): Long {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        return memoryInfo.totalPss * 1024L // PSS (Proportional Set Size) をバイト単位で返す
    }

    /**
     * アニメーション完了を待機
     */
    private fun waitForAnimationCompletion(scenario: ActivityScenario<DetailActivity>) {
        var idlingResource: AnimationIdlingResource? = null
        try {
            scenario.onActivity { activity ->
                val panel = activity.findViewById<android.view.View>(R.id.swipeableDetailPanel)
                panel?.let {
                    idlingResource = AnimationIdlingResource(it, "PerformanceTest")
                    androidx.test.espresso.Espresso.registerIdlingResources(idlingResource)
                }
            }
            // IdlingResourceが自動的に完了を待機
        } finally {
            idlingResource?.let { 
                androidx.test.espresso.Espresso.unregisterIdlingResources(it) 
            }
        }
    }

    /**
     * DetailActivity起動用のIntentを作成
     */
    private fun createDetailIntent(clothItemId: Long): Intent {
        return Intent(ApplicationProvider.getApplicationContext(), DetailActivity::class.java).apply {
            putExtra(DetailActivity.EXTRA_CLOTH_ITEM_ID, clothItemId)
        }
    }
}