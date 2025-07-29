package com.example.clothstock.ui.common

import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * ローディング状態の統一管理クラス
 * 
 * ViewModelのローディング状態とUI要素の表示/非表示を連携させる
 * TDDアプローチに従って実装され、テスト可能な設計
 */
class LoadingStateManager {

    /**
     * ローディング状態を表すシールドクラス
     */
    sealed class LoadingState {
        object Idle : LoadingState()
        data class Loading(val message: String? = null) : LoadingState()
        object Success : LoadingState()
        data class Error(val message: String, val throwable: Throwable? = null) : LoadingState()
    }

    /**
     * ローディング操作を表すシールドクラス
     */
    sealed class LoadingOperation {
        object CameraCapture : LoadingOperation()
        object SaveItem : LoadingOperation()
        object LoadItem : LoadingOperation()
        object UpdateItem : LoadingOperation()
        object DeleteItem : LoadingOperation()
        object LoadGallery : LoadingOperation()
        data class Custom(val operationName: String) : LoadingOperation()
    }

    /**
     * ローディング管理のビルダークラス
     */
    class Builder {
        private var progressBar: ProgressBar? = null
        private var overlayView: View? = null
        private var contentView: View? = null
        private var loadingTextView: android.widget.TextView? = null

        fun withProgressBar(progressBar: ProgressBar): Builder {
            this.progressBar = progressBar
            return this
        }

        fun withOverlay(overlayView: View): Builder {
            this.overlayView = overlayView
            return this
        }

        fun withContentView(contentView: View): Builder {
            this.contentView = contentView
            return this
        }

        fun withLoadingText(textView: android.widget.TextView): Builder {
            this.loadingTextView = textView
            return this
        }

        fun build(): LoadingStateManager {
            return LoadingStateManager(
                progressBar = progressBar,
                overlayView = overlayView,
                contentView = contentView,
                loadingTextView = loadingTextView
            )
        }
    }

    private val progressBar: ProgressBar?
    private val overlayView: View?
    private val contentView: View?
    private val loadingTextView: android.widget.TextView?

    private constructor(
        progressBar: ProgressBar?,
        overlayView: View?,
        contentView: View?,
        loadingTextView: android.widget.TextView?
    ) {
        this.progressBar = progressBar
        this.overlayView = overlayView
        this.contentView = contentView
        this.loadingTextView = loadingTextView
    }

    /**
     * LiveDataの boolean ローディング状態を監視してUIを更新
     * 
     * @param lifecycleOwner ライフサイクルオーナー
     * @param loadingLiveData ローディング状態のLiveData
     * @param operation ローディング操作の種類
     */
    fun observeLoadingState(
        lifecycleOwner: LifecycleOwner,
        loadingLiveData: LiveData<Boolean>,
        operation: LoadingOperation = LoadingOperation.Custom("処理中")
    ) {
        loadingLiveData.observe(lifecycleOwner, Observer { isLoading ->
            if (isLoading) {
                showLoading(getLoadingMessage(operation))
            } else {
                hideLoading()
            }
        })
    }

    /**
     * LiveDataの LoadingState を監視してUIを更新
     * 
     * @param lifecycleOwner ライフサイクルオーナー
     * @param loadingStateLiveData ローディング状態のLiveData
     */
    fun observeLoadingState(
        lifecycleOwner: LifecycleOwner,
        loadingStateLiveData: LiveData<LoadingState>
    ) {
        loadingStateLiveData.observe(lifecycleOwner, Observer { state ->
            when (state) {
                is LoadingState.Idle -> hideLoading()
                is LoadingState.Loading -> showLoading(state.message ?: "処理中...")
                is LoadingState.Success -> hideLoading()
                is LoadingState.Error -> hideLoading()
            }
        })
    }

    /**
     * ローディング表示を開始
     * 
     * @param message ローディングメッセージ
     */
    fun showLoading(message: String? = null) {
        // プログレスバーを表示
        progressBar?.visibility = View.VISIBLE
        
        // オーバーレイを表示（ユーザー操作を無効化）
        overlayView?.visibility = View.VISIBLE
        
        // コンテンツビューを非表示（任意）
        contentView?.visibility = View.GONE
        
        // ローディングメッセージを設定
        message?.let { msg ->
            loadingTextView?.let { textView ->
                textView.text = msg
                textView.visibility = View.VISIBLE
            }
        }
    }

    /**
     * ローディング表示を終了
     */
    fun hideLoading() {
        // プログレスバーを非表示
        progressBar?.visibility = View.GONE
        
        // オーバーレイを非表示（ユーザー操作を有効化）
        overlayView?.visibility = View.GONE
        
        // コンテンツビューを表示
        contentView?.visibility = View.VISIBLE
        
        // ローディングテキストを非表示
        loadingTextView?.visibility = View.GONE
    }

    /**
     * 操作タイプに応じたローディングメッセージを取得
     * 
     * @param operation ローディング操作
     * @return ローディングメッセージ
     */
    private fun getLoadingMessage(operation: LoadingOperation): String {
        return when (operation) {
            is LoadingOperation.CameraCapture -> "写真を撮影中..."
            is LoadingOperation.SaveItem -> "保存中..."
            is LoadingOperation.LoadItem -> "読み込み中..."
            is LoadingOperation.UpdateItem -> "更新中..."
            is LoadingOperation.DeleteItem -> "削除中..."
            is LoadingOperation.LoadGallery -> "ギャラリーを読み込み中..."
            is LoadingOperation.Custom -> operation.operationName
        }
    }

    companion object {
        /**
         * ビルダーを作成
         */
        fun builder(): Builder {
            return Builder()
        }

        /**
         * シンプルなローディング管理（プログレスバーのみ）
         * 
         * @param progressBar プログレスバー
         * @return LoadingStateManagerインスタンス
         */
        fun simple(progressBar: ProgressBar): LoadingStateManager {
            return builder()
                .withProgressBar(progressBar)
                .build()
        }

        /**
         * オーバーレイ付きローディング管理
         * 
         * @param progressBar プログレスバー
         * @param overlayView オーバーレイビュー
         * @return LoadingStateManagerインスタンス
         */
        fun withOverlay(progressBar: ProgressBar, overlayView: View): LoadingStateManager {
            return builder()
                .withProgressBar(progressBar)
                .withOverlay(overlayView)
                .build()
        }

        /**
         * フルカスタムローディング管理
         * 
         * @param progressBar プログレスバー
         * @param overlayView オーバーレイビュー
         * @param contentView コンテンツビュー
         * @param loadingTextView ローディングテキストビュー
         * @return LoadingStateManagerインスタンス
         */
        fun full(
            progressBar: ProgressBar,
            overlayView: View,
            contentView: View,
            loadingTextView: android.widget.TextView
        ): LoadingStateManager {
            return builder()
                .withProgressBar(progressBar)
                .withOverlay(overlayView)
                .withContentView(contentView)
                .withLoadingText(loadingTextView)
                .build()
        }
    }
}