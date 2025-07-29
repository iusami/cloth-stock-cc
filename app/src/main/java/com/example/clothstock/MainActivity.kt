package com.example.clothstock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.clothstock.databinding.ActivityMainBinding
import com.example.clothstock.ui.camera.CameraActivity
import com.example.clothstock.ui.gallery.GalleryFragment
import com.example.clothstock.ui.tagging.TaggingActivity

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
        private const val STATE_CURRENT_FRAGMENT = "current_fragment"
        private const val FRAGMENT_NONE = "none"
        private const val FRAGMENT_GALLERY = "gallery"
    }
    
    private lateinit var binding: ActivityMainBinding
    private var currentFragmentType: String = FRAGMENT_NONE
    
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
        
        // 状態復元
        restoreState(savedInstanceState)
        
        // ナビゲーションの初期化
        setupNavigation()
        
        // バックボタン処理の設定
        setupBackPressedCallback()
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
            showGalleryFragment()
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
        
        // 撮影された画像をTaggingActivityに渡してタグ編集画面に遷移
        val intent = Intent(this, TaggingActivity::class.java).apply {
            putExtra(TaggingActivity.EXTRA_IMAGE_URI, imageUri.toString())
            putExtra(TaggingActivity.EXTRA_EDIT_MODE, false)
        }
        startActivity(intent)
    }
    
    /**
     * ギャラリーFragmentを表示
     */
    private fun showGalleryFragment() {
        currentFragmentType = FRAGMENT_GALLERY
        
        // フェードアウトアニメーション付きでメインボタンを非表示
        binding.buttonCamera.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.buttonCamera.visibility = View.GONE
            }
            .start()
            
        binding.buttonGallery.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.buttonGallery.visibility = View.GONE
                
                // FragmentContainerをフェードイン表示
                binding.fragmentContainer.visibility = View.VISIBLE
                binding.fragmentContainer.alpha = 0f
                binding.fragmentContainer.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .start()
            }
            .start()
        
        // GalleryFragmentを表示（アニメーション付き）
        val fragment = GalleryFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(binding.fragmentContainer.id, fragment, FRAGMENT_GALLERY)
            .commit()
    }
    
    /**
     * メイン画面を表示（Fragment非表示）
     */
    private fun showMainScreen() {
        currentFragmentType = FRAGMENT_NONE
        
        // FragmentContainerをフェードアウト
        binding.fragmentContainer.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.fragmentContainer.visibility = View.GONE
                
                // メインボタンをフェードイン表示
                binding.buttonCamera.visibility = View.VISIBLE
                binding.buttonGallery.visibility = View.VISIBLE
                binding.buttonCamera.alpha = 0f
                binding.buttonGallery.alpha = 0f
                
                binding.buttonCamera.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .start()
                    
                binding.buttonGallery.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .start()
            }
            .start()
        
        // Fragmentを削除（アニメーション付き）
        val fragment = supportFragmentManager.findFragmentByTag(FRAGMENT_GALLERY)
        fragment?.let {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .remove(it)
                .commit()
        }
    }
    
    /**
     * バックボタン処理の設定
     */
    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentFragmentType) {
                    FRAGMENT_GALLERY -> {
                        // ギャラリー表示中はメイン画面に戻る
                        showMainScreen()
                    }
                    else -> {
                        // メイン画面では通常のバック処理
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }
    
    /**
     * 状態復元
     */
    private fun restoreState(savedInstanceState: Bundle?) {
        savedInstanceState?.let { bundle ->
            currentFragmentType = bundle.getString(STATE_CURRENT_FRAGMENT, FRAGMENT_NONE)
            
            // 状態に応じて画面復元
            when (currentFragmentType) {
                FRAGMENT_GALLERY -> {
                    // FragmentContainerを表示状態にする
                    binding.fragmentContainer.visibility = View.VISIBLE
                    binding.buttonCamera.visibility = View.GONE
                    binding.buttonGallery.visibility = View.GONE
                }
                else -> {
                    showMainScreen()
                }
            }
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CURRENT_FRAGMENT, currentFragmentType)
    }
}