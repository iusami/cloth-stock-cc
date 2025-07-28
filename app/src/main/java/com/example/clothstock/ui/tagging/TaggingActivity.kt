package com.example.clothstock.ui.tagging

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.clothstock.R
import com.example.clothstock.data.model.TagData
import com.example.clothstock.data.repository.ClothRepositoryImpl
import com.example.clothstock.databinding.ActivityTaggingBinding

/**
 * タグ入力画面
 * 
 * 撮影した写真にサイズ、色、カテゴリのタグを付けて保存する
 * TaggingViewModelと連携してMVVMアーキテクチャを実装
 */
class TaggingActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_CLOTH_ITEM_ID = "extra_cloth_item_id"
        const val EXTRA_EDIT_MODE = "extra_edit_mode"
    }
    
    private lateinit var binding: ActivityTaggingBinding
    private lateinit var viewModel: TaggingViewModel
    private var imageUri: Uri? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Data Binding初期化
        binding = DataBindingUtil.setContentView(this, R.layout.activity_tagging)
        binding.lifecycleOwner = this
        
        // ViewModelの初期化（簡易的なDI）
        val repository = ClothRepositoryImpl.getInstance(this)
        val viewModelFactory = TaggingViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[TaggingViewModel::class.java]
        binding.viewModel = viewModel
        
        // 編集モードの検出と初期化
        val isEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)
        val clothItemId = intent.getLongExtra(EXTRA_CLOTH_ITEM_ID, -1L)
        
        if (isEditMode) {
            setupEditMode(clothItemId)
        } else {
            setupNewEntryMode()
        }
        
        // 画像URIの取得と表示
        setupImageDisplay()
        
        // NumberPicker初期化
        setupNumberPicker()
        
        // 入力フィールドの設定
        setupInputFields()
        
        // ボタンの設定
        setupButtons()
        
        // ViewModelの監視
        observeViewModel()
        
        // バックボタンの処理
        setupBackPressedCallback()
    }
    
    /**
     * 編集モードの初期化
     */
    private fun setupEditMode(clothItemId: Long) {
        if (clothItemId <= 0) {
            showError("アイテムが見つかりません")
            finish()
            return
        }
        
        // タイトルを編集モードに変更
        title = "タグを編集"
        
        // ViewModelに編集モードを設定
        viewModel.setEditMode(clothItemId)
    }
    
    /**
     * 新規作成モードの初期化
     */
    private fun setupNewEntryMode() {
        // タイトルはデフォルトのまま
        title = "タグを追加"
    }
    
    /**
     * 画像表示の設定
     */
    private fun setupImageDisplay() {
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val isEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)
        
        if (imageUriString.isNullOrEmpty() && !isEditMode) {
            // 新規作成モードでは画像URIが必須
            showError("画像が見つかりません")
            finish()
            return
        } else if (!imageUriString.isNullOrEmpty()) {
            // 画像URIがある場合は表示
            imageUri = Uri.parse(imageUriString)
            displayImage(imageUri)
        }
        // 編集モードで画像URIがない場合は既存データから読み込み（ViewModelで処理）
    }
    
    /**
     * 画像を表示する
     */
    private fun displayImage(uri: Uri?) {
        
        // Glideを使用して画像を表示（エラーハンドリング・最適化付き）
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .override(800, 600) // メモリ最適化のためリサイズ
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    showError("画像の読み込みに失敗しました")
                    return false
                }
                
                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .into(binding.imageViewCaptured)
    }
    
    /**
     * NumberPickerの初期化
     */
    private fun setupNumberPicker() {
        binding.numberPickerSize.apply {
            minValue = TagData.MIN_SIZE
            maxValue = TagData.MAX_SIZE
            value = TagData.DEFAULT_SIZE
            
            // NumberPickerの値変更監視
            setOnValueChangedListener { _, _, newVal ->
                viewModel.updateSize(newVal)
            }
        }
    }
    
    /**
     * 入力フィールドの設定
     */
    private fun setupInputFields() {
        // 色入力フィールド
        binding.editTextColor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateColor(s?.toString() ?: "")
                // エラー状態をクリア
                if (!s.isNullOrEmpty()) {
                    binding.textInputLayoutColor.error = null
                }
            }
        })
        
        // カテゴリ入力フィールド
        binding.editTextCategory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateCategory(s?.toString() ?: "")
                // エラー状態をクリア
                if (!s.isNullOrEmpty()) {
                    binding.textInputLayoutCategory.error = null
                }
            }
        })
        
        // IMEアクション設定
        binding.editTextColor.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {
                binding.editTextCategory.requestFocus()
                true
            } else {
                false
            }
        }
        
        binding.editTextCategory.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                binding.editTextCategory.clearFocus()
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }
    
    /**
     * ボタンの設定
     */
    private fun setupButtons() {
        // 保存ボタン
        binding.buttonSave.setOnClickListener {
            val imageUriString = imageUri?.toString()
            if (imageUriString != null) {
                viewModel.saveTaggedItem(imageUriString)
            } else {
                showError("画像の保存に失敗しました")
            }
        }
        
        // キャンセルボタン
        binding.buttonCancel.setOnClickListener {
            showCancelConfirmationDialog()
        }
    }
    
    /**
     * ViewModelの監視設定
     */
    private fun observeViewModel() {
        // バリデーションエラーの監視（個別フィールド）
        viewModel.validationError.observe(this) { errorMessage ->
            updateFieldValidationState(errorMessage)
        }
        
        // ローディング状態の監視
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonSave.isEnabled = !isLoading
            binding.buttonCancel.isEnabled = !isLoading
        }
        
        // 保存結果の監視
        viewModel.saveResult.observe(this) { result ->
            when (result) {
                is TaggingViewModel.SaveResult.Success -> {
                    val message = if (intent.getBooleanExtra(EXTRA_EDIT_MODE, false)) "更新しました" else "保存しました"
                    showSuccessMessage(message)
                    setResult(RESULT_OK)
                    finish()
                }
                is TaggingViewModel.SaveResult.Error -> {
                    handleSaveError(result)
                }
            }
        }
        
        // 編集モード用のデータ読み込み監視
        viewModel.clothItem.observe(this) { clothItem ->
            clothItem?.let {
                // フィールドを既存データで事前入力
                binding.editTextColor.setText(it.tagData.color)
                binding.editTextCategory.setText(it.tagData.category)
                binding.numberPickerSize.value = it.tagData.size
                
                // 画像を表示（編集モードで画像URIがない場合）
                if (imageUri == null && !it.imagePath.isNullOrEmpty()) {
                    imageUri = Uri.parse(it.imagePath)
                    displayImage(imageUri)
                }
            }
        }
        
        // エラー処理の監視
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                showError(it)
            }
        }
        
        // 未保存変更の監視
        viewModel.hasUnsavedChanges.observe(this) { hasChanges ->
            // 必要に応じてUI状態を更新（例：保存ボタンの強調表示など）
            // 現在は特に処理なし
        }
    }
    
    /**
     * バックボタン押下時の確認処理
     */
    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showCancelConfirmationDialog()
            }
        })
    }
    
    /**
     * フィールド別バリデーション状態の更新
     */
    private fun updateFieldValidationState(errorMessage: String?) {
        // 全フィールドのエラー状態をクリア
        binding.textInputLayoutColor.error = null
        binding.textInputLayoutCategory.error = null
        binding.textViewError.visibility = View.GONE
        
        errorMessage?.let { message ->
            when {
                message.contains("色") -> {
                    binding.textInputLayoutColor.error = message
                }
                message.contains("カテゴリ") -> {
                    binding.textInputLayoutCategory.error = message
                }
                else -> {
                    // 全般的なエラーは従来通り表示
                    binding.textViewError.text = message
                    binding.textViewError.visibility = View.VISIBLE
                }
            }
        }
    }
    
    /**
     * エラーメッセージの表示
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * 成功メッセージの表示（アニメーション付き）
     */
    private fun showSuccessMessage(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.show()
    }
    
    /**
     * 保存エラーの種類に応じた処理
     */
    private fun handleSaveError(error: TaggingViewModel.SaveResult.Error) {
        when (error.errorType) {
            TaggingViewModel.ErrorType.VALIDATION -> {
                showError("入力内容を確認してください: ${error.message}")
            }
            TaggingViewModel.ErrorType.NETWORK -> {
                if (error.isRetryable) {
                    showRetryDialog("ネットワークエラー", error.message, isNetworkError = true)
                } else {
                    showError("ネットワークエラー: ${error.message}")
                }
            }
            TaggingViewModel.ErrorType.DATABASE -> {
                if (error.isRetryable) {
                    showRetryDialog("データベースエラー", error.message)
                } else {
                    showError("データベースエラー: ${error.message}")
                }
            }
            TaggingViewModel.ErrorType.FILE_SYSTEM -> {
                showRetryDialog("ファイルエラー", error.message)
            }
            TaggingViewModel.ErrorType.UNKNOWN -> {
                if (error.isRetryable) {
                    showRetryDialog("予期しないエラー", error.message)
                } else {
                    showError("予期しないエラー: ${error.message}")
                }
            }
        }
    }
    
    /**
     * リトライダイアログの表示（強化版）
     */
    private fun showRetryDialog(
        title: String,
        message: String,
        isNetworkError: Boolean = false
    ) {
        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("再試行") { _, _ ->
                val imageUriString = imageUri?.toString()
                if (imageUriString != null) {
                    viewModel.saveTaggedItem(imageUriString)
                } else {
                    showError("画像の保存に失敗しました")
                }
            }
            .setNegativeButton("キャンセル", null)
            .setNeutralButton("編集を続ける", null)
        
        if (isNetworkError) {
            builder.setMessage("$message\n\nネットワーク接続を確認してください。")
        }
        
        builder.show()
    }
    
    /**
     * ソフトキーボードを非表示にする
     */
    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val currentFocus = currentFocus
        if (currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }
    
    /**
     * キャンセル確認ダイアログの表示
     */
    private fun showCancelConfirmationDialog() {
        val isEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)
        val hasUnsavedChanges = viewModel.hasUnsavedChanges()
        
        // 変更がない場合は確認なしで終了
        if (!hasUnsavedChanges) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        
        val message = if (isEditMode) "変更を破棄しますか？" else "入力を破棄しますか？"
        val builder = AlertDialog.Builder(this)
            .setTitle("確認")
            .setMessage(message)
            .setPositiveButton("破棄") { _, _ ->
                setResult(RESULT_CANCELED)
                finish()
            }
            .setNegativeButton("続行", null)
        
        // 編集モードの場合は「元に戻す」オプションを追加
        if (isEditMode) {
            builder.setNeutralButton("元に戻す") { _, _ ->
                viewModel.revertToOriginal()
            }
        }
        
        builder.show()
    }
    
    /**
     * フィールド更新アニメーション
     */
    private fun animateFieldUpdate(editText: android.widget.EditText, newText: String) {
        editText.apply {
            // フェードアウト
            animate()
                .alpha(0.3f)
                .setDuration(150)
                .withEndAction {
                    setText(newText)
                    // フェードイン
                    animate()
                        .alpha(1.0f)
                        .setDuration(150)
                        .start()
                }
                .start()
        }
    }
    
    /**
     * 編集モードでの初期フォーカス設定
     */
    private fun setEditModeFocus() {
        // 最初の編集フィールド（色）にフォーカスを当てる
        binding.editTextColor.requestFocus()
        
        // キーボードを表示
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        inputMethodManager.showSoftInput(binding.editTextColor, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
}

/**
 * TaggingViewModel用のViewModelFactory
 */
class TaggingViewModelFactory(
    private val repository: com.example.clothstock.data.repository.ClothRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaggingViewModel::class.java)) {
            return TaggingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}