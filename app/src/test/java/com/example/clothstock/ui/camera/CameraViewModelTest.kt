package com.example.clothstock.ui.camera

import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*
import java.io.File

/**
 * CameraViewModel のユニットテスト
 * 
 * TDD アプローチに従い、まず失敗するテストを作成してから実装を行う
 * カメラの状態管理と画像キャプチャ機能のテスト
 * 
 * Note: Android固有のクラス（CameraX等）はインストルメンテーションテストで検証
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CameraViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockCameraStateObserver: Observer<CameraState>

    @Mock
    private lateinit var mockCaptureResultObserver: Observer<CaptureResult?>

    @Mock
    private lateinit var mockErrorObserver: Observer<CameraError?>

    @Mock
    private lateinit var mockFile: File

    @Mock
    private lateinit var mockUri: Uri

    private lateinit var cameraViewModel: CameraViewModel

    @Before
    fun setUp() {
        cameraViewModel = CameraViewModel()
        
        // LiveDataのオブザーバーを設定
        cameraViewModel.cameraState.observeForever(mockCameraStateObserver)
        cameraViewModel.captureResult.observeForever(mockCaptureResultObserver)
        cameraViewModel.cameraError.observeForever(mockErrorObserver)
        
        // setUp時の初期状態変更を記録から除外
        reset(mockCameraStateObserver, mockCaptureResultObserver, mockErrorObserver)
    }

    // ===== 初期状態テスト =====

    @Test
    fun `初期状態_IDLE状態でエラーなし`() {
        // When & Then
        assertEquals("初期状態はIDLEであるべき", CameraState.IDLE, cameraViewModel.cameraState.value)
        assertNull("初期状態ではエラーはないべき", cameraViewModel.cameraError.value)
        assertNull("初期状態ではキャプチャ結果はないべき", cameraViewModel.captureResult.value)
    }

    // ===== 状態管理テスト =====

    @Test
    fun `isReady_IDLE状態_falseを返す`() {
        // When & Then
        assertFalse("IDLE状態ではisReady()はfalse", cameraViewModel.isReady())
    }

    @Test
    fun `isCapturing_IDLE状態_falseを返す`() {
        // When & Then
        assertFalse("IDLE状態ではisCapturing()はfalse", cameraViewModel.isCapturing())
    }

    // ===== エラーハンドリングテスト =====

    @Test
    fun `handleCameraError_エラー設定_ERROR状態になる`() {
        // When
        cameraViewModel.handleCameraError(CameraError.INITIALIZATION_FAILED)

        // Then
        verify(mockCameraStateObserver).onChanged(CameraState.ERROR)
        verify(mockErrorObserver).onChanged(CameraError.INITIALIZATION_FAILED)
    }

    @Test
    fun `clearError_エラークリア_エラー状態解除`() {
        // Given
        cameraViewModel.handleCameraError(CameraError.CAPTURE_FAILED)

        // When
        cameraViewModel.clearError()

        // Then
        verify(mockErrorObserver).onChanged(null)
    }

    // ===== LiveData テスト =====

    @Test
    fun `cameraState_LiveData_正しく観測可能`() {
        // When
        cameraViewModel.handleCameraError(CameraError.HARDWARE_ERROR)

        // Then
        assertNotNull("cameraStateのLiveDataが設定されているべき", cameraViewModel.cameraState.value)
        assertEquals("エラー状態が正しく設定されるべき", CameraState.ERROR, cameraViewModel.cameraState.value)
    }

    @Test
    fun `captureResult_成功時_正しい結果を設定`() {
        // Given
        val successResult = CaptureResult.Success(mockUri, "test/path")

        // When
        cameraViewModel.setCaptureResult(successResult)

        // Then
        verify(mockCaptureResultObserver).onChanged(successResult)
        assertEquals("キャプチャ結果が正しく設定されるべき", successResult, cameraViewModel.captureResult.value)
    }

    @Test
    fun `captureResult_失敗時_エラー結果を設定`() {
        // Given
        val errorResult = CaptureResult.Error(RuntimeException("test"), "テストエラー")

        // When
        cameraViewModel.setCaptureResult(errorResult)

        // Then
        verify(mockCaptureResultObserver).onChanged(errorResult)
        assertEquals("エラー結果が正しく設定されるべき", errorResult, cameraViewModel.captureResult.value)
    }

    // ===== キャプチャ状態テスト =====

    @Test
    fun `captureImage_IDLE状態_falseを返す`() {
        // When
        val result = cameraViewModel.captureImage()

        // Then
        assertFalse("IDLE状態ではキャプチャできないべき", result)
    }

    // ===== リソース管理テスト =====

    @Test
    fun `releaseCamera_リソース解放_IDLE状態に戻る`() {
        // Given - まずエラー状態などに変更しておく
        cameraViewModel.handleCameraError(CameraError.HARDWARE_ERROR)
        reset(mockCameraStateObserver) // 前の呼び出しをリセット
        
        // When
        cameraViewModel.releaseCamera()

        // Then
        verify(mockCameraStateObserver).onChanged(CameraState.IDLE)
    }

    // ===== プレビュー取得テスト =====

    @Test
    fun `getPreview_初期状態_nullを返す`() {
        // When & Then
        assertNull("初期状態ではPreviewはnull", cameraViewModel.getPreview())
    }

    // ===== ViewModel ライフサイクルテスト =====

    @Test
    fun `releaseCamera_リソース解放_適切にクリーンアップ`() {
        // Given - 何らかの状態を設定（実際のカメラ初期化なし）
        cameraViewModel.handleCameraError(CameraError.INITIALIZATION_FAILED)
        reset(mockCameraStateObserver)

        // When
        cameraViewModel.releaseCamera()

        // Then
        verify(mockCameraStateObserver).onChanged(CameraState.IDLE)
        assertEquals("リソース解放後はIDLE状態になるべき", CameraState.IDLE, cameraViewModel.cameraState.value)
    }

    // ===== エラー状態からの回復テスト =====

    @Test
    fun `clearError_ERROR状態から_IDLE状態に戻る`() {
        // Given
        cameraViewModel.handleCameraError(CameraError.CAPTURE_FAILED)
        reset(mockCameraStateObserver)

        // When
        cameraViewModel.clearError()

        // Then
        // 初期化されていない場合はIDLE状態に戻る
        assertEquals("エラークリア後は適切な状態に戻るべき", CameraState.IDLE, cameraViewModel.cameraState.value)
    }
}