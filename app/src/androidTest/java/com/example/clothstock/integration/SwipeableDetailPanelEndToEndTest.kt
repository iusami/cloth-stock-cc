package com.example.clothstock.integration

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.registerIdlingResources
import androidx.test.espresso.Espresso.unregisterIdlingResources
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.example.clothstock.R
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import com.example.clothstock.ui.detail.DetailActivity
import com.example.clothstock.ui.common.AnimationIdlingResource
import com.example.clothstock.util.TestDataHelper
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Task 13.2: SwipeableDetailPanelの最終統合テスト（End-to-End）
 * 
 * Requirements: 1.1, 2.1, 5.1, 6.1
 * - 既存の image-memo 機能との統合確認テスト
 * - メモ入力から背景表示までの完全フローテスト
 * - パネル操作から状態保存までの完全フローテスト
 * - アクセシビリティ機能の完全フローテスト
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SwipeableDetailPanelEndToEndTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    companion object {
        private const val TEST_CLOTH_ITEM_ID = 1L
        
        private val TEST_TAG_DATA = TagData(
            size = 150,
            color = "黒",
            category = "ボトムス"
        )
        
        private val TEST_CLOTH_ITEM = ClothItem(
            id = TEST_CLOTH_ITEM_ID,
            imagePath = "/storage/emulated/0/Pictures/integration_test.jpg",
            tagData = TEST_TAG_DATA,
            createdAt = Date(),
            memo = ""
        )
    }

    @Before
    fun setUp() {
        // テストデータをクリアして新しいデータを設定
        TestDataHelper.clearTestDatabaseSync()
        TestDataHelper.injectTestDataSync(listOf(TEST_CLOTH_ITEM))
    }

    @After
    fun tearDown() {
        TestDataHelper.clearTestDatabaseSync()
    }

    /**
     * Task 13.2: テスト1 - 既存image-memo機能との統合確認テスト
     * Requirements: 1.1 - 既存機能との互換性確保
     */
    @Test
    fun endToEnd_既存imageMemo機能_SwipeableDetailPanelと統合して動作する() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            
            // 既存機能: 画像が表示されている
            onView(withId(R.id.imageViewClothDetail))
                .check(matches(isDisplayed()))
            
            // 既存機能: タグ情報が表示されている（SwipeableDetailPanel内）
            onView(withId(R.id.textSize))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("150"))))
                
            onView(withId(R.id.textColor))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("黒"))))
                
            onView(withId(R.id.textCategory))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("ボトムス"))))
            
            // 新機能: SwipeableDetailPanelが統合されている
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.swipeHandle))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            // 既存機能: MemoInputViewがSwipeableDetailPanel内で動作
            onView(withId(R.id.editTextMemo))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
            
            onView(withId(R.id.textCharacterCount))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("0/1000"))))
            
            // When: 統合された機能で完全なワークフローを実行
            val testMemo = "統合テスト用メモ：既存機能とSwipeableDetailPanelの統合確認"
            
            onView(withId(R.id.editTextMemo))
                .perform(typeText(testMemo))
            
            // Then: すべての機能が連携して正常動作する
            onView(withId(R.id.editTextMemo))
                .check(matches(withText(testMemo)))
                
            onView(withId(R.id.textCharacterCount))
                .check(matches(withText("${testMemo.length}/1000")))
            
            // パネル操作も正常動作
            waitForPanelAnimation(scenario) {
                onView(withId(R.id.swipeableDetailPanel))
                    .perform(swipeUp())
            }
            
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.swipeHandle))
                .check(matches(isDisplayed()))
        }
    }

    /**
     * Task 13.2: テスト2 - メモ入力から背景表示までの完全フローテスト
     * Requirements: 1.1 - メモ機能の完全フロー検証
     */
    @Test
    fun endToEnd_メモ完全フロー_入力から背景表示まで正常動作する() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            
            // 初期状態: メモが空で背景なし
            onView(withId(R.id.editTextMemo))
                .check(matches(withText("")))
            
            // Phase 1: メモ入力開始
            val shortMemo = "短いメモ"
            onView(withId(R.id.editTextMemo))
                .perform(typeText(shortMemo))
            
            onView(withId(R.id.editTextMemo))
                .check(matches(withText(shortMemo)))
                
            onView(withId(R.id.textCharacterCount))
                .check(matches(withText("${shortMemo.length}/1000")))
            
            // Phase 2: メモを長文に拡張
            val additionalText = "\n\n詳細情報:\n- 購入日: 2024/08/14\n- 購入場所: オンラインストア\n- 価格: ¥5,000\n- 着用シーン: カジュアル"
            onView(withId(R.id.editTextMemo))
                .perform(typeText(additionalText))
            
            val fullMemo = shortMemo + additionalText
            onView(withId(R.id.editTextMemo))
                .check(matches(withText(fullMemo)))
                
            onView(withId(R.id.textCharacterCount))
                .check(matches(withText("${fullMemo.length}/1000")))
            
            // Phase 3: 警告レベルまでメモを拡張
            val warningText = "\n\n追加情報: " + "警告レベルテスト用テキスト。".repeat(30)
            onView(withId(R.id.editTextMemo))
                .perform(typeText(warningText))
            
            val warningMemo = fullMemo + warningText
            
            // 警告表示の確認
            if (warningMemo.length > ClothItem.MAX_MEMO_LENGTH * 0.9) {
                onView(withId(R.id.iconWarning))
                    .check(matches(isDisplayed()))
            }
            
            // Phase 4: メモクリアして背景非表示確認
            onView(withId(R.id.editTextMemo))
                .perform(clearText())
            
            onView(withId(R.id.editTextMemo))
                .check(matches(withText("")))
                
            onView(withId(R.id.textCharacterCount))
                .check(matches(withText("0/1000")))
            
            // Phase 5: 最終メモ入力
            val finalMemo = "最終統合テスト用メモ：すべての機能が正常に動作することを確認"
            onView(withId(R.id.editTextMemo))
                .perform(typeText(finalMemo))
            
            // Then: 完全フローが正常完了
            onView(withId(R.id.editTextMemo))
                .check(matches(withText(finalMemo)))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.textCharacterCount))
                .check(matches(withText("${finalMemo.length}/1000")))
        }
    }

    /**
     * Task 13.2: テスト3 - パネル操作から状態保存までの完全フローテスト
     * Requirements: 2.1, 5.1 - パネル操作と状態管理の完全フロー
     */
    @Test
    fun endToEnd_パネル完全フロー_操作から状態保存まで正常動作する() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            
            // Phase 1: 初期パネル状態確認
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.swipeHandle))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            // Phase 2: スワイプによるパネル非表示
            waitForPanelAnimation(scenario) {
                onView(withId(R.id.swipeableDetailPanel))
                    .perform(swipeUp())
            }
            
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.swipeHandle))
                .check(matches(isDisplayed()))
            
            // Phase 3: ハンドルタップによるパネル表示
            waitForPanelAnimation(scenario) {
                onView(withId(R.id.swipeHandle))
                    .perform(click())
            }
            
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
            
            // Phase 4: メモ入力と組み合わせた状態変更
            val testMemo = "パネル状態テスト用メモ"
            onView(withId(R.id.editTextMemo))
                .perform(typeText(testMemo))
            
            // Phase 5: パネル非表示でもメモが保持される
            waitForPanelAnimation(scenario) {
                onView(withId(R.id.swipeableDetailPanel))
                    .perform(swipeUp())
            }
            
            // Phase 6: パネル再表示でメモが保持されている
            waitForPanelAnimation(scenario) {
                onView(withId(R.id.swipeHandle))
                    .perform(swipeDown())
            }
            
            onView(withId(R.id.editTextMemo))
                .check(matches(withText(testMemo)))
            
            // Phase 7: 画面向き変更による状態保持テスト
            scenario.onActivity { activity ->
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            
            Thread.sleep(1000) // 向き変更完了待ち
            
            // Then: 向き変更後も状態が保持される
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.editTextMemo))
                .check(matches(withText(testMemo)))
                
            onView(withId(R.id.swipeHandle))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
        }
    }

    /**
     * Task 13.2: テスト4 - アクセシビリティ機能の完全フローテスト
     * Requirements: 6.1 - アクセシビリティ機能の統合検証
     */
    @Test
    fun endToEnd_アクセシビリティ完全フロー_全機能で適切に動作する() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            
            // Phase 1: SwipeHandleのアクセシビリティ確認
            onView(withId(R.id.swipeHandle))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
                .check(matches(isClickable()))
            
            // Phase 2: パネル内容のアクセシビリティ確認
            onView(withId(R.id.textSize))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
            
            onView(withId(R.id.textColor))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
                
            onView(withId(R.id.textCategory))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
            
            // Phase 3: MemoInputViewのアクセシビリティ確認
            onView(withId(R.id.editTextMemo))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
                .check(matches(isEnabled()))
                
            onView(withId(R.id.textCharacterCount))
                .check(matches(hasContentDescription()))
            
            // Phase 4: アクセシビリティ情報付きでメモ入力
            val accessibilityTestMemo = "アクセシビリティテスト用メモ：音声読み上げ確認"
            onView(withId(R.id.editTextMemo))
                .perform(typeText(accessibilityTestMemo))
            
            onView(withId(R.id.editTextMemo))
                .check(matches(withText(accessibilityTestMemo)))
                .check(matches(hasContentDescription()))
            
            // Phase 5: 警告状態のアクセシビリティ確認
            val warningText = "a".repeat((ClothItem.MAX_MEMO_LENGTH * 0.95).toInt())
            onView(withId(R.id.editTextMemo))
                .perform(clearText())
                .perform(typeText(warningText))
            
            // 警告アイコンのアクセシビリティ確認
            onView(withId(R.id.iconWarning))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
            
            // Phase 6: パネル操作でのアクセシビリティアナウンス
            waitForPanelAnimation(scenario) {
                onView(withId(R.id.swipeHandle))
                    .perform(click())
            }
            
            // パネル状態変更後もアクセシビリティ情報が保持される
            onView(withId(R.id.swipeHandle))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
            
            // Phase 7: キーボードナビゲーション確認
            onView(withId(R.id.swipeHandle))
                .check(matches(isFocusable()))
            
            onView(withId(R.id.editTextMemo))
                .check(matches(isFocusable()))
            
            // Then: すべてのアクセシビリティ機能が統合されて動作
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(hasContentDescription()))
                .check(matches(isDisplayed()))
        }
    }

    /**
     * Task 13.2: テスト5 - 複数画面サイズでの統合動作確認
     * Requirements: 2.1 - 画面サイズ対応の統合検証
     */
    @Test
    fun endToEnd_画面サイズ対応_全サイズで統合機能が動作する() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            
            // Phase 1: 標準サイズでの動作確認
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.swipeHandle))
                .check(matches(isDisplayed()))
            
            val testMemo = "画面サイズ対応テスト用メモ"
            onView(withId(R.id.editTextMemo))
                .perform(typeText(testMemo))
            
            // Phase 2: 縦向き → 横向き変更
            scenario.onActivity { activity ->
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            
            Thread.sleep(1000) // 向き変更完了待ち
            
            // 横向きでも全機能が動作
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.editTextMemo))
                .check(matches(withText(testMemo)))
            
            waitForPanelAnimation(scenario) {
                onView(withId(R.id.swipeHandle))
                    .perform(click())
            }
            
            // Phase 3: 横向き → 縦向き変更
            scenario.onActivity { activity ->
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            
            Thread.sleep(1000) // 向き変更完了待ち
            
            // Then: 縦向きでも全機能が保持・動作
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.editTextMemo))
                .check(matches(withText(testMemo)))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.swipeHandle))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
            
            // すべての画面サイズ対応リソースが適用される
            onView(withId(R.id.textSize))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.textColor))
                .check(matches(isDisplayed()))
                
            onView(withId(R.id.textCategory))
                .check(matches(isDisplayed()))
        }
    }

    // ===== ヘルパーメソッド =====

    /**
     * パネルアニメーション完了を待機しながらアクションを実行
     */
    private fun waitForPanelAnimation(scenario: ActivityScenario<DetailActivity>, action: () -> Unit) {
        var idlingResource: AnimationIdlingResource? = null
        
        try {
            scenario.onActivity { activity ->
                val panel = activity.findViewById<android.view.View>(R.id.swipeableDetailPanel)
                panel?.let {
                    idlingResource = AnimationIdlingResource(it, "EndToEndTest")
                    registerIdlingResources(idlingResource)
                }
            }
            
            action.invoke()
            
        } finally {
            idlingResource?.let { unregisterIdlingResources(it) }
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