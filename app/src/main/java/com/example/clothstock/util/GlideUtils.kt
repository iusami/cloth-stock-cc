package com.example.clothstock.util

import com.bumptech.glide.request.RequestOptions

/**
 * Glide設定ユーティリティ
 * 
 * 用途に応じた最適化設定を提供し、パフォーマンスと機能性のバランスを取る
 */
object GlideUtils {
    
    /**
     * 表示専用画像のRequestOptions
     * ハードウェアビットマップ有効で高速描画を実現
     */
    fun getDisplayOnlyOptions(): RequestOptions {
        return RequestOptions()
            .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)
            // ハードウェアビットマップ有効（高速描画）
    }
    
    /**
     * 編集可能画像のRequestOptions  
     * ハードウェアビットマップ無効で編集機能を保証
     */
    fun getEditableOptions(): RequestOptions {
        return RequestOptions()
            .format(com.bumptech.glide.load.DecodeFormat.PREFER_ARGB_8888) // 編集には高品質
            .disallowHardwareConfig() // 編集のためハードウェアビットマップ無効
    }
    
    /**
     * サムネイル用RequestOptions
     * メモリ効率とパフォーマンスを重視
     */
    fun getThumbnailOptions(size: Int = 300): RequestOptions {
        return RequestOptions()
            .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)
            .override(size, size)
            .centerCrop()
            // ハードウェアビットマップ有効（サムネイル描画高速化）
    }
    
    /**
     * フルサイズ表示用RequestOptions
     * 高品質表示とパフォーマンスの両立
     */
    fun getFullSizeDisplayOptions(): RequestOptions {
        return RequestOptions()
            .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)
            .override(1080, 1920) // メモリ最適化
            .centerCrop()
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            // ハードウェアビットマップ有効（DetailActivity用）
    }
}