package com.example.clothstock.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.repository.ClothRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * DetailActivity用ViewModel
 * 
 * TDD Greenフェーズ実装
 * ClothItemの詳細データ管理とUI状態制御
 */
class DetailViewModel(
    private val repository: ClothRepository
) : ViewModel() {

    // ClothItemデータ
    private val _clothItem = MutableLiveData<ClothItem?>()
    val clothItem: LiveData<ClothItem?> = _clothItem

    // ローディング状態
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // エラーメッセージ
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 画像読み込み状態
    private val _isImageLoading = MutableLiveData<Boolean>()  
    val isImageLoading: LiveData<Boolean> = _isImageLoading

    // パフォーマンス最適化: コルーチンJob管理
    private var loadingJob: Job? = null

    // パフォーマンス最適化: リトライ機能
    private var retryCount = 0
    private val maxRetryCount = 3

    // パフォーマンス最適化: メモリリーク防止
    override fun onCleared() {
        super.onCleared()
        loadingJob?.cancel()
    }

    /**
     * ClothItemを読み込む（最適化版）
     */
    fun loadClothItem(clothItemId: Long) {
        if (clothItemId <= 0) {
            _errorMessage.value = "無効なアイテムIDです"
            return
        }

        // 既存のジョブをキャンセル（重複リクエスト防止）
        loadingJob?.cancel()
        
        loadingJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                retryCount = 0

                val item = repository.getItemById(clothItemId)
                if (item != null) {
                    _clothItem.value = item
                } else {
                    _errorMessage.value = "アイテムが見つかりません"
                }
                
            } catch (e: Exception) {
                handleLoadingError(e, clothItemId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * パフォーマンス最適化: エラーハンドリングとリトライ機能
     */
    private suspend fun handleLoadingError(exception: Exception, clothItemId: Long) {
        when {
            retryCount < maxRetryCount -> {
                retryCount++
                delay(1000L * retryCount) // 指数バックオフ
                try {
                    val item = repository.getItemById(clothItemId)
                    if (item != null) {
                        _clothItem.value = item
                        return
                    }
                } catch (retryException: Exception) {
                    // リトライも失敗した場合は元のエラーを表示
                }
            }
        }
        
        _errorMessage.value = "データの読み込みに失敗しました: ${exception.message}"
        _clothItem.value = null
    }

    /**
     * データを再読み込み（最適化版）
     */
    fun refreshData() {
        val currentItem = _clothItem.value
        if (currentItem != null) {
            // リトライカウントをリセットして再読み込み
            retryCount = 0
            loadClothItem(currentItem.id)
        }
    }

    /**
     * 画像読み込み開始
     */
    fun onImageLoadStart() {
        _isImageLoading.value = true
    }

    /**
     * 画像読み込み完了
     */
    fun onImageLoadComplete() {
        _isImageLoading.value = false
    }

    /**
     * 画像読み込み失敗
     */
    fun onImageLoadFailed() {
        _isImageLoading.value = false
    }

    /**
     * エラーメッセージをクリア
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * UI状態取得 - データが存在するか
     */
    fun hasData(): Boolean {
        return _clothItem.value != null
    }

    /**
     * UI状態取得 - エラー状態か
     */
    fun hasError(): Boolean {
        return _errorMessage.value != null
    }

    /**
     * フォーマット済み作成日取得
     */
    fun getFormattedCreatedDate(): String {
        val item = _clothItem.value ?: return ""
        return item.getFormattedDate()
    }

    /**
     * タグ情報サマリー取得
     */
    fun getTagSummary(): String {
        val item = _clothItem.value ?: return ""
        return item.getSummary()
    }
}