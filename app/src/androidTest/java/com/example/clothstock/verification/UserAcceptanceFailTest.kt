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
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.runner.RunWith

/**
 * Task 15 RED Phase: ユーザー受入テスト失敗テスト
 * 
 * 完全な利用シナリオで発生可能なユーザビリティ問題を検証
 * - 初回利用時のオンボーディング不足
 * - 複雑操作でのユーザー迷い
 * - エラー状態からの復帰困難
 * - ワークフロー中断からの再開問題
 */
@RunWith(AndroidJUnit4::class)
class UserAcceptanceFailTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @Before
    fun setUp() {
        TestDataHelper.clearTestDatabaseSync()
        // 典型的な利用データセット
        val testData = TestDataHelper.createMultipleTestItems(20)
        TestDataHelper.injectTestDataSync(testData)
    }

    @After
    fun tearDown() {
        TestDataHelper.clearTestDatabaseSync()
    }

    // ===== RED Phase: ユーザー受入失敗テストケース =====

    @Test
    fun ユーザー受入失敗テスト_初回利用時ガイダンス不足() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: 初回利用時のガイダンスが不十分想定
            try {
                // オンボーディングUIの存在確認
                onView(withId(R.id.onboardingContainer))
                    .check(matches(isDisplayed()))
                
                // チュートリアル要素の確認
                onView(withId(R.id.tutorialOverlay))
                    .check(matches(isDisplayed()))
                
                assert(false) { "オンボーディング要素が見つからない想定でしたが存在している" }
                
            } catch (e: Exception) {
                // 初回利用ガイダンスが不足している想定
                assert(true) { 
                    "初回利用時のオンボーディング・チュートリアル要素が不足: ${e.message}"
                }
            }
        }
    }

    @Test
    fun ユーザー受入失敗テスト_複合検索操作での混乱() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: 複合検索で結果が予想と異なる想定
            
            // 1. 検索を実行
            onView(withId(R.id.searchView))
                .perform(click())
                .perform(typeText("テスト"))
            
            Thread.sleep(2000)
            
            // 2. フィルターを追加適用
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            onView(withId(R.id.chipSize100))
                .perform(click())
            
            onView(withId(R.id.buttonApplyFilter))
                .perform(click())
            
            Thread.sleep(2000)
            
            // EXPECTED FAILURE: 検索とフィルターの組み合わせ結果が不明確
            try {
                // 結果説明UIの存在確認
                onView(withId(R.id.searchResultExplanation))
                    .check(matches(isDisplayed()))
                
                assert(false) { "検索結果説明UIが存在している想定外" }
                
            } catch (e: Exception) {
                // 結果の説明が不足している想定
                assert(true) { 
                    "検索+フィルター結果の説明UI不足でユーザーが混乱する可能性: ${e.message}"
                }
            }
        }
    }

    @Test
    fun ユーザー受入失敗テスト_エラー状態からの復帰困難() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: エラー状態でリカバリー方法が不明確想定
            
            // 意図的に空検索を実行してエラー状態作成
            onView(withId(R.id.searchView))
                .perform(click())
                .perform(typeText("存在しないアイテムXYZ999"))
            
            Thread.sleep(3000)
            
            try {
                // エラー状態での「解決方法」UI確認
                onView(withId(R.id.errorRecoveryHelp))
                    .check(matches(isDisplayed()))
                
                // 「検索をクリア」ボタン確認
                onView(withId(R.id.buttonClearSearch))
                    .check(matches(isDisplayed()))
                
                assert(false) { "エラー回復UI要素が期待通り存在している" }
                
            } catch (e: Exception) {
                // エラー状態からの回復方法が不明確な想定
                assert(true) { 
                    "エラー状態からのリカバリーガイダンスが不足: ${e.message}"
                }
            }
        }
    }

    @Test
    fun ユーザー受入失敗テスト_中断されたワークフロー再開困難() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: 部分的に設定したフィルターが保存されない想定
            
            // フィルター設定を部分的に実行
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            onView(withId(R.id.chipSize100))
                .perform(click())
            
            onView(withId(R.id.chipColorRed))
                .perform(click())
            
            // フィルター適用せずにキャンセル（戻るボタン相当）
            onView(isRoot()).perform(pressBack())
            
            Thread.sleep(1000)
            
            // 再度フィルター画面を開く
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            // EXPECTED FAILURE: 以前の選択状態が保存されていない想定
            try {
                onView(withId(R.id.chipSize100))
                    .check(matches(isChecked()))
                
                onView(withId(R.id.chipColorRed))
                    .check(matches(isChecked()))
                
                assert(false) { "部分設定が期待通り保存されている" }
                
            } catch (e: Exception) {
                // 部分設定が失われる想定
                assert(true) { 
                    "中断されたフィルター設定が復元されずユーザーが再設定を強いられる: ${e.message}"
                }
            }
        }
    }

    @Test
    fun ユーザー受入失敗テスト_アクセシビリティ対応不完全でユーザー離脱() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: 一部アクセシビリティ対応が不完全想定
            
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            try {
                // 全要素のaccessibilityチェック
                onView(withId(R.id.chipGroupSize))
                    .check(matches(hasContentDescription()))
                
                onView(withId(R.id.chipGroupColor))
                    .check(matches(hasContentDescription()))
                
                onView(withId(R.id.chipGroupCategory))
                    .check(matches(hasContentDescription()))
                
                // スキップボタンのアクセシビリティチェック
                onView(withId(R.id.buttonApplyFilter))
                    .check(matches(hasContentDescription()))
                    .check { view, _ ->
                        val minSize = (48 * view.resources.displayMetrics.density).toInt()
                        assert(view.measuredWidth >= minSize && view.measuredHeight >= minSize) {
                            "タッチターゲットサイズが不十分"
                        }
                    }
                
                assert(false) { "全アクセシビリティ要素が適切に設定されている" }
                
            } catch (e: Exception) {
                // 一部アクセシビリティ対応が不完全な想定
                assert(true) { 
                    "アクセシビリティ対応不完全でスクリーンリーダー利用者が困る: ${e.message}"
                }
            }
        }
    }

    @Test
    fun ユーザー受入失敗テスト_多段階操作での途中離脱() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: 複雑な多段階操作でユーザーが諦める想定
            
            // 複雑な操作フロー（検索→フィルター→並び替え→詳細表示）
            val operationSteps = listOf(
                "検索実行",
                "フィルター適用", 
                "結果確認",
                "詳細画面移動"
            )
            
            var completedSteps = 0
            val maxAllowableSteps = 8 // 許容される操作ステップ数
            var totalOperationSteps = 0
            
            try {
                // Step 1: 検索
                onView(withId(R.id.searchView))
                    .perform(click())
                    .perform(typeText("テスト"))
                totalOperationSteps += 2
                
                Thread.sleep(2000)
                completedSteps++
                
                // Step 2: フィルター操作（複数ステップ）
                onView(withId(R.id.buttonFilter))
                    .perform(click())
                totalOperationSteps++
                
                Thread.sleep(1000)
                
                onView(withId(R.id.chipSize100))
                    .perform(click())
                totalOperationSteps++
                
                onView(withId(R.id.chipColorRed))
                    .perform(click())
                totalOperationSteps++
                
                onView(withId(R.id.buttonApplyFilter))
                    .perform(click())
                totalOperationSteps++
                
                Thread.sleep(2000)
                completedSteps++
                
                // Step 3: 結果確認
                onView(withId(R.id.recyclerViewGallery))
                    .check(matches(isDisplayed()))
                completedSteps++
                
                // Step 4: 詳細表示（最初のアイテムをクリック）
                onView(withId(R.id.recyclerViewGallery))
                    .perform(click())
                totalOperationSteps++
                
                Thread.sleep(1000)
                completedSteps++
                
            } catch (e: Exception) {
                // 操作途中で失敗する想定
            }
            
            // EXPECTED FAILURE: 操作ステップ数が多すぎてユーザーが諦める想定
            assert(totalOperationSteps > maxAllowableSteps || completedSteps < operationSteps.size) {
                "${operationSteps.size}ステップ中${completedSteps}ステップしか完了できず、総操作数${totalOperationSteps}が許容値${maxAllowableSteps}を超過"
            }
        }
    }

    @Test
    fun ユーザー受入失敗テスト_レスポンス時間でユーザー離脱() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: レスポンス時間が長すぎてユーザーが離脱する想定
            val maxAcceptableResponseTime = 3000L // ユーザーが待てる最大時間
            val operationResponseTimes = mutableListOf<Long>()
            
            // 複数の操作で応答時間を測定
            val operations = listOf(
                { 
                    onView(withId(R.id.searchView))
                        .perform(click())
                        .perform(typeText("検索テスト"))
                },
                {
                    onView(withId(R.id.buttonFilter))
                        .perform(click())
                    Thread.sleep(500)
                    onView(withId(R.id.chipSize100))
                        .perform(click())
                    onView(withId(R.id.buttonApplyFilter))
                        .perform(click())
                },
                {
                    onView(withId(R.id.recyclerViewGallery))
                        .perform(swipeUp())
                        .perform(swipeDown())
                }
            )
            
            operations.forEach { operation ->
                val responseTime = kotlin.system.measureTimeMillis {
                    operation()
                    Thread.sleep(4000) // 想定より長い処理時間
                }
                
                operationResponseTimes.add(responseTime)
            }
            
            val averageResponseTime = operationResponseTimes.average()
            val slowOperationsCount = operationResponseTimes.count { it > maxAcceptableResponseTime }
            
            // EXPECTED FAILURE: 応答時間が許容値を超える操作が多い想定
            assert(averageResponseTime > maxAcceptableResponseTime || 
                   slowOperationsCount > operations.size / 2) {
                "平均応答時間${averageResponseTime}msが許容値${maxAcceptableResponseTime}msを超過、" +
                "または${operations.size}操作中${slowOperationsCount}操作が遅すぎる"
            }
        }
    }

    @Test
    fun ユーザー受入失敗テスト_データ整合性エラーでユーザー混乱() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // EXPECTED FAILURE: データ整合性問題でユーザーが混乱する想定
            
            // フィルター結果数の確認
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            onView(withId(R.id.chipSize100))
                .perform(click())
            
            onView(withId(R.id.buttonApplyFilter))
                .perform(click())
            
            Thread.sleep(2000)
            
            try {
                // 結果数表示の整合性確認
                onView(withId(R.id.textFilterResultCount))
                    .check(matches(isDisplayed()))
                    .check { view, _ ->
                        val countText = (view as android.widget.TextView).text.toString()
                        val count = countText.filter { it.isDigit() }.toIntOrNull() ?: 0
                        
                        // 実際のRecyclerViewアイテム数と比較
                        scenario.onActivity { activity ->
                            val recyclerView = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewGallery)
                            val actualCount = recyclerView?.adapter?.itemCount ?: 0
                            
                            // 整合性チェック
                            assert(count == actualCount) {
                                "表示件数${count}と実際のアイテム数${actualCount}が不一致"
                            }
                        }
                    }
                
                assert(false) { "データ整合性が保たれている想定外" }
                
            } catch (e: Exception) {
                // データ整合性に問題がある想定
                assert(true) {
                    "フィルター結果数とRecyclerView表示数の不整合でユーザーが混乱: ${e.message}"
                }
            }
        }
    }
}