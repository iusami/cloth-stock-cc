package com.example.clothstock.util

import android.content.Context
import android.net.Uri
import android.util.Log
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
    
    private const val TAG = "FileUtils"
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
            try {
                Log.w(TAG, "ストレージ容量チェック中にエラーが発生しました", e)
            } catch (logException: Exception) {
                // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
            }
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
            // ファイル削除エラーをログに記録（アプリの動作に影響しないため処理は継続）
            try {
                Log.w(TAG, "ファイルクリーンアップ中にエラーが発生しました", e)
            } catch (logException: Exception) {
                // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
            }
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
            try {
                Log.w(TAG, "ディレクトリサイズ計算中にエラーが発生しました", e)
            } catch (logException: Exception) {
                // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
            }
            0L
        }
    }
    
    /**
     * 指定されたパスの画像ファイルを削除
     * 
     * @param imagePath 削除する画像ファイルのパス
     * @return 削除成功時はtrue、失敗時はfalse
     */
    fun deleteImageFile(imagePath: String): Boolean {
        // REFACTOR Phase: 入力検証とファイル処理の強化
        if (imagePath.isBlank()) {
            try {
                Log.w(TAG, "削除対象のファイルパスが空です")
            } catch (logException: Exception) {
                // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
            }
            return false
        }
        
        val file = File(imagePath)
        return deleteImageFile(file)
    }
    
    /**
     * 指定されたFileオブジェクトの画像ファイルを削除
     * 
     * @param file 削除する画像ファイル
     * @return 削除成功時はtrue、失敗時はfalse
     */
    fun deleteImageFile(file: File): Boolean {
        // REFACTOR Phase: 包括的な検証、権限チェック、エラーハンドリング
        return try {
            // ファイル存在確認
            if (!file.exists()) {
                try {
                    Log.d(TAG, "削除対象ファイルが存在しません: ${file.absolutePath}")
                } catch (logException: Exception) {
                    // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
                }
                return false
            }
            
            // ディレクトリでないことを確認
            if (file.isDirectory) {
                try {
                    Log.w(TAG, "削除対象がディレクトリです（ファイルのみ削除可能）: ${file.absolutePath}")
                } catch (logException: Exception) {
                    // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
                }
                return false
            }
            
            // 書き込み権限確認
            if (!file.canWrite()) {
                try {
                    Log.w(TAG, "ファイルに書き込み権限がありません: ${file.absolutePath}")
                } catch (logException: Exception) {
                    // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
                }
                return false
            }
            
            // 削除実行
            val deletionResult = file.delete()
            
            if (deletionResult) {
                try {
                    Log.d(TAG, "ファイル削除成功: ${file.absolutePath}")
                } catch (logException: Exception) {
                    // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
                }
            } else {
                try {
                    Log.w(TAG, "ファイル削除失敗（削除処理でfalseが返された）: ${file.absolutePath}")
                } catch (logException: Exception) {
                    // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
                }
            }
            
            deletionResult
            
        } catch (e: SecurityException) {
            try {
                Log.e(TAG, "ファイル削除でセキュリティ例外が発生しました: ${file.absolutePath}", e)
            } catch (logException: Exception) {
                // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
            }
            false
        } catch (e: Exception) {
            try {
                Log.e(TAG, "ファイル削除で予期しない例外が発生しました: ${file.absolutePath}", e)
            } catch (logException: Exception) {
                // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
            }
            false
        }
    }
    
    /**
     * 削除前のファイル検証を実行
     * 
     * @param file 検証対象のファイル
     * @return 削除可能な場合はtrue、削除不可能な場合はfalse
     */
    fun validateImageFileForDeletion(file: File): Boolean {
        // 最終REFACTOR Phase: 包括的な検証とログ記録
        return try {
            // ファイル存在確認
            if (!file.exists()) {
                try {
                    Log.d(TAG, "検証: ファイルが存在しません: ${file.absolutePath}")
                } catch (logException: Exception) {
                    // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
                }
                return false
            }
            
            // ディレクトリでないことを確認
            if (file.isDirectory) {
                try {
                    Log.w(TAG, "検証: 削除対象がディレクトリです: ${file.absolutePath}")
                } catch (logException: Exception) {
                    // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
                }
                return false
            }
            
            // 書き込み権限確認
            if (!file.canWrite()) {
                try {
                    Log.w(TAG, "検証: ファイルに書き込み権限がありません: ${file.absolutePath}")
                } catch (logException: Exception) {
                    // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
                }
                return false
            }
            
            try {
                Log.d(TAG, "検証: ファイル削除可能確認完了: ${file.absolutePath}")
            } catch (logException: Exception) {
                // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
            }
            true
            
        } catch (e: SecurityException) {
            try {
                Log.e(TAG, "検証でセキュリティ例外が発生しました: ${file.absolutePath}", e)
            } catch (logException: Exception) {
                // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
            }
            false
        } catch (e: Exception) {
            try {
                Log.e(TAG, "検証で予期しない例外が発生しました: ${file.absolutePath}", e)
            } catch (logException: Exception) {
                // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
            }
            false
        }
    }
    
    /**
     * ファイル削除後の検証を実行
     * 
     * @param file 検証対象のファイル
     * @return ファイルが削除されていればtrue、削除されていなければfalse
     */
    fun verifyFileDeletion(file: File): Boolean {
        // 最終REFACTOR Phase: 包括的な削除後検証とログ記録
        return try {
            val fileExists = file.exists()
            
            if (!fileExists) {
                try {
                    Log.d(TAG, "削除検証: ファイルは正常に削除されました: ${file.absolutePath}")
                } catch (logException: Exception) {
                    // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
                }
                true
            } else {
                try {
                    Log.w(TAG, "削除検証: ファイルが削除されていません: ${file.absolutePath}")
                } catch (logException: Exception) {
                    // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
                }
                false
            }
            
        } catch (e: SecurityException) {
            try {
                Log.e(TAG, "削除検証でセキュリティ例外が発生しました: ${file.absolutePath}", e)
            } catch (logException: Exception) {
                // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
            }
            false
        } catch (e: Exception) {
            try {
                Log.e(TAG, "削除検証で予期しない例外が発生しました: ${file.absolutePath}", e)
            } catch (logException: Exception) {
                // ログ出力でも例外が発生した場合は無視（テスト環境での制限）
            }
            false
        }
    }
}