package com.example.clothstock.ui.camera

/**
 * カメラの状態を表すenumクラス
 * 
 * CameraViewModelで使用してカメラの現在の状態を管理する
 */
enum class CameraState {
    /**
     * 初期状態：カメラの初期化前
     */
    IDLE,
    
    /**
     * 初期化中：CameraXの初期化処理を実行中
     */
    INITIALIZING,
    
    /**
     * 準備完了：カメラのプレビューが開始され撮影可能
     */
    READY,
    
    /**
     * 撮影中：画像キャプチャ処理を実行中
     */
    CAPTURING,
    
    /**
     * エラー：カメラ関連のエラーが発生
     */
    ERROR;
    
    /**
     * カメラが使用可能な状態かどうかを判定
     * 
     * @return 撮影可能な状態の場合はtrue
     */
    fun isReady(): Boolean = this == READY
    
    /**
     * カメラが処理中（初期化中または撮影中）かどうかを判定
     * 
     * @return 処理中の場合はtrue
     */
    fun isProcessing(): Boolean = this == INITIALIZING || this == CAPTURING
    
    /**
     * エラー状態かどうかを判定
     * 
     * @return エラー状態の場合はtrue
     */
    fun isError(): Boolean = this == ERROR
}

/**
 * 画像キャプチャの結果を表すsealed class
 */
sealed class CaptureResult {
    /**
     * キャプチャ成功
     * 
     * @param imageUri 保存された画像のURI
     * @param filePath ファイルパス
     */
    data class Success(
        val imageUri: android.net.Uri,
        val filePath: String
    ) : CaptureResult()
    
    /**
     * キャプチャ失敗
     * 
     * @param error エラー原因
     * @param message エラーメッセージ
     */
    data class Error(
        val error: Throwable,
        val message: String
    ) : CaptureResult()
}

/**
 * カメラエラーの種類を表すenum
 */
enum class CameraError {
    /**
     * カメラの初期化エラー
     */
    INITIALIZATION_FAILED,
    
    /**
     * 画像キャプチャエラー
     */
    CAPTURE_FAILED,
    
    /**
     * ストレージアクセスエラー
     */
    STORAGE_ERROR,
    
    /**
     * 権限エラー
     */
    PERMISSION_DENIED,
    
    /**
     * カメラハードウェアエラー
     */
    HARDWARE_ERROR,
    
    /**
     * 不明なエラー
     */
    UNKNOWN_ERROR;
    
    /**
     * ユーザー向けエラーメッセージを取得
     * 
     * @return エラーメッセージ
     */
    fun getUserMessage(): String {
        return when (this) {
            INITIALIZATION_FAILED -> "カメラの初期化に失敗しました"
            CAPTURE_FAILED -> "写真の撮影に失敗しました"
            STORAGE_ERROR -> "写真の保存に失敗しました"
            PERMISSION_DENIED -> "カメラの権限が必要です"
            HARDWARE_ERROR -> "カメラが使用できません"
            UNKNOWN_ERROR -> "予期しないエラーが発生しました"
        }
    }
}