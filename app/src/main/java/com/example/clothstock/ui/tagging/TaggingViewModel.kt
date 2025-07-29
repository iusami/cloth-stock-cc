package com.example.clothstock.ui.tagging

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.clothstock.R
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.TagData
import com.example.clothstock.data.model.ValidationResult
import com.example.clothstock.data.repository.ClothRepository
import kotlinx.coroutines.launch
import java.util.Date

/**
 * タグ編集画面のViewModel
 * 
 * タグデータの管理、バリデーション、保存機能を提供
 * TDDアプローチに従って実装され、テストカバレッジの高い設計
 */
class TaggingViewModel(
    application: Application,
    private val clothRepository: ClothRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "TaggingViewModel"
    }

    // ===== LiveData定義 =====

    private val _tagData = MutableLiveData<TagData>()
    val tagData: LiveData<TagData> = _tagData

    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveResult = MutableLiveData<SaveResult>()
    val saveResult: LiveData<SaveResult> = _saveResult
    
    // 編集モード用のLiveData
    private val _clothItem = MutableLiveData<ClothItem?>()
    val clothItem: LiveData<ClothItem?> = _clothItem
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    // 編集モードフラグ
    private var isEditMode: Boolean = false
    private var editingItemId: Long = -1L
    
    // 変更追跡用のフラグ
    private val _hasUnsavedChanges = MutableLiveData<Boolean>(false)
    val hasUnsavedChanges: LiveData<Boolean> = _hasUnsavedChanges
    
    // 元のデータの保持（編集モード時）
    private var originalTagData: TagData? = null

    // ===== 初期化 =====

    init {
        // デフォルト値で初期化
        _tagData.value = TagData.createDefault()
        _validationError.value = null
        _isLoading.value = false
    }

    // ===== パブリックメソッド =====

    /**
     * サイズを更新
     * 
     * @param size 新しいサイズ
     */
    fun updateSize(size: Int) {
        updateTagDataSafely { it.withSize(size) }
    }

    /**
     * 色を更新
     * 
     * @param color 新しい色
     */
    fun updateColor(color: String) {
        updateTagDataSafely { it.withColor(color) }
    }

    /**
     * カテゴリを更新
     * 
     * @param category 新しいカテゴリ
     */
    fun updateCategory(category: String) {
        updateTagDataSafely { it.withCategory(category) }
    }

    /**
     * デフォルト状態にリセット
     */
    fun resetToDefault() {
        _tagData.value = TagData.createDefault()
        _validationError.value = null
        _hasUnsavedChanges.value = false
    }
    
    /**
     * 元のデータに戻す（編集モード時のリセット）
     */
    fun revertToOriginal() {
        if (isEditMode) {
            originalTagData?.let { original ->
                _tagData.value = original
                _hasUnsavedChanges.value = false
                _validationError.value = null
            }
        }
    }

    /**
     * 編集モードを設定
     * 
     * @param clothItemId 編集するアイテムのID
     */
    fun setEditMode(clothItemId: Long) {
        isEditMode = true
        editingItemId = clothItemId
        _hasUnsavedChanges.value = false
        loadClothItem(clothItemId)
    }
    
    /**
     * 既存のアイテムを読み込み
     * 
     * @param clothItemId 読み込むアイテムのID
     */
    private fun loadClothItem(clothItemId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val item = clothRepository.getItemById(clothItemId)
                
                if (item != null) {
                    _clothItem.value = item
                    _tagData.value = item.tagData
                    originalTagData = item.tagData // 元データを保存
                    _hasUnsavedChanges.value = false
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = getApplication<Application>().getString(R.string.error_item_not_found)
                }
            } catch (e: Exception) {
                _errorMessage.value = getApplication<Application>().getString(R.string.error_data_load_failed, e.message ?: "")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * タグ付きアイテムを保存
     * 
     * @param imagePath 画像ファイルのパス
     */
    fun saveTaggedItem(imagePath: String?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                if (isEditMode) {
                    // 編集モード: 既存アイテムを更新
                    updateExistingItem()
                } else {
                    // 新規作成モード: 新しいアイテムを保存
                    saveNewItem(imagePath)
                }

            } catch (e: Exception) {
                val (errorType, isRetryable) = categorizeException(e)
                _saveResult.value = SaveResult.Error(
                    message = e.message ?: getApplication<Application>().getString(R.string.error_unknown),
                    errorType = errorType,
                    isRetryable = isRetryable
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 新しいアイテムを保存
     * 
     * @param imagePath 画像ファイルのパス
     */
    private suspend fun saveNewItem(imagePath: String?) {
        Log.d(TAG, "saveNewItem started - imagePath: $imagePath")
        
        // 事前バリデーション
        val validationError = validateSaveRequest(imagePath)
        if (validationError != null) {
            Log.e(TAG, "Validation error: $validationError")
            _saveResult.value = SaveResult.Error(validationError)
            return
        }

        // 保存処理実行
        val currentTagData = _tagData.value ?: TagData.createDefault()
        Log.d(TAG, "Current tagData: size=${currentTagData.size}, color='${currentTagData.color}', category='${currentTagData.category}'")
        
        try {
            val clothItem = createClothItem(imagePath!!, currentTagData)
            Log.d(TAG, "ClothItem created successfully: id=${clothItem.id}, imagePath='${clothItem.imagePath}', tagData=${clothItem.tagData}")
            
            Log.d(TAG, "Calling repository.insertItem...")
            val insertedId = clothRepository.insertItem(clothItem)
            Log.d(TAG, "Repository.insertItem returned: $insertedId")
            
            _saveResult.value = if (insertedId > 0) {
                SaveResult.Success(insertedId)
            } else {
                SaveResult.Error(getApplication<Application>().getString(R.string.error_save_failed))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveNewItem: ${e.message}", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            throw e // Re-throw to be caught by parent try-catch
        }
    }
    
    /**
     * 既存アイテムを更新
     */
    private suspend fun updateExistingItem() {
        val currentItem = _clothItem.value
        if (currentItem == null) {
            _saveResult.value = SaveResult.Error(getApplication<Application>().getString(R.string.error_update_target_not_found))
            return
        }
        
        val currentTagData = _tagData.value ?: TagData.createDefault()
        val validationResult = currentTagData.validate()
        if (validationResult.isError()) {
            _saveResult.value = SaveResult.Error(validationResult.getErrorMessage() ?: getApplication<Application>().getString(R.string.error_validation))
            return
        }
        
        // 更新用のClothItemを作成
        val updatedItem = currentItem.copy(
            tagData = currentTagData
        )
        
        val updateResult = clothRepository.updateItem(updatedItem)
        _saveResult.value = if (updateResult) {
            SaveResult.Success(editingItemId)
        } else {
            SaveResult.Error(getApplication<Application>().getString(R.string.error_update_failed))
        }
    }

    /**
     * 保存リクエストのバリデーション
     * 
     * @param imagePath 画像パス
     * @return エラーメッセージ（成功時はnull）
     */
    private fun validateSaveRequest(imagePath: String?): String? {
        // 編集モードでは画像パスの検証をスキップ
        if (!isEditMode) {
            // 画像パスの検証
            if (imagePath.isNullOrBlank()) {
                return getApplication<Application>().getString(R.string.error_image_path_required)
            }
        }

        // タグデータの検証
        val currentTagData = _tagData.value ?: TagData.createDefault()
        val validationResult = currentTagData.validate()
        
        return if (validationResult.isError()) {
            validationResult.getErrorMessage() ?: getApplication<Application>().getString(R.string.error_validation)
        } else {
            null
        }
    }

    /**
     * ClothItemインスタンスの作成
     * 
     * @param imagePath 画像パス（検証済み）
     * @param tagData タグデータ（検証済み）
     * @return 作成されたClothItem
     */
    private fun createClothItem(imagePath: String, tagData: TagData): ClothItem {
        return ClothItem(
            id = 0, // 新規作成時は0
            imagePath = imagePath,
            tagData = tagData,
            createdAt = Date()
        )
    }

    // ===== プライベートメソッド =====

    /**
     * 現在のタグデータをバリデーション
     * 
     * データ更新時に自動的に呼び出され、UIに即座にエラー状態を反映
     */
    private fun validateCurrentData() {
        val currentData = _tagData.value ?: return
        val validationResult = currentData.validate()
        
        _validationError.value = if (validationResult.isError()) {
            validationResult.getErrorMessage()
        } else {
            null
        }
    }

    /**
     * タグデータの安全な更新
     * 
     * null安全性とバリデーション実行を保証する共通処理
     * 
     * @param updateFunction TagDataを更新する関数
     */
    private fun updateTagDataSafely(updateFunction: (TagData) -> TagData) {
        val currentData = _tagData.value ?: TagData.createDefault()
        val updatedData = updateFunction(currentData)
        _tagData.value = updatedData
        validateCurrentData()
        
        // 編集モードの場合、変更を検出
        if (isEditMode) {
            checkForChanges(updatedData)
        }
    }
    
    /**
     * データの変更を検出してダーティフラグを更新
     * 
     * @param currentData 現在のタグデータ
     */
    private fun checkForChanges(currentData: TagData) {
        val hasChanges = originalTagData?.let { original ->
            original.size != currentData.size ||
            original.color != currentData.color ||
            original.category != currentData.category
        } ?: false
        
        _hasUnsavedChanges.value = hasChanges
    }
    
    /**
     * 変更があるかどうかを確認
     * 
     * @return 未保存の変更がある場合true
     */
    fun hasUnsavedChanges(): Boolean {
        return _hasUnsavedChanges.value ?: false
    }
    
    /**
     * 例外をエラータイプに分類
     * 
     * @param exception 発生した例外
     * @return エラータイプとリトライ可能性のペア
     */
    private fun categorizeException(exception: Exception): Pair<ErrorType, Boolean> {
        return when {
            exception is IllegalArgumentException -> ErrorType.VALIDATION to false
            exception.message?.contains("database", ignoreCase = true) == true -> ErrorType.DATABASE to true
            exception.message?.contains("network", ignoreCase = true) == true -> ErrorType.NETWORK to true
            exception.message?.contains("file", ignoreCase = true) == true -> ErrorType.FILE_SYSTEM to true
            exception is java.io.IOException -> ErrorType.FILE_SYSTEM to true
            exception is java.net.UnknownHostException -> ErrorType.NETWORK to true
            exception is java.sql.SQLException -> ErrorType.DATABASE to true
            else -> ErrorType.UNKNOWN to true
        }
    }

    // ===== 結果クラス =====

    /**
     * 保存結果を表すシールドクラス
     */
    sealed class SaveResult {
        /**
         * 保存成功
         * 
         * @param itemId 保存されたアイテムのID
         */
        data class Success(val itemId: Long) : SaveResult()

        /**
         * 保存失敗
         * 
         * @param message エラーメッセージ
         * @param errorType エラータイプ
         * @param isRetryable リトライ可能かどうか
         */
        data class Error(
            val message: String,
            val errorType: ErrorType = ErrorType.UNKNOWN,
            val isRetryable: Boolean = true
        ) : SaveResult()
    }
    
    /**
     * エラータイプの分類
     */
    enum class ErrorType {
        VALIDATION,    // バリデーションエラー
        DATABASE,      // データベースエラー
        NETWORK,       // ネットワークエラー
        FILE_SYSTEM,   // ファイルシステムエラー
        UNKNOWN        // 不明なエラー
    }
}