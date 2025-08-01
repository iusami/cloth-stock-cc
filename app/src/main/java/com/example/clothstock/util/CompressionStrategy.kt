package com.example.clothstock.util

/**
 * 画像圧縮戦略を定義する列挙型（TDD GREEN段階）
 */
enum class CompressionStrategy {
    /**
     * サムネイル用 - 高圧縮（小サイズ優先）
     */
    THUMBNAIL,
    
    /**
     * ギャラリー表示用 - 中圧縮（バランス重視）
     */
    GALLERY_DISPLAY,
    
    /**
     * 詳細表示用 - 低圧縮（品質重視）
     */
    DETAIL_VIEW,
    
    /**
     * 編集用 - 最小圧縮（品質最優先）
     */
    EDITING
}