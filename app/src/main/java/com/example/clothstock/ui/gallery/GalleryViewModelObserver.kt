package com.example.clothstock.ui.gallery

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.example.clothstock.data.model.FilterState

/**
 * ViewModel監視管理クラス
 * 
 * Observer Patternを適用
 * ViewModelの状態監視を一元管理
 */
class GalleryViewModelObserver(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: GalleryViewModel,
    private val adapter: ClothItemAdapter,
    private val animationManager: GalleryAnimationManager,
    private val errorHandler: GalleryErrorHandler
) {
    
    companion object {
        private const val TAG = "GalleryViewModelObserver"
    }
    
    /**
     * 全ての監視を設定
     */
    fun observeAll() {
        observeBasicStates()
        observeFilterAndSearchStates()
    }
    
    /**
     * 基本状態の監視
     */
    private fun observeBasicStates() {
        // 衣服アイテムの監視
        viewModel.clothItems.observe(lifecycleOwner) { items ->
            Log.d(TAG, "clothItems updated: ${items.size} items")
            adapter.submitList(items) {
                if (items.isNotEmpty()) {
                    animationManager.animateRecyclerViewEntry()
                }
            }
        }
        
        // 空状態の監視
        viewModel.isEmpty.observe(lifecycleOwner) { isEmpty ->
            Log.d(TAG, "isEmpty updated: $isEmpty")
            animationManager.animateFilteredStateChange(isEmpty)
        }
        
        // ローディング状態の監視
        viewModel.isLoading.observe(lifecycleOwner) { isLoading ->
            Log.d(TAG, "isLoading updated: $isLoading")
            val shouldShowLoading = isLoading && adapter.itemCount == 0
            animationManager.animateFilterLoadingState(shouldShowLoading)
        }
        
        // エラーメッセージの監視
        viewModel.errorMessage.observe(lifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Log.e(TAG, "Error received: $it")
                errorHandler.showBasicError(it)
                viewModel.clearErrorMessage()
            }
        }
    }
    
    /**
     * フィルター・検索状態の監視
     */
    private fun observeFilterAndSearchStates() {
        // フィルター状態の監視
        viewModel.currentFilters.observe(lifecycleOwner) { filterState ->
            Log.d(TAG, "Filter state updated")
            filterState?.let {
                onFilterStateChanged(it)
            }
        }
        
        // 検索テキストの監視
        viewModel.currentSearchText.observe(lifecycleOwner) { searchText ->
            Log.d(TAG, "Search text updated: '$searchText'")
            onSearchTextChanged(searchText ?: "")
        }
        
        // フィルターアクティブ状態の監視
        viewModel.isFiltersActive.observe(lifecycleOwner) { isActive ->
            Log.d(TAG, "Filter active state: $isActive")
            onFilterActiveStateChanged(isActive)
        }
    }
    
    /**
     * フィルター状態変更時のコールバック
     */
    private fun onFilterStateChanged(@Suppress("UNUSED_PARAMETER") filterState: FilterState) {
        // フィルター状態変更時の処理をここに実装
        // 必要に応じてUIManagerに委譲
    }
    
    /**
     * 検索テキスト変更時のコールバック
     */
    private fun onSearchTextChanged(@Suppress("UNUSED_PARAMETER") searchText: String) {
        // 検索テキスト変更時の処理をここに実装
        // 必要に応じてSearchManagerに委譲
    }
    
    /**
     * フィルターアクティブ状態変更時のコールバック
     */
    private fun onFilterActiveStateChanged(@Suppress("UNUSED_PARAMETER") isActive: Boolean) {
        // フィルターアクティブ状態変更時の処理をここに実装
        // 必要に応じてUIManagerに委譲
    }
}
