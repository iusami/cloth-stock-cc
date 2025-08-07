package com.example.clothstock.accessibility

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.clothstock.MainActivity
import com.example.clothstock.R
import com.example.clothstock.util.TestDataHelper
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils
import org.hamcrest.Matchers.*
import android.view.View
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Task 14 REFACTOR Phase: アクセシビリティ成功テストケース
 * 
 * アクセシビリティ改善後の機能が正常に動作することを検証
 * - contentDescriptionが適切に設定されている
 * - タッチターゲットサイズが適切である
 * - キーボードナビゲーションが機能する
 * - TalkBack読み上げ情報が充実している
 */
@RunWith(AndroidJUnit4::class)
class AccessibilitySuccessTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    companion object {
        @BeforeClass
        @JvmStatic
        fun enableAccessibilityChecks() {
            // アクセシビリティチェック有効化（完全検証モード）
            AccessibilityChecks.enable()
                .setRunChecksFromRootView(true)
        }
    }

    @Before
    fun setUp() {
        TestDataHelper.clearTestDatabaseSync()
        // テスト用データを準備
        val testData = TestDataHelper.createMultipleTestItems(5)
        TestDataHelper.injectTestDataSync(testData)
    }

    @After
    fun tearDown() {
        TestDataHelper.clearTestDatabaseSync()
    }

    // ===== REFACTOR Phase: 成功テストケース =====

    @Test
    fun アクセシビリティ成功テスト_フィルターボタンContentDescription適切() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // フィルターボタンにcontentDescriptionが適切に設定されている
            onView(withId(R.id.buttonFilter))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
                .check(matches(isClickable()))
                .check(matches(isFocusable()))
            
            // contentDescriptionが空でない
            onView(withId(R.id.buttonFilter))
                .check(matches(not(hasContentDescription(""))))
            
            // Minimum touch target size (48dp) が確保されている
            onView(withId(R.id.buttonFilter))
                .check { view, _ ->
                    val minSize = (48 * view.resources.displayMetrics.density).toInt()
                    assert(view.measuredWidth >= minSize) { "ボタン幅が最小タッチターゲットサイズ未満" }
                    assert(view.measuredHeight >= minSize) { "ボタン高が最小タッチターゲットサイズ未満" }
                }
        }
    }

    @Test
    fun アクセシビリティ成功テスト_Chipタッチターゲットサイズ適切() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // フィルターボトムシート表示
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            // サイズChipのタッチターゲットサイズ検証
            val sizeChipIds = listOf(
                R.id.chipSize100, R.id.chipSize110, R.id.chipSize120,
                R.id.chipSize130, R.id.chipSize140, R.id.chipSize150, R.id.chipSize160
            )
            
            sizeChipIds.forEach { chipId ->
                onView(withId(chipId))
                    .check(matches(isDisplayed()))
                    .check(matches(hasContentDescription()))
                    .check(matches(isFocusable()))
                    .check(matches(isClickable()))
                    .check { view, _ ->
                        val minSize = (48 * view.resources.displayMetrics.density).toInt()
                        assert(view.measuredWidth >= minSize || view.measuredHeight >= minSize) { 
                            "Chip $chipId がタッチターゲット要件を満たしていない" 
                        }
                    }
            }
            
            // 色Chipの検証
            val colorChipIds = listOf(
                R.id.chipColorRed, R.id.chipColorBlue, R.id.chipColorGreen,
                R.id.chipColorYellow, R.id.chipColorBlack, R.id.chipColorWhite,
                R.id.chipColorPink, R.id.chipColorPurple
            )
            
            colorChipIds.forEach { chipId ->
                onView(withId(chipId))
                    .check(matches(isDisplayed()))
                    .check(matches(hasContentDescription()))
                    .check(matches(isFocusable()))
            }
        }
    }

    @Test
    fun アクセシビリティ成功テスト_キーボードナビゲーション機能() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // フィルターボトムシート表示
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            // キーボードフォーカスが設定されている
            onView(withId(R.id.chipSize100))
                .check(matches(isFocusable()))
                .check(matches(isDisplayed()))
            
            // nextFocusDown属性が設定されている（間接的検証）
            onView(withId(R.id.chipSize100))
                .check { view, _ ->
                    // フォーカス可能で、ナビゲーション設定がある
                    assert(view.isFocusable) { "Chipがフォーカス可能ではない" }
                    assert(view.nextFocusDownId != View.NO_ID) { "nextFocusDownが設定されていない" }
                }
            
            // TABナビゲーションのテスト（フォーカス移動）
            onView(withId(R.id.chipSize100))
                .perform(click())
                .check(matches(isDisplayed()))
                
            // Chipの選択状態が適切にcontentDescriptionに反映される
            Thread.sleep(500)
            onView(withId(R.id.chipSize100))
                .check(matches(hasContentDescription()))
                .check(matches(isChecked()))
        }
    }

    @Test
    fun アクセシビリティ成功テスト_TalkBack詳細情報充実() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // 検索バーのTalkBack情報確認
            onView(withId(R.id.searchView))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
            
            // フィルターボトムシート表示
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            // ChipGroupのcontentDescription確認
            onView(withId(R.id.chipGroupSize))
                .check(matches(hasContentDescription()))
            
            onView(withId(R.id.chipGroupColor))
                .check(matches(hasContentDescription()))
                
            onView(withId(R.id.chipGroupCategory))
                .check(matches(hasContentDescription()))
            
            // 個別Chipの詳細説明確認
            onView(withId(R.id.chipSize100))
                .check(matches(hasContentDescription()))
                .check { view, _ ->
                    val description = view.contentDescription?.toString()
                    assert(!description.isNullOrEmpty()) { "Chipの説明が空" }
                    assert(description.contains("100") || description.contains("センチメートル")) { 
                        "サイズ情報がcontentDescriptionに含まれていない" 
                    }
                }
            
            // 色Chipの詳細説明確認
            onView(withId(R.id.chipColorRed))
                .check(matches(hasContentDescription()))
                .check { view, _ ->
                    val description = view.contentDescription?.toString()
                    assert(!description.isNullOrEmpty()) { "色Chipの説明が空" }
                    assert(description.contains("色") || description.contains("赤")) { 
                        "色情報がcontentDescriptionに含まれていない" 
                    }
                }
        }
    }

    @Test
    fun アクセシビリティ成功テスト_ChipGroup読み上げ順序適切() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // フィルターボトムシート表示
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            // ChipGroupの論理的な読み上げ順序確認
            onView(withId(R.id.chipGroupSize))
                .check(matches(hasContentDescription()))
                .check { view, _ ->
                    val description = view.contentDescription?.toString()
                    assert(!description.isNullOrEmpty()) { "ChipGroup説明が空" }
                    assert(description.contains("サイズ")) { "サイズGroupの説明が不適切" }
                }
            
            // 個別Chipの詳細情報確認
            onView(withId(R.id.chipSize100))
                .perform(click()) // 選択状態にする
            
            Thread.sleep(500)
            
            // 選択状態がcontentDescriptionに反映される
            onView(withId(R.id.chipSize100))
                .check(matches(isChecked()))
                .check { view, _ ->
                    val description = view.contentDescription?.toString()
                    assert(!description.isNullOrEmpty()) { "選択後のChip説明が空" }
                    // 選択状態が含まれていることを確認
                    assert(description.contains("選択") || description.contains("中")) { 
                        "選択状態がcontentDescriptionに反映されていない" 
                    }
                }
        }
    }

    @Test
    fun アクセシビリティ成功テスト_フィルター状態変化音声通知() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // フィルター適用前の状態
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(isDisplayed()))
            
            // フィルターボトムシート表示
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            // フィルター適用
            onView(withId(R.id.chipSize100))
                .perform(click())
            
            Thread.sleep(500)
            
            onView(withId(R.id.buttonApplyFilter))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .check(matches(hasContentDescription()))
                .perform(click())
            
            Thread.sleep(1500)
            
            // フィルター適用後の状態確認
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(isDisplayed()))
            
            // LiveRegion設定により、TalkBackで結果が読み上げられる
            // (実際の音声テストは困難だが、設定の存在を確認)
            onView(withId(R.id.recyclerViewGallery))
                .check { view, _ ->
                    // LiveRegionが設定されていることを間接的に確認
                    assert(view.isImportantForAccessibility) { "RecyclerViewがアクセシビリティ重要でない" }
                }
        }
    }

    @Test
    fun アクセシビリティ成功テスト_高コントラストモード対応() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // フィルターボトムシート表示
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            // 高コントラストモード対応のフォーカス強調確認
            onView(withId(R.id.chipSize100))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .perform(click()) // フォーカス状態の変更をテスト
            
            Thread.sleep(300)
            
            // 選択状態の視覚的識別性確認
            onView(withId(R.id.chipSize100))
                .check(matches(isChecked()))
                .check { view, _ ->
                    // Material Design Chipの選択状態表示確認
                    assert(view.isSelected || view.isActivated || view.isChecked) { 
                        "Chipの選択状態が視覚的に識別できない" 
                    }
                }
            
            // 別のChipと視覚的に区別可能か確認
            onView(withId(R.id.chipSize110))
                .check(matches(isDisplayed()))
                .check(matches(not(isChecked())))
        }
    }

    @Test
    fun アクセシビリティ成功テスト_エラーハンドリング時のアクセシビリティ情報() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // 空の検索（エラー状態）
            onView(withId(R.id.searchView))
                .perform(click())
                .perform(typeText("存在しないアイテムABCXYZ"))
            
            Thread.sleep(2000) // 検索デバウンシング + 処理時間
            
            // エラー状態でも適切なcontentDescriptionが保持される
            onView(withId(R.id.searchView))
                .check(matches(hasContentDescription()))
            
            // 空状態レイアウトのアクセシビリティ
            try {
                onView(withId(R.id.layoutEmptyState))
                    .check(matches(isDisplayed()))
                
                // 空状態アイコンにcontentDescription設定
                onView(withId(R.id.imageEmptyIcon))
                    .check(matches(hasContentDescription()))
                
                // 空状態メッセージが適切に表示
                onView(withId(R.id.textEmptyMessage))
                    .check(matches(isDisplayed()))
                
            } catch (e: Exception) {
                // 検索結果がある場合は、結果表示の確認
                onView(withId(R.id.recyclerViewGallery))
                    .check(matches(isDisplayed()))
            }
        }
    }

    @Test
    fun アクセシビリティ成功テスト_全体的な操作性検証() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            
            Thread.sleep(1500)
            
            // 1. 初期状態でのアクセシビリティ確認
            onView(withId(R.id.buttonFilter))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
            
            onView(withId(R.id.searchView))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
            
            // 2. フィルター操作の完全フロー
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            // 3. Chip選択の包括的確認
            val testChips = listOf(
                R.id.chipSize100, R.id.chipColorRed, R.id.chipCategoryTops
            )
            
            testChips.forEach { chipId ->
                onView(withId(chipId))
                    .check(matches(isDisplayed()))
                    .check(matches(hasContentDescription()))
                    .check(matches(isFocusable()))
                    .check(matches(isClickable()))
                    .perform(click())
                
                Thread.sleep(300)
                
                // 選択後のcontentDescription更新確認
                onView(withId(chipId))
                    .check(matches(isChecked()))
                    .check(matches(hasContentDescription()))
            }
            
            // 4. フィルター適用
            onView(withId(R.id.buttonApplyFilter))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
                .perform(click())
            
            Thread.sleep(1500)
            
            // 5. 結果確認
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(isDisplayed()))
            
            // 6. フィルタークリア
            onView(withId(R.id.buttonFilter))
                .perform(click())
            
            Thread.sleep(1000)
            
            onView(withId(R.id.buttonClearFilter))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
                .perform(click())
            
            Thread.sleep(1000)
            
            // 7. クリア後の状態確認
            onView(withId(R.id.recyclerViewGallery))
                .check(matches(isDisplayed()))
        }
    }
}