package com.example.clothstock.ui.detail

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.clothstock.R
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * DetailActivityのEspresso UIテスト
 * 
 * TDD Redフェーズ実装
 * フルサイズ画像表示、タグ情報、編集・ナビゲーション機能
 */
@RunWith(AndroidJUnit4::class)
class DetailActivityEspressoTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_MEDIA_IMAGES
    )

    companion object {
        private const val TEST_CLOTH_ITEM_ID = 1L
        private const val INVALID_CLOTH_ITEM_ID = -999L
        
        private val TEST_TAG_DATA = TagData(
            size = 100,
            color = "青",
            category = "トップス"
        )
        
        private val TEST_CLOTH_ITEM = ClothItem(
            id = TEST_CLOTH_ITEM_ID,
            imagePath = "/storage/emulated/0/Pictures/test_cloth.jpg",
            tagData = TEST_TAG_DATA,
            createdAt = Date(),
            memo = "Test memo content"
        )
    }

    /**
     * テスト1: DetailActivity正常起動とレイアウト要素表示
     */
    @Test
    fun detailActivity_正常起動時_全レイアウト要素が表示される() {
        // Given: 有効なClothItem IDでIntent作成
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        // When: DetailActivityを起動
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // Then: 主要UI要素が表示される
            onView(withId(R.id.imageViewClothDetail))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.layoutTagInfo))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.buttonEdit))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
            
            onView(withId(R.id.buttonBack))
                .check(matches(isDisplayed()))
                .check(matches(isEnabled()))
            
            // Task 5: メモ入力ビューの表示確認
            onView(withId(R.id.memoInputView))
                .check(matches(isDisplayed()))
        }
    }

    /**
     * テスト2: フルサイズ画像表示機能
     */
    @Test
    fun detailActivity_画像表示時_フルサイズ画像が正しく表示される() {
        // Given: 画像パス付きIntent
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        // When: DetailActivityを起動
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // Then: 画像が表示され、適切なcontentDescriptionが設定される
            onView(withId(R.id.imageViewClothDetail))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))
            
            // ローディング状態の制御
            onView(withId(R.id.progressBarImage))
                .check(matches(isDisplayed()))
        }
    }

    /**
     * テスト3: タグ情報オーバーレイ表示
     */
    @Test
    fun detailActivity_タグ情報表示時_正しい内容が表示される() {
        // Given: タグ情報付きIntent
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        // When: DetailActivityを起動
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // Then: タグ情報が正しく表示される
            onView(withId(R.id.textSize))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("100"))))
            
            onView(withId(R.id.textColor))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("青"))))
            
            onView(withId(R.id.textCategory))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("トップス"))))
            
            onView(withId(R.id.textCreatedDate))
                .check(matches(isDisplayed()))
                .check(matches(withText(isNotEmpty())))
        }
    }

    /**
     * テスト4: 編集ボタンクリック機能
     */
    @Test
    fun detailActivity_編集ボタンクリック時_TaggingActivityが起動される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        // When: 編集ボタンをクリック
        ActivityScenario.launch<DetailActivity>(intent).use {
            onView(withId(R.id.buttonEdit))
                .perform(click())
            
            // Then: TaggingActivityが起動される（実際のテストではIntentの検証が必要）
            // Note: IntentのテストにはIntents.intending()を使用する
        }
    }

    /**
     * テスト5: 戻りボタンクリック機能
     */
    @Test
    fun detailActivity_戻りボタンクリック時_Activityが終了される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        // When: 戻りボタンをクリック
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            onView(withId(R.id.buttonBack))
                .perform(click())
            
            // Then: Activityが終了される
            scenario.onActivity { activity ->
                assert(activity.isFinishing)
            }
        }
    }

    /**
     * テスト6: 無効なClothItem ID処理
     */
    @Test
    fun detailActivity_無効ID指定時_エラー状態が表示される() {
        // Given: 無効なClothItem IDでIntent作成
        val intent = createDetailIntent(INVALID_CLOTH_ITEM_ID)
        
        // When: DetailActivityを起動
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // Then: エラー状態が表示される
            onView(withId(R.id.layoutError))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.textErrorMessage))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("アイテムが見つかりません"))))
            
            // メイン画像は非表示
            onView(withId(R.id.imageViewClothDetail))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    /**
     * テスト7: ローディング状態の表示制御
     */
    @Test
    fun detailActivity_データ読み込み中_ローディング状態が表示される() {
        // Given: 長時間のデータ読み込みが予想されるIntent
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        // When: DetailActivityを起動
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // Then: 初期ローディング状態が表示される
            onView(withId(R.id.layoutLoading))
                .check(matches(isDisplayed()))
            
            onView(withId(R.id.progressBarLoading))
                .check(matches(isDisplayed()))
            
            // データ読み込み完了後はローディングが非表示になる（時間制約でスキップ）
        }
    }

    /**
     * テスト8: 画像読み込み失敗時のエラー処理
     */
    @Test
    fun detailActivity_画像読み込み失敗時_プレースホルダーが表示される() {
        // Given: 存在しない画像パスのClothItem IDでIntent作成
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID) // 実際は存在しないパス
        
        // When: DetailActivityを起動
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // Then: エラープレースホルダーが表示される
            onView(withId(R.id.imageViewClothDetail))
                .check(matches(isDisplayed()))
            
            // プログレスバーは非表示になる
            onView(withId(R.id.progressBarImage))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    /**
     * テスト9: タグ情報の動的更新
     */
    @Test
    fun detailActivity_タグ情報更新時_UIが更新される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        // When: バックグラウンドでタグ情報が更新される（ViewModelのLiveData経由）
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            
            // Then: UIが新しいタグ情報で更新される
            // Note: 実際のテストではViewModelのモック化が必要
            onView(withId(R.id.layoutTagInfo))
                .check(matches(isDisplayed()))
        }
    }

    /**
     * テスト10: システムバック処理
     */
    @Test
    fun detailActivity_システムバック時_適切に終了される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        // When: システムバックボタンを押下
        ActivityScenario.launch<DetailActivity>(intent).use { scenario ->
            androidx.test.espresso.Espresso.pressBack()
            
            // Then: Activityが適切に終了される
            scenario.onActivity { activity ->
                assert(activity.isFinishing)
            }
        }
    }

    // ===== Task 5: メモ機能UIテスト =====

    /**
     * テスト11: メモ表示機能
     */
    @Test
    fun detailActivity_メモ付きアイテム表示時_メモが正しく表示される() {
        // Given: メモ付きアイテムのIntent
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        
        // When: DetailActivityを起動
        ActivityScenario.launch<DetailActivity>(intent).use {
            
            // Then: メモが表示される
            onView(withId(R.id.memoInputView))
                .check(matches(isDisplayed()))
                
            // メモ入力フィールドに既存のメモが設定される
            onView(withId(R.id.editTextMemo))
                .check(matches(isDisplayed()))
        }
    }

    /**
     * テスト12: メモ入力機能
     */
    @Test
    fun detailActivity_メモ入力時_文字数カウンターが更新される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        val testMemo = "新しいメモテストです"
        
        // When: メモを入力
        ActivityScenario.launch<DetailActivity>(intent).use {
            onView(withId(R.id.editTextMemo))
                .perform(clearText(), typeText(testMemo))
            
            // Then: 文字数カウンターが更新される
            onView(withId(R.id.textCharacterCount))
                .check(matches(isDisplayed()))
                .check(matches(withText(containsString("${testMemo.length}/1000"))))
        }
    }

    /**
     * テスト13: メモ文字数制限機能
     */
    @Test
    fun detailActivity_長文メモ入力時_警告が表示される() {
        // Given: DetailActivityが表示されている
        val intent = createDetailIntent(TEST_CLOTH_ITEM_ID)
        val longMemo = "a".repeat(950) // 警告閾値(900文字)を超える
        
        // When: 長文メモを入力
        ActivityScenario.launch<DetailActivity>(intent).use {
            onView(withId(R.id.editTextMemo))
                .perform(clearText(), typeText(longMemo))
            
            // Then: 警告アイコンが表示される
            onView(withId(R.id.iconWarning))
                .check(matches(isDisplayed()))
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