package com.example.clothstock.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.clothstock.R
import com.example.clothstock.databinding.ActivityCameraBinding
import com.example.clothstock.ui.tagging.TaggingActivity
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * カメラ機能を提供するアクティビティ
 * 
 * CameraXを使用してカメラプレビューと写真撮影機能を実装
 * 撮影した写真のURIを呼び出し元に返す
 */
class CameraActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "CameraActivity"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
    
    private lateinit var binding: ActivityCameraBinding
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var capturedImageUri: Uri? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    // カメラ権限リクエスト
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            initializeCamera()
        } else {
            // 権限が拒否された場合の処理
            Log.e(TAG, "Camera permission denied")
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ViewBinding セットアップ
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // カメラエグゼキュータの初期化
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // UIイベントリスナーの設定
        setupClickListeners()
        
        // カメラ権限チェック・初期化
        if (allPermissionsGranted()) {
            initializeCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    /**
     * UIイベントリスナーの設定
     */
    private fun setupClickListeners() {
        // 戻るボタン
        binding.buttonBack.setOnClickListener {
            finish()
        }
        
        // 撮影ボタン
        binding.buttonCapture.setOnClickListener {
            capturePhoto()
        }
        
        // 再撮影ボタン
        binding.buttonRetake.setOnClickListener {
            returnToPreview()
        }
        
        // 保存ボタン
        binding.buttonSave.setOnClickListener {
            savePhoto()
        }
    }
    
    /**
     * カメラの初期化
     */
    private fun initializeCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (exc: Exception) {
                Log.e(TAG, "Camera provider initialization failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    /**
     * カメラユースケースのバインド
     */
    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return
        
        try {
            // 既存のバインドをクリア
            provider.unbindAll()
            
            // プレビューの設定
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
            
            // 画像キャプチャの設定
            imageCapture = ImageCapture.Builder()
                .build()
            
            // 背面カメラを選択
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            // カメラをライフサイクルにバインド
            provider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
            
            Log.d(TAG, "Camera use cases bound successfully")
            
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }
    
    /**
     * カメラユースケースのアンバインド
     */
    private fun unbindCameraUseCases() {
        try {
            cameraProvider?.unbindAll()
            imageCapture = null
            Log.d(TAG, "Camera use cases unbound successfully")
        } catch (exc: Exception) {
            Log.e(TAG, "Use case unbinding failed", exc)
        }
    }
    
    /**
     * 写真撮影
     */
    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return
        
        // 撮影ファイル名の生成
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        
        // 出力オプション（MediaStoreに保存）
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
        
        // 撮影実行
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedImageUri = output.savedUri
                    showPhotoConfirmation(output.savedUri)
                    Log.d(TAG, "Photo capture succeeded: ${output.savedUri}")
                }
                
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }
    
    /**
     * 写真確認画面の表示
     */
    private fun showPhotoConfirmation(imageUri: Uri?) {
        lifecycleScope.launch {
            // プレビューを非表示、確認画面を表示
            binding.layoutCameraControls.visibility = android.view.View.GONE
            binding.layoutPhotoConfirmation.visibility = android.view.View.VISIBLE
            
            // 撮影された写真を表示
            imageUri?.let {
                binding.imageViewCaptured.setImageURI(it)
            }
        }
    }
    
    /**
     * プレビュー画面に戻る
     */
    private fun returnToPreview() {
        binding.layoutPhotoConfirmation.visibility = android.view.View.GONE
        binding.layoutCameraControls.visibility = android.view.View.VISIBLE
        capturedImageUri = null
    }
    
    /**
     * 写真を保存してタグ入力画面に遷移
     */
    private fun savePhoto() {
        capturedImageUri?.let { uri ->
            val intent = Intent(this, TaggingActivity::class.java).apply {
                putExtra(TaggingActivity.EXTRA_IMAGE_URI, uri.toString())
            }
            startActivity(intent)
            finish()
        }
    }
    
    /**
     * 必要な権限がすべて許可されているかチェック
     */
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause - unbinding camera use cases")
        unbindCameraUseCases()
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - rebinding camera use cases")
        if (allPermissionsGranted() && cameraProvider != null) {
            bindCameraUseCases()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy - shutting down camera executor")
        unbindCameraUseCases()
        cameraExecutor.shutdown()
    }
}