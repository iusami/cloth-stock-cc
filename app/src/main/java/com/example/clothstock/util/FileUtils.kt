package com.example.clothstock.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ファイル操作のユーティリティクラス
 * 
 * 画像ファイルの保存、URI生成、ファイル管理などを担当
 */
object FileUtils {
    
    private const val IMAGES_DIRECTORY = "images"
    private const val AUTHORITY_SUFFIX = ".fileprovider"
    
    /**
     * 撮影した画像を保存するためのファイルを作成
     * 
     * @param context アプリケーションコンテキスト
     * @return 作成されたファイルオブジェクト
     */
    fun createImageFile(context: Context): File {
        // タイムスタンプベースのファイル名を生成
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "cloth_${timeStamp}.jpg"
        
        // アプリの内部ストレージにimagesディレクトリを作成
        val imagesDir = File(context.filesDir, IMAGES_DIRECTORY)
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        
        return File(imagesDir, fileName)
    }
    
    /**
     * ファイルからFileProvider経由のURIを生成
     * 
     * @param context アプリケーションコンテキスト
     * @param file URIを生成するファイル
     * @return FileProviderによるURI
     */
    fun getUriForFile(context: Context, file: File): Uri {
        val authority = "${context.packageName}$AUTHORITY_SUFFIX"
        return FileProvider.getUriForFile(context, authority, file)
    }
    
    /**
     * 利用可能なストレージ容量をチェック
     * 
     * @param context アプリケーションコンテキスト
     * @param requiredBytes 必要な容量（バイト）
     * @return 十分な容量がある場合はtrue
     */
    fun hasEnoughStorage(context: Context, requiredBytes: Long = 10 * 1024 * 1024): Boolean {
        return try {
            val filesDir = context.filesDir
            val freeSpace = filesDir.freeSpace
            freeSpace >= requiredBytes
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 古い画像ファイルを削除してストレージを最適化
     * 
     * @param context アプリケーションコンテキスト
     * @param maxFiles 保持する最大ファイル数
     */
    fun cleanupOldFiles(context: Context, maxFiles: Int = 100) {
        try {
            val imagesDir = File(context.filesDir, IMAGES_DIRECTORY)
            if (!imagesDir.exists()) return
            
            val files = imagesDir.listFiles()?.filter { it.isFile && it.name.endsWith(".jpg") }
            if (files == null || files.size <= maxFiles) return
            
            // 最終更新日時でソートして古いファイルを削除
            val sortedFiles = files.sortedBy { it.lastModified() }
            val filesToDelete = sortedFiles.take(files.size - maxFiles)
            
            filesToDelete.forEach { file ->
                file.delete()
            }
        } catch (e: Exception) {
            // ファイル削除エラーは無視（ログ記録のみ）
        }
    }
    
    /**
     * ファイルが存在し、かつ読み取り可能かチェック
     * 
     * @param file チェック対象のファイル
     * @return ファイルが有効な場合はtrue
     */
    fun isFileValid(file: File): Boolean {
        return file.exists() && file.canRead() && file.length() > 0
    }
    
    /**
     * ファイルサイズを人間が読みやすい形式で取得
     * 
     * @param file サイズを取得するファイル
     * @return フォーマットされたファイルサイズ（例: "1.2 MB"）
     */
    fun getFormattedFileSize(file: File): String {
        val sizeInBytes = file.length()
        return when {
            sizeInBytes < 1024 -> "${sizeInBytes} B"
            sizeInBytes < 1024 * 1024 -> String.format("%.1f KB", sizeInBytes / 1024.0)
            sizeInBytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", sizeInBytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", sizeInBytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * 画像ディレクトリの全体サイズを取得
     * 
     * @param context アプリケーションコンテキスト
     * @return ディレクトリサイズ（バイト）
     */
    fun getImageDirectorySize(context: Context): Long {
        return try {
            val imagesDir = File(context.filesDir, IMAGES_DIRECTORY)
            if (!imagesDir.exists()) return 0L
            
            imagesDir.listFiles()
                ?.filter { it.isFile }
                ?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}