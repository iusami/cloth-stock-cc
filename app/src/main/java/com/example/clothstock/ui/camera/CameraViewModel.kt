package com.example.clothstock.ui.camera

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.clothstock.util.FileUtils
import com.example.clothstock.ui.common.LoadingStateManager
import com.example.clothstock.ui.common.RetryMechanism
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

    private val _loadingState = MutableLiveData<LoadingStateManager.LoadingState>(LoadingStateManager.LoadingState.Idle)
    val loadingState: LiveData<LoadingStateManager.LoadingState> = _loadingState

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

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
        _isLoading.value = true
        _loadingState.value = LoadingStateManager.LoadingState.Loading("カメラを初期化中...")

        viewModelScope.launch {
            // リトライ機能付きで初期化を実行
            val retryResult = RetryMechanism.execute(
                config = RetryMechanism.RetryConfig.FILE_IO_DEFAULT
            ) {
                initializeCameraInternal(context)
            }

            when (retryResult) {
                is RetryMechanism.RetryResult.Success -> {
                    _cameraState.value = CameraState.READY
                    _loadingState.value = LoadingStateManager.LoadingState.Success
                    isInitialized = true
                    Log.d(TAG, "カメラの初期化が完了しました（${retryResult.attemptCount}回目の試行で成功）")
                }
                is RetryMechanism.RetryResult.Failure -> {
                    Log.e(TAG, "カメラの初期化に失敗しました（${retryResult.attemptCount}回試行）", retryResult.lastException)
                    _loadingState.value = LoadingStateManager.LoadingState.Error(
                        "カメラの初期化に失敗しました",
                        retryResult.lastException
                    )
                    handleCameraError(CameraError.INITIALIZATION_FAILED)
                }
            }
            
            _isLoading.value = false
        }
    }

    /**
     * カメラ初期化の内部実装（リトライ対応）
     * 
     * @param context アプリケーションコンテキスト
     */
    private suspend fun initializeCameraInternal(context: Context) {
        cameraProvider = getCameraProviderAsync(context)
        setupCamera()
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
        _isLoading.value = true
        _loadingState.value = LoadingStateManager.LoadingState.Loading("写真を撮影中...")

        viewModelScope.launch {
            // リトライ機能付きで画像キャプチャを実行
            val retryResult = RetryMechanism.executeForFileIO {
                captureImageInternal(context, imageCapture)
            }

            when (retryResult) {
                is RetryMechanism.RetryResult.Success -> {
                    _captureResult.value = retryResult.result
                    _cameraState.value = CameraState.READY
                    _loadingState.value = LoadingStateManager.LoadingState.Success
                    Log.d(TAG, "画像キャプチャが完了しました（${retryResult.attemptCount}回目の試行で成功）")
                    
                    // 古いファイルをクリーンアップ（バックグラウンドで実行）
                    withContext(Dispatchers.IO) {
                        try {
                            FileUtils.cleanupOldFiles(context)
                        } catch (e: Exception) {
                            Log.w(TAG, "古いファイルのクリーンアップ中にエラーが発生しました", e)
                        }
                    }
                }
                is RetryMechanism.RetryResult.Failure -> {
                    Log.e(TAG, "画像キャプチャに失敗しました（${retryResult.attemptCount}回試行）", retryResult.lastException)
                    _loadingState.value = LoadingStateManager.LoadingState.Error(
                        "画像キャプチャに失敗しました",
                        retryResult.lastException
                    )
                    handleCaptureError(retryResult.lastException)
                }
            }
            
            _isLoading.value = false
        }

        return true
    }

    /**
     * 画像キャプチャの内部実装（リトライ対応）
     * 
     * @param context コンテキスト
     * @param imageCapture ImageCaptureオブジェクト
     * @return キャプチャ結果
     */
    private suspend fun captureImageInternal(context: Context, imageCapture: ImageCapture): CaptureResult {
        return suspendCancellableCoroutine { continuation ->
            var outputFile: File? = null
            
            try {
                // ストレージ容量チェック
                if (!FileUtils.hasEnoughStorage(context)) {
                    continuation.resumeWithException(Exception("ストレージ容量が不足しています"))
                    return@suspendCancellableCoroutine
                }

                // 保存先ファイルを作成
                outputFile = FileUtils.createImageFile(context)
                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                // 画像キャプチャ実行
                imageCapture.takePicture(
                    outputFileOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            try {
                                val uri = FileUtils.getUriForFile(context, outputFile)
                                val result = CaptureResult.Success(uri, outputFile.absolutePath)
                                continuation.resume(result)
                            } catch (e: Exception) {
                                continuation.resumeWithException(e)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            // エラー時に作成済みファイルをクリーンアップ
                            outputFile?.let { cleanupOutputFile(it) }
                            continuation.resumeWithException(exception)
                        }
                    }
                )

                // キャンセル時の処理
                continuation.invokeOnCancellation {
                    Log.w(TAG, "画像キャプチャがキャンセルされました")
                    // キャンセル時に作成済みファイルをクリーンアップ
                    outputFile?.let { cleanupOutputFile(it) }
                }

            } catch (e: Exception) {
                // 例外発生時に作成済みファイルをクリーンアップ
                outputFile?.let { cleanupOutputFile(it) }
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * 出力ファイルの安全なクリーンアップ
     * 
     * キャンセルやエラー時に部分的に作成されたファイルを削除して
     * ストレージリークを防ぐ
     * 
     * @param outputFile クリーンアップ対象のファイル
     */
    private fun cleanupOutputFile(outputFile: File) {
        try {
            if (outputFile.exists()) {
                val deleted = outputFile.delete()
                if (deleted) {
                    Log.d(TAG, "出力ファイルをクリーンアップしました: ${outputFile.absolutePath}")
                } else {
                    Log.w(TAG, "出力ファイルの削除に失敗しました: ${outputFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "出力ファイルクリーンアップ中にエラーが発生しました", e)
        }
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
        _loadingState.value = LoadingStateManager.LoadingState.Idle
        if (_cameraState.value == CameraState.ERROR) {
            _cameraState.value = if (isInitialized) CameraState.READY else CameraState.IDLE
        }
    }

    /**
     * カメラ初期化を再試行
     */
    fun retryInitialization() {
        val context = currentContext
        if (context != null) {
            clearError()
            isInitialized = false
            initializeCamera(context)
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
     * このメソッドはユニットテストでのみ使用されることを想定している。
     * 実際のアプリでは画像キャプチャ処理によって自動的に結果が設定される。
     * 
     * @param result 設定するキャプチャ結果
     */
    @VisibleForTesting
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