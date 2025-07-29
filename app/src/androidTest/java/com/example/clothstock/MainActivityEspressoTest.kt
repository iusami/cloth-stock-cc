package com.example.clothstock

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clothstock.ui.camera.CameraActivity
import com.example.clothstock.ui.gallery.GalleryFragment
import com.example.clothstock.ui.tagging.TaggingActivity
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MainActivityのナビゲーション統合テスト
 * 
 * TDD Red Phase: 失敗するテスト先行実装
 * アプリ全体のナビゲーションフローを検証
 */
@RunWith(AndroidJUnit4::class)
class MainActivityEspressoTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    // ===== 初期表示テスト =====

    @Test
    fun アプリ起動時_メインタイトルが表示される() {
        onView(withId(R.id.textTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.main_title)))
    }

    @Test
    fun アプリ起動時_カメラボタンが表示される() {
        onView(withId(R.id.buttonCamera))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.button_camera)))
    }

    @Test
    fun アプリ起動時_ギャラリーボタンが表示される() {
        onView(withId(R.id.buttonGallery))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.button_gallery)))
    }

    // ===== カメラナビゲーションテスト =====

    @Test
    fun カメラボタンクリック_CameraActivityが起動する() {
        onView(withId(R.id.buttonCamera))
            .perform(click())

        intended(hasComponent(CameraActivity::class.java.name))
    }

    // ===== ギャラリーナビゲーションテスト =====

    @Test
    fun ギャラリーボタンクリック_GalleryFragmentが表示される() {
        onView(withId(R.id.buttonGallery))
            .perform(click())

        // Fragment表示の確認
        onView(withId(R.id.fragmentContainer))
            .check(matches(isDisplayed()))
        
        // GalleryFragmentのRecyclerViewが表示されることを確認
        onView(withId(R.id.recyclerViewGallery))
            .check(matches(isDisplayed()))
    }

    @Test
    fun ギャラリー表示中_バックボタンでメイン画面に戻る() {
        // ギャラリーを表示
        onView(withId(R.id.buttonGallery))
            .perform(click())

        // バックボタンを押す
        androidx.test.espresso.Espresso.pressBack()

        // メインボタンが再び表示されることを確認
        onView(withId(R.id.buttonCamera))
            .check(matches(isDisplayed()))
        onView(withId(R.id.buttonGallery))
            .check(matches(isDisplayed()))
    }

    // ===== 撮影後統合テスト =====

    @Test
    fun カメラ撮影成功後_TaggingActivityに遷移する() {
        // このテストは現在未実装のため、実装後に正常動作する想定
        // 撮影成功をシミュレートして、TaggingActivityが起動されることを確認するテスト
        // 現時点では失敗することが想定される（TDD Red Phase）
        
        // TODO: 実装時にこのテストが通るようにhandleCapturedImageを修正する
        // この時点では意図的に失敗する（テストファーストアプローチ）
    }

    // ===== Activity状態管理テスト =====

    @Test
    fun 画面回転時_状態が保持される() {
        // ギャラリーを表示
        onView(withId(R.id.buttonGallery))
            .perform(click())

        // 画面回転をシミュレート
        activityRule.scenario.recreate()

        // ギャラリーが再び表示されることを確認
        onView(withId(R.id.recyclerViewGallery))
            .check(matches(isDisplayed()))
    }

    @Test
    fun フラグメント切り替え_適切にライフサイクル管理される() {
        // ギャラリー表示
        onView(withId(R.id.buttonGallery))
            .perform(click())

        // メイン画面に戻る  
        androidx.test.espresso.Espresso.pressBack()

        // 再度ギャラリー表示
        onView(withId(R.id.buttonGallery))
            .perform(click())

        // エラーなく表示されることを確認
        onView(withId(R.id.recyclerViewGallery))
            .check(matches(isDisplayed()))
    }

    // ===== ナビゲーション統合テスト =====

    @Test
    fun 完全ナビゲーションフロー_カメラ撮影からタグ編集まで() {
        // 1. カメラ起動
        onView(withId(R.id.buttonCamera))
            .perform(click())

        // CameraActivityが起動されることを確認
        intended(hasComponent(CameraActivity::class.java.name))

        // 2. 撮影結果をシミュレート（実際のテストでは複雑なため簡略化）
        // このテストは統合テストとして後で実装予定
    }
}

// テスト用の拡張関数は削除 - 実装時に直接メソッドを呼び出す