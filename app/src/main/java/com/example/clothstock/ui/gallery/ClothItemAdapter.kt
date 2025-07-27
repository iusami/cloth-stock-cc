package com.example.clothstock.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.clothstock.R
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.databinding.ItemClothGridBinding

/**
 * 衣服アイテムグリッド表示用RecyclerView.Adapter
 * 
 * TDD Greenフェーズ実装
 * Glide画像表示、クリックリスナー、DiffUtil効率更新
 */
class ClothItemAdapter(
    private val onItemClick: (ClothItem) -> Unit
) : ListAdapter<ClothItem, ClothItemAdapter.ClothItemViewHolder>(ClothItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothItemViewHolder {
        val binding = ItemClothGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClothItemViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ClothItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder - データバインディング + Glide画像読み込み
     */
    class ClothItemViewHolder(
        private val binding: ItemClothGridBinding,
        private val onItemClick: (ClothItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(clothItem: ClothItem) {
            // データバインディングでモデルをセット
            binding.clothItem = clothItem
            binding.executePendingBindings()

            // Glideで画像読み込み
            Glide.with(binding.imageViewCloth.context)
                .load(clothItem.imagePath)
                .placeholder(R.drawable.ic_photo_placeholder) 
                .error(R.drawable.ic_error_photo)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imageViewCloth)

            // クリックリスナー設定
            binding.root.setOnClickListener {
                onItemClick(clothItem)
            }

            // お気に入り状態の表示設定（将来の拡張用、現在は非表示）
            binding.iconFavorite.visibility = android.view.View.GONE
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