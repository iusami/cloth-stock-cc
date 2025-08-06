package com.example.clothstock.ui.gallery

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.example.clothstock.data.model.FilterOptions
import com.example.clothstock.data.model.FilterState
import com.example.clothstock.data.model.FilterType
import com.example.clothstock.databinding.FragmentGalleryBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
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
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up filter UI", e)
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
        fragmentRef.get()?.let { fragment ->
            fragment.viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val filterOptions = viewModel.availableFilterOptions.value
                    if (filterOptions != null) {
                        updateFilterChips(filterOptions)
                        filterBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    } else {
                        showFilterLoadingError()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error showing filter bottom sheet", e)
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
    private fun updateSizeChips(filterOptions: FilterOptions, currentState: FilterState?) {
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
    private fun updateColorChips(filterOptions: FilterOptions, currentState: FilterState?) {
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
    private fun updateCategoryChips(filterOptions: FilterOptions, currentState: FilterState?) {
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
    private fun showFilterError(error: Exception) {
        fragmentRef.get()?.let { fragment ->
            // エラーハンドリングをGalleryErrorHandlerに委譲
        }
    }
    
    /**
     * リソースクリーンアップ
     */
    fun cleanup() {
        fragmentRef.clear()
    }
}