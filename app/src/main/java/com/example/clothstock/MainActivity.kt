package com.example.clothstock

import android.content.ComponentCallbacks2
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
import com.bumptech.glide.Glide
import com.example.clothstock.databinding.ActivityMainBinding
import com.example.clothstock.ui.camera.CameraActivity
import com.example.clothstock.ui.gallery.GalleryFragment
import com.example.clothstock.ui.tagging.TaggingActivity
import com.example.clothstock.util.MemoryPressureMonitor

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
    private lateinit var memoryPressureMonitor: MemoryPressureMonitor
    
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
        
        // MemoryPressureMonitorの初期化
        memoryPressureMonitor = MemoryPressureMonitor.getInstance(this)
        
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

    /**
     * アクティビティレベルのメモリ圧迫対応 (Android Q+対応)
     * 
     * Applicationレベルの処理と連携してpinning非推奨警告を解決
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        Log.d(TAG, "MainActivity onTrimMemory called with level: $level")
        
        // MemoryPressureMonitorに通知（統合有効化も確認）
        if (memoryPressureMonitor.isSystemIntegrationEnabled()) {
            memoryPressureMonitor.handleSystemTrimMemory(level)
        }
        
        when (level) {
            // UIがバックグラウンドに移行
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                Log.d(TAG, "TRIM_MEMORY_UI_HIDDEN - UI非表示状態でのクリーンアップ")
                performUIHiddenCleanup()
            }
            
            // フォアグラウンド実行中のメモリ圧迫
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                Log.d(TAG, "TRIM_MEMORY_RUNNING_MODERATE - 実行中の中程度メモリクリーンアップ")
                performRunningMemoryCleanup(false)
            }
            
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Log.d(TAG, "TRIM_MEMORY_RUNNING_LOW - 実行中の低メモリクリーンアップ") 
                performRunningMemoryCleanup(true)
            }
            
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.w(TAG, "TRIM_MEMORY_RUNNING_CRITICAL - 実行中のクリティカルメモリクリーンアップ")
                performCriticalMemoryCleanup()
            }
            
            // バックグラウンド状態でのメモリ圧迫
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                Log.d(TAG, "Background trim memory level: $level - バックグラウンドクリーンアップ")
                performBackgroundCleanup()
            }
            
            else -> {
                Log.d(TAG, "Unknown trim memory level: $level")
            }
        }
    }

    /**
     * UI非表示時のクリーンアップ
     */
    private fun performUIHiddenCleanup() {
        Log.d(TAG, "Performing UI hidden cleanup")
        
        try {
            // ギャラリーフラグメントがある場合、画像キャッシュをクリア
            val galleryFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_GALLERY)
            if (galleryFragment != null) {
                // Glideのメモリクリア（UIが非表示なので安全）
                Glide.get(this).clearMemory()
                Log.d(TAG, "Gallery fragment memory cleared")
            }
            
            // MemoryPressureMonitorのキャッシュクリア
            memoryPressureMonitor.getCacheManager().clearCache()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during UI hidden cleanup", e)
        }
    }

    /**
     * 実行中のメモリクリーンアップ
     */
    private fun performRunningMemoryCleanup(isLowMemory: Boolean) {
        Log.d(TAG, "Performing running memory cleanup (low memory: $isLowMemory)")
        
        try {
            // MemoryPressureMonitorに状況を通知
            if (!memoryPressureMonitor.isMonitoring()) {
                memoryPressureMonitor.startMonitoring()
                Log.d(TAG, "Started MemoryPressureMonitor")
            }
            
            // 低メモリ状況の場合は追加のクリーンアップ
            if (isLowMemory) {
                memoryPressureMonitor.getCacheManager().clearCache()
                
                // ギャラリー表示中の場合、一部の画像キャッシュをクリア
                val galleryFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_GALLERY)
                if (galleryFragment != null) {
                    // UIスレッドで安全にGlideメモリクリア
                    runOnUiThread {
                        Glide.get(this).clearMemory()
                        Log.d(TAG, "Cleared Glide memory during low memory situation")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during running memory cleanup", e)
        }
    }

    /**
     * クリティカルメモリクリーンアップ
     */
    private fun performCriticalMemoryCleanup() {
        Log.w(TAG, "Performing critical memory cleanup")
        
        try {
            // 即座にメモリクリーンアップを実行
            performRunningMemoryCleanup(true)
            
            // 緊急時のシステムGC実行
            System.gc()
            
            // MemoryPressureMonitorを緊急モードで開始
            if (!memoryPressureMonitor.isMonitoring()) {
                memoryPressureMonitor.startMonitoring()
            }
            
            Log.w(TAG, "Critical memory cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during critical memory cleanup", e)
        }
    }

    /**
     * バックグラウンド状態でのクリーンアップ
     */
    private fun performBackgroundCleanup() {
        Log.d(TAG, "Performing background cleanup")
        
        try {
            // バックグラウンドなので安全に包括的クリーンアップが可能
            Glide.get(this).clearMemory()
            memoryPressureMonitor.getCacheManager().clearCache()
            
            // バックグラウンドディスクキャッシュクリアも実行
            Thread {
                try {
                    Glide.get(this).clearDiskCache()
                    Log.d(TAG, "Background disk cache cleared")
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing disk cache", e)
                }
            }.start()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during background cleanup", e)
        }
    }
}