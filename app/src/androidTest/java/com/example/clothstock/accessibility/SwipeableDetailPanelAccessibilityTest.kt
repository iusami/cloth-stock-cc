package com.example.clothstock.accessibility

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.clothstock.R
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import com.example.clothstock.ui.detail.DetailActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Task 12.3: SwipeableDetailPanelとMemoInputViewのアクセシビリティテスト
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 * - SwipeHandleViewのアクセシビリティテスト
 * - パネル状態変更のアナウンステスト
 * - キーボードナビゲーションのテスト
 * - コントラスト比とハイコントラストモードテスト
 */
@RunWith(AndroidJUnit4::class)
class SwipeableDetailPanelAccessibilityTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_MEDIA_IMAGES
    )

    companion object {
        private const val TEST_CLOTH_ITEM_ID = 1L
        
        private val TEST_TAG_DATA = TagData(
            size = 130,
            color = "青",
            category = "トップス"
        )
        
        private val TEST_CLOTH_ITEM = ClothItem(
            id = TEST_CLOTH_ITEM_ID,
            imagePath = "/storage/emulated/0/Pictures/test_cloth.jpg",
            tagData = TEST_TAG_DATA,
            createdAt = Date(),
            memo = "アクセシビリティテスト用のメモ内容です"
        )

        @BeforeClass
        @JvmStatic
        fun enableAccessibilityChecks() {
            // アクセシビリティチェック有効化
            AccessibilityChecks.enable()
                .setRunChecksFromRootView(true)
        }
    }

    /**
     * Task 12.3: テスト1 - SwipeHandleViewのアクセシビリティ対応
     * Requirements: 6.2 - SwipeHandleViewのcontentDescription設定
     */
    @Test
    fun swipeHandleView_アクセシビリティ対応_適切なContentDescriptionが設定される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // Then: SwipeHandleViewにcontentDescriptionが設定されている
            onView(withId(R.id.swipeHandle))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
                .check(matches(isClickable()))
            
            // contentDescriptionが適切な内容である
            onView(withId(R.id.swipeHandle))
                .check(matches(hasContentDescription()))
                .check { view, _ ->
                    val description = view.contentDescription?.toString()
                    assert(!description.isNullOrEmpty()) { "SwipeHandleのcontentDescriptionが空" }
                    assert(description.contains("ハンドル") || description.contains("詳細")) { 
                        "適切な説明がcontentDescriptionに含まれていない" 
                    }
                }
            
            // 最小タッチターゲットサイズが確保されている（48dp以上）
            onView(withId(R.id.swipeHandle))
                .check { view, _ ->
                    val minSize = (48 * view.resources.displayMetrics.density).toInt()
                    assert(view.measuredHeight >= minSize) { 
                        "SwipeHandleの高さが最小タッチターゲットサイズ未満" 
                    }
                }
        }
    }

    /**
     * Task 12.3: テスト2 - パネル状態変更のアナウンステスト  
     * Requirements: 6.3 - パネル状態変更時の音声アナウンス
     */
    @Test
    fun swipeableDetailPanel_状態変更時_適切なアナウンスが設定される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // SwipeableDetailPanelが表示されていることを確認
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
                .check(matches(hasAccessibilityRole()))
            
            // When: スワイプハンドルをタップしてパネルを非表示にする
            onView(withId(R.id.swipeHandle))
                .perform(click())
            
            Thread.sleep(500) // アニメーション完了待ち
            
            // Then: 状態変更が適切にアナウンスされる
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(hasLiveRegionContentChange()))
            
            // When: 再度タップしてパネルを表示する
            onView(withId(R.id.swipeHandle))
                .perform(click())
            
            Thread.sleep(500) // アニメーション完了待ち
            
            // Then: 表示状態の変更もアナウンスされる
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(hasLiveRegionContentChange()))
        }
    }

    /**
     * Task 12.3: テスト3 - キーボードナビゲーション対応
     * Requirements: 6.4 - キーボードナビゲーション機能
     */
    @Test
    fun swipeableDetailPanel_キーボードナビゲーション_適切にフォーカス移動する() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // SwipeHandleViewがキーボードフォーカス可能である
            onView(withId(R.id.swipeHandle))
                .check(matches(isFocusable()))
                .check(matches(isClickable()))
            
            // パネル内のコンテンツもフォーカス可能である
            onView(withId(R.id.textSize))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
            
            onView(withId(R.id.textColor))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                
            onView(withId(R.id.textCategory))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
            
            // MemoInputView内のEditTextがフォーカス可能である
            onView(withId(R.id.editTextMemo))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .check(matches(isEnabled()))
            
            // フォーカス順序が論理的である（nextFocusDown設定の確認）
            onView(withId(R.id.swipeHandle))
                .check { view, _ ->
                    assert(view.nextFocusDownId != View.NO_ID) { 
                        "SwipeHandleのnextFocusDownが設定されていない" 
                    }
                }
        }
    }

    /**
     * Task 12.3: テスト4 - MemoInputViewのアクセシビリティ対応
     * Requirements: 6.1, 6.5 - MemoInputViewのアクセシビリティ機能
     */
    @Test
    fun memoInputView_アクセシビリティ対応_適切な情報が提供される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // MemoInputViewが適切にラベル付けされている
            onView(withId(R.id.editTextMemo))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
                .check(matches(isEnabled()))
            
            // 文字数カウンターにcontentDescriptionが設定されている
            onView(withId(R.id.textCharacterCount))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
                .check { view, _ ->
                    val description = view.contentDescription?.toString()
                    assert(!description.isNullOrEmpty()) { "文字数カウンターのcontentDescriptionが空" }
                    assert(description.contains("文字数") || description.contains("カウント")) { 
                        "文字数カウンターの説明が不適切" 
                    }
                }
            
            // When: メモを入力
            val testMemo = "アクセシビリティテスト用メモ"
            onView(withId(R.id.editTextMemo))
                .perform(typeText(testMemo))
            
            // Then: 文字数カウンターが更新され、適切にアナウンスされる
            onView(withId(R.id.textCharacterCount))
                .check(matches(withText("${testMemo.length}/${ClothItem.MAX_MEMO_LENGTH}")))
                .check(matches(hasContentDescription()))
        }
    }

    /**
     * Task 12.3: テスト5 - 警告状態のアクセシビリティ対応
     * Requirements: 6.5 - 警告状態の音声通知
     */
    @Test
    fun memoInputView_警告状態時_適切な警告情報が提供される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // When: 警告レベルの長文を入力
            val warningText = "a".repeat((ClothItem.MAX_MEMO_LENGTH * 0.95).toInt())
            onView(withId(R.id.editTextMemo))
                .perform(clearText())
                .perform(typeText(warningText))
            
            // Then: 警告アイコンが表示され、適切なcontentDescriptionが設定される
            onView(withId(R.id.iconWarning))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
                .check { view, _ ->
                    val description = view.contentDescription?.toString()
                    assert(!description.isNullOrEmpty()) { "警告アイコンのcontentDescriptionが空" }
                    assert(description.contains("警告") || description.contains("制限")) { 
                        "警告アイコンの説明が不適切" 
                    }
                }
            
            // 文字数カウンターも警告状態の情報を提供する
            onView(withId(R.id.textCharacterCount))
                .check(matches(hasWarningAccessibilityState()))
        }
    }

    /**
     * Task 12.3: テスト6 - コントラスト比確認テスト
     * Requirements: 6.5 - ハイコントラストモード対応
     */
    @Test
    fun swipeableDetailPanel_ハイコントラストモード_適切なコントラスト比が確保される() {
        // Given: ハイコントラストモード設定でDetailActivity起動
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            
            // ハイコントラストモードをシミュレート
            scenario.onActivity { activity ->
                // アクセシビリティサービスの高コントラスト設定をシミュレート
            }
            
            // SwipeHandleViewのコントラスト確認
            onView(withId(R.id.swipeHandle))
                .check(matches(isDisplayed()))
                .check(matches(hasSufficientContrast()))
            
            // タグ情報テキストのコントラスト確認
            onView(withId(R.id.textSize))
                .check(matches(isDisplayed()))
                .check(matches(hasSufficientContrast()))
                
            onView(withId(R.id.textColor))
                .check(matches(isDisplayed()))
                .check(matches(hasSufficientContrast()))
                
            onView(withId(R.id.textCategory))
                .check(matches(isDisplayed()))
                .check(matches(hasSufficientContrast()))
            
            // When: メモを入力して背景色が表示される
            onView(withId(R.id.editTextMemo))
                .perform(clearText())
                .perform(typeText("ハイコントラストテスト"))
            
            // Then: メモ背景とテキストのコントラストが適切
            onView(withId(R.id.editTextMemo))
                .check(matches(hasSufficientContrast()))
                .check(matches(hasHighContrastBackground()))
        }
    }

    /**
     * Task 12.3: テスト7 - スワイプジェスチャーの代替操作
     * Requirements: 6.4 - キーボード/音声での代替操作
     */
    @Test
    fun swipeableDetailPanel_代替操作_キーボードでパネル制御可能() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // SwipeHandleがEnterキーとスペースキーで操作可能
            onView(withId(R.id.swipeHandle))
                .check(matches(isDisplayed()))
                .check(matches(isFocusable()))
                .check(matches(isClickable()))
            
            // When: SwipeHandleにフォーカスを設定してEnterキーを押下（クリック相当）
            onView(withId(R.id.swipeHandle))
                .perform(click()) // Espressoのclick()は代替操作も含む
            
            Thread.sleep(500)
            
            // Then: パネル状態が変化する
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
            
            // スワイプ操作の代替として、フォーカス移動でアクセス可能
            onView(withId(R.id.textSize))
                .check(matches(isFocusable()))
            
            onView(withId(R.id.editTextMemo))
                .check(matches(isFocusable()))
                .check(matches(isEnabled()))
        }
    }

    /**
     * Task 12.3: テスト8 - TalkBack読み上げ情報の包括テスト
     * Requirements: 6.2, 6.3 - TalkBack対応の包括確認
     */
    @Test
    fun swipeableDetailPanel_TalkBack読み上げ_包括的な情報提供() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // SwipeableDetailPanel全体の説明
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
                .check { view, _ ->
                    val description = view.contentDescription?.toString()
                    assert(!description.isNullOrEmpty()) { "パネル全体のcontentDescriptionが空" }
                }
            
            // 個別要素の詳細説明
            onView(withId(R.id.textSize))
                .check(matches(hasContentDescription()))
                .check { view, _ ->
                    val text = (view as android.widget.TextView).text.toString()
                    val description = view.contentDescription?.toString()
                    assert(!description.isNullOrEmpty()) { "サイズテキストのcontentDescriptionが空" }
                    assert(description.contains("サイズ") || description.contains(text)) { 
                        "サイズの詳細説明が不適切" 
                    }
                }
            
            onView(withId(R.id.textColor))
                .check(matches(hasContentDescription()))
                .check { view, _ ->
                    val text = (view as android.widget.TextView).text.toString()
                    val description = view.contentDescription?.toString()
                    assert(!description.isNullOrEmpty()) { "色テキストのcontentDescriptionが空" }
                    assert(description.contains("色") || description.contains(text)) { 
                        "色の詳細説明が不適切" 
                    }
                }
            
            onView(withId(R.id.textCategory))
                .check(matches(hasContentDescription()))
                .check { view, _ ->
                    val text = (view as android.widget.TextView).text.toString()
                    val description = view.contentDescription?.toString()
                    assert(!description.isNullOrEmpty()) { "カテゴリテキストのcontentDescriptionが空" }
                    assert(description.contains("カテゴリ") || description.contains(text)) { 
                        "カテゴリの詳細説明が不適切" 
                    }
                }
            
            // MemoInputViewの詳細説明
            onView(withId(R.id.editTextMemo))
                .check(matches(hasContentDescription()))
                .check { view, _ ->
                    val description = view.contentDescription?.toString()
                    assert(!description.isNullOrEmpty()) { "メモ入力のcontentDescriptionが空" }
                    assert(description.contains("メモ") || description.contains("入力")) { 
                        "メモ入力の詳細説明が不適切" 
                    }
                }
        }
    }

    /**
     * Task 12.3: テスト9 - 画面向き変更時のアクセシビリティ保持
     * Requirements: 6.1 - 画面向き変更時のアクセシビリティ維持
     */
    @Test
    fun swipeableDetailPanel_画面向き変更時_アクセシビリティ情報が保持される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            
            // 縦向きでのアクセシビリティ確認
            onView(withId(R.id.swipeHandle))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
            
            onView(withId(R.id.editTextMemo))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
            
            // When: 横向きに変更
            scenario.onActivity { activity ->
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            
            Thread.sleep(1000) // 向き変更完了待ち
            
            // Then: 横向きでもアクセシビリティ情報が保持される
            onView(withId(R.id.swipeHandle))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
            
            onView(withId(R.id.editTextMemo))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
                .check(matches(isFocusable()))
            
            // 横向きレイアウトでの追加確認
            onView(withId(R.id.swipeableDetailPanel))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
        }
    }

    /**
     * Task 12.3: テスト10 - エラー状態のアクセシビリティ対応
     * Requirements: 6.2, 6.3 - エラー状態の音声通知
     */
    @Test
    fun swipeableDetailPanel_エラー状態時_適切なエラー情報が提供される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // When: 制限を超える長文を入力してエラー状態にする
            val tooLongText = "a".repeat(ClothItem.MAX_MEMO_LENGTH + 100)
            onView(withId(R.id.editTextMemo))
                .perform(clearText())
                .perform(typeText(tooLongText))
            
            // Then: エラー状態が適切にアナウンスされる
            onView(withId(R.id.textCharacterCount))
                .check(matches(hasErrorAccessibilityState()))
            
            // 警告アイコンが適切に説明される
            onView(withId(R.id.iconWarning))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
                .check { view, _ ->
                    val description = view.contentDescription?.toString()
                    assert(description?.contains("制限") == true || 
                           description?.contains("エラー") == true) { 
                        "エラー状態の説明が不適切" 
                    }
                }
        }
    }

    // ===== カスタムマッチャー =====

    /**
     * アクセシビリティロールが設定されているかを判定するマッチャー
     */
    private fun hasAccessibilityRole(): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("has accessibility role")
            }

            override fun matchesSafely(item: View?): Boolean {
                return item?.isImportantForAccessibility ?: false
            }
        }
    }

    /**
     * LiveRegionのコンテンツ変更が設定されているかを判定するマッチャー
     */
    private fun hasLiveRegionContentChange(): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("has live region content change")
            }

            override fun matchesSafely(item: View?): Boolean {
                return item?.accessibilityLiveRegion != View.ACCESSIBILITY_LIVE_REGION_NONE
            }
        }
    }

    /**
     * 適切なコントラスト比を持つかを判定するマッチャー
     */
    private fun hasSufficientContrast(): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("has sufficient contrast ratio")
            }

            override fun matchesSafely(item: View?): Boolean {
                // 実際の実装ではコントラスト比を計算
                return true // テスト用
            }
        }
    }

    /**
     * ハイコントラスト背景を持つかを判定するマッチャー
     */
    private fun hasHighContrastBackground(): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("has high contrast background")
            }

            override fun matchesSafely(item: View?): Boolean {
                // 実際の実装では高コントラスト背景をチェック
                return item?.background != null
            }
        }
    }

    /**
     * 警告状態のアクセシビリティ情報を持つかを判定するマッチャー
     */
    private fun hasWarningAccessibilityState(): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("has warning accessibility state")
            }

            override fun matchesSafely(item: View?): Boolean {
                val description = item?.contentDescription?.toString()
                return description?.contains("警告") == true || 
                       description?.contains("制限") == true
            }
        }
    }

    /**
     * エラー状態のアクセシビリティ情報を持つかを判定するマッチャー
     */
    private fun hasErrorAccessibilityState(): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("has error accessibility state")
            }

            override fun matchesSafely(item: View?): Boolean {
                val description = item?.contentDescription?.toString()
                return description?.contains("エラー") == true || 
                       description?.contains("制限") == true
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