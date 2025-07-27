package com.example.clothstock.ui.tagging

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.clothstock.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TaggingActivityのUIテスト
 * 
 * TDD Redフェーズ - 失敗するテストを先行作成
 * タグ入力インターフェース、バリデーション、ナビゲーションをテスト
 */
@RunWith(AndroidJUnit4::class)
class TaggingActivityEspressoTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private lateinit var testImageUri: Uri

    @Before
    fun setUp() {
        // テスト用の画像URIを準備
        testImageUri = Uri.parse("android.resource://com.example.clothstock/${R.drawable.ic_launcher_foreground}")
    }

    // ===== 画像表示テスト =====

    @Test
    fun 画像表示_Intent経由でURIを受け取り表示される() {
        // Given: 画像URIを含むIntent
        val intent = Intent(ApplicationProvider.getApplicationContext(), TaggingActivity::class.java).apply {
            putExtra(TaggingActivity.EXTRA_IMAGE_URI, testImageUri.toString())
        }

        // When: アクティビティを起動
        ActivityScenario.launch<TaggingActivity>(intent).use {
            // Then: 画像が表示される
            onView(withId(R.id.imageViewCaptured))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun 画像表示_URIなしで起動した場合エラー処理される() {
        // Given: 画像URIなしのIntent
        val intent = Intent(ApplicationProvider.getApplicationContext(), TaggingActivity::class.java)

        // When: アクティビティを起動
        ActivityScenario.launch<TaggingActivity>(intent).use {
            // Then: エラーメッセージが表示される
            onView(withText("画像が見つかりません"))
                .check(matches(isDisplayed()))
        }
    }

    // ===== サイズピッカーテスト =====

    @Test
    fun サイズピッカー_60から160の範囲で設定される() {
        // Given: 正常なIntent
        val intent = createValidIntent()

        // When: アクティビティを起動
        ActivityScenario.launch<TaggingActivity>(intent).use {
            // Then: NumberPickerが表示され、範囲が正しく設定される
            onView(withId(R.id.numberPickerSize))
                .check(matches(isDisplayed()))
            
            // 最小値・最大値の確認は後で実装する
        }
    }

    @Test
    fun サイズピッカー_デフォルト値100が設定される() {
        // Given: 正常なIntent
        val intent = createValidIntent()

        // When: アクティビティを起動
        ActivityScenario.launch<TaggingActivity>(intent).use {
            // Then: デフォルト値100が設定される
            // NumberPickerの値確認は後で実装する
        }
    }

    // ===== 入力フィールドテスト =====

    @Test
    fun 色入力フィールド_テキスト入力が可能() {
        // Given: 正常なIntent
        val intent = createValidIntent()

        // When: アクティビティを起動して色を入力
        ActivityScenario.launch<TaggingActivity>(intent).use {
            onView(withId(R.id.editTextColor))
                .perform(typeText("赤"))
                .perform(closeSoftKeyboard())

            // Then: 入力されたテキストが表示される
            onView(withId(R.id.editTextColor))
                .check(matches(withText("赤")))
        }
    }

    @Test
    fun カテゴリ入力フィールド_テキスト入力が可能() {
        // Given: 正常なIntent
        val intent = createValidIntent()

        // When: アクティビティを起動してカテゴリを入力
        ActivityScenario.launch<TaggingActivity>(intent).use {
            onView(withId(R.id.editTextCategory))
                .perform(typeText("トップス"))
                .perform(closeSoftKeyboard())

            // Then: 入力されたテキストが表示される
            onView(withId(R.id.editTextCategory))
                .check(matches(withText("トップス")))
        }
    }

    // ===== バリデーションテスト =====

    @Test
    fun バリデーション_空の色でエラー表示される() {
        // Given: 正常なIntent
        val intent = createValidIntent()

        // When: 空の色で保存ボタンを押す
        ActivityScenario.launch<TaggingActivity>(intent).use {
            onView(withId(R.id.editTextColor))
                .perform(typeText(""))
                .perform(closeSoftKeyboard())
            
            onView(withId(R.id.buttonSave))
                .perform(click())

            // Then: バリデーションエラーが表示される
            onView(withId(R.id.textViewError))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("色を入力してください"))))
        }
    }

    @Test
    fun バリデーション_空のカテゴリでエラー表示される() {
        // Given: 正常なIntent
        val intent = createValidIntent()

        // When: 空のカテゴリで保存ボタンを押す
        ActivityScenario.launch<TaggingActivity>(intent).use {
            onView(withId(R.id.editTextColor))
                .perform(typeText("青"))
                .perform(closeSoftKeyboard())
            
            onView(withId(R.id.editTextCategory))
                .perform(typeText(""))
                .perform(closeSoftKeyboard())
                
            onView(withId(R.id.buttonSave))
                .perform(click())

            // Then: バリデーションエラーが表示される
            onView(withId(R.id.textViewError))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("カテゴリを選択してください"))))
        }
    }

    // ===== ボタンテスト =====

    @Test
    fun 保存ボタン_有効なデータで保存処理が実行される() {
        // Given: 正常なIntent
        val intent = createValidIntent()

        // When: 有効なデータを入力して保存ボタンを押す
        ActivityScenario.launch<TaggingActivity>(intent).use {
            onView(withId(R.id.editTextColor))
                .perform(typeText("緑"))
                .perform(closeSoftKeyboard())
            
            onView(withId(R.id.editTextCategory))
                .perform(typeText("ボトムス"))
                .perform(closeSoftKeyboard())
                
            onView(withId(R.id.buttonSave))
                .perform(click())

            // Then: 成功メッセージが表示される（または画面が終了される）
            // 具体的な動作は実装後に確認
        }
    }

    @Test
    fun キャンセルボタン_確認ダイアログが表示される() {
        // Given: 正常なIntent
        val intent = createValidIntent()

        // When: キャンセルボタンを押す
        ActivityScenario.launch<TaggingActivity>(intent).use {
            onView(withId(R.id.buttonCancel))
                .perform(click())

            // Then: 確認ダイアログが表示される
            onView(withText("変更を破棄しますか？"))
                .check(matches(isDisplayed()))
        }
    }

    // ===== ヘルパーメソッド =====

    private fun createValidIntent(): Intent {
        return Intent(ApplicationProvider.getApplicationContext(), TaggingActivity::class.java).apply {
            putExtra(TaggingActivity.EXTRA_IMAGE_URI, testImageUri.toString())
        }
    }
}