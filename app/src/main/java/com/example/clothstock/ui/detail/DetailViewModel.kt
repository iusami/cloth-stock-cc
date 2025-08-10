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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlin.math.pow

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
    
    // メモ保存機能
    private val _memoSaveState = MutableLiveData<MemoSaveState>()
    val memoSaveState: LiveData<MemoSaveState> = _memoSaveState
    
    // メモ保存用のFlowとdebounceによる自動保存
    @OptIn(FlowPreview::class)
    private val memoUpdateFlow = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    private var memoSaveJob: Job? = null
    
    // Task 8: メモ保存リトライ機能
    private var memoSaveRetryCount = 0
    private val maxMemoSaveRetryCount = 3
    
    companion object {
        private const val MEMO_SAVE_DEBOUNCE_MS = 1000L // 1秒のdebounce
        private const val SAVE_SUCCESS_DISPLAY_TIME_MS = 2000L // 保存成功表示時間
        private const val ERROR_DISPLAY_TIME_MS = 5000L // エラー表示時間
    }
    
    /**
     * メモ保存状態を表すsealed class
     * Task 8: リトライ情報を含む拡張
     */
    sealed class MemoSaveState {
        object Idle : MemoSaveState()
        object Saving : MemoSaveState()
        object Saved : MemoSaveState()
        data class Error(val message: String, val canRetry: Boolean = true, val retryCount: Int = 0) : MemoSaveState()
        data class ValidationError(val message: String, val characterCount: Int) : MemoSaveState()
    }

    init {
        // メモ自動保存の初期化
        setupMemoAutoSave()
    }
    
    // パフォーマンス最適化: メモリリーク防止
    override fun onCleared() {
        super.onCleared()
        loadingJob?.cancel()
        memoSaveJob?.cancel()
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
     * 指数バックオフによる負荷制御（1秒→2秒→4秒）
     */
    private suspend fun handleLoadingError(exception: Exception, clothItemId: Long) {
        when {
            retryCount < maxRetryCount -> {
                retryCount++
                // 真の指数バックオフ: 1秒, 2秒, 4秒の遅延
                val delayMs = (1000L * (2.0.pow(retryCount - 1))).toLong()
                delay(delayMs)
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
    
    // ===== メモ関連機能 =====
    
    /**
     * メモ自動保存のセットアップ（debounce機能付き）
     */
    @OptIn(FlowPreview::class)
    private fun setupMemoAutoSave() {
        memoSaveJob = viewModelScope.launch {
            memoUpdateFlow
                .debounce(MEMO_SAVE_DEBOUNCE_MS)
                .collect { memo ->
                    saveMemoInternal(memo)
                }
        }
    }
    
    /**
     * メモ変更時に呼び出される（UIから）
     * Task 8: バリデーション機能を追加
     * debounce機能により、一定時間後に自動保存される
     * 
     * @param memo 新しいメモテキスト
     */
    fun onMemoChanged(memo: String) {
        // Requirements 1.3, 1.4: 文字数制限バリデーション
        if (memo.length > ClothItem.MAX_MEMO_LENGTH) {
            _memoSaveState.value = MemoSaveState.ValidationError(
                "メモは${ClothItem.MAX_MEMO_LENGTH}文字以内で入力してください",
                memo.length
            )
            return
        }
        
        // バリデーション成功時は通常の保存フローへ
        memoUpdateFlow.tryEmit(memo)
    }
    
    /**
     * メモ即座保存（手動保存時）
     * Task 8: バリデーション機能とリトライ機能を追加
     * debounceを待たずに即座にメモを保存する
     * 
     * @param memo 保存するメモテキスト
     */
    fun saveMemoImmediately(memo: String) {
        // Requirements 1.3, 1.4: 文字数制限バリデーション
        if (memo.length > ClothItem.MAX_MEMO_LENGTH) {
            _memoSaveState.value = MemoSaveState.ValidationError(
                "メモは${ClothItem.MAX_MEMO_LENGTH}文字以内で入力してください",
                memo.length
            )
            return
        }
        
        viewModelScope.launch {
            saveMemoInternal(memo)
        }
    }
    
    /**
     * 内部メモ保存処理
     * Task 8: リトライ機能とエラーハンドリング強化
     * Requirements 2.4: メモ更新失敗時のエラー表示と元のメモ保持
     * 
     * @param memo 保存するメモテキスト
     * @param isRetry リトライかどうか
     */
    private suspend fun saveMemoInternal(memo: String, isRetry: Boolean = false) {
        val currentItem = _clothItem.value ?: return
        
        try {
            _memoSaveState.value = MemoSaveState.Saving
            
            // メモを更新した新しいClothItemを作成
            val updatedItem = currentItem.withUpdatedMemo(memo)
            
            // リポジトリに保存
            repository.updateItem(updatedItem)
            
            // 成功時は現在のClothItemも更新
            _clothItem.value = updatedItem
            _memoSaveState.value = MemoSaveState.Saved
            
            // 成功時はリトライカウントをリセット
            memoSaveRetryCount = 0
            
            // 2秒後にIdleに戻す（UIフィードバックのため）
            delay(SAVE_SUCCESS_DISPLAY_TIME_MS)
            _memoSaveState.value = MemoSaveState.Idle
            
        } catch (e: Exception) {
            handleMemoSaveError(e, memo, isRetry)
        }
    }
    
    /**
     * Task 8: メモ保存エラーのハンドリング
     * Requirements 2.4: 元のメモを保持し、リトライ可能性を判定
     */
    @Suppress("UnusedParameter") 
    private suspend fun handleMemoSaveError(e: Exception, memo: String, isRetry: Boolean) {
        val canRetry = memoSaveRetryCount < maxMemoSaveRetryCount
        val errorMessage = when (e) {
            is kotlinx.coroutines.TimeoutCancellationException -> 
                "メモの保存がタイムアウトしました"
            is java.io.IOException -> 
                "データの保存に失敗しました"
            is android.database.sqlite.SQLiteException -> 
                "データベースエラーが発生しました"
            else -> 
                "メモの保存に失敗しました: ${e.message}"
        }
        
        if (!isRetry) {
            memoSaveRetryCount++
        }
        
        _memoSaveState.value = MemoSaveState.Error(
            message = errorMessage,
            canRetry = canRetry,
            retryCount = memoSaveRetryCount
        )
        
        // エラー状態を一定時間維持（UIでリトライボタンを表示するため）
        delay(ERROR_DISPLAY_TIME_MS)
        if (_memoSaveState.value is MemoSaveState.Error) {
            _memoSaveState.value = MemoSaveState.Idle
        }
    }
    
    /**
     * 現在のメモテキストを取得
     * 
     * @return 現在のメモテキスト（アイテムが存在しない場合は空文字列）
     */
    fun getCurrentMemo(): String {
        return _clothItem.value?.memo ?: ""
    }
    
    /**
     * メモ保存状態をリセット
     */
    fun clearMemoSaveState() {
        _memoSaveState.value = MemoSaveState.Idle
        memoSaveRetryCount = 0  // Task 8: リトライカウントもリセット
    }
    
    /**
     * Task 8: メモ保存のリトライ実行
     * Requirements 2.4: リトライ機能の実装
     * 
     * @param memo リトライするメモテキスト
     */
    fun retryMemoSave(memo: String) {
        if (memoSaveRetryCount >= maxMemoSaveRetryCount) {
            _memoSaveState.value = MemoSaveState.Error(
                "最大リトライ回数に達しました",
                canRetry = false,
                retryCount = memoSaveRetryCount
            )
            return
        }
        
        viewModelScope.launch {
            saveMemoInternal(memo, isRetry = true)
        }
    }
    
    /**
     * Task 8: メモバリデーション機能
     * Requirements 1.3: 文字数制限チェック
     * 
     * @param memo チェックするメモテキスト
     * @return バリデーション結果
     */
    fun validateMemo(memo: String): Boolean {
        return memo.length <= ClothItem.MAX_MEMO_LENGTH
    }
}