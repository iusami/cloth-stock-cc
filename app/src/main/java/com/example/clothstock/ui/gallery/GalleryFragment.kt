package com.example.clothstock.ui.gallery

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clothstock.R
import com.example.clothstock.data.repository.ClothRepositoryImpl
import com.example.clothstock.databinding.FragmentGalleryBinding
import com.example.clothstock.ui.camera.CameraActivity
import com.example.clothstock.ui.detail.DetailActivity
import com.example.clothstock.data.model.FilterState
import com.google.android.material.snackbar.Snackbar
import android.util.Log
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.example.clothstock.data.model.FilterOptions
import com.example.clothstock.data.model.FilterType
import androidx.core.view.children
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import com.example.clothstock.accessibility.AccessibilityHelper

/**
 * ギャラリー画面Fragment
 * 
 * TDD Greenフェーズ実装
 * 衣服写真をグリッド表示、空状態、エラーハンドリング
 */
class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: GalleryViewModel
    private lateinit var adapter: ClothItemAdapter
    
    // Task7: フィルター機能用プロパティ
    private lateinit var filterBottomSheetBehavior: BottomSheetBehavior<*>
    
    // Task8: 検索機能用プロパティ
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupSwipeRefresh()
        setupEmptyStateActions()
        setupFab()
        setupFilterUI() // Task7: フィルターUI初期化
        setupSearchBar() // Task8: 検索バー初期化
        setupAccessibility() // Task14: アクセシビリティ設定
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        
        // Task8 REFACTOR: 検索ジョブのキャンセル（メモリリーク防止）
        searchJob?.cancel()
        searchJob = null
        Log.d(TAG, "Search job cancelled and cleared")
        
        // Android Q+ pinning非推奨対応: メモリリークの防止
        try {
            // Glideの関連付けをクリア（pinning回避）
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // RecyclerViewとAdapterの関連付けをクリア
                binding.recyclerViewGallery.adapter = null
                Log.d(TAG, "RecyclerView adapter cleared for Android Q+")
            }
            
            // bindingの解放
            _binding = null
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException during onDestroyView cleanup", e)
            _binding = null
        } catch (e: android.view.InflateException) {
            Log.e(TAG, "InflateException during onDestroyView cleanup", e)
            _binding = null
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
        
        // Android Q+ pinning非推奨対応: 一時的なメモリクリーンアップ
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Glideの一時的なメモリクリア（pinning回避）
                com.bumptech.glide.Glide.with(this).pauseRequests()
                Log.d(TAG, "Glide requests paused for Android Q+")
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException during onPause cleanup", e)
        } catch (e: android.view.InflateException) {
            Log.e(TAG, "InflateException during onPause cleanup", e)
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        
        // Android Q+ pinning非推奨対応: リクエスト再開
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Glideリクエストの再開
                com.bumptech.glide.Glide.with(this).resumeRequests()
                Log.d(TAG, "Glide requests resumed for Android Q+")
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException during onResume cleanup", e)
        } catch (e: android.view.InflateException) {
            Log.e(TAG, "InflateException during onResume cleanup", e)
        }
    }

    /**
     * ViewModelの初期化
     */
    private fun setupViewModel() {
        val repository = ClothRepositoryImpl.getInstance(requireContext())
        val filterManager = com.example.clothstock.data.repository.FilterManager()
        val viewModelFactory = GalleryViewModelFactory(repository, filterManager)
        viewModel = ViewModelProvider(this, viewModelFactory)[GalleryViewModel::class.java]
        binding.viewModel = viewModel
    }

    /**
     * RecyclerViewの設定（Task9フィルター結果対応最適化済み）
     */
    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView with filter support (Task9 enhanced)")
        
        // GridLayoutManagerを設定（列数は画面サイズに応じて調整）
        val spanCount = calculateSpanCount()
        val layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.recyclerViewGallery.layoutManager = layoutManager

        // Task9: フィルター結果対応RecyclerViewパフォーマンス最適化
        binding.recyclerViewGallery.apply {
            setHasFixedSize(true) // サイズ固定でパフォーマンス向上
            
            // Task9: フィルター結果変更に対応したアニメーション設定
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = FILTER_ANIMATION_DURATION // フィルター結果追加時
                removeDuration = FILTER_TRANSITION_DURATION // フィルター結果削除時
                moveDuration = FILTER_TRANSITION_DURATION // フィルター結果移動時
                changeDuration = FILTER_ANIMATION_DURATION // フィルター結果変更時
                
                // フィルター操作時のスムーズなアニメーション設定
                supportsChangeAnimations = true // アイテム変更アニメーション有効化
            }
            
            // Task9: フィルター結果に対応したViewHolderプールサイズの動的最適化
            recycledViewPool.apply {
                setMaxRecycledViews(0, spanCount * RECYCLERVIEW_POOL_SIZE_MULTIPLIER) // フィルター結果用に拡張
                // フィルター操作での頻繁な表示切り替えに対応
                clear() // 初期化時にプールをクリア
            }
            
            // Task9: フィルター結果変更時のスクロール位置保持設定
            preserveFocusAfterLayout = false // フィルター結果変更時は先頭に戻る
        }

        // Task9: フィルター結果対応Adapterを設定（強化版）
        adapter = createFilterAwareClothItemAdapter()
        binding.recyclerViewGallery.adapter = adapter
        
        // Task15: パフォーマンス最適化機能追加
        optimizeScrollPerformance()
        
        Log.d(TAG, "RecyclerView setup completed with filter support and performance optimization")
    }

    /**
     * Task9: フィルター結果対応ClothItemAdapterの作成（強化版）
     */
    private fun createFilterAwareClothItemAdapter(): ClothItemAdapter {
        Log.d(TAG, "Creating filter-aware ClothItemAdapter")
        
        return ClothItemAdapter { clothItem ->
            Log.d(TAG, "Filter-aware adapter: Item clicked - ID: ${clothItem.id}")
            
            try {
                // DetailActivityに遷移（フィルター状態情報付き）
                navigateToDetailActivityWithFilterContext(clothItem.id)
                
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to navigate to detail with filter context", e)
                // フォールバック: 通常のナビゲーション
                navigateToDetailActivity(clothItem.id)
            }
        }.apply {
            // Task9: フィルター結果変更時のDiffUtil最適化
            // ListAdapterのDiffUtilがフィルター結果変更を効率的に処理
            Log.d(TAG, "Filter-aware adapter configured with DiffUtil optimization")
        }
    }

    /**
     * Task9: フィルター状態情報付きDetailActivity遷移（強化版）
     */
    private fun navigateToDetailActivityWithFilterContext(clothItemId: Long) {
        Log.d(TAG, "Navigating to detail with filter context for item: $clothItemId")
        
        val intent = Intent(requireContext(), DetailActivity::class.java).apply {
            putExtra(DetailActivity.EXTRA_CLOTH_ITEM_ID, clothItemId)
            
            // Task9: 現在のフィルター状態を詳細画面に引き継ぎ（拡張機能）
            val currentFilters = viewModel.currentFilters.value
            val currentSearchText = viewModel.currentSearchText.value
            
            if (currentFilters?.hasActiveFilters() == true) {
                putExtra("EXTRA_FILTER_STATE", currentFilters.toString())
                Log.d(TAG, "Added filter state to detail intent: ${currentFilters}")
            }
            
            if (!currentSearchText.isNullOrBlank()) {
                putExtra("EXTRA_SEARCH_TEXT", currentSearchText)
                Log.d(TAG, "Added search text to detail intent: '$currentSearchText'")
            }
        }
        
        startActivity(intent)
    }

    /**
     * SwipeRefreshLayoutの設定
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }
    }

    /**
     * 空状態アクションの設定
     */
    private fun setupEmptyStateActions() {
        binding.buttonTakePhoto.setOnClickListener {
            launchCameraActivity()
        }
    }

    /**
     * FloatingActionButtonの設定
     */
    private fun setupFab() {
        binding.fabCamera.setOnClickListener {
            launchCameraActivity()
        }
    }

    /**
     * Task7: フィルターUIの初期化 (REFACTOR強化版)
     */
    private fun setupFilterUI() {
        Log.d(TAG, "Setting up filter UI")
        
        try {
            // フィルターボトムシートの初期化
            filterBottomSheetBehavior = BottomSheetBehavior.from(binding.includeBottomSheetFilter.bottomSheetFilter)
            filterBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            filterBottomSheetBehavior.isHideable = true
            
            // ボトムシートのコールバック設定（パフォーマンス最適化）
            filterBottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    Log.d(TAG, "Bottom sheet state changed to: $newState")
                    when (newState) {
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            // 展開時にフィルターオプションを更新
                            viewModel.availableFilterOptions.value?.let { filterOptions ->
                                updateFilterChips(filterOptions)
                            }
                        }
                    }
                }
                
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // スライド中のパフォーマンス最適化のため、何もしない
                }
            })
            
            // フィルターボタンのクリックリスナー設定
            binding.buttonFilter.setOnClickListener {
                Log.d(TAG, "Filter button clicked")
                showFilterBottomSheet()
            }
            
            // フィルターチップのリスナー設定
            setupFilterChipListeners()
            
            Log.d(TAG, "Filter UI setup completed")
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException during filter UI setup", e)
            // エラー時はフィルターUIを無効化（アルファ値含む）
            disableFilterUI()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException during filter UI setup", e)
            disableFilterUI()
        }
    }

    /**
     * Task7: フィルターボトムシートを表示 (Task9 REFACTOR: UI応答性最適化版)
     */
    private fun showFilterBottomSheet() {
        Log.d(TAG, "Showing filter bottom sheet (Task9 optimized)")
        
        try {
            // Task9 REFACTOR: UI応答性最適化 - フィルター表示前のプリロード
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // ViewModelから利用可能なフィルターオプションを最適化して取得
                    val filterOptions = viewModel.availableFilterOptions.value
                    
                    if (filterOptions != null) {
                        // フィルターチップを構造化された並行性で安全に更新（レースコンディション回避）
                        coroutineScope {
                            launch { setupSizeFilterChips(filterOptions) }
                            launch { setupColorFilterChips(filterOptions) }
                            launch { setupCategoryFilterChips(filterOptions) }
                        }
                        
                        // 全てのフィルターチップ更新完了後にボトムシート表示（同期保証）
                        filterBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        Log.d(TAG, "Filter bottom sheet displayed with synchronized performance")
                        
                    } else {
                        Log.w(TAG, "Filter options not available, showing error feedback")
                        showFilterLoadingError("フィルターオプションの読み込み中です。しばらくお待ちください。")
                    }
                    
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "IllegalStateException during filter bottom sheet display", e)
                    showComprehensiveFilterError("フィルター表示中にエラーが発生しました", e)
                } catch (e: UninitializedPropertyAccessException) {
                    Log.e(TAG, "UninitializedPropertyAccessException during filter bottom sheet display", e)
                    showComprehensiveFilterError("フィルター初期化エラーが発生しました", e)
                }
            }
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException showing filter bottom sheet", e)
            showComprehensiveFilterError("フィルター機能が利用できません", e)
        }
    }

    /**
     * Task7: フィルターチップのリスナー設定
     */
    private fun setupFilterChipListeners() {
        Log.d(TAG, "Setting up filter chip listeners")
        
        // フィルタークリアボタンのリスナー
        binding.includeBottomSheetFilter.buttonClearFilter.setOnClickListener {
            clearAllFilterHandler()
        }
        
        // フィルター適用ボタンのリスナー
        binding.includeBottomSheetFilter.buttonApplyFilter.setOnClickListener {
            applyFilterHandler()
        }
        
        Log.d(TAG, "Filter chip listeners setup completed")
    }

    /**
     * Task7: サイズフィルターチップの動的設定
     */
    private fun setupSizeFilterChips(filterOptions: FilterOptions) {
        Log.d(TAG, "Setting up size filter chips with ${filterOptions.availableSizes.size} options")
        
        val chipGroup = binding.includeBottomSheetFilter.chipGroupSize
        val currentState = viewModel.currentFilters.value
        
        // 既存のチップの選択状態を更新
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.let { chipView ->
                val sizeText = chipView.text.toString()
                val size = sizeText.toIntOrNull()
                if (size != null) {
                    chipView.isChecked = currentState?.sizeFilters?.contains(size) == true
                    
                    // チップのクリックリスナー設定
                    chipView.setOnCheckedChangeListener { _, isChecked ->
                        Log.d(TAG, "Size chip $size changed to $isChecked")
                        if (isChecked) {
                            viewModel.applyFilter(FilterType.SIZE, size.toString())
                        } else {
                            viewModel.removeFilter(FilterType.SIZE, size.toString())
                        }
                    }
                }
            }
        }
        
        Log.d(TAG, "Size filter chips setup completed")
    }

    /**
     * Task7: 色フィルターチップの動的設定
     */
    private fun setupColorFilterChips(filterOptions: FilterOptions) {
        Log.d(TAG, "Setting up color filter chips with ${filterOptions.availableColors.size} options")
        
        val chipGroup = binding.includeBottomSheetFilter.chipGroupColor
        val currentState = viewModel.currentFilters.value
        
        // 既存のチップの選択状態を更新
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.let { chipView ->
                val colorText = chipView.text.toString()
                chipView.isChecked = currentState?.colorFilters?.contains(colorText) == true
                
                // チップのクリックリスナー設定
                chipView.setOnCheckedChangeListener { _, isChecked ->
                    Log.d(TAG, "Color chip $colorText changed to $isChecked")
                    if (isChecked) {
                        viewModel.applyFilter(FilterType.COLOR, colorText)
                    } else {
                        viewModel.removeFilter(FilterType.COLOR, colorText)
                    }
                }
            }
        }
        
        Log.d(TAG, "Color filter chips setup completed")
    }

    /**
     * Task7: カテゴリフィルターチップの動的設定
     */
    private fun setupCategoryFilterChips(filterOptions: FilterOptions) {
        Log.d(TAG, "Setting up category filter chips with ${filterOptions.availableCategories.size} options")
        
        val chipGroup = binding.includeBottomSheetFilter.chipGroupCategory
        val currentState = viewModel.currentFilters.value
        
        // 既存のチップの選択状態を更新
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.let { chipView ->
                val categoryText = chipView.text.toString()
                chipView.isChecked = currentState?.categoryFilters?.contains(categoryText) == true
                
                // チップのクリックリスナー設定
                chipView.setOnCheckedChangeListener { _, isChecked ->
                    Log.d(TAG, "Category chip $categoryText changed to $isChecked")
                    if (isChecked) {
                        viewModel.applyFilter(FilterType.CATEGORY, categoryText)
                    } else {
                        viewModel.removeFilter(FilterType.CATEGORY, categoryText)
                    }
                }
            }
        }
        
        Log.d(TAG, "Category filter chips setup completed")
    }

    /**
     * Task7: 全フィルタークリアハンドラー
     */
    private fun clearAllFilterHandler() {
        Log.d(TAG, "Clearing all filters")
        viewModel.clearAllFilters()
        
        // ボトムシートのチップ選択状態もクリア
        clearAllChipSelections()
        
        Log.d(TAG, "All filters cleared")
    }
    
    /**
     * Task7: フィルター適用ハンドラー
     */
    private fun applyFilterHandler() {
        Log.d(TAG, "Applying filters")
        
        // ボトムシートを隠す
        filterBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        
        Log.d(TAG, "Filters applied and bottom sheet hidden")
    }
    
    /**
     * Task7: 全チップの選択状態をクリア
     */
    private fun clearAllChipSelections() {
        Log.d(TAG, "Clearing all chip selections")
        
        // サイズチップの選択をクリア
        val sizeChipGroup = binding.includeBottomSheetFilter.chipGroupSize
        for (i in 0 until sizeChipGroup.childCount) {
            val chip = sizeChipGroup.getChildAt(i) as? Chip
            chip?.isChecked = false
        }
        
        // 色チップの選択をクリア
        val colorChipGroup = binding.includeBottomSheetFilter.chipGroupColor
        for (i in 0 until colorChipGroup.childCount) {
            val chip = colorChipGroup.getChildAt(i) as? Chip
            chip?.isChecked = false
        }
        
        // カテゴリチップの選択をクリア
        val categoryChipGroup = binding.includeBottomSheetFilter.chipGroupCategory
        for (i in 0 until categoryChipGroup.childCount) {
            val chip = categoryChipGroup.getChildAt(i) as? Chip
            chip?.isChecked = false
        }
        
        Log.d(TAG, "All chip selections cleared")
    }
    
    /**
     * Task7: フィルターチップの最適化された更新 (REFACTOR)
     */
    private fun updateFilterChips(filterOptions: FilterOptions) {
        Log.d(TAG, "Updating filter chips optimally")
        
        try {
            // 並行して各フィルターチップを更新（パフォーマンス最適化）
            setupSizeFilterChips(filterOptions)
            setupColorFilterChips(filterOptions)
            setupCategoryFilterChips(filterOptions)
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException updating filter chips", e)
            // エラー時のフォールバック: フィルターを無効化
            disableFilterUI()
        } catch (e: ClassCastException) {
            Log.e(TAG, "ClassCastException updating filter chips", e)
            disableFilterUI()
        }
    }
    
    /**
     * Task7: フィルターUI無効化（エラー時のフォールバック）
     */
    private fun disableFilterUI() {
        Log.w(TAG, "Disabling filter UI due to error")
        
        try {
            binding.buttonFilter.isEnabled = false
            // 条件的アルファ値適用: 無効化時のみ設定
            if (binding.buttonFilter.alpha != DISABLED_ALPHA) {
                binding.buttonFilter.alpha = DISABLED_ALPHA
            }
            
            // 全チップを無効化
            binding.includeBottomSheetFilter.chipGroupSize.children.forEach { view ->
                (view as? Chip)?.isEnabled = false
            }
            binding.includeBottomSheetFilter.chipGroupColor.children.forEach { view ->
                (view as? Chip)?.isEnabled = false
            }
            binding.includeBottomSheetFilter.chipGroupCategory.children.forEach { view ->
                (view as? Chip)?.isEnabled = false
            }
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException disabling filter UI", e)
        } catch (e: ClassCastException) {
            Log.e(TAG, "ClassCastException disabling filter UI", e)
        }
    }
    
    /**
     * Task7: フィルターUI有効化（再有効化時のアルファ値リセット）
     */
    private fun enableFilterUI() {
        Log.d(TAG, "Enabling filter UI")
        
        try {
            binding.buttonFilter.isEnabled = true
            // 条件的アルファ値適用: 再有効化時に1.0fにリセット
            if (binding.buttonFilter.alpha != ENABLED_ALPHA) {
                binding.buttonFilter.alpha = ENABLED_ALPHA
            }
            
            // 全チップを有効化
            binding.includeBottomSheetFilter.chipGroupSize.children.forEach { view ->
                (view as? Chip)?.isEnabled = true
            }
            binding.includeBottomSheetFilter.chipGroupColor.children.forEach { view ->
                (view as? Chip)?.isEnabled = true
            }
            binding.includeBottomSheetFilter.chipGroupCategory.children.forEach { view ->
                (view as? Chip)?.isEnabled = true
            }
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException enabling filter UI", e)
        } catch (e: ClassCastException) {
            Log.e(TAG, "ClassCastException enabling filter UI", e)
        }
    }
    
    /**
     * Task8: 検索バーの初期化 (TDD GREEN + REFACTOR強化版)
     */
    private fun setupSearchBar() {
        Log.d(TAG, "Setting up search bar with performance optimizations")
        
        try {
            // 検索バーの初期設定（パフォーマンス最適化）
            binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Log.d(TAG, "Search submitted: $query")
                    // 検索実行時にキーボードを閉じる
                    binding.searchView.clearFocus()
                    // 即座に検索実行（デバウンスをスキップ）
                    searchJob?.cancel()
                    performImmediateSearch(query ?: "")
                    return true
                }
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    Log.d(TAG, "Search text changed: $newText")
                    performDebouncedSearch(newText ?: "")
                    return true
                }
            })
            
            // 検索バーのクリアボタン処理（強化版）
            binding.searchView.setOnCloseListener {
                Log.d(TAG, "Search cleared via close button")
                searchJob?.cancel()
                viewModel.clearSearch()
                false
            }
            
            // 検索バーのフォーカス処理（ユーザビリティ向上）
            binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    Log.d(TAG, "Search view gained focus")
                } else {
                    Log.d(TAG, "Search view lost focus")
                }
            }
            
            Log.d(TAG, "Search bar setup completed with optimizations")
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException during search bar setup", e)
            showSearchErrorFallback()
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "UninitializedPropertyAccessException during search bar setup", e)
            showSearchErrorFallback()
        }
    }
    
    /**
     * Task8 REFACTOR: 即座に検索実行（submit時用）
     */
    private fun performImmediateSearch(searchText: String) {
        Log.d(TAG, "Performing immediate search for: '$searchText'")
        
        val trimmedText = searchText.trim()
        
        when {
            trimmedText.isEmpty() -> {
                Log.d(TAG, "Immediate search cleared, showing all items")
                viewModel.clearSearch()
            }
            trimmedText.length < MIN_SEARCH_LENGTH -> {
                Log.d(TAG, "Immediate search text too short, ignoring")
                // 最小文字数未満でも submit されたら検索する（ユーザー意図尊重）
                viewModel.performSearch(trimmedText)
            }
            else -> {
                Log.d(TAG, "Performing immediate search for: '$trimmedText'")
                viewModel.performSearch(trimmedText)
            }
        }
    }
    
    /**
     * Task8 REFACTOR: 検索エラー時のフォールバック処理
     */
    private fun showSearchErrorFallback() {
        Log.w(TAG, "Search functionality disabled due to error")
        
        try {
            binding.searchView.isEnabled = false
            binding.searchView.alpha = DISABLED_ALPHA
            
            // エラーメッセージを表示
            showEnhancedErrorMessage("検索機能でエラーが発生しました。アプリを再起動してください。")
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to show search error fallback", e)
        }
    }
    
    /**
     * Task8: デバウンス付き検索実行 (Task9 REFACTOR: UI応答性最適化 + 包括的エラーフィードバック強化版)
     */
    private fun performDebouncedSearch(searchText: String) {
        Log.d(TAG, "Starting optimized debounced search for: '$searchText'")
        
        // 前の検索ジョブをキャンセル（パフォーマンス最適化）
        searchJob?.cancel()
        
        searchJob = lifecycleScope.launch {
            try {
                // Task9 REFACTOR: UI応答性最適化 - 動的デバウンス調整
                val adaptiveDelay = if (searchText.length > MIN_SEARCH_LENGTH * 2) {
                    SEARCH_DEBOUNCE_DELAY_MS / 2 // 長いテキストは早めに反応
                } else {
                    SEARCH_DEBOUNCE_DELAY_MS
                }
                
                delay(adaptiveDelay)
                
                // Task9 REFACTOR: 検索テキストバリデーション強化版
                val trimmedText = searchText.trim()
                val validationResult = validateSearchText(trimmedText)
                
                when (validationResult.status) {
                    SearchValidationStatus.VALID -> {
                        Log.d(TAG, "Performing optimized search for: '${validationResult.processedText}'")
                        viewModel.performSearch(validationResult.processedText)
                    }
                    SearchValidationStatus.EMPTY -> {
                        Log.d(TAG, "Search cleared with UI feedback")
                        viewModel.clearSearch()
                    }
                    SearchValidationStatus.TOO_SHORT -> {
                        Log.d(TAG, "Search text too short, showing user feedback")
                        showSearchValidationFeedback("検索は${MIN_SEARCH_LENGTH}文字以上で入力してください")
                        return@launch
                    }
                    SearchValidationStatus.TOO_LONG -> {
                        Log.d(TAG, "Search text truncated with user notification")
                        showSearchValidationFeedback("検索テキストを${MAX_SEARCH_LENGTH}文字に短縮しました")
                        viewModel.performSearch(validationResult.processedText)
                    }
                    SearchValidationStatus.INVALID -> {
                        Log.w(TAG, "Invalid search text detected")
                        showSearchValidationFeedback("検索テキストに無効な文字が含まれています")
                        return@launch
                    }
                }
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Optimized search debounce cancelled (expected behavior)")
                // キャンセルは正常な動作なので例外を再スローしない
                throw e // CancellationExceptionは再スロー（Detekt SwallowedException回避）
            } catch (e: IllegalStateException) {
                Log.e(TAG, "IllegalStateException during optimized debounced search", e)
                showComprehensiveSearchError("検索処理中にエラーが発生しました", e)
            } catch (e: UninitializedPropertyAccessException) {
                Log.e(TAG, "UninitializedPropertyAccessException during search", e)
                showComprehensiveSearchError("検索初期化エラーが発生しました", e)
            }
        }
    }

    /**
     * ViewModelの監視設定 (Task9強化版: フィルター・検索状態監視追加)
     */
    private fun observeViewModel() {
        Log.d(TAG, "Setting up ViewModel observers (Task9 enhanced)")
        
        // 基本的なViewModel監視を設定
        observeBasicViewModelStates()
        
        // Task7&9: フィルター・検索関連のViewModel監視を設定
        observeFilterAndSearchStates()
    }

    /**
     * Task9: 基本的なViewModelの状態監視（分割版）
     */
    private fun observeBasicViewModelStates() {
        // 衣服アイテムの監視（フィルター対応アニメーション付き）
        viewModel.clothItems.observe(viewLifecycleOwner) { items ->
            Log.d(TAG, "clothItems observer: Received ${items.size} items (filtered)")
            adapter.submitList(items) {
                Log.d(TAG, "clothItems observer: Filtered list submitted to adapter")
                // フィルター結果のスムーズなトランジション
                if (items.isNotEmpty() && binding.recyclerViewGallery.visibility == View.GONE) {
                    Log.d(TAG, "clothItems observer: Animating filtered RecyclerView entry")
                    animateFilteredRecyclerViewEntry()
                }
            }
        }

        // 空状態の監視（フィルター結果対応アニメーション付き）
        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            Log.d(TAG, "isEmpty observer: isEmpty = $isEmpty (after filter/search)")
            animateFilteredStateChange(isEmpty)
        }

        // ローディング状態の監視（フィルター操作対応改善版）
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "isLoading observer: isLoading = $isLoading (filter/search operation)")
            
            // SwipeRefreshLayoutの状態更新
            if (binding.swipeRefreshLayout.isRefreshing != isLoading) {
                binding.swipeRefreshLayout.isRefreshing = isLoading
                Log.d(TAG, "isLoading observer: Updated SwipeRefreshLayout for filter operation")
            }
            
            // フィルター・検索操作時のローディングインジケーター表示
            val shouldShowFullLoading = isLoading && adapter.itemCount == 0
            Log.d(TAG, "isLoading observer: shouldShowFilterLoading = $shouldShowFullLoading")
            animateFilterLoadingState(shouldShowFullLoading)
        }

        // エラーメッセージの監視（包括的エラーフィードバック強化版）
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                Log.e(TAG, "errorMessage observer: Filter/search error received - $errorMessage")
                showComprehensiveErrorFeedback(errorMessage)
                viewModel.clearErrorMessage()
            } else {
                Log.d(TAG, "errorMessage observer: Filter/search error cleared")
            }
        }
    }

    /**
     * Task9: フィルター・検索状態のViewModel監視（分割版）
     */
    private fun observeFilterAndSearchStates() {
        // Task7: 利用可能フィルターオプションの監視（UI状態管理）
        viewModel.availableFilterOptions.observe(viewLifecycleOwner) { filterOptions ->
            if (filterOptions != null) {
                Log.d(TAG, "availableFilterOptions observer: Options available, enabling filter UI")
                enableFilterUI()
            } else {
                Log.d(TAG, "availableFilterOptions observer: No options available, disabling filter UI")
                disableFilterUI()
            }
        }

        // Task9: フィルター状態LiveDataの監視（強化版observeViewModel）
        viewModel.currentFilters.observe(viewLifecycleOwner) { filterState ->
            Log.d(TAG, "currentFilters observer: Filter state updated")
            if (filterState != null) {
                Log.d(TAG, "currentFilters observer: Active size filters: ${filterState.sizeFilters}")
                Log.d(TAG, "currentFilters observer: Active color filters: ${filterState.colorFilters}")
                Log.d(TAG, "currentFilters observer: Active category filters: ${filterState.categoryFilters}")
                
                // フィルター状態変更時のUIフィードバック
                updateFilterUIState(filterState)
            }
        }

        // Task9: 検索状態LiveDataの監視（強化版observeViewModel）
        viewModel.currentSearchText.observe(viewLifecycleOwner) { searchText ->
            Log.d(TAG, "currentSearchText observer: Search text updated to: '$searchText'")
            
            // 検索状態変更時のUIフィードバック
            updateSearchUIState(searchText ?: "")
        }

        // Task9: フィルターアクティブ状態LiveDataの監視（強化版observeViewModel）
        viewModel.isFiltersActive.observe(viewLifecycleOwner) { isActive ->
            Log.d(TAG, "isFiltersActive observer: Filter active state = $isActive")
            
            // フィルターアクティブ状態に応じたUI表示調整
            updateFilterActiveIndicator(isActive)
        }
    }

    /**
     * Task 15: デバイス構成に応じたグリッド列数を計算（デバイス互換性強化）
     */
    private fun calculateSpanCount(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val configuration = resources.configuration
        
        return when {
            // タブレット横画面: 3-4列（左カラムエリアの場合）
            configuration.screenWidthDp >= TABLET_SCREEN_WIDTH_DP && 
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE -> {
                if (screenWidthDp > EXTRA_LARGE_TABLET_SCREEN_WIDTH_DP) SPAN_COUNT_4 else SPAN_COUNT_3
            }
            
            // タブレット縦画面: 3-4列
            configuration.screenWidthDp >= TABLET_SCREEN_WIDTH_DP -> {
                if (screenWidthDp > LARGE_TABLET_SCREEN_WIDTH_DP) SPAN_COUNT_4 else SPAN_COUNT_3
            }
            
            // スマートフォン横画面: 3列
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE -> {
                SPAN_COUNT_3
            }
            
            // 小画面スマートフォン: 2列
            screenWidthDp < SMALL_SCREEN_WIDTH_DP -> {
                2
            }
            
            // 通常スマートフォン縦画面: 2列
            else -> {
                2
            }
        }.coerceAtLeast(2) // 最低2列は保証
    }

    // Task 15: 大量データ対応のパフォーマンス監視機能（将来拡張用）
    // 現在は使用していないが、実際のプロダクション環境での監視に有用
    // private fun monitorMemoryUsage() { ... }

    /**
     * Task 15: スクロールパフォーマンス最適化
     */
    private fun optimizeScrollPerformance() {
        binding.recyclerViewGallery.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var scrollStartTime = 0L
            
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                
                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        scrollStartTime = System.currentTimeMillis()
                        // スクロール中は画像読み込みを一時停止
                        com.bumptech.glide.Glide.with(this@GalleryFragment).pauseRequests()
                    }
                    
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        val scrollDuration = System.currentTimeMillis() - scrollStartTime
                        Log.d(TAG, "Scroll performance: ${scrollDuration}ms")
                        
                        // スクロール完了後に画像読み込み再開
                        com.bumptech.glide.Glide.with(this@GalleryFragment).resumeRequests()
                        
                        // パフォーマンス警告
                        if (scrollDuration > SCROLL_PERFORMANCE_THRESHOLD_MS) {
                            Log.w(TAG, "Slow scroll detected: ${scrollDuration}ms")
                        }
                    }
                }
            }
        })
    }

    /**
     * カメラアクティビティを起動
     */
    private fun launchCameraActivity() {
        val intent = Intent(requireContext(), CameraActivity::class.java)
        startActivity(intent)
    }

    /**
     * DetailActivityに遷移
     */
    private fun navigateToDetailActivity(clothItemId: Long) {
        val intent = Intent(requireContext(), DetailActivity::class.java).apply {
            putExtra(DetailActivity.EXTRA_CLOTH_ITEM_ID, clothItemId)
        }
        startActivity(intent)
    }

    /**
     * 改善されたエラーメッセージ表示
     */
    private fun showEnhancedErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("再試行") {
                viewModel.refreshData()
            }
            .setActionTextColor(requireContext().getColor(android.R.color.holo_blue_light))
            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            .show()
    }

    /**
     * Task9: 包括的エラーフィードバック表示（強化版）
     */
    private fun showComprehensiveErrorFeedback(message: String) {
        Log.d(TAG, "Showing comprehensive error feedback: $message")
        
        try {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setAction("再試行") {
                    Log.d(TAG, "Error feedback retry button clicked")
                    viewModel.refreshData()
                }
                .setActionTextColor(requireContext().getColor(android.R.color.holo_blue_light))
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .addCallback(object : Snackbar.Callback() {
                    override fun onShown(sb: Snackbar?) {
                        Log.d(TAG, "Error feedback snackbar shown")
                    }
                    
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        Log.d(TAG, "Error feedback snackbar dismissed with event: $event")
                    }
                })
                .show()
                
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to show comprehensive error feedback", e)
            // フォールバック: 基本的なエラー表示
            showEnhancedErrorMessage(message)
        }
    }


    /**
     * 状態変更アニメーション（空状態⇔データ表示）
     */
    private fun animateStateChange(isEmpty: Boolean) {
        val fadeOut = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out)
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        
        fadeOut.duration = 150
        fadeIn.duration = 150
        
        if (isEmpty) {
            // データ→空状態
            binding.recyclerViewGallery.startAnimation(fadeOut)
            binding.recyclerViewGallery.visibility = View.GONE
            
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.layoutEmptyState.startAnimation(fadeIn)
        } else {
            // 空状態→データ
            binding.layoutEmptyState.startAnimation(fadeOut)
            binding.layoutEmptyState.visibility = View.GONE
            
            binding.recyclerViewGallery.visibility = View.VISIBLE
            binding.recyclerViewGallery.startAnimation(fadeIn)
        }
    }

    /**
     * ローディング状態アニメーション
     */
    private fun animateLoadingState(shouldShow: Boolean) {
        Log.d(TAG, "animateLoadingState: shouldShow = $shouldShow, current visibility = ${binding.layoutLoading.visibility}")
        
        if (shouldShow) {
            if (binding.layoutLoading.visibility != View.VISIBLE) {
                Log.d(TAG, "animateLoadingState: Showing loading layout")
                binding.layoutLoading.visibility = View.VISIBLE
                val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
                fadeIn.duration = 200
                binding.layoutLoading.startAnimation(fadeIn)
            } else {
                Log.d(TAG, "animateLoadingState: Loading layout already visible")
            }
        } else {
            if (binding.layoutLoading.visibility == View.VISIBLE) {
                Log.d(TAG, "animateLoadingState: Hiding loading layout")
                val fadeOut = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out)
                fadeOut.duration = 200
                fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {
                        Log.d(TAG, "animateLoadingState: Fade out animation started")
                    }
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        Log.d(TAG, "animateLoadingState: Fade out animation ended, hiding layout")
                        binding.layoutLoading.visibility = View.GONE
                    }
                })
                binding.layoutLoading.startAnimation(fadeOut)
            } else {
                Log.d(TAG, "animateLoadingState: Loading layout already hidden")
            }
        }
    }

    // ===== Task9: フィルター対応アニメーション関数群（スムーズなトランジション対応） =====

    /**
     * Task9: フィルター結果のRecyclerView表示アニメーション（強化版）
     */
    private fun animateFilteredRecyclerViewEntry() {
        Log.d(TAG, "animateFilteredRecyclerViewEntry: Starting filtered RecyclerView entry animation")
        
        try {
            binding.recyclerViewGallery.visibility = View.VISIBLE
            val slideIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left)
            slideIn.duration = FILTER_ANIMATION_DURATION
            binding.recyclerViewGallery.startAnimation(slideIn)
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to animate filtered RecyclerView entry", e)
            // フォールバック: 基本的な表示
            binding.recyclerViewGallery.visibility = View.VISIBLE
        }
    }

    /**
     * Task9: フィルター結果の状態変更アニメーション（空状態⇔フィルター結果表示）
     */
    private fun animateFilteredStateChange(isEmpty: Boolean) {
        Log.d(TAG, "animateFilteredStateChange: isEmpty = $isEmpty (filtered state)")
        
        try {
            val fadeOut = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out)
            val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
            
            fadeOut.duration = FILTER_TRANSITION_DURATION
            fadeIn.duration = FILTER_TRANSITION_DURATION
            
            if (isEmpty) {
                // フィルター結果→空状態（スムーズなトランジション）
                binding.recyclerViewGallery.startAnimation(fadeOut)
                binding.recyclerViewGallery.visibility = View.GONE
                
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.layoutEmptyState.startAnimation(fadeIn)
                Log.d(TAG, "animateFilteredStateChange: Filtered data to empty state transition")
            } else {
                // 空状態→フィルター結果（スムーズなトランジション）
                binding.layoutEmptyState.startAnimation(fadeOut)
                binding.layoutEmptyState.visibility = View.GONE
                
                binding.recyclerViewGallery.visibility = View.VISIBLE
                binding.recyclerViewGallery.startAnimation(fadeIn)
                Log.d(TAG, "animateFilteredStateChange: Empty state to filtered data transition")
            }
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to animate filtered state change", e)
            // フォールバック: 基本的な状態変更
            animateStateChange(isEmpty)
        }
    }

    /**
     * Task9: フィルター操作時のローディング状態アニメーション（強化版）
     */
    private fun animateFilterLoadingState(shouldShow: Boolean) {
        Log.d(TAG, "animateFilterLoadingState: shouldShow = $shouldShow (filter operation)")
        
        try {
            if (shouldShow) {
                if (binding.layoutLoading.visibility != View.VISIBLE) {
                    Log.d(TAG, "animateFilterLoadingState: Showing filter loading indicator")
                    binding.layoutLoading.visibility = View.VISIBLE
                    val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
                    fadeIn.duration = FILTER_LOADING_ANIMATION_DURATION
                    binding.layoutLoading.startAnimation(fadeIn)
                }
            } else {
                if (binding.layoutLoading.visibility == View.VISIBLE) {
                    Log.d(TAG, "animateFilterLoadingState: Hiding filter loading indicator")
                    val fadeOut = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out)
                    fadeOut.duration = FILTER_LOADING_ANIMATION_DURATION
                    fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                        override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                        override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                        override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                            binding.layoutLoading.visibility = View.GONE
                            Log.d(TAG, "animateFilterLoadingState: Filter loading animation completed")
                        }
                    })
                    binding.layoutLoading.startAnimation(fadeOut)
                }
            }
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to animate filter loading state", e)
            // フォールバック: 基本的なローディング表示
            animateLoadingState(shouldShow)
        }
    }

    // ===== Task9: フィルター・検索UIフィードバック関数群（UI応答性最適化対応） =====

    /**
     * Task9: フィルター状態変更時のUIフィードバック更新
     */
    private fun updateFilterUIState(filterState: FilterState) {
        Log.d(TAG, "updateFilterUIState: Updating UI for filter state changes")
        
        try {
            // フィルター状態に応じたチップ選択状態の更新（UI応答性最適化）
            viewLifecycleOwner.lifecycleScope.launch {
                // フィルターチップの選択状態を効率的に更新
                updateSizeChipSelections(filterState.sizeFilters)
                updateColorChipSelections(filterState.colorFilters)
                updateCategoryChipSelections(filterState.categoryFilters)
            }
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to update filter UI state", e)
        }
    }

    /**
     * Task9: 検索状態変更時のUIフィードバック更新
     */
    private fun updateSearchUIState(searchText: String) {
        Log.d(TAG, "updateSearchUIState: Updating UI for search text: '$searchText'")
        
        try {
            // 検索バーの状態更新（UI応答性最適化）
            if (binding.searchView.query.toString() != searchText) {
                binding.searchView.setQuery(searchText, false)
            }
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to update search UI state", e)
        }
    }

    /**
     * Task9: フィルターアクティブ状態インジケーター更新
     */
    private fun updateFilterActiveIndicator(isActive: Boolean) {
        Log.d(TAG, "updateFilterActiveIndicator: Filter active = $isActive")
        
        try {
            // フィルターボタンの見た目を更新（アクティブ状態表示）
            if (isActive) {
                binding.buttonFilter.setBackgroundColor(requireContext().getColor(R.color.filter_active_color))
                binding.buttonFilter.alpha = FILTER_ACTIVE_ALPHA
            } else {
                binding.buttonFilter.setBackgroundColor(requireContext().getColor(android.R.color.transparent))
                binding.buttonFilter.alpha = ENABLED_ALPHA
            }
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to update filter active indicator", e)
        } catch (e: android.content.res.Resources.NotFoundException) {
            Log.e(TAG, "Filter active color resource not found, using fallback", e)
            // フォールバック: デフォルトのアルファ値のみ変更
            binding.buttonFilter.alpha = if (isActive) FILTER_ACTIVE_ALPHA else ENABLED_ALPHA
        }
    }

    // ===== Task9: フィルターチップ選択状態更新ヘルパー関数群 =====

    /**
     * Task9: サイズチップ選択状態の効率的更新
     */
    private fun updateSizeChipSelections(activeSizes: Set<Int>) {
        val chipGroup = binding.includeBottomSheetFilter.chipGroupSize
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.let { chipView ->
                val sizeText = chipView.text.toString()
                val size = sizeText.toIntOrNull()
                if (size != null) {
                    chipView.isChecked = activeSizes.contains(size)
                }
            }
        }
    }

    /**
     * Task9: 色チップ選択状態の効率的更新
     */
    private fun updateColorChipSelections(activeColors: Set<String>) {
        val chipGroup = binding.includeBottomSheetFilter.chipGroupColor
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.let { chipView ->
                val colorText = chipView.text.toString()
                chipView.isChecked = activeColors.contains(colorText)
            }
        }
    }

    /**
     * Task9: カテゴリチップ選択状態の効率的更新
     */
    private fun updateCategoryChipSelections(activeCategories: Set<String>) {
        val chipGroup = binding.includeBottomSheetFilter.chipGroupCategory
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.let { chipView ->
                val categoryText = chipView.text.toString()
                chipView.isChecked = activeCategories.contains(categoryText)
            }
        }
    }

    // ===== Task9 REFACTOR: UI応答性最適化 + 包括的エラーフィードバック関数群 =====

    /**
     * Task9 REFACTOR: 検索テキストバリデーション（包括的）
     */
    private fun validateSearchText(text: String): SearchValidationResult {
        return when {
            text.isEmpty() -> SearchValidationResult(SearchValidationStatus.EMPTY, text)
            text.length < MIN_SEARCH_LENGTH -> SearchValidationResult(SearchValidationStatus.TOO_SHORT, text)
            text.length > MAX_SEARCH_LENGTH -> SearchValidationResult(
                SearchValidationStatus.TOO_LONG,
                text.substring(0, MAX_SEARCH_LENGTH)
            )
            text.contains(Regex("[<>&\"']")) -> SearchValidationResult(SearchValidationStatus.INVALID, text)
            else -> SearchValidationResult(SearchValidationStatus.VALID, text)
        }
    }

    /**
     * Task9 REFACTOR: 検索バリデーションフィードバック表示
     */
    private fun showSearchValidationFeedback(message: String) {
        try {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to show search validation feedback", e)
        }
    }

    /**
     * Task9 REFACTOR: 包括的検索エラーフィードバック
     */
    private fun showComprehensiveSearchError(message: String, error: Exception) {
        Log.e(TAG, "Comprehensive search error: $message", error)
        
        try {
            val errorDetail = when (error) {
                is kotlinx.coroutines.TimeoutCancellationException -> "検索がタイムアウトしました"
                is IllegalStateException -> "検索機能の状態エラー"
                is SecurityException -> "検索権限エラー"
                else -> "検索処理エラー"
            }
            
            Snackbar.make(binding.root, "$message: $errorDetail", Snackbar.LENGTH_LONG)
                .setAction("再試行") {
                    Log.d(TAG, "Search error retry requested")
                    // 検索バーをクリアして再開可能状態にする
                    binding.searchView.setQuery("", false)
                    viewModel.clearSearch()
                }
                .setActionTextColor(requireContext().getColor(android.R.color.holo_orange_light))
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show()
                
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to show comprehensive search error", e)
        }
    }

    /**
     * Task9 REFACTOR: フィルター読み込みエラーフィードバック
     */
    private fun showFilterLoadingError(message: String) {
        try {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                .setAction("更新") {
                    Log.d(TAG, "Filter loading error refresh requested")
                    viewModel.refreshData()
                }
                .setActionTextColor(requireContext().getColor(android.R.color.holo_blue_light))
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to show filter loading error", e)
        }
    }

    /**
     * Task9 REFACTOR: 包括的フィルターエラーフィードバック
     */
    private fun showComprehensiveFilterError(message: String, error: Exception) {
        Log.e(TAG, "Comprehensive filter error: $message", error)
        
        try {
            val errorDetail = when (error) {
                is IllegalStateException -> "フィルター状態エラー"
                is UninitializedPropertyAccessException -> "フィルター初期化エラー"
                is SecurityException -> "フィルター権限エラー"
                is android.content.res.Resources.NotFoundException -> "フィルターリソースエラー"
                else -> "フィルター機能エラー"
            }
            
            Snackbar.make(binding.root, "$message: $errorDetail", Snackbar.LENGTH_LONG)
                .setAction("リセット") {
                    Log.d(TAG, "Filter error reset requested")
                    // フィルターをクリアして再開可能状態にする
                    viewModel.clearAllFilters()
                    try {
                        filterBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Failed to hide bottom sheet during reset", e)
                    }
                }
                .setActionTextColor(requireContext().getColor(android.R.color.holo_red_light))
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                .show()
                
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to show comprehensive filter error", e)
        }
    }
    
    // ===== Task 14: アクセシビリティ対応メソッド =====
    
    /**
     * Task 14 GREEN: アクセシビリティ機能の初期設定
     */
    private fun setupAccessibility() {
        Log.d(TAG, "Setting up accessibility features")
        
        try {
            // フィルターボタンの初期contentDescription設定
            updateFilterButtonAccessibility()
            
            // 検索バーの初期contentDescription設定
            updateSearchViewAccessibility("")
            
            // ChipGroupアクセシビリティ設定
            setupChipGroupAccessibility()
            
            // キーボードナビゲーション設定
            setupKeyboardNavigation()
            
            // LiveRegion設定（動的コンテンツ変更通知用）
            setupLiveRegions()
            
            // 高コントラストモード対応
            enhanceForHighContrast()
            
            Log.d(TAG, "Accessibility setup completed")
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException setting up accessibility", e)
        } catch (e: RuntimeException) {
            Log.e(TAG, "RuntimeException setting up accessibility", e)
        }
    }
    
    /**
     * フィルターボタンのアクセシビリティ情報を更新
     */
    private fun updateFilterButtonAccessibility() {
        val activeFilters = viewModel.currentFilters.value
        val description = activeFilters?.let { filters ->
            if (filters.hasActiveFilters()) {
                val filterParts = mutableListOf<String>()
                
                if (filters.sizeFilters.isNotEmpty()) {
                    filterParts.add("サイズ: ${filters.sizeFilters.joinToString(", ")}")
                }
                if (filters.colorFilters.isNotEmpty()) {
                    filterParts.add("色: ${filters.colorFilters.joinToString(", ")}")
                }
                if (filters.categoryFilters.isNotEmpty()) {
                    filterParts.add("カテゴリ: ${filters.categoryFilters.joinToString(", ")}")
                }
                
                filterParts.joinToString(", ")
            } else {
                null
            }
        }
        
        AccessibilityHelper.updateFilterButtonDescription(binding.buttonFilter, description)
    }
    
    /**
     * 検索バーのアクセシビリティ情報を更新
     */
    private fun updateSearchViewAccessibility(currentQuery: String) {
        AccessibilityHelper.updateSearchViewDescription(binding.searchView, currentQuery)
    }
    
    /**
     * ChipGroupのアクセシビリティ設定
     */
    private fun setupChipGroupAccessibility() {
        // サイズChipGroup
        AccessibilityHelper.setupChipGroupAccessibility(
            binding.includeBottomSheetFilter.chipGroupSize,
            getString(R.string.filter_size_group_description)
        )
        
        // 色ChipGroup
        AccessibilityHelper.setupChipGroupAccessibility(
            binding.includeBottomSheetFilter.chipGroupColor,
            getString(R.string.filter_color_group_description)
        )
        
        // カテゴリChipGroup
        AccessibilityHelper.setupChipGroupAccessibility(
            binding.includeBottomSheetFilter.chipGroupCategory,
            getString(R.string.filter_category_group_description)
        )
        
        // 個別Chipの選択状態監視
        setupIndividualChipAccessibility()
    }
    
    /**
     * 個別Chipのアクセシビリティ監視設定
     */
    private fun setupIndividualChipAccessibility() {
        // サイズChip
        val sizeChips = listOf(
            binding.includeBottomSheetFilter.chipSize100 to SIZE_100,
            binding.includeBottomSheetFilter.chipSize110 to SIZE_110,
            binding.includeBottomSheetFilter.chipSize120 to SIZE_120,
            binding.includeBottomSheetFilter.chipSize130 to SIZE_130,
            binding.includeBottomSheetFilter.chipSize140 to SIZE_140,
            binding.includeBottomSheetFilter.chipSize150 to SIZE_150,
            binding.includeBottomSheetFilter.chipSize160 to SIZE_160
        )
        
        sizeChips.forEach { (chip, size) ->
            chip.setOnCheckedChangeListener { _, isChecked ->
                AccessibilityHelper.updateSizeChipContentDescription(chip, size, isChecked)
            }
            // 初期状態設定
            AccessibilityHelper.updateSizeChipContentDescription(chip, size, chip.isChecked)
        }
        
        // 色Chip
        val colorChips = mapOf(
            binding.includeBottomSheetFilter.chipColorRed to R.string.chip_color_red_description,
            binding.includeBottomSheetFilter.chipColorBlue to R.string.chip_color_blue_description,
            binding.includeBottomSheetFilter.chipColorGreen to R.string.chip_color_green_description,
            binding.includeBottomSheetFilter.chipColorYellow to R.string.chip_color_yellow_description,
            binding.includeBottomSheetFilter.chipColorBlack to R.string.chip_color_black_description,
            binding.includeBottomSheetFilter.chipColorWhite to R.string.chip_color_white_description,
            binding.includeBottomSheetFilter.chipColorPink to R.string.chip_color_pink_description,
            binding.includeBottomSheetFilter.chipColorPurple to R.string.chip_color_purple_description
        )
        
        colorChips.forEach { (chip, descriptionResId) ->
            chip?.setOnCheckedChangeListener { _, isChecked ->
                val baseDescription = getString(descriptionResId)
                AccessibilityHelper.updateChipContentDescription(chip, baseDescription, isChecked)
            }
            // 初期状態設定
            chip?.let {
                val baseDescription = getString(descriptionResId)
                AccessibilityHelper.updateChipContentDescription(it, baseDescription, it.isChecked)
            }
        }
        
        // カテゴリChip
        val categoryChips = mapOf(
            binding.includeBottomSheetFilter.chipCategoryTops to R.string.chip_category_tops_description,
            binding.includeBottomSheetFilter.chipCategoryBottoms to R.string.chip_category_bottoms_description,
            binding.includeBottomSheetFilter.chipCategoryOuterwear to R.string.chip_category_outerwear_description,
            binding.includeBottomSheetFilter.chipCategoryShoes to R.string.chip_category_shoes_description,
            binding.includeBottomSheetFilter.chipCategoryAccessories to R.string.chip_category_accessories_description
        )
        
        categoryChips.forEach { (chip, descriptionResId) ->
            chip?.setOnCheckedChangeListener { _, isChecked ->
                val baseDescription = getString(descriptionResId)
                AccessibilityHelper.updateChipContentDescription(chip, baseDescription, isChecked)
            }
            // 初期状態設定
            chip?.let {
                val baseDescription = getString(descriptionResId)
                AccessibilityHelper.updateChipContentDescription(it, baseDescription, it.isChecked)
            }
        }
    }
    
    /**
     * キーボードナビゲーション設定
     */
    private fun setupKeyboardNavigation() {
        val keyboardInstructions = getString(R.string.keyboard_navigation_hint)
        
        // フィルターボトムシート全体にキーボードナビゲーション設定
        AccessibilityHelper.setupKeyboardNavigationDelegate(
            binding.includeBottomSheetFilter.bottomSheetFilter,
            keyboardInstructions
        )
        
        // Chip選択の説明
        val chipInstructions = getString(R.string.chip_selection_hint)
        
        // 全Chipにキーボード操作説明を設定
        binding.includeBottomSheetFilter.chipGroupSize.children.forEach { chip ->
            AccessibilityHelper.setupKeyboardNavigationDelegate(chip, chipInstructions)
        }
        
        binding.includeBottomSheetFilter.chipGroupColor.children.forEach { chip ->
            AccessibilityHelper.setupKeyboardNavigationDelegate(chip, chipInstructions)
        }
        
        binding.includeBottomSheetFilter.chipGroupCategory.children.forEach { chip ->
            AccessibilityHelper.setupKeyboardNavigationDelegate(chip, chipInstructions)
        }
    }
    
    /**
     * LiveRegion設定（動的コンテンツ変更通知）
     */
    private fun setupLiveRegions() {
        // RecyclerViewをLiveRegionに設定（フィルター結果変更通知用）
        AccessibilityHelper.setLiveRegion(binding.recyclerViewGallery)
        
        // 空状態レイアウトもLiveRegionに設定
        AccessibilityHelper.setLiveRegion(binding.layoutEmptyState)
    }
    
    /**
     * 高コントラストモード対応
     */
    private fun enhanceForHighContrast() {
        // フィルターボタンの高コントラスト対応
        AccessibilityHelper.enhanceFocusForHighContrast(binding.buttonFilter)
        
        // 全Chipの高コントラスト対応
        binding.includeBottomSheetFilter.chipGroupSize.children.forEach { chip ->
            AccessibilityHelper.enhanceFocusForHighContrast(chip)
        }
        
        binding.includeBottomSheetFilter.chipGroupColor.children.forEach { chip ->
            AccessibilityHelper.enhanceFocusForHighContrast(chip)
        }
        
        binding.includeBottomSheetFilter.chipGroupCategory.children.forEach { chip ->
            AccessibilityHelper.enhanceFocusForHighContrast(chip)
        }
        
        // ボタン類の高コントラスト対応
        AccessibilityHelper.enhanceFocusForHighContrast(binding.includeBottomSheetFilter.buttonApplyFilter)
        AccessibilityHelper.enhanceFocusForHighContrast(binding.includeBottomSheetFilter.buttonClearFilter)
    }
    
    // Task 14: アクセシビリティ通知メソッドは将来の機能拡張時に使用予定

    companion object {
        private const val TAG = "GalleryFragment"
        
        // Task7: フィルターUI用定数
        private const val DISABLED_ALPHA = 0.5f
        private const val ENABLED_ALPHA = 1.0f
        
        // Task8: 検索機能用定数
        private const val SEARCH_DEBOUNCE_DELAY_MS = 300L
        private const val MIN_SEARCH_LENGTH = 2
        private const val MAX_SEARCH_LENGTH = 50 // パフォーマンス考慮
        
        // Task9: フィルター対応アニメーション用定数（スムーズなトランジション）
        private const val FILTER_ANIMATION_DURATION = 250L // フィルター結果表示アニメーション時間
        private const val FILTER_TRANSITION_DURATION = 200L // フィルター状態変更トランジション時間
        private const val FILTER_LOADING_ANIMATION_DURATION = 150L // フィルター操作ローディング表示時間
        private const val FILTER_ACTIVE_ALPHA = 0.8f // フィルターアクティブ時のアルファ値
        private const val RECYCLERVIEW_POOL_SIZE_MULTIPLIER = 5 // RecyclerViewプールサイズ乗数
        
        // Task 14: アクセシビリティ用定数
        private const val SIZE_100 = 100
        
        // Task15: 大量データパフォーマンス最適化定数
        // 将来の大量データ対応機能で使用予定
        // private const val LARGE_DATASET_THRESHOLD = 500 // 大量データとみなす閾値
        // private const val PREFETCH_DISTANCE = 5 // 先読み距離
        private const val SCROLL_PERFORMANCE_THRESHOLD_MS = 16 // スムーズスクロール閾値
        // private const val MEMORY_WARNING_THRESHOLD_MB = 100 // メモリ警告閾値
        // private const val VIEWPORT_MULTIPLIER = 3 // ビューポート範囲外アイテム乗数
        
        // Task15: デバイス構成計算用定数
        private const val TABLET_SCREEN_WIDTH_DP = 600
        private const val LARGE_TABLET_SCREEN_WIDTH_DP = 800
        private const val EXTRA_LARGE_TABLET_SCREEN_WIDTH_DP = 900
        private const val SPAN_COUNT_3 = 3
        private const val SPAN_COUNT_4 = 4
        private const val SMALL_SCREEN_WIDTH_DP = 360
        private const val MEMORY_DIVISOR = 1024 * 1024 // MB換算
        private const val SIZE_110 = 110
        private const val SIZE_120 = 120
        private const val SIZE_130 = 130
        private const val SIZE_140 = 140
        private const val SIZE_150 = 150
        private const val SIZE_160 = 160
        
        /**
         * GalleryFragmentの新しいインスタンスを作成
         */
        fun newInstance(): GalleryFragment {
            return GalleryFragment()
        }
    }
}

/**
 * GalleryViewModel用のViewModelFactory
 */
class GalleryViewModelFactory(
    private val repository: com.example.clothstock.data.repository.ClothRepository,
    private val filterManager: com.example.clothstock.data.repository.FilterManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            return GalleryViewModel(repository, filterManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

