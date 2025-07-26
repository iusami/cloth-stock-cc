package com.example.clothstock.ui.camera

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * カメラ権限の管理クラス
 * 
 * カメラアクセス権限の状態管理、リクエスト処理、
 * ユーザーへの説明ダイアログ表示判定を担当する
 */
class CameraPermissionManager(
    private val context: Context,
    private val permissionLauncher: ActivityResultLauncher<String>,
    private val onPermissionRationaleNeeded: () -> Unit
) {
    
    // 権限状態の管理
    private var _isPermissionGranted: Boolean = false
    private var _isPermanentlyDenied: Boolean = false
    private var _hasRequestedOnce: Boolean = false

    /**
     * カメラ権限の現在の状態をチェック
     * 
     * @return 権限が許可されている場合はtrue、そうでなければfalse
     */
    fun checkCameraPermission(): Boolean {
        return try {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.CAMERA
            )
            val isGranted = permissionStatus == PackageManager.PERMISSION_GRANTED
            _isPermissionGranted = isGranted
            isGranted
        } catch (e: Exception) {
            // セキュリティ例外やその他のエラーが発生した場合は権限なしとみなす
            _isPermissionGranted = false
            false
        }
    }

    /**
     * カメラ権限のリクエストを実行
     * 
     * 権限が未許可の場合のみActivityResultLauncherを使用してリクエストを送信
     */
    fun requestCameraPermission() {
        try {
            if (!checkCameraPermission()) {
                _hasRequestedOnce = true
                permissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            // 権限リクエスト中の例外をキャッチして適切にハンドリング
            // ログ記録や代替処理を実装可能
        }
    }

    /**
     * 権限リクエストの結果を処理
     * 
     * @param isGranted ユーザーが権限を許可した場合はtrue
     */
    fun handlePermissionResult(isGranted: Boolean) {
        _isPermissionGranted = isGranted
        
        if (!isGranted && _hasRequestedOnce) {
            // 2回目以降の拒否は永続拒否の可能性が高い
            // 実際のActivityでshouldShowRequestPermissionRationaleをチェックして判定
        }
    }

    /**
     * 権限の説明ダイアログを表示すべきかを判定
     * 
     * @return 説明ダイアログが必要な場合はtrue
     */
    fun shouldShowRationale(): Boolean {
        return !_isPermissionGranted && !_isPermanentlyDenied && _hasRequestedOnce
    }

    /**
     * 永続的に拒否された状態としてマーク
     * 
     * ユーザーが「今後表示しない」を選択した場合に呼び出される
     */
    fun markAsPermanentlyDenied() {
        _isPermanentlyDenied = true
    }

    /**
     * 現在の権限許可状態を取得
     * 
     * @return 権限が許可されている場合はtrue
     */
    fun isPermissionGranted(): Boolean = _isPermissionGranted

    /**
     * 永続拒否状態かどうかを取得
     * 
     * @return 永続的に拒否されている場合はtrue
     */
    fun isPermanentlyDenied(): Boolean = _isPermanentlyDenied

    /**
     * すべての権限状態をリセット
     * 
     * 新しいセッションや設定リセット時に使用
     */
    fun resetState() {
        _isPermissionGranted = false
        _isPermanentlyDenied = false
        _hasRequestedOnce = false
    }
}