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