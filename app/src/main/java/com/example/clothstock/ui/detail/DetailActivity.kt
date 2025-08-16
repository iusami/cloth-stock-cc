package com.example.clothstock.ui.detail

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.clothstock.BuildConfig
import com.example.clothstock.R
import com.example.clothstock.data.repository.ClothRepositoryImpl
import com.example.clothstock.data.preferences.DetailPreferencesManager
import com.example.clothstock.databinding.ActivityDetailBinding
import com.example.clothstock.ui.tagging.TaggingActivity
import com.example.clothstock.ui.common.MemoInputView
import com.example.clothstock.ui.common.MemoErrorHandler
import com.example.clothstock.ui.common.SwipeableDetailPanel
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
        private const val KEY_PANEL_STATE = "panel_state" // Task 10.3: パネル状態保存用
        
        // Task 10.3: 画面サイズ閾値の定数
        private const val SMALL_SCREEN_WIDTH_DP = 360f
        private const val LARGE_SCREEN_WIDTH_DP = 720f
        
        // フィードバック制御の定数
        private const val FEEDBACK_COOLDOWN_MILLISECONDS = 2000L
    }

    private lateinit var binding: ActivityDetailBinding
    private lateinit var viewModel: DetailViewModel
    private lateinit var memoInputView: MemoInputView
    private lateinit var memoErrorHandler: MemoErrorHandler  // Task 8: メモエラーハンドラー
    private var swipeableDetailPanel: SwipeableDetailPanel? = null // Task 10.2: SwipeableDetailPanel
    private var clothItemId: Long = INVALID_CLOTH_ITEM_ID
    private var shouldFocusMemo: Boolean = false // Task6: メモフォーカス用フラグ
    
    // メモ保存フィードバック制御用
    private var lastFeedbackTime: Long = 0
    private val feedbackCooldownMs = FEEDBACK_COOLDOWN_MILLISECONDS // 2秒間のクールダウン
    
    // Observer実行制御改善用（点滅防止）
    private var lastClothItemHash: Int? = null
    private var lastLoadingState: Boolean? = null
    private var lastErrorMessage: String? = null
    private var lastImageLoadingState: Boolean? = null
    
    // バインディング実行最適化用（点滅防止）
    private var lastBoundClothItem: com.example.clothstock.data.model.ClothItem? = null
    
    // View可視性制御改善用（点滅防止）
    private var currentLayoutState: LayoutState? = null
    
    // 画像点滅防止用（Phase 2追加）
    private var currentImagePath: String? = null
    private var isImageProcessing: Boolean = false
    private var imageViewAnimationInProgress: Boolean = false
    
    /**
     * レイアウト状態の定義
     */
    private enum class LayoutState {
        MAIN_CONTENT,
        LOADING,
        ERROR
    }

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
        
        // Task 10.3: 向き変更時のパネル状態復元
        restorePanelStateIfNeeded(savedInstanceState)
        
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
        val preferencesManager = DetailPreferencesManager(this)
        val viewModelFactory = DetailViewModelFactory(repository, preferencesManager)
        viewModel = ViewModelProvider(this, viewModelFactory)[DetailViewModel::class.java]
        binding.viewModel = viewModel
    }

    /**
     * UI初期設定
     */
    private fun setupUI() {
        // Task 10.2: SwipeableDetailPanelの初期化を試行
        setupSwipeableDetailPanel()
        
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
     * ViewModelの監視設定（Observer実行制御改善版）
     */
    private fun observeViewModel() {
        // ClothItemデータの監視（重複実行防止）
        viewModel.clothItem.observe(this) { clothItem ->
            if (BuildConfig.DEBUG) {
                android.util.Log.d("DetailActivity", "observeViewModel: clothItem changed, clothItem=$clothItem")
            }
            if (clothItem != null) {
                // ハッシュコードで実際の内容変更をチェック（点滅防止）
                val currentHash = clothItem.hashCode()
                if (lastClothItemHash != currentHash) {
                    lastClothItemHash = currentHash
                    displayClothItem(clothItem)
                    showMainContent()
                    
                    // Task6: メモフォーカス処理
                    if (shouldFocusMemo) {
                        focusOnMemoField()
                        shouldFocusMemo = false // 一度だけ実行
                    }
                }
            }
        }

        // ローディング状態の監視（重複実行防止）
        viewModel.isLoading.observe(this) { isLoading ->
            if (BuildConfig.DEBUG) {
                android.util.Log.d("DetailActivity", "observeViewModel: isLoading changed, isLoading=$isLoading")
            }
            // 状態が実際に変更された場合のみ処理（点滅防止）
            if (lastLoadingState != isLoading) {
                lastLoadingState = isLoading
                if (isLoading) {
                    showLoading()
                }
            }
        }

        // エラーメッセージの監視（重複実行防止）
        viewModel.errorMessage.observe(this) { errorMessage ->
            if (BuildConfig.DEBUG) {
                android.util.Log.d(
                    "DetailActivity", 
                    "observeViewModel: errorMessage changed, errorMessage=$errorMessage"
                )
            }
            // エラーメッセージが実際に変更された場合のみ処理（点滅防止）
            if (lastErrorMessage != errorMessage && errorMessage != null) {
                lastErrorMessage = errorMessage
                showError(errorMessage)
                viewModel.clearErrorMessage()
            }
        }

        // 画像読み込み状態の監視（重複実行防止）
        viewModel.isImageLoading.observe(this) { isImageLoading ->
            // 状態が実際に変更された場合のみ処理（点滅防止）
            if (lastImageLoadingState != isImageLoading) {
                lastImageLoadingState = isImageLoading
                binding.progressBarImage.visibility = if (isImageLoading) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
        
        // メモ保存状態の監視
        viewModel.memoSaveState.observe(this) { saveState ->
            handleMemoSaveState(saveState)
        }
        
        // メモ専用LiveDataの監視（点滅防止用）
        viewModel.memoContent.observe(this) { memoContent ->
            // メモのみの更新時は軽量な処理のみ実行（画像処理は完全にバイパス）
            displayMemoInformation(memoContent)
        }
        
        // Task 10.2: パネル状態の監視（SwipeableDetailPanel統合時のみ）
        viewModel.panelState.observe(this) { panelState ->
            handlePanelStateChange(panelState)
        }
    }

    /**
     * ClothItemを表示（最適化版）
     */
    private fun displayClothItem(clothItem: com.example.clothstock.data.model.ClothItem) {
        if (BuildConfig.DEBUG) {
                android.util.Log.d("DetailActivity", "displayClothItem: called with clothItem=$clothItem")
            }
        
        // データバインディング実行最適化：実際に変更された場合のみ更新（点滅防止）
        if (lastBoundClothItem != clothItem) {
            lastBoundClothItem = clothItem
            binding.clothItem = clothItem
        }

        // 画像処理とデータ処理を分離（Phase 2改善）
        displayClothItemImage(clothItem)
        displayClothItemData(clothItem)
    }

    /**
     * ClothItem画像の表示（Phase 2: 点滅防止版）
     */
    private fun displayClothItemImage(clothItem: com.example.clothstock.data.model.ClothItem) {
        val imagePath = clothItem.imagePath
        
        // 同じ画像パスの場合は処理をスキップ（点滅防止）
        if (currentImagePath == imagePath && !isImageProcessing) {
            if (BuildConfig.DEBUG) {
                android.util.Log.d("DetailActivity", "Same image path, skipping image load: $imagePath")
            }
            return
        }
        
        // 画像処理状態を更新
        currentImagePath = imagePath
        isImageProcessing = true
        
        // パフォーマンス最適化されたGlide設定
        viewModel.onImageLoadStart()
        
        // フルサイズ表示用の最適化設定（ハードウェアビットマップ有効）
        val requestOptions = GlideUtils.getFullSizeDisplayOptions()
        
        // Glide設定（アニメーション条件制御・キャッシュ戦略最適化）
        val glideRequest = Glide.with(this)
            .load(imagePath)
            .apply(requestOptions)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // リソースキャッシュ戦略
            .skipMemoryCache(false) // メモリキャッシュを有効化
            .placeholder(R.drawable.ic_photo_placeholder)
            .error(R.drawable.ic_error_photo)
        
        // 初回読み込み時のみクロスフェードアニメーションを実行（点滅防止）
        val finalRequest = if (currentImagePath == null) {
            glideRequest.transition(DrawableTransitionOptions.withCrossFade(300))
        } else {
            // 同じ種類の画像の場合はアニメーションをスキップ
            glideRequest
        }
        
        finalRequest
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.e("DetailActivity", "Glide image load failed", e)
                    isImageProcessing = false // 処理完了
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
                    android.util.Log.d("DetailActivity", "Glide image load success")
                    isImageProcessing = false // 処理完了
                    viewModel.onImageLoadComplete()
                    
                    // 画像表示成功時のスケールインアニメーション（アニメーション重複防止）
                    if (!imageViewAnimationInProgress && isFirstResource) {
                        imageViewAnimationInProgress = true
                        val animation = AnimationUtils.loadAnimation(this@DetailActivity, R.anim.scale_in)
                        animation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                            override fun onAnimationStart(animation: android.view.animation.Animation?) {
                                // アニメーション開始時の処理（必要に応じて実装）
                            }
                            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                                imageViewAnimationInProgress = false
                            }
                            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {
                                // アニメーション繰り返し時の処理（必要に応じて実装）
                            }
                        })
                        binding.imageViewClothDetail.startAnimation(animation)
                    }
                    return false
                }
            })
            .into(binding.imageViewClothDetail)
    }

    /**
     * ClothItemデータの表示（画像以外）
     */
    private fun displayClothItemData(clothItem: com.example.clothstock.data.model.ClothItem) {
        // Task 10.2: SwipeableDetailPanelまたはフォールバックにタグ情報を表示
        displayTagInformation(clothItem)
        
        // メモ情報を表示
        displayMemoInformation(clothItem)
    }

    /**
     * タグ情報を表示（アニメーション付き）
     */
    private fun displayTagInformation(clothItem: com.example.clothstock.data.model.ClothItem) {
        if (swipeableDetailPanel != null) {
            // SwipeableDetailPanel内のTextViewに設定
            try {
                val panel = binding.swipeableDetailPanel
                panel.findViewById<TextView>(R.id.textSize)?.text = "サイズ: ${clothItem.tagData.size}"
                panel.findViewById<TextView>(R.id.textColor)?.text = "色: ${clothItem.tagData.color}"
                panel.findViewById<TextView>(R.id.textCategory)?.text = "カテゴリ: ${clothItem.tagData.category}"
                panel.findViewById<TextView>(R.id.textCreatedDate)?.text = clothItem.getFormattedDate()
            } catch (e: Exception) {
                android.util.Log.w("DetailActivity", "SwipeableDetailPanel内のタグ情報表示でエラー", e)
            }
        } else {
            // フォールバック: 既存のTextViewに設定
            binding.textSize.text = "サイズ: ${clothItem.tagData.size}"
            binding.textColor.text = "色: ${clothItem.tagData.color}"
            binding.textCategory.text = "カテゴリ: ${clothItem.tagData.category}"
            binding.textCreatedDate.text = clothItem.getFormattedDate()
            
            // タグ情報表示時のスライドアップアニメーション
            binding.layoutTagInfo.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.slide_up)
            )
        }
    }

    /**
     * メインコンテンツを表示（アニメーション付き）
     */
    private fun showMainContent() {
        if (BuildConfig.DEBUG) {
                android.util.Log.d("DetailActivity", "showMainContent: called")
            }
        
        // レイアウト状態が既にメインコンテンツの場合は処理をスキップ（点滅防止）
        if (currentLayoutState == LayoutState.MAIN_CONTENT) {
            return
        }
        
        currentLayoutState = LayoutState.MAIN_CONTENT
        
        binding.imageViewClothDetail.visibility = View.VISIBLE
        
        // Task 10.2: SwipeableDetailPanelまたはフォールバック表示
        showDetailPanel()
        
        binding.layoutLoading.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        
        // フェードインアニメーション
        binding.imageViewClothDetail.alpha = 0f
        
        binding.imageViewClothDetail.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    /**
     * ローディング状態を表示
     */
    private fun showLoading() {
        if (BuildConfig.DEBUG) {
                android.util.Log.d("DetailActivity", "showLoading: called")
            }
        
        // レイアウト状態が既にローディングの場合は処理をスキップ（点滅防止）
        if (currentLayoutState == LayoutState.LOADING) {
            return
        }
        
        currentLayoutState = LayoutState.LOADING
        
        binding.layoutLoading.visibility = View.VISIBLE
        binding.imageViewClothDetail.visibility = View.GONE
        binding.layoutTagInfo.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }

    /**
     * エラー状態を表示
     */
    private fun showError(message: String) {
        if (BuildConfig.DEBUG) {
                android.util.Log.d("DetailActivity", "showError: called with message=$message")
            }
        
        // レイアウト状態が既にエラーで、同じメッセージの場合は処理をスキップ（点滅防止）
        if (currentLayoutState == LayoutState.ERROR && binding.textErrorMessage.text.toString() == message) {
            return
        }
        
        currentLayoutState = LayoutState.ERROR
        
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
     * 頻繁な保存による画面点滅を防ぐため、クールダウン期間を設ける
     */
    private fun showMemoSavedFeedback() {
        val currentTime = System.currentTimeMillis()
        
        // 前回のフィードバックから十分な時間が経過した場合のみ表示
        if (currentTime - lastFeedbackTime > feedbackCooldownMs) {
            lastFeedbackTime = currentTime
            
            // 短時間のSnackbarで保存完了を通知
            Snackbar.make(binding.root, "メモを保存しました", Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.buttonEdit) // 編集ボタンの上に表示
                .show()
        }
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
            if (BuildConfig.DEBUG) {
                android.util.Log.d("DetailActivity", "Starting memo field focus sequence")
            }
            
            // MemoInputViewにフォーカスを設定
            memoInputView.requestMemoFocus()
            
            // ViewTreeObserverでレイアウト完了を待ってからキーボードを表示
            val viewTreeObserver = memoInputView.viewTreeObserver
            if (viewTreeObserver.isAlive) {
                val layoutListener = object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        android.util.Log.d("DetailActivity", "Layout completed, attempting to show keyboard")
                        
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
    
    // ===== Task 10.2: SwipeableDetailPanel統合関連メソッド =====
    
    /**
     * SwipeableDetailPanelの初期化
     * Requirements: 2.3, 2.4 - SwipeableDetailPanelとの統合
     */
    private fun setupSwipeableDetailPanel() {
        try {
            android.util.Log.i("DetailActivity", "SwipeableDetailPanel初期化を開始")
            
            swipeableDetailPanel = binding.swipeableDetailPanel
            swipeableDetailPanel?.let { panel ->
                
                // パネル状態変更リスナーの設定
                panel.onPanelStateChangedListener = { panelState ->
                    viewModel.setPanelState(panelState)
                }
                
                // SwipeableDetailPanel内のコンポーネントを初期化
                setupSwipeableDetailPanelComponents(panel)
                
                // Task 10.3: 画面サイズとレイアウト最適化
                optimizeLayoutForScreenSize()
                
                android.util.Log.i("DetailActivity", "SwipeableDetailPanel初期化完了")
            } ?: run {
                // binding.swipeableDetailPanelがnullの場合
                android.util.Log.w("DetailActivity", "binding.swipeableDetailPanelがnullです。レイアウトファイルを確認してください。")
                swipeableDetailPanel = null
            }
        } catch (e: Exception) {
            // 初期化失敗時は常にログ出力（DEBUGビルドに関係なく）
            android.util.Log.e("DetailActivity", "SwipeableDetailPanel初期化失敗、フォールバックを使用", e)
            swipeableDetailPanel = null
            
            // 初期化失敗時はフォールバックを確実に有効化
            ensureFallbackLayoutIsAvailable()
        }
    }
    
    /**
     * フォールバックレイアウトが利用可能であることを確実にする
     */
    private fun ensureFallbackLayoutIsAvailable() {
        try {
            // フォールバックレイアウト（layoutTagInfo）が存在することを確認
            val fallbackLayout = binding.layoutTagInfo
            if (fallbackLayout != null) {
                android.util.Log.i("DetailActivity", "フォールバックレイアウト（layoutTagInfo）を使用します")
                
                // フォールバックレイアウトが確実に使えるように準備
                // MemoInputViewの初期化もフォールバック用で行う
                val fallbackMemoInputView = fallbackLayout.findViewById<MemoInputView>(R.id.memoInputView)
                if (fallbackMemoInputView != null && !::memoInputView.isInitialized) {
                    memoInputView = fallbackMemoInputView
                    setupMemoInputView()
                    android.util.Log.i("DetailActivity", "フォールバック用MemoInputView初期化完了")
                }
            } else {
                android.util.Log.e("DetailActivity", "フォールバックレイアウト（layoutTagInfo）も見つかりません！レイアウトファイルに重大な問題があります。")
            }
        } catch (e: Exception) {
            android.util.Log.e("DetailActivity", "フォールバックレイアウトの初期化に失敗", e)
        }
    }
    
    /**
     * SwipeableDetailPanel内のコンポーネントを初期化
     */
    private fun setupSwipeableDetailPanelComponents(panel: SwipeableDetailPanel) {
        try {
            if (BuildConfig.DEBUG) {
                android.util.Log.d("DetailActivity", "SwipeableDetailPanel内コンポーネント初期化開始")
            }
            
            // SwipeableDetailPanel内のMemoInputViewを取得
            val panelMemoInputView = panel.findViewById<MemoInputView>(R.id.memoInputView)
            panelMemoInputView?.let { memoView ->
                // メモ変更リスナーを設定
                memoView.setOnMemoChangedListener { memo ->
                    viewModel.onMemoChanged(memo)
                }
                
                // SwipeableDetailPanel内のMemoInputViewを使用
                memoInputView = memoView
                
                // プログラマティック背景強制：確実に白色背景を設定
                forceProgrammaticBackgrounds(panel)
                
                android.util.Log.i("DetailActivity", "SwipeableDetailPanel内のMemoInputView初期化完了")
            } ?: run {
                android.util.Log.w("DetailActivity", "SwipeableDetailPanel内にMemoInputViewが見つかりません")
                // MemoInputViewが見つからない場合の対処
                if (!::memoInputView.isInitialized) {
                    // フォールバック用のMemoInputViewを使用
                    val fallbackMemoInputView = binding.layoutTagInfo.findViewById<MemoInputView>(R.id.memoInputView)
                    fallbackMemoInputView?.let { fallbackMemo ->
                        memoInputView = fallbackMemo
                        android.util.Log.i("DetailActivity", "フォールバック用MemoInputViewを使用")
                    }
                }
            }
            
            if (BuildConfig.DEBUG) {
                android.util.Log.d("DetailActivity", "SwipeableDetailPanel内コンポーネント初期化完了")
            }
        } catch (e: Exception) {
            android.util.Log.e("DetailActivity", "SwipeableDetailPanel内コンポーネント初期化失敗", e)
            
            // 初期化失敗時もMemoInputViewだけは確保する
            if (!::memoInputView.isInitialized) {
                try {
                    val fallbackMemoInputView = binding.layoutTagInfo.findViewById<MemoInputView>(R.id.memoInputView)
                    fallbackMemoInputView?.let { fallbackMemo ->
                        memoInputView = fallbackMemo
                        setupMemoInputView()
                        android.util.Log.i("DetailActivity", "エラー回復: フォールバック用MemoInputView初期化完了")
                    }
                } catch (fallbackException: Exception) {
                    android.util.Log.e("DetailActivity", "フォールバック用MemoInputView初期化も失敗", fallbackException)
                }
            }
        }
    }
    
    /**
     * プログラマティック背景強制設定
     * XMLで設定した背景が確実に適用されることを保証
     */
    private fun forceProgrammaticBackgrounds(panel: com.example.clothstock.ui.common.SwipeableDetailPanel) {
        try {
            android.util.Log.i("DetailActivity", "プログラマティック背景強制開始")
            
            // contentContainerの背景強制
            val contentContainer = panel.findViewById<android.widget.LinearLayout>(R.id.contentContainer)
            contentContainer?.setBackgroundColor(android.graphics.Color.WHITE)
            
            // tagInfoContainerの背景強制
            val tagInfoContainer = panel.findViewById<android.widget.LinearLayout>(R.id.tagInfoContainer)
            tagInfoContainer?.setBackgroundColor(android.graphics.Color.WHITE)
            
            // MemoInputViewの背景とalpha強制
            val memoInputView = panel.findViewById<com.example.clothstock.ui.common.MemoInputView>(R.id.memoInputView)
            memoInputView?.let { memo ->
                memo.setBackgroundColor(android.graphics.Color.WHITE)
                memo.alpha = 1.0f // 完全不透明に強制
            }
            
            // 各TextViewの背景強制
            panel.findViewById<android.widget.TextView>(R.id.textSize)
                ?.setBackgroundColor(android.graphics.Color.WHITE)
            panel.findViewById<android.widget.TextView>(R.id.textColor)
                ?.setBackgroundColor(android.graphics.Color.WHITE)
            panel.findViewById<android.widget.TextView>(R.id.textCategory)
                ?.setBackgroundColor(android.graphics.Color.WHITE)
            panel.findViewById<android.widget.TextView>(R.id.textCreatedDate)
                ?.setBackgroundColor(android.graphics.Color.WHITE)
            
            android.util.Log.i("DetailActivity", "プログラマティック背景強制完了")
        } catch (e: Exception) {
            android.util.Log.e("DetailActivity", "プログラマティック背景強制失敗", e)
        }
    }
    
    /**
     * パネル状態変更のハンドリング
     * Requirements: 5.1, 5.2 - パネル状態管理
     */
    private fun handlePanelStateChange(panelState: SwipeableDetailPanel.PanelState) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("DetailActivity", "Panel state changed: $panelState")
        }
        
        // パネル状態に応じたUI調整（現在は最小実装）
        when (panelState) {
            SwipeableDetailPanel.PanelState.SHOWN -> {
                // パネル表示時の処理
            }
            SwipeableDetailPanel.PanelState.HIDDEN -> {
                // パネル非表示時の処理（フルスクリーン画像）
            }
            SwipeableDetailPanel.PanelState.ANIMATING -> {
                // アニメーション中の処理
            }
        }
    }
    
    /**
     * 詳細パネルの表示
     * SwipeableDetailPanelまたはフォールバックレイアウトを表示
     */
    private fun showDetailPanel() {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("DetailActivity", "詳細パネル表示開始 - SwipeableDetailPanel: ${swipeableDetailPanel != null}")
        }
        
        try {
            if (swipeableDetailPanel != null) {
                // SwipeableDetailPanelを使用
                android.util.Log.i("DetailActivity", "SwipeableDetailPanelを表示")
                
                binding.swipeableDetailPanel.visibility = View.VISIBLE
                binding.swipeableDetailPanel.alpha = 0f
                binding.swipeableDetailPanel.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setStartDelay(150)
                    .start()
                
                // フォールバックレイアウトは非表示
                binding.layoutTagInfo.visibility = View.GONE
            } else {
                // フォールバック: 既存のlayoutTagInfoを使用
                android.util.Log.i("DetailActivity", "フォールバックレイアウト（layoutTagInfo）を表示")
                
                // フォールバックレイアウトが存在することを確認
                if (binding.layoutTagInfo != null) {
                    binding.layoutTagInfo.visibility = View.VISIBLE
                    binding.layoutTagInfo.alpha = 0f
                    binding.layoutTagInfo.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setStartDelay(150)
                        .start()
                    
                    // SwipeableDetailPanelは非表示
                    binding.swipeableDetailPanel.visibility = View.GONE
                    
                    android.util.Log.i("DetailActivity", "フォールバックレイアウト表示完了")
                } else {
                    android.util.Log.e("DetailActivity", "フォールバックレイアウトも見つからません！")
                    // 最後の手段：エラーメッセージ表示
                    showError("詳細情報の表示に失敗しました")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DetailActivity", "詳細パネル表示中にエラーが発生", e)
            // エラー時はフォールバックレイアウトを強制表示
            try {
                binding.layoutTagInfo.visibility = View.VISIBLE
                binding.swipeableDetailPanel.visibility = View.GONE
            } catch (fallbackException: Exception) {
                android.util.Log.e("DetailActivity", "フォールバック表示も失敗", fallbackException)
                showError("詳細情報の表示に失敗しました")
            }
        }
    }
    
    /**
     * メモ情報の表示
     * SwipeableDetailPanelまたはフォールバックに対応
     */
    private fun displayMemoInformation(clothItem: com.example.clothstock.data.model.ClothItem) {
        try {
            if (::memoInputView.isInitialized) {
                // 現在のメモと異なる場合のみ更新（無駄な更新を防ぐ）
                val currentMemo = memoInputView.getMemo()
                val newMemo = clothItem.memo ?: ""
                
                if (currentMemo != newMemo) {
                    memoInputView.setMemo(clothItem.memo)
                }
            } else {
                android.util.Log.e("DetailActivity", "memoInputView is not initialized")
            }
        } catch (e: Exception) {
            android.util.Log.e("DetailActivity", "メモ情報表示でエラー", e)
        }
    }

    /**
     * メモ情報のみを表示（軽量版・点滅防止用）
     * 
     * @param memoContent メモ内容
     */
    private fun displayMemoInformation(memoContent: String) {
        try {
            if (::memoInputView.isInitialized) {
                // 現在のメモと異なる場合のみ更新（無駄な更新を防ぐ）
                val currentMemo = memoInputView.getMemo()
                
                if (currentMemo != memoContent) {
                    memoInputView.setMemo(memoContent)
                }
            } else {
                android.util.Log.e("DetailActivity", "memoInputView is not initialized")
            }
        } catch (e: Exception) {
            android.util.Log.e("DetailActivity", "メモ情報表示でエラー（軽量版）", e)
        }
    }


    /**
     * バインディング状態をリセット（画面復帰時など）
     */
    private fun resetBindingCache() {
        lastBoundClothItem = null
        lastClothItemHash = null
        lastLoadingState = null
        lastErrorMessage = null
        lastImageLoadingState = null
        currentLayoutState = null // View可視性制御のキャッシュもリセット
        
        // Phase 2: 画像点滅防止キャッシュもリセット
        currentImagePath = null
        isImageProcessing = false
        imageViewAnimationInProgress = false
    }
    
    override fun onResume() {
        super.onResume()
        // 画面復帰時はバインディングキャッシュをリセット（状態の整合性確保）
        resetBindingCache()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 向き変更時もバインディングキャッシュをリセット
        resetBindingCache()
        
        // Task 10.3: 向き変更時のレイアウト再最適化
        optimizeLayoutForScreenSize()
        
        if (BuildConfig.DEBUG) {
            val orientation = if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                "landscape" 
            } else { 
                "portrait" 
            }
            android.util.Log.d("DetailActivity", "Configuration changed to: $orientation")
        }
    }
    
    // ===== Task 10.3: レイアウト最適化とリファクタリング =====
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        
        // Task 10.3: パネル状態の保存
        swipeableDetailPanel?.let { panel ->
            outState.putString(KEY_PANEL_STATE, panel.getPanelState().name)
        }
    }
    
    /**
     * パネル状態の復元
     * Requirements: 4.2, 4.3 - 向き変更時の状態保持
     */
    private fun restorePanelStateIfNeeded(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return
        
        val panelStateName = savedInstanceState.getString(KEY_PANEL_STATE) ?: return
        
        try {
            val panelState = SwipeableDetailPanel.PanelState.valueOf(panelStateName)
            swipeableDetailPanel?.setPanelState(panelState, notifyListener = false)
            
            if (BuildConfig.DEBUG) {
                android.util.Log.d("DetailActivity", "Panel state restored: $panelState")
            }
        } catch (e: IllegalArgumentException) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w("DetailActivity", "Invalid panel state: $panelStateName", e)
            }
        }
    }
    
    /**
     * 画面サイズに応じたレイアウト最適化
     * Requirements: 4.2, 4.3, 5.3 - 異なる画面サイズ対応
     */
    private fun optimizeLayoutForScreenSize() {
        try {
            val displayMetrics = resources.displayMetrics
            val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
            val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density
            
            swipeableDetailPanel?.let { panel ->
                // 画面サイズに応じたスワイプ閾値調整
                when {
                    screenWidthDp < SMALL_SCREEN_WIDTH_DP -> {
                        // 小画面: スワイプ閾値を緩くする
                        adjustSwipeThresholdForSmallScreen()
                    }
                    screenWidthDp > LARGE_SCREEN_WIDTH_DP -> {
                        // 大画面: スワイプ閾値を厳しくする
                        adjustSwipeThresholdForLargeScreen()
                    }
                    else -> {
                        // 標準画面: デフォルト設定を維持
                    }
                }
                
                // 縦横比による最適化
                optimizeForOrientation(screenWidthDp, screenHeightDp)
            }
            
            if (BuildConfig.DEBUG) {
                android.util.Log.d("DetailActivity", 
                    "Layout optimized for screen: ${screenWidthDp}dp x ${screenHeightDp}dp")
            }
            
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w("DetailActivity", "Failed to optimize layout for screen size", e)
            }
        }
    }
    
    /**
     * 小画面向けスワイプ閾値調整
     */
    private fun adjustSwipeThresholdForSmallScreen() {
        // 小画面では指の移動量が制限されるため、閾値を緩くする
        // NOTE: [Task11]で具体的なスワイプ閾値調整ロジックを実装予定
        if (BuildConfig.DEBUG) {
            android.util.Log.d("DetailActivity", "Adjusted swipe threshold for small screen")
        }
    }
    
    /**
     * 大画面向けスワイプ閾値調整
     */
    private fun adjustSwipeThresholdForLargeScreen() {
        // 大画面では誤操作防止のため、閾値を厳しくする
        // NOTE: [Task11]で具体的なスワイプ閾値調整ロジックを実装予定
        if (BuildConfig.DEBUG) {
            android.util.Log.d("DetailActivity", "Adjusted swipe threshold for large screen")
        }
    }
    
    /**
     * 画面向きに応じた最適化
     * Requirements: 4.2, 4.3 - 縦向き・横向き対応
     */
    private fun optimizeForOrientation(screenWidthDp: Float, screenHeightDp: Float) {
        val isLandscape = screenWidthDp > screenHeightDp
        
        swipeableDetailPanel?.let {
            if (isLandscape) {
                // 横向き: パネル高さを調整してより多くの画像領域を確保
                optimizePanelHeightForLandscape()
            } else {
                // 縦向き: 標準のパネル高さを使用
                optimizePanelHeightForPortrait()
            }
        }
        
        if (BuildConfig.DEBUG) {
            android.util.Log.d("DetailActivity", 
                "Optimized for orientation: ${if (isLandscape) "landscape" else "portrait"}")
        }
    }
    
    /**
     * 横向きレイアウト用のパネル高さ最適化
     */
    private fun optimizePanelHeightForLandscape() {
        // 横向きでは画面の高さが制限されるため、パネルサイズを調整
        // NOTE: [Task11]で具体的なパネル高さ調整ロジックを実装予定
        if (BuildConfig.DEBUG) {
            android.util.Log.d("DetailActivity", "Optimized panel height for landscape")
        }
    }
    
    /**
     * 縦向きレイアウト用のパネル高さ最適化
     */
    private fun optimizePanelHeightForPortrait() {
        // 縦向きでは標準的なパネル高さを使用
        // NOTE: [Task11]で具体的なパネル高さ調整ロジックを実装予定
        if (BuildConfig.DEBUG) {
            android.util.Log.d("DetailActivity", "Optimized panel height for portrait")
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
            if (BuildConfig.DEBUG) {
                android.util.Log.w("DetailActivity", "Error during MemoErrorHandler cleanup", e)
            }
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
            if (BuildConfig.DEBUG) {
                android.util.Log.w("DetailActivity", "Error during ViewTreeObserver cleanup", e)
            }
        }
        
        // Glideは自動的にActivityのライフサイクルを管理するため、
        // 手動でのクリアは不要（むしろクラッシュの原因になる）
        // Glide.with(this)はActivityのライフサイクルに自動的にバインドされているため、
        // Activityがdestroyされると自動的にリクエストもキャンセルされる
    }
}