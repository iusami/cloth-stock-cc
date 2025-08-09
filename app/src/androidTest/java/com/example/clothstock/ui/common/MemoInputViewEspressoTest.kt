package com.example.clothstock.ui.common

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.clothstock.MainActivity
import com.example.clothstock.R
import com.example.clothstock.data.model.ClothItem
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MemoInputViewのEspressoテスト
 * 
 * UI要素の動作とユーザーインタラクションをテスト
 * 
 * テスト対象:
 * - メモ入力機能
 * - 文字数カウント表示
 * - 文字数制限の視覚的フィードバック
 * - アクセシビリティ対応
 */
@RunWith(AndroidJUnit4::class)
class MemoInputViewEspressoTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var context: Context
    private lateinit var memoInputView: MemoInputView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        
        // テスト用のMemoInputViewを作成
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            memoInputView = MemoInputView(context)
        }
    }

    // ===== メモ入力機能のテスト =====

    @Test
    fun testMemoInputBasicFunctionality() {
        activityScenarioRule.scenario.onActivity { activity ->
            // MemoInputViewをActivityに追加
            val rootView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            rootView.addView(memoInputView)
        }

        val testMemo = "テストメモ入力"

        // メモを入力
        onView(withId(R.id.editTextMemo))
            .perform(typeText(testMemo))

        // 入力されたテキストが正しく表示されることを確認
        onView(withId(R.id.editTextMemo))
            .check(matches(withText(testMemo)))
        
        // 文字数カウントが正しく表示されることを確認
        onView(withId(R.id.textCharacterCount))
            .check(matches(withText("${testMemo.length}/${ClothItem.MAX_MEMO_LENGTH}")))
    }

    @Test
    fun testMemoInputMultiline() {
        activityScenarioRule.scenario.onActivity { activity ->
            val rootView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            rootView.addView(memoInputView)
        }

        val multilineMemo = "行1\n行2\n行3"

        // 複数行のメモを入力
        onView(withId(R.id.editTextMemo))
            .perform(typeText(multilineMemo))

        // 複数行テキストが正しく表示されることを確認
        onView(withId(R.id.editTextMemo))
            .check(matches(withText(multilineMemo)))
        
        // 文字数カウントが正しく計算されることを確認
        onView(withId(R.id.textCharacterCount))
            .check(matches(withText("${multilineMemo.length}/${ClothItem.MAX_MEMO_LENGTH}")))
    }

    @Test
    fun testMemoInputClear() {
        activityScenarioRule.scenario.onActivity { activity ->
            val rootView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            rootView.addView(memoInputView)
        }

        // メモを入力してからクリア
        onView(withId(R.id.editTextMemo))
            .perform(typeText("クリアテスト"))
            .perform(clearText())

        // テキストがクリアされることを確認
        onView(withId(R.id.editTextMemo))
            .check(matches(withText("")))
        
        // 文字数カウントが0になることを確認
        onView(withId(R.id.textCharacterCount))
            .check(matches(withText("0/${ClothItem.MAX_MEMO_LENGTH}")))
    }

    // ===== 文字数制限機能のテスト =====

    @Test
    fun testCharacterLimit() {
        activityScenarioRule.scenario.onActivity { activity ->
            val rootView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            rootView.addView(memoInputView)
        }

        // 制限を超える長いテキストを入力
        val longText = "a".repeat(ClothItem.MAX_MEMO_LENGTH + 100)
        val expectedText = "a".repeat(ClothItem.MAX_MEMO_LENGTH)

        onView(withId(R.id.editTextMemo))
            .perform(typeText(longText))

        // 制限文字数でトリミングされることを確認
        onView(withId(R.id.editTextMemo))
            .check(matches(withText(expectedText)))
        
        // 文字数カウントが最大値を表示することを確認
        onView(withId(R.id.textCharacterCount))
            .check(matches(withText("${ClothItem.MAX_MEMO_LENGTH}/${ClothItem.MAX_MEMO_LENGTH}")))
    }

    @Test
    fun testCharacterLimitExactly() {
        activityScenarioRule.scenario.onActivity { activity ->
            val rootView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            rootView.addView(memoInputView)
        }

        // 制限文字数ちょうどのテキストを入力
        val exactLimitText = "b".repeat(ClothItem.MAX_MEMO_LENGTH)

        onView(withId(R.id.editTextMemo))
            .perform(typeText(exactLimitText))

        // 全文が入力されることを確認
        onView(withId(R.id.editTextMemo))
            .check(matches(withText(exactLimitText)))
        
        // 文字数カウントが最大値を表示することを確認
        onView(withId(R.id.textCharacterCount))
            .check(matches(withText("${ClothItem.MAX_MEMO_LENGTH}/${ClothItem.MAX_MEMO_LENGTH}")))
    }

    // ===== 視覚的フィードバック機能のテスト =====

    @Test
    fun testWarningStateDisplay() {
        activityScenarioRule.scenario.onActivity { activity ->
            val rootView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            rootView.addView(memoInputView)
        }

        // 警告状態になる文字数（90%以上）のテキストを入力
        val warningThreshold = (ClothItem.MAX_MEMO_LENGTH * 0.9).toInt()
        val warningText = "c".repeat(warningThreshold + 10)

        onView(withId(R.id.editTextMemo))
            .perform(typeText(warningText))

        // 警告アイコンが表示されることを確認
        onView(withId(R.id.iconWarning))
            .check(matches(isDisplayed()))

        // 文字数カウンターが警告色で表示されることを確認
        onView(withId(R.id.textCharacterCount))
            .check(matches(hasWarningTextColor()))
    }

    @Test
    fun testNormalStateDisplay() {
        activityScenarioRule.scenario.onActivity { activity ->
            val rootView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            rootView.addView(memoInputView)
        }

        // 通常範囲内のテキストを入力
        val normalText = "通常のメモテキスト"

        onView(withId(R.id.editTextMemo))
            .perform(typeText(normalText))

        // 警告アイコンが表示されないことを確認
        onView(withId(R.id.iconWarning))
            .check(matches(not(isDisplayed())))

        // 文字数カウンターが通常色で表示されることを確認
        onView(withId(R.id.textCharacterCount))
            .check(matches(not(hasWarningTextColor())))
    }

    @Test
    fun testWarningStateTransition() {
        activityScenarioRule.scenario.onActivity { activity ->
            val rootView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            rootView.addView(memoInputView)
        }

        // 通常状態 → 警告状態への遷移
        val normalText = "通常テキスト"
        val warningThreshold = (ClothItem.MAX_MEMO_LENGTH * 0.9).toInt()
        val warningText = "d".repeat(warningThreshold + 50)

        // まず通常テキストを入力
        onView(withId(R.id.editTextMemo))
            .perform(typeText(normalText))

        // 警告アイコンが表示されないことを確認
        onView(withId(R.id.iconWarning))
            .check(matches(not(isDisplayed())))

        // テキストをクリアして警告レベルのテキストを入力
        onView(withId(R.id.editTextMemo))
            .perform(clearText())
            .perform(typeText(warningText))

        // 警告アイコンが表示されることを確認
        onView(withId(R.id.iconWarning))
            .check(matches(isDisplayed()))

        // 文字数カウンターが警告色になることを確認
        onView(withId(R.id.textCharacterCount))
            .check(matches(hasWarningTextColor()))
    }

    // ===== アクセシビリティのテスト =====

    @Test
    fun testContentDescriptions() {
        activityScenarioRule.scenario.onActivity { activity ->
            val rootView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            rootView.addView(memoInputView)
        }

        // EditTextのcontentDescriptionを確認
        onView(withId(R.id.editTextMemo))
            .check(matches(hasContentDescription()))

        // 文字数カウンターのcontentDescriptionを確認
        onView(withId(R.id.textCharacterCount))
            .check(matches(hasContentDescription()))

        // 警告アイコンのcontentDescriptionを確認（警告状態にしてから）
        val warningText = "e".repeat((ClothItem.MAX_MEMO_LENGTH * 0.95).toInt())
        onView(withId(R.id.editTextMemo))
            .perform(typeText(warningText))

        onView(withId(R.id.iconWarning))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun testFocusability() {
        activityScenarioRule.scenario.onActivity { activity ->
            val rootView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
            rootView.addView(memoInputView)
        }

        // EditTextがフォーカス可能であることを確認
        onView(withId(R.id.editTextMemo))
            .check(matches(isFocusable()))

        // フォーカスを設定できることを確認
        onView(withId(R.id.editTextMemo))
            .perform(click())
            .check(matches(hasFocus()))
    }

    // ===== カスタムマッチャー =====

    /**
     * 警告色のテキストカラーを持つかどうかを判定するマッチャー
     */
    private fun hasWarningTextColor(): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("has warning text color")
            }

            override fun matchesSafely(item: View?): Boolean {
                if (item !is android.widget.TextView) return false
                
                val currentColor = item.currentTextColor
                val warningColor = androidx.core.content.ContextCompat.getColor(
                    item.context, 
                    android.R.color.holo_red_light
                )
                
                return currentColor == warningColor
            }
        }
    }

    /**
     * ContentDescriptionを持つかどうかを判定するマッチャー
     */
    private fun hasContentDescription(): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description?) {
                description?.appendText("has content description")
            }

            override fun matchesSafely(item: View?): Boolean {
                return item?.contentDescription != null && 
                       item.contentDescription.toString().isNotBlank()
            }
        }
    }
}