package com.example.clothstock.ui.camera

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * カメラ権限の説明ダイアログ関連のクラス
 * 
 * ユーザーに権限の必要性を説明し、適切なアクションへ誘導する
 * 初回拒否時と永続拒否後で異なる対応を提供
 */
object PermissionRationaleDialog {

    /**
     * ダイアログ表示情報を格納するデータクラス
     */
    data class DialogInfo(
        val title: String,
        val message: String,
        val showAllowButton: Boolean,
        val showSettingsButton: Boolean,
        val allowButtonText: String,
        val denyButtonText: String,
        val settingsButtonText: String
    )

    /**
     * ダイアログメッセージを格納するデータクラス
     */
    data class DialogMessages(
        val title: String,
        val message: String
    )

    /**
     * ダイアログのコントローラークラス
     * 
     * ダイアログの表示制御とユーザーアクションの処理を担当
     */
    class Controller(
        private val context: Context,
        private val onPermissionGranted: () -> Unit,
        private val onPermissionDenied: () -> Unit,
        private val onSettingsRequested: () -> Unit
    ) {

        /**
         * 権限説明ダイアログの表示情報を構築
         * 
         * @param isFirstTime 初回表示かどうか
         * @return ダイアログ表示に必要な情報
         */
        fun buildRationaleDialogInfo(isFirstTime: Boolean): DialogInfo {
            val messages = getDialogMessages(isFirstTime)
            
            return DialogInfo(
                title = messages.title,
                message = messages.message,
                showAllowButton = isFirstTime,
                showSettingsButton = !isFirstTime,
                allowButtonText = "許可",
                denyButtonText = "拒否",
                settingsButtonText = "設定"
            )
        }

        /**
         * 状況に応じたダイアログメッセージを取得
         * 
         * @param isFirstTime 初回表示かどうか
         * @return ダイアログのタイトルとメッセージ
         */
        fun getDialogMessages(isFirstTime: Boolean): DialogMessages {
            return if (isFirstTime) {
                DialogMessages(
                    title = "カメラ権限が必要です",
                    message = """
                        衣服の写真を撮影するためにカメラへのアクセスが必要です。
                        
                        この権限により、以下の機能が利用できます：
                        • 衣服の写真撮影
                        • 撮影した画像の保存
                        • タグ付けによる整理
                        
                        プライバシーは保護され、撮影した写真はデバイス内にのみ保存されます。
                    """.trimIndent()
                )
            } else {
                DialogMessages(
                    title = "設定からカメラ権限を有効にしてください",
                    message = """
                        カメラ機能を使用するには、アプリ設定で権限を手動で有効にする必要があります。
                        
                        設定手順：
                        1. 「設定」ボタンをタップ
                        2. 「権限」セクションを選択
                        3. 「カメラ」権限を有効にする
                        
                        権限を有効にした後、アプリに戻ってカメラ機能をお試しください。
                    """.trimIndent()
                )
            }
        }

        /**
         * 「許可」ボタンクリック時の処理
         */
        fun handleAllowButtonClick() {
            try {
                onPermissionGranted()
            } catch (e: Exception) {
                handleException(e)
            }
        }

        /**
         * 「拒否」ボタンクリック時の処理
         */
        fun handleDenyButtonClick() {
            try {
                onPermissionDenied()
            } catch (e: Exception) {
                handleException(e)
            }
        }

        /**
         * 「設定」ボタンクリック時の処理
         */
        fun handleSettingsButtonClick() {
            try {
                onSettingsRequested()
            } catch (e: Exception) {
                handleException(e)
            }
        }

        /**
         * アプリ設定画面へのインテントを作成
         * 
         * @return 設定画面に遷移するためのIntent
         */
        fun createSettingsIntent(): Intent {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            return intent
        }

        /**
         * 設定画面用インテントの有効性を検証
         * 
         * @param intent 検証対象のIntent
         * @return インテントが有効な場合はtrue
         */
        fun isValidSettingsIntent(intent: Intent): Boolean {
            return try {
                intent.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS &&
                intent.data != null &&
                intent.data.toString().contains("package:")
            } catch (e: Exception) {
                false
            }
        }

        /**
         * 例外発生時の処理
         * 
         * @param exception 発生した例外
         */
        fun handleException(exception: Exception) {
            // 例外をログに記録（実装では適切なロガーを使用）
            // 本番環境では例外レポートサービスに送信することも考慮
            // 現在は例外を無視してクラッシュを防ぐ
        }
    }
}