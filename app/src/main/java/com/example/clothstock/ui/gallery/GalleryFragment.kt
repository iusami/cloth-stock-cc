package com.example.clothstock.ui.gallery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.clothstock.data.repository.ClothRepositoryImpl
import com.example.clothstock.databinding.FragmentGalleryBinding
import com.example.clothstock.ui.camera.CameraActivity
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
     * RecyclerViewの設定
     */
    private fun setupRecyclerView() {
        // GridLayoutManagerを設定（列数は画面サイズに応じて調整）
        val spanCount = calculateSpanCount()
        binding.recyclerViewGallery.layoutManager = GridLayoutManager(requireContext(), spanCount)

        // Adapterを設定
        adapter = ClothItemAdapter { clothItem ->
            // アイテムクリックで詳細画面へ遷移（TODO: DetailActivity実装後に有効化）
            // navigateToDetail(clothItem)
            showItemDetails(clothItem)
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
        // 衣服アイテムの監視
        viewModel.clothItems.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }

        // 空状態の監視
        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerViewGallery.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        // ローディング状態の監視
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            binding.layoutLoading.visibility = if (isLoading && adapter.itemCount == 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        // エラーメッセージの監視
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let { message ->
                showErrorMessage(message)
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
     * アイテム詳細表示（暫定実装）
     */
    private fun showItemDetails(clothItem: com.example.clothstock.data.model.ClothItem) {
        val message = "アイテム詳細: ${clothItem.tagData.category} - ${clothItem.tagData.color}"
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    /**
     * エラーメッセージの表示
     */
    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("再試行") {
                viewModel.refreshData()
            }
            .show()
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