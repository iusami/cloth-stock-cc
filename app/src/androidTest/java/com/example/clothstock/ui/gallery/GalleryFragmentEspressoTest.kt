package com.example.clothstock.ui.gallery

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.clothstock.R
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GalleryFragmentのUIテスト
 * 
 * TDD Redフェーズ - 失敗するテストを先行作成
 * ギャラリー表示、RecyclerView、ナビゲーションをテスト
 */
@RunWith(AndroidJUnit4::class)
class GalleryFragmentEspressoTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_MEDIA_IMAGES
    )

    @Before
    fun setUp() {
        // テスト前の初期化処理
    }

    // ===== Fragment表示テスト =====

    @Test
    fun Fragment表示_正常に初期化される() {
        // When: GalleryFragmentを起動
        launchFragmentInContainer<GalleryFragment>()

        // Then: 基本的なビューが表示される
        onView(withId(R.id.recyclerViewGallery))
            .check(matches(isDisplayed()))
    }

    @Test
    fun Fragment表示_RecyclerViewが設定される() {
        // When: GalleryFragmentを起動
        launchFragmentInContainer<GalleryFragment>()

        // Then: RecyclerViewが表示されて適切に設定される
        onView(withId(R.id.recyclerViewGallery))
            .check(matches(isDisplayed()))
            .check(matches(hasMinimumChildCount(0))) // 初期状態
    }

    // ===== 空状態表示テスト =====

    @Test
    fun 空状態_データなし時に空状態ビューが表示される() {
        // When: データが空の状態でFragmentを起動
        launchFragmentInContainer<GalleryFragment>()

        // Then: 空状態ビューが表示される
        onView(withId(R.id.layoutEmptyState))
            .check(matches(isDisplayed()))
    }

    @Test
    fun 空状態_空状態メッセージが正しく表示される() {
        // When: データが空の状態でFragmentを起動
        launchFragmentInContainer<GalleryFragment>()

        // Then: 空状態メッセージが表示される
        onView(withId(R.id.textEmptyMessage))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("写真がありません"))))
    }

    @Test
    fun 空状態_カメラボタンが表示される() {
        // When: データが空の状態でFragmentを起動
        launchFragmentInContainer<GalleryFragment>()

        // Then: カメラ撮影へのボタンが表示される
        onView(withId(R.id.buttonTakePhoto))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("写真を撮る"))))
    }

    // ===== データ表示テスト =====

    @Test
    fun データ表示_アイテムがある場合RecyclerViewに表示される() {
        // Given: モックデータを設定（実装時にテストデータを準備）
        launchFragmentInContainer<GalleryFragment>()

        // When: データが読み込まれる
        // TODO: テスト用データ注入処理を実装

        // Then: RecyclerViewにアイテムが表示される
        onView(withId(R.id.recyclerViewGallery))
            .check(matches(isDisplayed()))
        
        // 空状態ビューは非表示になる
        onView(withId(R.id.layoutEmptyState))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun データ表示_アイテムクリックで詳細画面に遷移() {
        // Given: アイテムが表示されている状態
        launchFragmentInContainer<GalleryFragment>()
        
        // TODO: テスト用データを準備してRecyclerViewにアイテムを表示

        // When: 最初のアイテムをクリック
        onView(withId(R.id.recyclerViewGallery))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Then: 詳細画面への遷移が発生する
        // TODO: 詳細画面遷移の確認（DetailActivityが実装された後）
    }

    // ===== ローディング状態テスト =====

    @Test
    fun ローディング状態_データ読み込み中にプログレスバーが表示される() {
        // When: Fragment起動（初期ローディング）
        launchFragmentInContainer<GalleryFragment>()

        // Then: プログレスバーが一時的に表示される
        // TODO: ローディング状態の制御テスト
    }

    @Test
    fun ローディング状態_SwipeRefresh動作確認() {
        // Given: Fragment表示
        launchFragmentInContainer<GalleryFragment>()

        // When: SwipeRefreshを実行
        onView(withId(R.id.swipeRefreshLayout))
            .perform(swipeDown())

        // Then: リフレッシュ処理が実行される
        // TODO: リフレッシュ後の状態確認
    }

    // ===== エラー状態テスト =====

    @Test
    fun エラー状態_ネットワークエラー時にエラーメッセージが表示される() {
        // Given: エラー状態を模擬
        launchFragmentInContainer<GalleryFragment>()

        // TODO: エラー状態の注入とメッセージ表示確認
        
        // Then: エラーメッセージが表示される
        // エラー時のSnackbar or Toast表示確認
    }

    // ===== RecyclerViewアイテムテスト =====

    @Test
    fun RecyclerViewアイテム_画像が正しく表示される() {
        // Given: テストデータ準備
        launchFragmentInContainer<GalleryFragment>()

        // TODO: RecyclerViewにアイテムが表示された状態

        // Then: 各アイテムの画像が表示される
        onView(withId(R.id.recyclerViewGallery))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0))

        // アイテム内の画像表示確認
        onView(allOf(withId(R.id.imageViewCloth), isDisplayed()))
            .check(matches(isDisplayed()))
    }

    @Test
    fun RecyclerViewアイテム_タグ情報が表示される() {
        // Given: テストデータ準備
        launchFragmentInContainer<GalleryFragment>()

        // TODO: タグ情報を含むテストデータ

        // Then: タグ情報（サイズ、色、カテゴリ）が表示される
        onView(allOf(withId(R.id.textTagInfo), isDisplayed()))
            .check(matches(isDisplayed()))
    }

    // ===== フィルタリング機能テスト =====

    @Test
    fun フィルタリング_カテゴリフィルタが機能する() {
        // Given: 複数カテゴリのデータ準備
        launchFragmentInContainer<GalleryFragment>()

        // TODO: フィルタUI実装後にテスト追加
        // カテゴリ選択でフィルタリング動作確認
    }

    @Test
    fun フィルタリング_色フィルタが機能する() {
        // Given: 複数色のデータ準備
        launchFragmentInContainer<GalleryFragment>()

        // TODO: 色フィルタテスト実装
    }

    // ===== ナビゲーションテスト =====

    @Test
    fun ナビゲーション_戻るボタンで前画面に戻る() {
        // Given: Fragment表示
        launchFragmentInContainer<GalleryFragment>()

        // When: 戻るボタン押下
        // TODO: 戻るボタン動作テスト（MainActivity統合後）

        // Then: 前画面に戻る
    }

    // ===== グリッドレイアウトテスト =====

    @Test
    fun グリッドレイアウト_適切な列数で表示される() {
        // Given: 複数アイテムのデータ
        launchFragmentInContainer<GalleryFragment>()

        // TODO: GridLayoutManagerの列数確認
        // 画面サイズに応じた適切な列数表示
        
        // Then: グリッド形式で表示される
        onView(withId(R.id.recyclerViewGallery))
            .check(matches(isDisplayed()))
    }

    @Test
    fun グリッドレイアウト_スクロール動作が正常() {
        // Given: 多数のアイテム（スクロール可能な数）
        launchFragmentInContainer<GalleryFragment>()

        // TODO: 多数のテストデータ準備

        // When: スクロール操作
        onView(withId(R.id.recyclerViewGallery))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(10))

        // Then: スムーズにスクロールする
        onView(withId(R.id.recyclerViewGallery))
            .check(matches(isDisplayed()))
    }
}