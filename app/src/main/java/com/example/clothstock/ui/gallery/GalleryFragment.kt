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
        _binding = null
    }

    /**
     * ViewModelの初期化
     */
    private fun setupViewModel() {
        val repository = ClothRepositoryImpl.getInstance(requireContext())
        val viewModelFactory = GalleryViewModelFactory(repository)
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
        // 衣服アイテムの監視（アニメーション付き）
        viewModel.clothItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items) {
                // リスト更新完了後のコールバック
                if (items.isNotEmpty() && binding.recyclerViewGallery.visibility == View.GONE) {
                    animateRecyclerViewEntry()
                }
            }
        }

        // 空状態の監視（アニメーション付き）
        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            animateStateChange(isEmpty)
        }

        // ローディング状態の監視（改善版）
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            
            // 初回ローディング時のみフルスクリーンローディング表示
            val shouldShowFullLoading = isLoading && adapter.itemCount == 0
            animateLoadingState(shouldShowFullLoading)
        }

        // エラーメッセージの監視（改善版）
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let { message ->
                showEnhancedErrorMessage(message)
                viewModel.clearErrorMessage()
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
        if (shouldShow) {
            binding.layoutLoading.visibility = View.VISIBLE
            val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
            fadeIn.duration = 200
            binding.layoutLoading.startAnimation(fadeIn)
        } else {
            val fadeOut = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out)
            fadeOut.duration = 200
            fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    binding.layoutLoading.visibility = View.GONE
                }
            })
            binding.layoutLoading.startAnimation(fadeOut)
        }
    }

    companion object {
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
    private val repository: com.example.clothstock.data.repository.ClothRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            return GalleryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}