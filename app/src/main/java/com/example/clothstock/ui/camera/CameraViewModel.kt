package com.example.clothstock.ui.camera

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.clothstock.util.FileUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
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

    companion object {
        private const val TAG = "CameraViewModel"
    }

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
    private var lifecycleOwner: androidx.lifecycle.LifecycleOwner? = null

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
                cameraProvider = getCameraProviderAsync(context)
                
                setupCamera()
                
                _cameraState.value = CameraState.READY
                isInitialized = true
                
                Log.d(TAG, "カメラの初期化が完了しました")
                
            } catch (e: Exception) {
                Log.e(TAG, "カメラの初期化中にエラーが発生しました", e)
                handleCameraError(CameraError.INITIALIZATION_FAILED)
            }
        }
    }

    /**
     * ProcessCameraProviderを非同期で取得
     * 
     * ListenableFutureを適切にコルーチンと統合して、ブロッキング呼び出しを回避する
     * 
     * @param context アプリケーションコンテキスト
     * @return ProcessCameraProvider
     */
    private suspend fun getCameraProviderAsync(context: Context): ProcessCameraProvider {
        return suspendCancellableCoroutine { continuation ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            
            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    continuation.resume(provider)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }, cameraExecutor)
            
            // キャンセル時の処理
            continuation.invokeOnCancellation {
                try {
                    cameraProviderFuture.cancel(true)
                } catch (e: Exception) {
                    Log.w(TAG, "CameraProviderFutureのキャンセル中にエラーが発生しました", e)
                }
            }
        }
    }

    /**
     * CameraXのUseCaseを設定
     * 
     * このメソッドはUseCaseオブジェクトの準備のみを行う。
     * 実際のLifecycleOwnerへのバインドはbindToLifecycle()メソッドで実行される。
     */
    private fun setupCamera() {
        try {
            // プレビューの設定
            preview = Preview.Builder().build()

            // 画像キャプチャの設定
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // UseCaseオブジェクトの準備完了
            // 実際のLifecycleOwnerへのバインドはbindToLifecycle()で実行される
            
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
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            viewModelScope.launch(Dispatchers.Main) {
                                try {
                                    val uri = FileUtils.getUriForFile(context, outputFile)
                                    val result = CaptureResult.Success(uri, outputFile.absolutePath)
                                    _captureResult.value = result
                                    _cameraState.value = CameraState.READY
                                    
                                    // 古いファイルをクリーンアップ（バックグラウンドで実行）
                                    withContext(Dispatchers.IO) {
                                        FileUtils.cleanupOldFiles(context)
                                    }
                                    
                                } catch (e: Exception) {
                                    handleCaptureError(e)
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            viewModelScope.launch(Dispatchers.Main) {
                                handleCaptureError(exception)
                            }
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
     * LifecycleOwnerを設定してカメラをバインド
     * 
     * カメラが初期化済みの場合のみバインドを実行する。
     * 初期化されていない場合は、initializeCamera()を先に呼び出す必要がある。
     * 
     * @param lifecycleOwner LifecycleOwner（通常はActivityまたはFragment）
     */
    fun bindToLifecycle(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner
        
        if (!isInitialized) {
            Log.w(TAG, "カメラが初期化されていません。先にinitializeCamera()を呼び出してください。")
            return
        }
        
        val cameraProvider = this.cameraProvider
        if (cameraProvider == null) {
            Log.e(TAG, "CameraProviderが利用できません")
            handleCameraError(CameraError.INITIALIZATION_FAILED)
            return
        }
        
        if (preview == null || imageCapture == null) {
            Log.e(TAG, "UseCaseが準備されていません")
            handleCameraError(CameraError.INITIALIZATION_FAILED)
            return
        }
        
        try {
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // 既存のUseCaseをアンバインド
            cameraProvider.unbindAll()
            
            // 新しいUseCaseをバインド
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            
            Log.d(TAG, "カメラのLifecycleバインドが完了しました")
            
        } catch (e: Exception) {
            Log.e(TAG, "カメラのLifecycleバインド中にエラーが発生しました", e)
            handleCameraError(CameraError.INITIALIZATION_FAILED)
        }
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