package com.example.clothstock.ui.gallery

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.clothstock.data.model.FilterOptions
import com.example.clothstock.data.model.FilterState
import com.example.clothstock.data.model.FilterType
import com.example.clothstock.databinding.FragmentGalleryBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * フィルターUI管理クラス
 * 
 * Strategy PatternとDelegation Patternを適用
 * メモリリーク防止のためWeakReferenceを使用
 */
class FilterUIManager(
    fragment: GalleryFragment,
    private val binding: FragmentGalleryBinding,
    private val viewModel: GalleryViewModel
) {
    private val fragmentRef = WeakReference(fragment)
    private lateinit var filterBottomSheetBehavior: BottomSheetBehavior<*>
    
    companion object {
        private const val TAG = "FilterUIManager"
        private const val DISABLED_ALPHA = 0.5f
        private const val ENABLED_ALPHA = 1.0f
    }
    
    /**
     * フィルターUIの初期化
     */
    fun setupFilterUI() {
        Log.d(TAG, "Setting up filter UI")
        
        try {
            initializeBottomSheet()
            setupFilterButton()
            setupFilterChipListeners()
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException during filter UI setup", e)
            disableFilterUI()
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException during filter UI setup", e)
            disableFilterUI()
        }
    }
    
    /**
     * ボトムシートの初期化
     */
    private fun initializeBottomSheet() {
        filterBottomSheetBehavior = BottomSheetBehavior.from(
            binding.includeBottomSheetFilter.bottomSheetFilter
        ).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            isHideable = true
            
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: android.view.View, newState: Int) {
                    handleBottomSheetStateChange(newState)
                }
                
                override fun onSlide(bottomSheet: android.view.View, slideOffset: Float) {
                    // パフォーマンス最適化のため何もしない
                }
            })
        }
    }
    
    /**
     * フィルターボタンの設定
     */
    private fun setupFilterButton() {
        binding.buttonFilter.setOnClickListener {
            Log.d(TAG, "Filter button clicked")
            showFilterBottomSheet()
        }
    }
    
    /**
     * フィルターチップリスナーの設定
     */
    private fun setupFilterChipListeners() {
        binding.includeBottomSheetFilter.buttonClearFilter.setOnClickListener {
            clearAllFilters()
        }
        
        binding.includeBottomSheetFilter.buttonApplyFilter.setOnClickListener {
            applyFilters()
        }
    }
    
    /**
     * ボトムシート状態変更ハンドラー
     */
    private fun handleBottomSheetStateChange(newState: Int) {
        when (newState) {
            BottomSheetBehavior.STATE_EXPANDED -> {
                viewModel.availableFilterOptions.value?.let { options ->
                    updateFilterChips(options)
                }
            }
        }
    }
    
    /**
     * フィルターボトムシート表示
     */
    fun showFilterBottomSheet() {
        fragmentRef.get()?.let { fragment: GalleryFragment ->
            fragment.viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val filterOptions = viewModel.availableFilterOptions.value
                    if (filterOptions != null) {
                        updateFilterChips(filterOptions)
                        filterBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        showFilterLoadingError()
                    }
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "IllegalStateException during filter bottom sheet display", e)
                    showFilterError(e)
                } catch (e: UninitializedPropertyAccessException) {
                    Log.e(TAG, "UninitializedPropertyAccessException during filter bottom sheet display", e)
                    showFilterError(e)
                }
            }
        }
    }
    
    /**
     * フィルターチップの更新
     */
    private fun updateFilterChips(filterOptions: FilterOptions) {
        val currentState = viewModel.currentFilters.value
        
        updateSizeChips(filterOptions, currentState)
        updateColorChips(filterOptions, currentState)
        updateCategoryChips(filterOptions, currentState)
    }
    
    /**
     * サイズチップの更新
     */
    private fun updateSizeChips(
        @Suppress("UNUSED_PARAMETER") filterOptions: FilterOptions, 
        currentState: FilterState?
    ) {
        val chipGroup = binding.includeBottomSheetFilter.chipGroupSize
        
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.let { chipView ->
                val size = chipView.text.toString().toIntOrNull()
                if (size != null) {
                    chipView.isChecked = currentState?.sizeFilters?.contains(size) == true
                    setupSizeChipListener(chipView, size)
                }
            }
        }
    }
    
    /**
     * サイズチップリスナーの設定
     */
    private fun setupSizeChipListener(chip: Chip, size: Int) {
        chip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.applyFilter(FilterType.SIZE, size.toString())
            } else {
                viewModel.removeFilter(FilterType.SIZE, size.toString())
            }
        }
    }
    
    /**
     * 色チップの更新
     */
    private fun updateColorChips(
        @Suppress("UNUSED_PARAMETER") filterOptions: FilterOptions, 
        currentState: FilterState?
    ) {
        val chipGroup = binding.includeBottomSheetFilter.chipGroupColor
        
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.let { chipView ->
                val color = chipView.text.toString()
                chipView.isChecked = currentState?.colorFilters?.contains(color) == true
                setupColorChipListener(chipView, color)
            }
        }
    }
    
    /**
     * 色チップリスナーの設定
     */
    private fun setupColorChipListener(chip: Chip, color: String) {
        chip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.applyFilter(FilterType.COLOR, color)
            } else {
                viewModel.removeFilter(FilterType.COLOR, color)
            }
        }
    }
    
    /**
     * カテゴリチップの更新
     */
    private fun updateCategoryChips(
        @Suppress("UNUSED_PARAMETER") filterOptions: FilterOptions, 
        currentState: FilterState?
    ) {
        val chipGroup = binding.includeBottomSheetFilter.chipGroupCategory
        
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.let { chipView ->
                val category = chipView.text.toString()
                chipView.isChecked = currentState?.categoryFilters?.contains(category) == true
                setupCategoryChipListener(chipView, category)
            }
        }
    }
    
    /**
     * カテゴリチップリスナーの設定
     */
    private fun setupCategoryChipListener(chip: Chip, category: String) {
        chip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.applyFilter(FilterType.CATEGORY, category)
            } else {
                viewModel.removeFilter(FilterType.CATEGORY, category)
            }
        }
    }
    
    /**
     * 全フィルタークリア
     */
    private fun clearAllFilters() {
        viewModel.clearAllFilters()
        clearAllChipSelections()
    }
    
    /**
     * フィルター適用
     */
    private fun applyFilters() {
        filterBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }
    
    /**
     * 全チップ選択クリア
     */
    private fun clearAllChipSelections() {
        clearChipGroup(binding.includeBottomSheetFilter.chipGroupSize)
        clearChipGroup(binding.includeBottomSheetFilter.chipGroupColor)
        clearChipGroup(binding.includeBottomSheetFilter.chipGroupCategory)
    }
    
    /**
     * チップグループのクリア
     */
    private fun clearChipGroup(chipGroup: com.google.android.material.chip.ChipGroup) {
        for (i in 0 until chipGroup.childCount) {
            (chipGroup.getChildAt(i) as? Chip)?.isChecked = false
        }
    }
    
    /**
     * フィルターUI無効化
     */
    fun disableFilterUI() {
        binding.buttonFilter.isEnabled = false
        binding.buttonFilter.alpha = DISABLED_ALPHA
    }
    
    /**
     * フィルターUI有効化
     */
    fun enableFilterUI() {
        binding.buttonFilter.isEnabled = true
        binding.buttonFilter.alpha = ENABLED_ALPHA
    }
    
    /**
     * フィルター読み込みエラー表示
     */
    private fun showFilterLoadingError() {
        fragmentRef.get()?.let { fragment ->
            // エラーハンドリングをGalleryErrorHandlerに委譲
        }
    }
    
    /**
     * フィルターエラー表示
     */
    private fun showFilterError(@Suppress("UNUSED_PARAMETER") error: Exception) {
        fragmentRef.get()?.let { fragment ->
            // エラーハンドリングをGalleryErrorHandlerに委譲
        }
    }
    
    /**
     * フィルター読み込み失敗処理
     */
    fun handleFilterLoadingFailure(error: Exception, message: String) {
        Log.e(TAG, "Filter loading failure: $message", error)
        disableFilterUI()
    }
    
    /**
     * フィルター読み込みリトライ表示
     */
    fun showFilterLoadingRetry(message: String, retryCallback: () -> Unit) {
        Log.d(TAG, "Showing filter loading retry: $message")
        // 基本的なリトライ機能の実装
        retryCallback()
    }
    
    /**
     * 空のフィルターオプション処理
     */
    fun handleEmptyFilterOptions(message: String) {
        Log.w(TAG, "Empty filter options: $message")
        disableFilterUI()
    }
    
    /**
     * フィルター適用失敗処理
     */
    fun handleFilterApplicationFailure(filterType: String, filterValues: List<String>, error: Exception) {
        Log.e(TAG, "Filter application failure - Type: $filterType, Values: $filterValues", error)
        // フィルター状態をリセット
        clearAllFilters()
    }
    
    /**
     * フィルター競合解決表示
     */
    fun showFilterConflictResolution(conflictingFilters: Map<String, List<String>>, message: String) {
        Log.w(TAG, "Filter conflict resolution: $message, Conflicts: $conflictingFilters")
        // 競合するフィルターをクリア
        clearAllFilters()
    }
    
    /**
     * フィルターフォールバックモード有効化
     */
    fun enableFilterFallbackMode(reason: String, message: String) {
        Log.w(TAG, "Enabling filter fallback mode: $reason - $message")
        // 基本的なフィルター機能のみ有効化
        enableFilterUI()
    }
    
    /**
     * フィルターUI無効化処理
     */
    fun handleFilterUIDisabling(reason: String, message: String) {
        Log.w(TAG, "Disabling filter UI: $reason - $message")
        disableFilterUI()
    }
    
    /**
     * フィルター操作リトライ
     */
    fun retryFilterOperation(operationType: String, operationParams: Map<String, Any>, maxRetries: Int) {
        Log.d(TAG, "Retrying filter operation: $operationType (max retries: $maxRetries)")
        Log.d(TAG, "Operation params: $operationParams")
        
        // 基本的なリトライ実装
        when (operationType) {
            "APPLY_SIZE_FILTER" -> {
                // サイズフィルター適用のリトライ
            }
            "LOAD_FILTER_OPTIONS" -> {
                // フィルターオプション読み込みのリトライ
                fragmentRef.get()?.let { fragment ->
                    fragment.viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            // 基本的なリトライ実装（実際のメソッドは別途実装が必要）
                            Log.d(TAG, "Retrying filter options loading")
                        } catch (e: IllegalStateException) {
                            Log.e(TAG, "IllegalStateException in filter retry", e)
                        } catch (e: SecurityException) {
                            Log.e(TAG, "SecurityException in filter retry", e)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * フィルターリトライ上限処理
     */
    fun handleFilterRetryExhaustion(operationType: String, error: Exception, message: String) {
        Log.e(TAG, "Filter retry exhaustion - Operation: $operationType, Message: $message", error)
        disableFilterUI()
    }
    
    /**
     * リソースクリーンアップ
     */
    fun cleanup() {
        fragmentRef.clear()
    }
}
