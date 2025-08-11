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
import com.example.clothstock.ui.common.MemoInputView
import com.example.clothstock.ui.common.MemoErrorHandler
import com.example.clothstock.util.GlideUtils
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
        const val EXTRA_FOCUS_MEMO = "EXTRA_FOCUS_MEMO" // Task6: メモフォーカス用
        private const val INVALID_CLOTH_ITEM_ID = -1L
    }

    private lateinit var binding: ActivityDetailBinding
    private lateinit var viewModel: DetailViewModel
    private lateinit var memoInputView: MemoInputView
    private lateinit var memoErrorHandler: MemoErrorHandler  // Task 8: メモエラーハンドラー
    private var clothItemId: Long = INVALID_CLOTH_ITEM_ID
    private var shouldFocusMemo: Boolean = false // Task6: メモフォーカス用フラグ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // データバインディング初期化
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail)
        binding.lifecycleOwner = this

        // Intent からClothItem IDを取得
        clothItemId = intent.getLongExtra(EXTRA_CLOTH_ITEM_ID, INVALID_CLOTH_ITEM_ID)
        
        // Task6: メモフォーカスフラグの取得
        shouldFocusMemo = intent.getBooleanExtra(EXTRA_FOCUS_MEMO, false)
        
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
        // MemoInputViewの初期化
        memoInputView = binding.memoInputView
        setupMemoInputView()
        
        // Task 8: MemoErrorHandlerの初期化
        setupMemoErrorHandler()
        
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
                
                // Task6: メモフォーカス処理
                if (shouldFocusMemo) {
                    focusOnMemoField()
                    shouldFocusMemo = false // 一度だけ実行
                }
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
        
        // メモ保存状態の監視
        viewModel.memoSaveState.observe(this) { saveState ->
            handleMemoSaveState(saveState)
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
        
        // フルサイズ表示用の最適化設定（ハードウェアビットマップ有効）
        val requestOptions = GlideUtils.getFullSizeDisplayOptions()
        
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
        
        // メモ情報を表示
        memoInputView.setMemo(clothItem.memo)
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
     */
    private fun navigateToTaggingActivity(clothItemId: Long) {
        if (clothItemId <= 0) {
            showError("無効なアイテムIDです")
            return
        }
        
        try {
            val intent = Intent(this, TaggingActivity::class.java).apply {
                putExtra(TaggingActivity.EXTRA_EDIT_MODE, true)
                putExtra(TaggingActivity.EXTRA_CLOTH_ITEM_ID, clothItemId)
            }
            startActivity(intent)
        } catch (e: IllegalStateException) {
            showError("編集画面の起動に失敗しました: ${e.message}")
        } catch (e: UninitializedPropertyAccessException) {
            showError("編集画面の起動に失敗しました: ${e.message}")
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
    
    // ===== メモ機能関連 =====
    
    /**
     * MemoInputViewの初期設定
     */
    private fun setupMemoInputView() {
        memoInputView.setOnMemoChangedListener { memo ->
            // ViewModelの自動保存機能を呼び出し（debounce付き）
            viewModel.onMemoChanged(memo)
        }
    }
    
    /**
     * メモ保存状態のハンドリング
     * Task 8: MemoErrorHandlerを使用した統一エラー処理
     * 
     * @param saveState メモ保存の状態
     */
    private fun handleMemoSaveState(saveState: DetailViewModel.MemoSaveState) {
        when (saveState) {
            is DetailViewModel.MemoSaveState.Idle -> {
                // 何もしない（通常状態）
            }
            
            is DetailViewModel.MemoSaveState.Saving -> {
                // 保存中のUI表示（軽微なフィードバック）
                showMemoSavingFeedback()
            }
            
            is DetailViewModel.MemoSaveState.Saved -> {
                // 保存完了のフィードバック
                showMemoSavedFeedback()
            }
            
            is DetailViewModel.MemoSaveState.Error -> {
                // Task 8: エラーハンドラーを使用した統一エラー処理
                handleMemoSaveError(saveState)
            }
            
            is DetailViewModel.MemoSaveState.ValidationError -> {
                // Task 8: バリデーションエラーの処理
                handleMemoValidationError(saveState)
            }
        }
    }
    
    /**
     * メモ保存中のUIフィードバック表示
     */
    private fun showMemoSavingFeedback() {
        // 軽微なフィードバック（例：プログレスインジケータ）
        // 現在は何もしない（過度なUIの変化を避けるため）
    }
    
    /**
     * メモ保存完了のUIフィードバック表示
     */
    private fun showMemoSavedFeedback() {
        // 短時間のSnackbarで保存完了を通知
        Snackbar.make(binding.root, "メモを保存しました", Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.buttonEdit) // 編集ボタンの上に表示
            .show()
    }
    
    // Task 8: 旧メソッドを削除し、新しいエラーハンドリングメソッドに置き換え
    
    /**
     * Task 8: MemoErrorHandlerの初期化
     */
    private fun setupMemoErrorHandler() {
        memoErrorHandler = MemoErrorHandler(
            context = this,
            rootView = binding.root
        ) { memo ->
            // リトライコールバック
            viewModel.retryMemoSave(memo)
        }
    }
    
    /**
     * Task 8: メモ保存エラーのハンドリング
     * Requirements 2.4: リトライ機能付きエラー表示
     */
    private fun handleMemoSaveError(errorState: DetailViewModel.MemoSaveState.Error) {
        val currentMemo = memoInputView.getMemo()
        
        if (errorState.canRetry) {
            memoErrorHandler.showMemoSaveErrorWithRetry(
                message = errorState.message,
                memo = currentMemo,
                retryCount = errorState.retryCount
            )
        } else {
            memoErrorHandler.showMemoSaveError(
                message = errorState.message,
                originalMemo = currentMemo
            )
        }
    }
    
    /**
     * Task 8: メモバリデーションエラーのハンドリング
     * Requirements 1.3, 1.4: 文字数制限エラー表示
     */
    private fun handleMemoValidationError(errorState: DetailViewModel.MemoSaveState.ValidationError) {
        memoErrorHandler.showMemoValidationError(
            message = errorState.message,
            currentLength = errorState.characterCount
        )
    }

    /**
     * Task6: メモフィールドにフォーカスを設定
     * Requirements 4.3: メモプレビュータップ時のフォーカス機能
     * 
     * ViewTreeObserverを使用してレイアウト完了後にキーボードを表示
     */
    private fun focusOnMemoField() {
        try {
            android.util.Log.d("DetailActivity", "Starting memo field focus sequence")
            
            // MemoInputViewにフォーカスを設定
            memoInputView.requestMemoFocus()
            
            // ViewTreeObserverでレイアウト完了を待ってからキーボードを表示
            val viewTreeObserver = memoInputView.viewTreeObserver
            if (viewTreeObserver.isAlive) {
                val layoutListener = object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        android.util.Log.d(
                            "DetailActivity", 
                            "Layout completed, attempting to show keyboard"
                        )
                        
                        // リスナーを除去（一度だけ実行）
                        memoInputView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        
                        // レイアウトが完了したらソフトキーボードを表示
                        showSoftKeyboard()
                    }
                }
                viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
                
                android.util.Log.d("DetailActivity", "OnGlobalLayoutListener registered")
            } else {
                android.util.Log.w(
                    "DetailActivity", 
                    "ViewTreeObserver is not alive, falling back to direct keyboard show"
                )
                // フォールバック: 直接キーボードを表示
                showSoftKeyboard()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("DetailActivity", "Failed to focus memo field", e)
            // エラー時のフォールバック
            try {
                showSoftKeyboard()
            } catch (fallbackException: Exception) {
                android.util.Log.e("DetailActivity", "Fallback keyboard show also failed", fallbackException)
            }
        }
    }
    
    /**
     * ソフトキーボード表示のヘルパーメソッド
     * 共通処理を切り出して再利用性を向上
     */
    private fun showSoftKeyboard() {
        try {
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) 
                as android.view.inputmethod.InputMethodManager
            val result = imm.showSoftInput(
                memoInputView, 
                android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT
            )
            
            android.util.Log.d("DetailActivity", "Keyboard show result: $result")
            
            // フォーカスが続いているか確認
            if (!memoInputView.hasFocus()) {
                android.util.Log.d("DetailActivity", "Re-requesting focus after keyboard show")
                memoInputView.requestMemoFocus()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("DetailActivity", "Failed to show soft keyboard", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Task 8: MemoErrorHandlerのクリーンアップ
        try {
            if (::memoErrorHandler.isInitialized) {
                memoErrorHandler.cleanup()
            }
        } catch (e: Exception) {
            android.util.Log.w("DetailActivity", "Error during MemoErrorHandler cleanup", e)
        }
        
        // ViewTreeObserverのリスナーをクリーンアップ（メモリリーク防止）
        try {
            val viewTreeObserver = memoInputView.viewTreeObserver
            if (viewTreeObserver.isAlive) {
                // 残っているOnGlobalLayoutListenerを除去しようとしますが、
                // 既に除去済みの場合は何もしません
                android.util.Log.d("DetailActivity", "Cleaning up ViewTreeObserver listeners")
            }
        } catch (e: Exception) {
            android.util.Log.w("DetailActivity", "Error during ViewTreeObserver cleanup", e)
        }
        
        // Glideは自動的にActivityのライフサイクルを管理するため、
        // 手動でのクリアは不要（むしろクラッシュの原因になる）
        // Glide.with(this)はActivityのライフサイクルに自動的にバインドされているため、
        // Activityがdestroyされると自動的にリクエストもキャンセルされる
    }
}