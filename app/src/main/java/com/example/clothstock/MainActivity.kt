package com.example.clothstock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.clothstock.databinding.ActivityMainBinding
import com.example.clothstock.ui.camera.CameraActivity

/**
 * cloth-stock アプリケーションのメインアクティビティ
 * 
 * 衣服管理アプリの中央ハブとして機能し、以下の機能への導線を提供：
 * - カメラ機能（写真撮影）
 * - ギャラリー（衣服アイテム一覧）
 * - 詳細表示・タグ編集
 */
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var binding: ActivityMainBinding
    
    // カメラアクティビティからの結果を受け取る
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUriString = result.data?.getStringExtra(CameraActivity.EXTRA_IMAGE_URI)
            imageUriString?.let { uriString ->
                val imageUri = Uri.parse(uriString)
                handleCapturedImage(imageUri)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ViewBinding セットアップ
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // ナビゲーションの初期化
        setupNavigation()
    }
    
    /**
     * アプリ内ナビゲーションの初期化
     */
    private fun setupNavigation() {
        // カメラボタンのクリックリスナー設定
        binding.buttonCamera.setOnClickListener {
            launchCameraActivity()
        }
        
        // ギャラリーボタンのクリックリスナー設定
        binding.buttonGallery.setOnClickListener {
            // TODO: ギャラリーアクティビティへの遷移を実装予定
            Log.d(TAG, "Gallery button clicked - not implemented yet")
        }
    }
    
    /**
     * カメラアクティビティを起動
     */
    private fun launchCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }
    
    /**
     * 撮影された画像の処理
     */
    private fun handleCapturedImage(imageUri: Uri) {
        Log.d(TAG, "Captured image URI: $imageUri")
        
        // TODO: 撮影された画像をTaggingActivityに渡して
        // タグ編集画面に遷移する処理を実装予定
        // 現在はログ出力のみ
    }
}