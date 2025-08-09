package com.example.clothstock.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.clothstock.R
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.databinding.ItemClothGridBinding
import com.example.clothstock.util.GlideUtils
import android.util.Log
import java.io.File

/**
 * 衣服アイテムグリッド表示用RecyclerView.Adapter
 * 
 * TDD Greenフェーズ実装 + Task 6実装
 * Glide画像表示、クリックリスナー、DiffUtil効率更新、メモプレビュー機能
 */
class ClothItemAdapter(
    private val onItemClick: (ClothItem) -> Unit,
    private val onMemoPreviewClick: (ClothItem) -> Unit = { /* デフォルトは何もしない */ }
) : ListAdapter<ClothItem, ClothItemAdapter.ClothItemViewHolder>(ClothItemDiffCallback()) {
    
    companion object {
        private const val TAG = "ClothItemAdapter"
        private const val IMAGE_LOAD_TIMEOUT_MS = 10000 // 10秒タイムアウト
        private const val THUMBNAIL_SIZE = 300 // サムネイルサイズ
        private const val GLIDE_ENCODE_QUALITY = 85 // Glide画像品質（Android Q+対応）
        private const val MEMO_PREVIEW_MAX_LENGTH = 30 // メモプレビュー最大文字数
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothItemViewHolder {
        val binding = ItemClothGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClothItemViewHolder(binding, onItemClick, onMemoPreviewClick)
    }

    override fun onBindViewHolder(holder: ClothItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder - データバインディング + Glide画像読み込み + メモプレビュー機能
     */
    class ClothItemViewHolder(
        private val binding: ItemClothGridBinding,
        private val onItemClick: (ClothItem) -> Unit,
        private val onMemoPreviewClick: (ClothItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(clothItem: ClothItem) {
            // データバインディングでモデルをセット
            binding.clothItem = clothItem
            binding.executePendingBindings()

            // 詳細な画像パス情報のログ出力（デバッグ用）
            Log.d(TAG, "=== ClothItem ${clothItem.id} Binding Debug ===")
            Log.d(TAG, "ImagePath: '${clothItem.imagePath}'")
            Log.d(TAG, "ImagePath length: ${clothItem.imagePath?.length ?: 0}")
            Log.d(TAG, "ImagePath starts with content://: ${clothItem.imagePath?.startsWith("content://") ?: false}")
            Log.d(TAG, "ImagePath starts with file://: ${clothItem.imagePath?.startsWith("file://") ?: false}")

            // ローディング状態の初期設定
            binding.progressBarImage.visibility = android.view.View.VISIBLE
            Log.d(TAG, "Progress bar set to VISIBLE for item ${clothItem.id}")

            // 画像パスの妥当性チェック
            val isValid = isValidImagePath(clothItem.imagePath)
            Log.d(TAG, "Image path validation result for item ${clothItem.id}: $isValid")
            
            if (!isValid) {
                Log.e(TAG, "Invalid image path for item ${clothItem.id}: ${clothItem.imagePath}")
                showErrorState()
                return
            }

            // Glideで画像読み込み（Android Q+ pinning非推奨対応版）
            val glideRequest = Glide.with(binding.imageViewCloth.context)
                .load(clothItem.imagePath)
                .apply(GlideUtils.getThumbnailOptions(THUMBNAIL_SIZE))
                .timeout(IMAGE_LOAD_TIMEOUT_MS)
                .placeholder(R.drawable.ic_photo_placeholder) 
                .error(R.drawable.ic_error_photo)
                .dontAnimate() // TransitionDrawable問題回避のためアニメーション無効化
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // キャッシュ戦略最適化
                
            // Android Q+対応: pinning非推奨警告対策
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                glideRequest
                    .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565) // メモリ使用量削減
                    .encodeFormat(android.graphics.Bitmap.CompressFormat.JPEG) // pinning回避
                    .encodeQuality(GLIDE_ENCODE_QUALITY) // 品質バランス
                    .disallowHardwareConfig() // ハードウェアビットマップ無効化（pinning回避）
                    .dontTransform() // 不要な変換回避
                    .skipMemoryCache(false) // メモリキャッシュ使用だがpinning回避
            }
            
            glideRequest
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e(TAG, "=== Glide Load FAILED for item ${clothItem.id} ===")
                        Log.e(TAG, "Model: $model")
                        Log.e(TAG, "ImagePath: ${clothItem.imagePath}")
                        Log.e(TAG, "IsFirstResource: $isFirstResource")
                        Log.e(TAG, "Exception: $e")
                        logGlideError(e, clothItem)
                        showErrorState()
                        return false
                    }
                    
                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d(TAG, "=== Glide Load SUCCESS for item ${clothItem.id} ===")
                        Log.d(TAG, "Model: $model")
                        Log.d(TAG, "DataSource: $dataSource")
                        Log.d(TAG, "IsFirstResource: $isFirstResource")
                        Log.d(TAG, "Resource: ${resource.javaClass.simpleName}")
                        showSuccessState()
                        return false
                    }
                })
                .into(binding.imageViewCloth)

            // クリックリスナー設定
            binding.root.setOnClickListener {
                onItemClick(clothItem)
            }

            // メモインジケーターの表示設定
            binding.memoIndicator.setHasMemo(clothItem.hasMemo())
            Log.d(TAG, "Memo indicator set for item ${clothItem.id}: hasMemo=${clothItem.hasMemo()}")

            // Task 6: メモプレビュー表示設定
            setupMemoPreview(clothItem)

            // お気に入り状態の表示設定（将来の拡張用、現在は非表示）
            binding.iconFavorite.visibility = android.view.View.GONE
        }
        
        /**
         * 画像パスの妥当性チェック（Scoped Storage対応版）
         */
        private fun isValidImagePath(imagePath: String?): Boolean {
            if (imagePath.isNullOrBlank()) {
                Log.w(TAG, "Image path is null or blank")
                return false
            }
            
            // URIの場合とファイルパスの場合の両方をチェック
            return if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                // URI形式の場合（Scoped Storage対応）
                try {
                    val uri = android.net.Uri.parse(imagePath)
                    if (uri == null) {
                        Log.e(TAG, "Failed to parse URI: $imagePath")
                        return false
                    }
                    
                    // Content URIの場合、MediaStore形式かチェック
                    if (imagePath.startsWith("content://")) {
                        val isMediaStoreUri = imagePath.contains("media") || 
                                            imagePath.contains("external") ||
                                            imagePath.contains("images")
                        Log.d(TAG, "Content URI validation - isMediaStore: $isMediaStoreUri, URI: $imagePath")
                        return isMediaStoreUri
                    }
                    
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Invalid URI: $imagePath", e)
                    false
                }
            } else {
                // ファイルパスの場合（レガシー対応）
                try {
                    val file = File(imagePath)
                    val exists = file.exists() && file.isFile()
                    if (!exists) {
                        Log.w(TAG, "File does not exist: $imagePath")
                    }
                    exists
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking file: $imagePath", e)
                    false
                }
            }
        }
        
        /**
         * Glideエラーの詳細ログ出力
         */
        private fun logGlideError(exception: com.bumptech.glide.load.engine.GlideException?, clothItem: ClothItem) {
            if (exception != null) {
                Log.e(TAG, "Glide error details for item ${clothItem.id}:")
                Log.e(TAG, "  - Root cause: ${exception.rootCauses}")
                Log.e(TAG, "  - Causes: ${exception.causes}")
                Log.e(TAG, "  - Message: ${exception.message}")
            }
        }
        
        /**
         * エラー状態の表示
         */
        private fun showErrorState() {
            Log.d(TAG, "showErrorState called")
            try {
                binding.progressBarImage.visibility = android.view.View.GONE
                Log.d(TAG, "Progress bar set to GONE (error state)")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting progress bar visibility in showErrorState", e)
            }
        }
        
        /**
         * 成功状態の表示
         */
        private fun showSuccessState() {
            Log.d(TAG, "showSuccessState called")
            try {
                binding.progressBarImage.visibility = android.view.View.GONE
                Log.d(TAG, "Progress bar set to GONE (success state)")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting progress bar visibility in showSuccessState", e)
            }
        }

        /**
         * Task 6: メモプレビュー表示設定
         * 
         * Requirements 4.2: メモテキストが長い場合は省略表示
         * Requirements 4.3: メモプレビュータップ時のDetailActivity遷移
         * 
         * @param clothItem 表示するClothItem
         */
        private fun setupMemoPreview(clothItem: ClothItem) {
            val hasMemo = clothItem.hasMemo()
            Log.d(TAG, "Setup memo preview for item ${clothItem.id}: hasMemo=$hasMemo")
            
            if (hasMemo) {
                val memoPreview = clothItem.getMemoPreview(MEMO_PREVIEW_MAX_LENGTH)
                
                // メモプレビューを表示
                binding.textMemoPreview.text = memoPreview
                binding.textMemoPreview.visibility = android.view.View.VISIBLE
                
                // メモプレビュータップ時のリスナー設定
                binding.textMemoPreview.setOnClickListener {
                    Log.d(TAG, "Memo preview tapped for item ${clothItem.id}")
                    onMemoPreviewClick(clothItem)
                }
                
                // アクセシビリティ対応
                binding.textMemoPreview.contentDescription = 
                    binding.textMemoPreview.context.getString(
                        R.string.memo_preview_description, 
                        memoPreview
                    )
                
                Log.d(TAG, "Memo preview displayed for item ${clothItem.id}: '$memoPreview'")
            } else {
                // メモがない場合は非表示
                binding.textMemoPreview.visibility = android.view.View.GONE
                binding.textMemoPreview.setOnClickListener(null)
                
                Log.d(TAG, "No memo for item ${clothItem.id}, preview hidden")
            }
        }
    }

    /**
     * DiffUtil.ItemCallback - 効率的なリスト更新
     */
    class ClothItemDiffCallback : DiffUtil.ItemCallback<ClothItem>() {
        override fun areItemsTheSame(oldItem: ClothItem, newItem: ClothItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ClothItem, newItem: ClothItem): Boolean {
            return oldItem == newItem
        }
    }
}