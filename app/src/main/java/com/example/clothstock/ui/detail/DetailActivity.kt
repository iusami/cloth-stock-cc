package com.example.clothstock.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.clothstock.R
import com.example.clothstock.data.repository.ClothRepositoryImpl
import com.example.clothstock.databinding.ActivityDetailBinding
import com.example.clothstock.ui.tagging.TaggingActivity
import com.google.android.material.snackbar.Snackbar

/**
 * 衣服アイテム詳細表示Activity
 * 
 * TDD Greenフェーズ実装
 * フルサイズ画像とタグ情報を表示、編集機能への遷移
 */
class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CLOTH_ITEM_ID = "extra_cloth_item_id"
        private const val INVALID_CLOTH_ITEM_ID = -1L
    }

    private lateinit var binding: ActivityDetailBinding
    private lateinit var viewModel: DetailViewModel
    private var clothItemId: Long = INVALID_CLOTH_ITEM_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // データバインディング初期化
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)
        binding.lifecycleOwner = this

        // Intent からClothItem IDを取得
        clothItemId = intent.getLongExtra(EXTRA_CLOTH_ITEM_ID, INVALID_CLOTH_ITEM_ID)
        
        // ViewModel初期化
        setupViewModel()
        
        // UI初期化
        setupUI()
        
        // ViewModelの監視設定
        observeViewModel()
        
        // バックプレス処理の設定
        setupBackPressedCallback()
        
        // データ読み込み
        if (clothItemId != INVALID_CLOTH_ITEM_ID) {
            viewModel.loadClothItem(clothItemId)
        } else {
            showError("無効なアイテムIDです")
        }
    }

    /**
     * ViewModelの初期化
     */
    private fun setupViewModel() {
        val repository = ClothRepositoryImpl.getInstance(this)
        val viewModelFactory = DetailViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[DetailViewModel::class.java]
        binding.viewModel = viewModel
    }

    /**
     * UI初期設定
     */
    private fun setupUI() {
        // 戻りボタン
        binding.buttonBack.setOnClickListener {
            finish()
        }
        
        // 編集ボタン
        binding.buttonEdit.setOnClickListener {
            val item = viewModel.clothItem.value
            if (item != null) {
                navigateToTaggingActivity(item.id)
            }
        }
        
        // 再試行ボタン
        binding.buttonRetry.setOnClickListener {
            if (clothItemId != INVALID_CLOTH_ITEM_ID) {
                viewModel.loadClothItem(clothItemId)
            }
        }
    }

    /**
     * ViewModelの監視設定
     */
    private fun observeViewModel() {
        // ClothItemデータの監視
        viewModel.clothItem.observe(this) { clothItem ->
            if (clothItem != null) {
                displayClothItem(clothItem)
                showMainContent()
            }
        }

        // ローディング状態の監視
        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showLoading()
            }
        }

        // エラーメッセージの監視
        viewModel.errorMessage.observe(this) { errorMessage ->
            if (errorMessage != null) {
                showError(errorMessage)
                viewModel.clearErrorMessage()
            }
        }

        // 画像読み込み状態の監視
        viewModel.isImageLoading.observe(this) { isImageLoading ->
            binding.progressBarImage.visibility = if (isImageLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    /**
     * ClothItemを表示（最適化版）
     */
    private fun displayClothItem(clothItem: com.example.clothstock.data.model.ClothItem) {
        // データバインディングでClothItemをセット
        binding.clothItem = clothItem

        // パフォーマンス最適化されたGlide設定
        viewModel.onImageLoadStart()
        
        val requestOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL) // ディスクキャッシュ最適化
            .override(1080, 1920) // メモリ使用量最適化
            .centerCrop()
        
        Glide.with(this)
            .load(clothItem.imagePath)
            .apply(requestOptions)
            .placeholder(R.drawable.ic_photo_placeholder)
            .error(R.drawable.ic_error_photo)
            .transition(DrawableTransitionOptions.withCrossFade(300)) // アニメーション時間最適化
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    viewModel.onImageLoadFailed()
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    viewModel.onImageLoadComplete()
                    // 画像表示成功時のスケールインアニメーション
                    binding.imageViewClothDetail.startAnimation(
                        AnimationUtils.loadAnimation(this@DetailActivity, R.anim.scale_in)
                    )
                    return false
                }
            })
            .into(binding.imageViewClothDetail)

        // タグ情報を表示（アニメーション付き）
        displayTagInformation(clothItem)
    }

    /**
     * タグ情報を表示（アニメーション付き）
     */
    private fun displayTagInformation(clothItem: com.example.clothstock.data.model.ClothItem) {
        binding.textSize.text = "サイズ: ${clothItem.tagData.size}"
        binding.textColor.text = "色: ${clothItem.tagData.color}"
        binding.textCategory.text = "カテゴリ: ${clothItem.tagData.category}"
        binding.textCreatedDate.text = clothItem.getFormattedDate()
        
        // タグ情報表示時のスライドアップアニメーション
        binding.layoutTagInfo.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_up)
        )
    }

    /**
     * メインコンテンツを表示（アニメーション付き）
     */
    private fun showMainContent() {
        binding.imageViewClothDetail.visibility = View.VISIBLE
        binding.layoutTagInfo.visibility = View.VISIBLE
        binding.layoutLoading.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        
        // フェードインアニメーション
        binding.imageViewClothDetail.alpha = 0f
        binding.layoutTagInfo.alpha = 0f
        
        binding.imageViewClothDetail.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
            
        binding.layoutTagInfo.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(150)
            .start()
    }

    /**
     * ローディング状態を表示
     */
    private fun showLoading() {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.imageViewClothDetail.visibility = View.GONE
        binding.layoutTagInfo.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }

    /**
     * エラー状態を表示
     */
    private fun showError(message: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.textErrorMessage.text = message
        binding.imageViewClothDetail.visibility = View.GONE
        binding.layoutTagInfo.visibility = View.GONE
        binding.layoutLoading.visibility = View.GONE
        
        // Snackbarでもエラーを表示
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("再試行") {
                if (clothItemId != INVALID_CLOTH_ITEM_ID) {
                    viewModel.loadClothItem(clothItemId)
                }
            }
            .show()
    }

    /**
     * TaggingActivityへ遷移（編集モード）
     * TODO: 編集モード対応は後のタスクで実装
     */
    private fun navigateToTaggingActivity(clothItemId: Long) {
        // 現在は編集機能未実装のため、情報表示のみ
        val clothItem = viewModel.clothItem.value
        if (clothItem != null) {
            Snackbar.make(binding.root, "編集機能は次のタスクで実装予定です", Snackbar.LENGTH_LONG).show()
        }
    }

    /**
     * バックプレス処理の設定（最新Android推奨方式）
     */
    private fun setupBackPressedCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // DetailActivityからの戻り処理
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // メモリリーク防止: Glideのクリア
        Glide.with(this).clear(binding.imageViewClothDetail)
    }
}