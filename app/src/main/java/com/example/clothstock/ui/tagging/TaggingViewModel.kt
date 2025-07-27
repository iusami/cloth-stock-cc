package com.example.clothstock.ui.tagging

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val clothRepository: ClothRepository
) : ViewModel() {

    // ===== LiveData定義 =====

    private val _tagData = MutableLiveData<TagData>()
    val tagData: LiveData<TagData> = _tagData

    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveResult = MutableLiveData<SaveResult>()
    val saveResult: LiveData<SaveResult> = _saveResult

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

                // 事前バリデーション
                val validationError = validateSaveRequest(imagePath)
                if (validationError != null) {
                    _saveResult.value = SaveResult.Error(validationError)
                    return@launch
                }

                // 保存処理実行
                val currentTagData = _tagData.value ?: TagData.createDefault()
                val clothItem = createClothItem(imagePath!!, currentTagData)
                val insertedId = clothRepository.insertItem(clothItem)
                
                _saveResult.value = if (insertedId > 0) {
                    SaveResult.Success(insertedId)
                } else {
                    SaveResult.Error("保存に失敗しました")
                }

            } catch (e: Exception) {
                val (errorType, isRetryable) = categorizeException(e)
                _saveResult.value = SaveResult.Error(
                    message = e.message ?: "不明なエラーが発生しました",
                    errorType = errorType,
                    isRetryable = isRetryable
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 保存リクエストのバリデーション
     * 
     * @param imagePath 画像パス
     * @return エラーメッセージ（成功時はnull）
     */
    private fun validateSaveRequest(imagePath: String?): String? {
        // 画像パスの検証
        if (imagePath.isNullOrBlank()) {
            return "画像パスが必要です"
        }

        // タグデータの検証
        val currentTagData = _tagData.value ?: TagData.createDefault()
        val validationResult = currentTagData.validate()
        
        return if (validationResult.isError()) {
            validationResult.getErrorMessage() ?: "バリデーションエラー"
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