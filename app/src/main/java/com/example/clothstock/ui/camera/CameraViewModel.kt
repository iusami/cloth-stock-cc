package com.example.clothstock.ui.camera

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.clothstock.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * カメラ機能のViewModel
 * 
 * CameraXを使用した画像キャプチャ機能の状態管理と処理を担当
 * MVVMパターンに従い、UIとビジネスロジックを分離
 */
class CameraViewModel : ViewModel() {

    // ===== LiveData =====
    
    private val _cameraState = MutableLiveData<CameraState>(CameraState.IDLE)
    val cameraState: LiveData<CameraState> = _cameraState

    private val _captureResult = MutableLiveData<CaptureResult?>()
    val captureResult: LiveData<CaptureResult?> = _captureResult

    private val _cameraError = MutableLiveData<CameraError?>()
    val cameraError: LiveData<CameraError?> = _cameraError

    // ===== CameraX関連 =====
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // ===== 状態管理 =====
    
    private var isInitialized = false
    private var currentContext: Context? = null

    /**
     * カメラの初期化を実行
     * 
     * @param context アプリケーションコンテキスト
     */
    fun initializeCamera(context: Context) {
        if (isInitialized) {
            return // 既に初期化済みの場合は何もしない
        }

        currentContext = context
        _cameraState.value = CameraState.INITIALIZING

        viewModelScope.launch {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProvider = cameraProviderFuture.get()
                
                setupCamera()
                
                _cameraState.value = CameraState.READY
                isInitialized = true
                
            } catch (e: Exception) {
                handleCameraError(CameraError.INITIALIZATION_FAILED)
            }
        }
    }

    /**
     * CameraXのUseCaseを設定
     */
    private fun setupCamera() {
        val cameraProvider = this.cameraProvider ?: return

        // プレビューの設定
        preview = Preview.Builder().build()

        // 画像キャプチャの設定
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        // カメラセレクター（背面カメラを選択）
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // 既存のUseCaseをアンバインド
            cameraProvider.unbindAll()

            // 新しいUseCaseをバインド
            camera = cameraProvider.bindToLifecycle(
                // 注意: 実際の実装ではLifecycleOwnerを渡す必要がある
                // ここではテスト用の簡略実装
                null as androidx.lifecycle.LifecycleOwner?, 
                cameraSelector,
                preview,
                imageCapture
            )

        } catch (e: Exception) {
            handleCameraError(CameraError.INITIALIZATION_FAILED)
        }
    }

    /**
     * 画像キャプチャを実行
     * 
     * @return キャプチャが開始された場合はtrue
     */
    fun captureImage(): Boolean {
        val currentState = _cameraState.value
        
        // キャプチャ可能な状態かチェック
        if (currentState != CameraState.READY) {
            return false
        }

        val imageCapture = this.imageCapture ?: return false
        val context = currentContext ?: return false

        _cameraState.value = CameraState.CAPTURING

        viewModelScope.launch {
            try {
                // ストレージ容量チェック
                if (!FileUtils.hasEnoughStorage(context)) {
                    throw Exception("ストレージ容量が不足しています")
                }

                // 保存先ファイルを作成
                val outputFile = FileUtils.createImageFile(context)
                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                // 画像キャプチャ実行
                imageCapture.takePicture(
                    outputFileOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            try {
                                val uri = FileUtils.getUriForFile(context, outputFile)
                                val result = CaptureResult.Success(uri, outputFile.absolutePath)
                                _captureResult.value = result
                                _cameraState.value = CameraState.READY
                                
                                // 古いファイルをクリーンアップ
                                FileUtils.cleanupOldFiles(context)
                                
                            } catch (e: Exception) {
                                handleCaptureError(e)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            handleCaptureError(exception)
                        }
                    }
                )

            } catch (e: Exception) {
                handleCaptureError(e)
            }
        }

        return true
    }

    /**
     * キャプチャエラーを処理
     * 
     * @param error 発生したエラー
     */
    private fun handleCaptureError(error: Throwable) {
        val errorResult = CaptureResult.Error(error, error.message ?: "画像キャプチャに失敗しました")
        _captureResult.value = errorResult
        _cameraState.value = CameraState.READY
        handleCameraError(CameraError.CAPTURE_FAILED)
    }

    /**
     * カメラエラーを処理
     * 
     * @param error エラーの種類
     */
    fun handleCameraError(error: CameraError) {
        _cameraState.value = CameraState.ERROR
        _cameraError.value = error
    }

    /**
     * エラー状態をクリア
     */
    fun clearError() {
        _cameraError.value = null
        if (_cameraState.value == CameraState.ERROR) {
            _cameraState.value = if (isInitialized) CameraState.READY else CameraState.IDLE
        }
    }

    /**
     * カメラリソースを解放
     */
    fun releaseCamera() {
        cameraProvider?.unbindAll()
        camera = null
        preview = null
        imageCapture = null
        isInitialized = false
        _cameraState.value = CameraState.IDLE
    }

    /**
     * キャプチャ結果を設定（テスト用）
     * 
     * @param result 設定するキャプチャ結果
     */
    fun setCaptureResult(result: CaptureResult) {
        _captureResult.value = result
    }

    /**
     * カメラが準備完了状態かチェック
     * 
     * @return 準備完了の場合はtrue
     */
    fun isReady(): Boolean {
        return _cameraState.value?.isReady() == true
    }

    /**
     * カメラが撮影中かチェック
     * 
     * @return 撮影中の場合はtrue
     */
    fun isCapturing(): Boolean {
        return _cameraState.value == CameraState.CAPTURING
    }

    /**
     * PreviewUseCaseを取得
     * 
     * @return Previewオブジェクト
     */
    fun getPreview(): Preview? = preview

    /**
     * ViewModelのクリア時にリソースを解放
     */
    override fun onCleared() {
        super.onCleared()
        releaseCamera()
        cameraExecutor.shutdown()
    }
}