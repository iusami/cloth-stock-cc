package com.example.clothstock.ui.gallery

import android.content.Intent
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
import com.google.android.material.snackbar.Snackbar
import android.util.Log
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.example.clothstock.data.model.FilterOptions
import com.example.clothstock.data.model.FilterType
import androidx.core.view.children

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
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        
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
     * RecyclerViewの設定（最適化済み）
     */
    private fun setupRecyclerView() {
        // GridLayoutManagerを設定（列数は画面サイズに応じて調整）
        val spanCount = calculateSpanCount()
        val layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.recyclerViewGallery.layoutManager = layoutManager

        // RecyclerViewパフォーマンス最適化
        binding.recyclerViewGallery.apply {
            setHasFixedSize(true) // サイズ固定でパフォーマンス向上
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 200
                removeDuration = 200
                moveDuration = 200
                changeDuration = 200
            }
            // ViewHolderプールサイズの最適化
            recycledViewPool.setMaxRecycledViews(0, spanCount * 3)
        }

        // Adapterを設定
        adapter = ClothItemAdapter { clothItem ->
            // DetailActivityに遷移
            navigateToDetailActivity(clothItem.id)
        }
        binding.recyclerViewGallery.adapter = adapter
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
     * Task7: フィルターボトムシートを表示
     */
    private fun showFilterBottomSheet() {
        Log.d(TAG, "Showing filter bottom sheet")
        
        // ViewModelから利用可能なフィルターオプションを取得
        viewModel.availableFilterOptions.value?.let { filterOptions ->
            // フィルターチップを動的に更新
            setupSizeFilterChips(filterOptions)
            setupColorFilterChips(filterOptions)
            setupCategoryFilterChips(filterOptions)
        }
        
        // ボトムシートを表示
        filterBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        
        Log.d(TAG, "Filter bottom sheet displayed")
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
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException updating filter chips", e)
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
     * ViewModelの監視設定
     */
    private fun observeViewModel() {
        Log.d(TAG, "Setting up ViewModel observers")
        
        // 衣服アイテムの監視（アニメーション付き）
        viewModel.clothItems.observe(viewLifecycleOwner) { items ->
            Log.d(TAG, "clothItems observer: Received ${items.size} items")
            adapter.submitList(items) {
                Log.d(TAG, "clothItems observer: List submitted to adapter")
                // リスト更新完了後のコールバック
                if (items.isNotEmpty() && binding.recyclerViewGallery.visibility == View.GONE) {
                    Log.d(TAG, "clothItems observer: Animating RecyclerView entry")
                    animateRecyclerViewEntry()
                }
            }
        }

        // 空状態の監視（アニメーション付き）
        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            Log.d(TAG, "isEmpty observer: isEmpty = $isEmpty")
            animateStateChange(isEmpty)
        }

        // ローディング状態の監視（改善版）
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "isLoading observer: isLoading = $isLoading, adapter.itemCount = ${adapter.itemCount}")
            
            // SwipeRefreshLayoutの状態更新
            if (binding.swipeRefreshLayout.isRefreshing != isLoading) {
                binding.swipeRefreshLayout.isRefreshing = isLoading
                Log.d(TAG, "isLoading observer: Updated SwipeRefreshLayout to $isLoading")
            }
            
            // 初回ローディング時のみフルスクリーンローディング表示
            val shouldShowFullLoading = isLoading && adapter.itemCount == 0
            Log.d(TAG, "isLoading observer: shouldShowFullLoading = $shouldShowFullLoading")
            animateLoadingState(shouldShowFullLoading)
        }

        // エラーメッセージの監視（改善版）
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                Log.e(TAG, "errorMessage observer: Error received - $errorMessage")
                showEnhancedErrorMessage(errorMessage)
                viewModel.clearErrorMessage()
            } else {
                Log.d(TAG, "errorMessage observer: Error cleared")
            }
        }

        // Task7: 利用可能フィルターオプションの監視（UI状態管理）
        viewModel.availableFilterOptions.observe(viewLifecycleOwner) { filterOptions ->
            if (filterOptions != null) {
                Log.d(TAG, "availableFilterOptions observer: Options available, enabling filter UI")
                // フィルターオプションが利用可能になったらUIを有効化
                enableFilterUI()
            } else {
                Log.d(TAG, "availableFilterOptions observer: No options available, disabling filter UI")
                // フィルターオプションが利用不可の場合はUIを無効化
                disableFilterUI()
            }
        }
    }

    /**
     * 画面サイズに応じたグリッド列数を計算
     */
    private fun calculateSpanCount(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val itemWidthDp = 150 // 各アイテムの幅（dp）
        return (screenWidthDp / itemWidthDp).toInt().coerceAtLeast(2)
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
     * RecyclerView表示アニメーション
     */
    private fun animateRecyclerViewEntry() {
        binding.recyclerViewGallery.visibility = View.VISIBLE
        val animation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        animation.duration = 300
        binding.recyclerViewGallery.startAnimation(animation)
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

    companion object {
        private const val TAG = "GalleryFragment"
        
        // Task7: フィルターUI用定数
        private const val DISABLED_ALPHA = 0.5f
        private const val ENABLED_ALPHA = 1.0f
        
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