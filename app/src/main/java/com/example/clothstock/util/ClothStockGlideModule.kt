package com.example.clothstock.util

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

/**
 * Glide設定モジュール - パフォーマンス最適化
 * 
 * DetailActivityでの画像表示パフォーマンスを向上させるため
 * メモリ使用量とキャッシュ戦略を最適化
 */
@GlideModule
class ClothStockGlideModule : AppGlideModule() {

    companion object {
        // キャッシュサイズ定数（可読性重視）
        private const val DISK_CACHE_SIZE_MB = 100
        private const val BYTES_PER_MB = 1024 * 1024
        private const val DISK_CACHE_SIZE_BYTES = DISK_CACHE_SIZE_MB * BYTES_PER_MB
    }

    override fun applyOptions(context: Context, builder: com.bumptech.glide.GlideBuilder) {
        // メモリキャッシュサイズ計算 - デバイスメモリの20%
        val calculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(2f) // 画面2枚分
            .setBitmapPoolScreens(3f)  // ビットマッププール3枚分
            .build()
        
        // メモリキャッシュ設定
        builder.setMemoryCache(LruResourceCache(calculator.memoryCacheSize.toLong()))
        
        // ディスクキャッシュ設定（定数使用で可読性向上）
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, DISK_CACHE_SIZE_BYTES.toLong()))
        
        // デフォルトリクエストオプション設定（パフォーマンス重視）
        val requestOptions = RequestOptions()
            .format(DecodeFormat.PREFER_RGB_565) // メモリ使用量削減
            // ハードウェアビットマップはデフォルトで有効（パフォーマンス向上）
            // 編集が必要な場合は個別にdisallowHardwareConfig()を指定
        
        builder.setDefaultRequestOptions(requestOptions)
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // 将来的にカスタムローダーなどを登録する場合はここに追加
    }

    override fun isManifestParsingEnabled(): Boolean {
        // Manifestからの自動設定を無効化（明示的設定のため）
        return false
    }
}