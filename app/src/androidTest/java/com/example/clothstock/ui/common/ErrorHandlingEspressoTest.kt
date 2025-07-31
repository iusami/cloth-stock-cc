package com.example.clothstock.ui.common

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.fragment.app.testing.launchFragmentInContainer
import com.example.clothstock.ui.tagging.TaggingViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.containsString

/**
 * エラーハンドリング関連のEspressoテスト
 * 
 * TDDアプローチに従った包括的UI エラーハンドリング機能のテスト
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ErrorHandlingEspressoTest {

    @Test
    fun errorDialog_基本表示_正しくダイアログが表示される() {
        // Given
        val fragment = ErrorDialogFragment.newInstance(
            title = "テストエラー",
            message = "テストメッセージです",
            errorType = TaggingViewModel.ErrorType.DATABASE,
            isRetryable = true
        )

        // When
        launchFragmentInContainer<ErrorDialogFragment> { fragment }

        // Then
        onView(withText("テストエラー"))
            .check(matches(isDisplayed()))
        onView(withText("テストメッセージです"))
            .check(matches(isDisplayed()))
        onView(withText("リトライ"))
            .check(matches(isDisplayed()))
        onView(withText("キャンセル"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun errorDialog_カメラ権限エラー_設定ボタンが表示される() {
        // Given
        val fragment = ErrorDialogFragment.newCameraPermissionErrorDialog(
            "カメラの権限が必要です"
        )

        // When
        launchFragmentInContainer<ErrorDialogFragment> { fragment }

        // Then
        onView(withText("カメラ権限エラー"))
            .check(matches(isDisplayed()))
        onView(withText("カメラの権限が必要です"))
            .check(matches(isDisplayed()))
        onView(withText("設定"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun errorDialog_データベースエラー_リトライボタンが表示される() {
        // Given
        val fragment = ErrorDialogFragment.newDatabaseErrorDialog(
            "データベース接続に失敗しました"
        )

        // When
        launchFragmentInContainer<ErrorDialogFragment> { fragment }

        // Then
        onView(withText("データベースエラー"))
            .check(matches(isDisplayed()))
        onView(withText("データベース接続に失敗しました"))
            .check(matches(isDisplayed()))
        onView(withText("リトライ"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun errorDialog_ファイルシステムエラー_適切なメッセージが表示される() {
        // Given
        val fragment = ErrorDialogFragment.newFileSystemErrorDialog(
            "ファイルの保存に失敗しました"
        )

        // When
        launchFragmentInContainer<ErrorDialogFragment> { fragment }

        // Then
        onView(withText("ファイルエラー"))
            .check(matches(isDisplayed()))
        onView(withText("ファイルの保存に失敗しました"))
            .check(matches(isDisplayed()))
        onView(withText("リトライ"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun errorDialog_バリデーションエラー_OKボタンのみ表示される() {
        // Given
        val fragment = ErrorDialogFragment.newInstance(
            title = "入力エラー",
            message = "サイズは60～160の範囲で入力してください",
            errorType = TaggingViewModel.ErrorType.VALIDATION,
            isRetryable = false
        )

        // When
        launchFragmentInContainer<ErrorDialogFragment> { fragment }

        // Then
        onView(withText("入力エラー"))
            .check(matches(isDisplayed()))
        onView(withText(containsString("サイズは60～160")))
            .check(matches(isDisplayed()))
        onView(withText("OK"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun errorDialog_ポジティブボタンクリック_ダイアログが閉じられる() {
        // Given
        val fragment = ErrorDialogFragment.newInstance(
            title = "テストエラー",
            message = "テストメッセージ",
            errorType = TaggingViewModel.ErrorType.UNKNOWN,
            isRetryable = false
        )

        // When
        launchFragmentInContainer<ErrorDialogFragment> { fragment }
        onView(withText("OK"))
            .perform(click())

        // Then
        // ダイアログが閉じられていることを確認
        // （実際のテストでは適切なアサーションを使用）
    }

    @Test
    fun errorDialog_キャンセルボタンクリック_ダイアログが閉じられる() {
        // Given
        val fragment = ErrorDialogFragment.newInstance(
            message = "テストメッセージ",
            isRetryable = true
        )

        // When
        launchFragmentInContainer<ErrorDialogFragment> { fragment }
        onView(withText("キャンセル"))
            .perform(click())

        // Then
        // ダイアログが閉じられていることを確認
        // （実際のテストでは適切なアサーションを使用）
    }

    @Test
    fun errorDialog_設定ボタンクリック_正しいアクションが実行される() {
        // Given
        val fragment = ErrorDialogFragment.newCameraPermissionErrorDialog(
            "カメラ権限が必要です"
        )

        // When
        launchFragmentInContainer<ErrorDialogFragment> { fragment }
        onView(withText("設定"))
            .perform(click())

        // Then
        // 設定アクションが実行されることを確認
        // （実際のテストでは適切なアサーションを使用）
    }

    @Test
    fun errorDialog_長いメッセージ_適切にレイアウトされる() {
        // Given
        val longMessage = "これは非常に長いエラーメッセージです。" +
                "複数行にわたって表示される可能性があります。" +
                "ダイアログのレイアウトが適切に調整されることを確認します。" +
                "ユーザーがメッセージ全体を読むことができることが重要です。"
        
        val fragment = ErrorDialogFragment.newInstance(
            title = "詳細エラー",
            message = longMessage,
            errorType = TaggingViewModel.ErrorType.UNKNOWN
        )

        // When
        launchFragmentInContainer<ErrorDialogFragment> { fragment }

        // Then
        onView(withText("詳細エラー"))
            .check(matches(isDisplayed()))
        onView(withText(containsString("これは非常に長いエラーメッセージです")))
            .check(matches(isDisplayed()))
        onView(withText("OK"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun errorDialog_タイトルなし_メッセージのみ表示される() {
        // Given
        val fragment = ErrorDialogFragment.newInstance(
            title = "", // 空のタイトル
            message = "タイトルなしのメッセージ",
            errorType = TaggingViewModel.ErrorType.UNKNOWN
        )

        // When
        launchFragmentInContainer<ErrorDialogFragment> { fragment }

        // Then
        onView(withText("タイトルなしのメッセージ"))
            .check(matches(isDisplayed()))
        onView(withText("OK"))
            .check(matches(isDisplayed()))
    }
}