package com.example.clothstock.util

import android.content.Context
import android.net.Uri
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*
import java.io.File

/**
 * FileUtils のユニットテスト
 * 
 * ファイル操作とストレージ管理機能のテスト
 * Androidコンテキストが必要な機能はインストルメンテーションテストで検証
 */
@RunWith(MockitoJUnitRunner::class)
class FileUtilsTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockFile: File

    @Mock
    private lateinit var mockFilesDir: File

    @Before
    fun setUp() {
        // Context の設定
        `when`(mockContext.packageName).thenReturn("com.example.clothstock")
        `when`(mockContext.filesDir).thenReturn(mockFilesDir)
    }

    // ===== ファイル名生成テスト =====

    @Test
    fun `createImageFile_ファイル名生成_適切な形式`() {
        // Given
        val imagesDir = mock(File::class.java)
        `when`(mockFilesDir.exists()).thenReturn(true)
        `when`(File(mockFilesDir, "images")).thenReturn(imagesDir)
        `when`(imagesDir.exists()).thenReturn(true)

        // When
        val result = FileUtils.createImageFile(mockContext)

        // Then
        assertNotNull("ファイルが作成されるべき", result)
        assertTrue("ファイル名がcloth_で始まるべき", result.name.startsWith("cloth_"))
        assertTrue("ファイル名が.jpgで終わるべき", result.name.endsWith(".jpg"))
        assertTrue("タイムスタンプが含まれるべき", result.name.length > 10)
    }

    // ===== ストレージ容量チェックテスト =====

    @Test
    fun `hasEnoughStorage_十分な容量_trueを返す`() {
        // Given
        `when`(mockFilesDir.freeSpace).thenReturn(50 * 1024 * 1024L) // 50MB

        // When
        val result = FileUtils.hasEnoughStorage(mockContext, 10 * 1024 * 1024L) // 10MB必要

        // Then
        assertTrue("十分な容量がある場合はtrueを返すべき", result)
    }

    @Test
    fun `hasEnoughStorage_不十分な容量_falseを返す`() {
        // Given
        `when`(mockFilesDir.freeSpace).thenReturn(5 * 1024 * 1024L) // 5MB

        // When
        val result = FileUtils.hasEnoughStorage(mockContext, 10 * 1024 * 1024L) // 10MB必要

        // Then
        assertFalse("容量不足の場合はfalseを返すべき", result)
    }

    @Test
    fun `hasEnoughStorage_例外発生_falseを返す`() {
        // Given
        `when`(mockFilesDir.freeSpace).thenThrow(RuntimeException("ストレージアクセスエラー"))

        // When
        val result = FileUtils.hasEnoughStorage(mockContext)

        // Then
        assertFalse("例外発生時はfalseを返すべき", result)
    }

    // ===== ファイル検証テスト =====

    @Test
    fun `isFileValid_有効なファイル_trueを返す`() {
        // Given
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFile.canRead()).thenReturn(true)
        `when`(mockFile.length()).thenReturn(1024L)

        // When
        val result = FileUtils.isFileValid(mockFile)

        // Then
        assertTrue("有効なファイルの場合はtrueを返すべき", result)
    }

    @Test
    fun `isFileValid_存在しないファイル_falseを返す`() {
        // Given
        `when`(mockFile.exists()).thenReturn(false)
        `when`(mockFile.canRead()).thenReturn(true)
        `when`(mockFile.length()).thenReturn(1024L)

        // When
        val result = FileUtils.isFileValid(mockFile)

        // Then
        assertFalse("存在しないファイルの場合はfalseを返すべき", result)
    }

    @Test
    fun `isFileValid_読み取り不可ファイル_falseを返す`() {
        // Given
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFile.canRead()).thenReturn(false)
        `when`(mockFile.length()).thenReturn(1024L)

        // When
        val result = FileUtils.isFileValid(mockFile)

        // Then
        assertFalse("読み取り不可ファイルの場合はfalseを返すべき", result)
    }

    @Test
    fun `isFileValid_空ファイル_falseを返す`() {
        // Given
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFile.canRead()).thenReturn(true)
        `when`(mockFile.length()).thenReturn(0L)

        // When
        val result = FileUtils.isFileValid(mockFile)

        // Then
        assertFalse("空ファイルの場合はfalseを返すべき", result)
    }

    // ===== ファイルサイズフォーマットテスト =====

    @Test
    fun `getFormattedFileSize_バイト単位_正しいフォーマット`() {
        // Given
        `when`(mockFile.length()).thenReturn(512L)

        // When
        val result = FileUtils.getFormattedFileSize(mockFile)

        // Then
        assertEquals("バイト単位のフォーマットが正しい", "512 B", result)
    }

    @Test
    fun `getFormattedFileSize_キロバイト単位_正しいフォーマット`() {
        // Given
        `when`(mockFile.length()).thenReturn(1536L) // 1.5KB

        // When
        val result = FileUtils.getFormattedFileSize(mockFile)

        // Then
        assertEquals("キロバイト単位のフォーマットが正しい", "1.5 KB", result)
    }

    @Test
    fun `getFormattedFileSize_メガバイト単位_正しいフォーマット`() {
        // Given
        `when`(mockFile.length()).thenReturn(2 * 1024 * 1024L + 512 * 1024L) // 2.5MB

        // When
        val result = FileUtils.getFormattedFileSize(mockFile)

        // Then
        assertEquals("メガバイト単位のフォーマットが正しい", "2.5 MB", result)
    }

    @Test
    fun `getFormattedFileSize_ギガバイト単位_正しいフォーマット`() {
        // Given
        `when`(mockFile.length()).thenReturn(2L * 1024 * 1024 * 1024 + 512L * 1024 * 1024) // 2.5GB

        // When
        val result = FileUtils.getFormattedFileSize(mockFile)

        // Then
        assertEquals("ギガバイト単位のフォーマットが正しい", "2.5 GB", result)
    }

    // ===== ディレクトリサイズ計算テスト =====

    @Test
    fun `getImageDirectorySize_ディレクトリ存在しない_0を返す`() {
        // Given
        val imagesDir = mock(File::class.java)
        `when`(File(mockFilesDir, "images")).thenReturn(imagesDir)
        `when`(imagesDir.exists()).thenReturn(false)

        // When
        val result = FileUtils.getImageDirectorySize(mockContext)

        // Then
        assertEquals("存在しないディレクトリのサイズは0", 0L, result)
    }

    @Test
    fun `getImageDirectorySize_例外発生_0を返す`() {
        // Given
        `when`(File(mockFilesDir, "images")).thenThrow(RuntimeException("アクセスエラー"))

        // When
        val result = FileUtils.getImageDirectorySize(mockContext)

        // Then
        assertEquals("例外発生時は0を返すべき", 0L, result)
    }

    // ===== エラーハンドリングテスト =====

    @Test
    fun `cleanupOldFiles_例外発生_クラッシュしない`() {
        // Given
        `when`(File(mockFilesDir, "images")).thenThrow(RuntimeException("アクセスエラー"))

        // When & Then - 例外が発生してもクラッシュしない
        try {
            FileUtils.cleanupOldFiles(mockContext, 10)
        } catch (e: Exception) {
            fail("例外が適切にハンドリングされていない: ${e.message}")
        }
    }
}