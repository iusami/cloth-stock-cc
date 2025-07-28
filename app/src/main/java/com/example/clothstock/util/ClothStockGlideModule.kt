package com.example.clothstock.util

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
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

    override fun applyOptions(context: Context, builder: com.bumptech.glide.GlideBuilder) {
        // メモリキャッシュサイズ計算 - デバイスメモリの20%
        val calculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(2f) // 画面2枚分
            .setBitmapPoolScreens(3f)  // ビットマッププール3枚分
            .build()
        
        // メモリキャッシュ設定
        builder.setMemoryCache(LruResourceCache(calculator.memoryCacheSize.toLong()))
        
        // デフォルトリクエストオプション設定
        val requestOptions = RequestOptions()
            .format(DecodeFormat.PREFER_RGB_565) // メモリ使用量削減
            .disallowHardwareConfig() // ハードウェアビットマップ無効化（編集可能性確保）
        
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