package com.example.clothstock.ui.common

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.clothstock.R
import com.example.clothstock.ui.tagging.TaggingViewModel

/**
 * エラー表示用のダイアログフラグメント
 * 
 * TDDアプローチに従って実装されたエラーハンドリング強化機能
 * 各種エラータイプに応じた適切なメッセージとアクションを提供
 */
class ErrorDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_ERROR_TYPE = "error_type"
        private const val ARG_IS_RETRYABLE = "is_retryable"
        private const val ARG_POSITIVE_BUTTON_TEXT = "positive_button_text"
        private const val ARG_SHOW_SETTINGS_BUTTON = "show_settings_button"

        /**
         * エラーダイアログのインスタンスを作成
         * 
         * @param title ダイアログのタイトル
         * @param message エラーメッセージ
         * @param errorType エラータイプ
         * @param isRetryable リトライ可能かどうか
         * @param showSettingsButton 設定ボタンを表示するか
         * @return ErrorDialogFragmentインスタンス
         */
        fun newInstance(
            title: String = "",
            message: String,
            errorType: TaggingViewModel.ErrorType = TaggingViewModel.ErrorType.UNKNOWN,
            isRetryable: Boolean = false,
            showSettingsButton: Boolean = false
        ): ErrorDialogFragment {
            val fragment = ErrorDialogFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_MESSAGE, message)
            args.putString(ARG_ERROR_TYPE, errorType.name)
            args.putBoolean(ARG_IS_RETRYABLE, isRetryable)
            args.putBoolean(ARG_SHOW_SETTINGS_BUTTON, showSettingsButton)
            
            // エラータイプに応じたデフォルトのボタンテキストを設定
            val positiveButtonText = when (errorType) {
                TaggingViewModel.ErrorType.VALIDATION -> "OK"
                TaggingViewModel.ErrorType.DATABASE,
                TaggingViewModel.ErrorType.NETWORK,
                TaggingViewModel.ErrorType.FILE_SYSTEM -> if (isRetryable) "リトライ" else "OK"
                TaggingViewModel.ErrorType.UNKNOWN -> if (isRetryable) "リトライ" else "OK"
            }
            args.putString(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText)
            
            fragment.arguments = args
            return fragment
        }

        /**
         * カメラ権限エラー用の専用ダイアログ
         */
        fun newCameraPermissionErrorDialog(message: String): ErrorDialogFragment {
            return newInstance(
                title = "カメラ権限エラー",
                message = message,
                errorType = TaggingViewModel.ErrorType.VALIDATION,
                isRetryable = false,
                showSettingsButton = true
            )
        }

        /**
         * データベースエラー用の専用ダイアログ
         */
        fun newDatabaseErrorDialog(message: String): ErrorDialogFragment {
            return newInstance(
                title = "データベースエラー",
                message = message,
                errorType = TaggingViewModel.ErrorType.DATABASE,
                isRetryable = true,
                showSettingsButton = false
            )
        }

        /**
         * ファイルシステムエラー用の専用ダイアログ
         */
        fun newFileSystemErrorDialog(message: String): ErrorDialogFragment {
            return newInstance(
                title = "ファイルエラー",
                message = message,
                errorType = TaggingViewModel.ErrorType.FILE_SYSTEM,
                isRetryable = true,
                showSettingsButton = false
            )
        }
    }

    /**
     * ダイアログのアクションを処理するリスナー
     */
    interface ErrorDialogListener {
        /**
         * ポジティブボタンがクリックされた時
         * 
         * @param errorType エラータイプ
         * @param isRetryAction リトライアクションかどうか
         */
        fun onPositiveButtonClicked(errorType: TaggingViewModel.ErrorType, isRetryAction: Boolean)
        
        /**
         * 設定ボタンがクリックされた時
         */
        fun onSettingsButtonClicked()
        
        /**
         * ダイアログがキャンセルされた時
         */
        fun onDialogCancelled()
    }

    private var listener: ErrorDialogListener? = null

    /**
     * リスナーを設定
     */
    fun setErrorDialogListener(listener: ErrorDialogListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments ?: throw IllegalStateException("Arguments must be provided")
        
        val title = args.getString(ARG_TITLE, "")
        val message = args.getString(ARG_MESSAGE, "")
        val errorTypeName = args.getString(ARG_ERROR_TYPE, TaggingViewModel.ErrorType.UNKNOWN.name)
        val isRetryable = args.getBoolean(ARG_IS_RETRYABLE, false)
        val positiveButtonText = args.getString(ARG_POSITIVE_BUTTON_TEXT, "OK")
        val showSettingsButton = args.getBoolean(ARG_SHOW_SETTINGS_BUTTON, false)
        
        val errorType = try {
            TaggingViewModel.ErrorType.valueOf(errorTypeName)
        } catch (e: IllegalArgumentException) {
            TaggingViewModel.ErrorType.UNKNOWN
        }

        val builder = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                listener?.onPositiveButtonClicked(errorType, isRetryable)
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                listener?.onDialogCancelled()
            }

        // タイトルが指定されている場合のみ設定
        if (title.isNotEmpty()) {
            builder.setTitle(title)
        }

        // 設定ボタンを表示する場合
        if (showSettingsButton) {
            builder.setNeutralButton("設定") { _, _ ->
                listener?.onSettingsButtonClicked()
            }
        }

        return builder.create()
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        listener?.onDialogCancelled()
    }
}