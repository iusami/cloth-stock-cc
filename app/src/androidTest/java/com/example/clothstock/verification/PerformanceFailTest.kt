package com.example.clothstock.verification

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.clothstock.MainActivity
import com.example.clothstock.R
import com.example.clothstock.util.TestDataHelper
import org.junit.*
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Task 15 RED Phase: パフォーマンス失敗テスト
 * 
 * 現実的なデータ量での性能問題を検証
 * - 大量データでの検索レスポンス時間
 * - フィルタリング処理のタイムアウト
 * - UI更新の遅延問題
 * - メモリリーク検出
 */
@RunWith(AndroidJUnit4::class)
class PerformanceFailTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    companion object {
        private const val LARGE_DATA_SIZE = 1000 // 現実的な大量データサイズ
        private const val SEARCH_TIMEOUT_MS = 2000L // 検索タイムアウト閾値
        private const val FILTER_TIMEOUT_MS = 3000L // フィルターアプリタイムアウト閾値
        private const val UI_UPDATE_TIMEOUT_MS = 1000L // UI更新タイムアウト閾値
    }

    @Before
    fun setUp() {
        TestDataHelper.clearTestDatabaseSync()
        // 大量データセットの準備
        val largeTestData = TestDataHelper.createMultipleTestItems(LARGE_DATA_SIZE)
        TestDataHelper.injectTestDataSync(largeTestData)
    }

    @After
    fun tearDown() {
        TestDataHelper.clearTestDatabaseSync()
    }

    // ===== RED Phase: パフォーマンス失敗テストケース =====

    @Test
    fun パフォーマンス失敗テスト_大量データ検索タイムアウト() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(2000) // データ読み込み待機
            
            // EXPECTED FAILURE: 大量データ検索でタイムアウト想定
            val searchTime = measureTimeMillis {
                onView(withId(R.id.searchView))
                    .perform(click())
                    .perform(typeText("テスト"))
                
                // 検索結果反映まで待機
                Thread.sleep(5000) // 長時間待機想定
            }
            
            // 検索レスポンスが閾値を超える想定
            assert(searchTime > SEARCH_TIMEOUT_MS) { 
                "大量データ(${LARGE_DATA_SIZE}件)での検索処理が${searchTime}msかかり、閾値${SEARCH_TIMEOUT_MS}msを超過" 
            }
        }
    }

    @Test
    fun パフォーマンス失敗テスト_複合フィルター処理タイムアウト() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(2000)
            
            // EXPECTED FAILURE: 複合フィルター処理でタイムアウト想定
            val filterTime = measureTimeMillis {
                // フィルターボトムシート表示
                onView(withId(R.id.buttonFilter))
                    .perform(click())
                
                Thread.sleep(1000)
                
                // 複数フィルター同時適用
                onView(withId(R.id.chipSize100))
                    .perform(click())
                    
                onView(withId(R.id.chipColorRed))
                    .perform(click())
                    
                onView(withId(R.id.chipCategoryTops))
                    .perform(click())
                
                // フィルター適用
                onView(withId(R.id.buttonApplyFilter))
                    .perform(click())
                
                // フィルター結果反映まで待機
                Thread.sleep(8000) // 長時間処理想定
            }
            
            // フィルター処理が閾値を超える想定
            assert(filterTime > FILTER_TIMEOUT_MS) { 
                "複合フィルター処理が${filterTime}msかかり、閾値${FILTER_TIMEOUT_MS}msを超過" 
            }
        }
    }

    @Test
    fun パフォーマンス失敗テスト_UI更新ラグ問題() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(2000)
            
            var uiUpdateCount = 0
            val maxUpdates = 20
            
            // EXPECTED FAILURE: 連続UI更新でパフォーマンス劣化想定
            val totalUpdateTime = measureTimeMillis {
                repeat(maxUpdates) { iteration ->
                    val singleUpdateTime = measureTimeMillis {
                        // 検索クエリの連続変更
                        onView(withId(R.id.searchView))
                            .perform(clearText())
                            .perform(typeText("test$iteration"))
                        
                        Thread.sleep(200) // UI更新デバウンシング待機
                    }
                    
                    if (singleUpdateTime > UI_UPDATE_TIMEOUT_MS) {
                        uiUpdateCount++
                    }
                }
            }
            
            // UI更新の遅延発生を想定
            assert(uiUpdateCount > maxUpdates / 4) { 
                "${maxUpdates}回中${uiUpdateCount}回のUI更新が閾値${UI_UPDATE_TIMEOUT_MS}msを超過" 
            }
        }
    }

    @Test
    fun パフォーマンス失敗テスト_メモリリーク問題() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(2000)
            
            // EXPECTED FAILURE: 大量操作後のメモリ増加想定
            val initialMemory = getCurrentMemoryUsage()
            
            repeat(50) { iteration ->
                // フィルター操作を繰り返し実行
                onView(withId(R.id.buttonFilter))
                    .perform(click())
                
                Thread.sleep(300)
                
                onView(withId(R.id.chipSize100))
                    .perform(click())
                
                onView(withId(R.id.buttonApplyFilter))
                    .perform(click())
                
                Thread.sleep(500)
                
                onView(withId(R.id.buttonFilter))
                    .perform(click())
                
                Thread.sleep(300)
                
                onView(withId(R.id.buttonClearFilter))
                    .perform(click())
                
                Thread.sleep(200)
            }
            
            val finalMemory = getCurrentMemoryUsage()
            val memoryIncreaseKB = (finalMemory - initialMemory) / 1024
            
            // 大幅なメモリ増加（メモリリーク）を想定
            assert(memoryIncreaseKB > 10240) { // 10MB以上の増加
                "50回の操作後、メモリ使用量が${memoryIncreaseKB}KB増加してメモリリークの可能性"
            }
        }
    }

    @Test
    fun パフォーマンス失敗テスト_データベースクエリ最適化不足() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(2000)
            
            val queryCount = 10
            val queryTimes = mutableListOf<Long>()
            
            repeat(queryCount) { iteration ->
                val queryTime = measureTimeMillis {
                    // 異なる検索クエリでデータベース負荷テスト
                    onView(withId(R.id.searchView))
                        .perform(clearText())
                        .perform(typeText("クエリ$iteration"))
                    
                    Thread.sleep(2000) // クエリ処理完了待機
                }
                
                queryTimes.add(queryTime)
            }
            
            val averageQueryTime = queryTimes.average()
            
            // EXPECTED FAILURE: データベースクエリが最適化されていない想定
            assert(averageQueryTime > 1500.0) { 
                "平均クエリ実行時間${averageQueryTime}msがパフォーマンス目標1500msを超過" 
            }
        }
    }

    @Test
    fun パフォーマンス失敗テスト_画像読み込みボトルネック() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(3000) // 大量データ読み込み待機
            
            // EXPECTED FAILURE: 大量画像読み込みでスクロール性能劣化想定
            val scrollTestStartTime = System.currentTimeMillis()
            
            repeat(5) { scrollIteration ->
                // RecyclerView高速スクロール
                onView(withId(R.id.recyclerViewGallery))
                    .perform(swipeUp())
                
                Thread.sleep(300) // スクロール処理待機
                
                onView(withId(R.id.recyclerViewGallery))
                    .perform(swipeDown())
                
                Thread.sleep(300)
            }
            
            val scrollTestDuration = System.currentTimeMillis() - scrollTestStartTime
            
            // スクロールパフォーマンスが想定より悪い
            assert(scrollTestDuration > 5000) { 
                "大量データ画面でのスクロールテストが${scrollTestDuration}msかかり、パフォーマンス劣化"
            }
        }
    }

    @Test
    fun パフォーマンス失敗テスト_コールドスタート時間超過() {
        // EXPECTED FAILURE: アプリ起動時間が長すぎる想定
        val startTime = System.currentTimeMillis()
        
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            // メイン UI 表示完了まで待機
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(isDisplayed()))
            
            val launchTime = System.currentTimeMillis() - startTime
            
            // 起動時間が2秒を超える想定
            assert(launchTime > 2000) { 
                "アプリ起動時間${launchTime}msがパフォーマンス目標2000msを超過" 
            }
        }
    }

    // ヘルパーメソッド
    private fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
}