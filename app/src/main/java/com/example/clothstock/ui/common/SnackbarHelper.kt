package com.example.clothstock.ui.common

import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import com.example.clothstock.R

/**
 * Snackbar表示の統一管理クラス
 * 
 * 成功・エラー・情報メッセージの統一された表示を提供
 * TDDアプローチに従って実装され、テスト可能な設計
 */
object SnackbarHelper {

    /**
     * Snackbarのタイプ
     */
    enum class Type {
        SUCCESS,    // 成功メッセージ（緑色）
        ERROR,      // エラーメッセージ（赤色）
        INFO,       // 情報メッセージ（デフォルト色）
        WARNING     // 警告メッセージ（オレンジ色）
    }

    /**
     * 成功メッセージを表示
     * 
     * @param view 親ビュー
     * @param message 表示するメッセージ
     * @param duration 表示時間
     * @param actionText アクションボタンのテキスト（任意）
     * @param action アクションボタンのクリックリスナー（任意）
     */
    fun showSuccess(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        show(view, message, Type.SUCCESS, duration, actionText, action)
    }

    /**
     * 成功メッセージを表示（文字列リソース版）
     */
    fun showSuccess(
        view: View,
        @StringRes messageRes: Int,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        showSuccess(view, view.context.getString(messageRes), duration, actionText, action)
    }

    /**
     * エラーメッセージを表示
     * 
     * @param view 親ビュー
     * @param message 表示するメッセージ
     * @param duration 表示時間
     * @param actionText アクションボタンのテキスト（任意）
     * @param action アクションボタンのクリックリスナー（任意）
     */
    fun showError(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_LONG,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        show(view, message, Type.ERROR, duration, actionText, action)
    }

    /**
     * エラーメッセージを表示（文字列リソース版）
     */
    fun showError(
        view: View,
        @StringRes messageRes: Int,
        duration: Int = Snackbar.LENGTH_LONG,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        showError(view, view.context.getString(messageRes), duration, actionText, action)
    }

    /**
     * 情報メッセージを表示
     * 
     * @param view 親ビュー
     * @param message 表示するメッセージ
     * @param duration 表示時間
     * @param actionText アクションボタンのテキスト（任意）
     * @param action アクションボタンのクリックリスナー（任意）
     */
    fun showInfo(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        show(view, message, Type.INFO, duration, actionText, action)
    }

    /**
     * 情報メッセージを表示（文字列リソース版）
     */
    fun showInfo(
        view: View,
        @StringRes messageRes: Int,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        showInfo(view, view.context.getString(messageRes), duration, actionText, action)
    }

    /**
     * 警告メッセージを表示
     * 
     * @param view 親ビュー
     * @param message 表示するメッセージ
     * @param duration 表示時間
     * @param actionText アクションボタンのテキスト（任意）
     * @param action アクションボタンのクリックリスナー（任意）
     */
    fun showWarning(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_LONG,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        show(view, message, Type.WARNING, duration, actionText, action)
    }

    /**
     * 警告メッセージを表示（文字列リソース版）
     */
    fun showWarning(
        view: View,
        @StringRes messageRes: Int,
        duration: Int = Snackbar.LENGTH_LONG,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        showWarning(view, view.context.getString(messageRes), duration, actionText, action)
    }

    /**
     * 保存成功メッセージを表示（専用メソッド）
     */
    fun showSaveSuccess(view: View) {
        showSuccess(view, view.context.getString(R.string.message_saved))
    }

    /**
     * 更新成功メッセージを表示（専用メソッド）
     */
    fun showUpdateSuccess(view: View) {
        showSuccess(view, view.context.getString(R.string.message_updated))
    }

    /**
     * リトライアクション付きエラーメッセージを表示
     * 
     * @param view 親ビュー
     * @param message エラーメッセージ
     * @param retryAction リトライアクション
     */
    fun showErrorWithRetry(
        view: View,
        message: String,
        retryAction: () -> Unit
    ) {
        showError(
            view = view,
            message = message,
            duration = Snackbar.LENGTH_INDEFINITE,
            actionText = view.context.getString(R.string.button_retry),
            action = retryAction
        )
    }

    /**
     * リトライアクション付きエラーメッセージを表示（文字列リソース版）
     */
    fun showErrorWithRetry(
        view: View,
        @StringRes messageRes: Int,
        retryAction: () -> Unit
    ) {
        showErrorWithRetry(view, view.context.getString(messageRes), retryAction)
    }

    /**
     * 基本的なSnackbar表示メソッド
     * 
     * @param view 親ビュー
     * @param message 表示するメッセージ
     * @param type Snackbarのタイプ
     * @param duration 表示時間
     * @param actionText アクションボタンのテキスト（任意）
     * @param action アクションボタンのクリックリスナー（任意）
     */
    private fun show(
        view: View,
        message: String,
        type: Type,
        duration: Int,
        actionText: String?,
        action: (() -> Unit)?
    ) {
        val snackbar = Snackbar.make(view, message, duration)

        // アクションボタンの設定
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }

        // タイプに応じた色設定
        val context = view.context
        when (type) {
            Type.SUCCESS -> {
                snackbar.setBackgroundTint(context.getColor(android.R.color.holo_green_dark))
                snackbar.setTextColor(context.getColor(android.R.color.white))
            }
            Type.ERROR -> {
                snackbar.setBackgroundTint(context.getColor(android.R.color.holo_red_dark))
                snackbar.setTextColor(context.getColor(android.R.color.white))
                snackbar.setActionTextColor(context.getColor(android.R.color.white))
            }
            Type.WARNING -> {
                snackbar.setBackgroundTint(context.getColor(android.R.color.holo_orange_dark))
                snackbar.setTextColor(context.getColor(android.R.color.white))
            }
            Type.INFO -> {
                // デフォルトの色を使用
            }
        }

        snackbar.show()
    }
}