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
    }

    // ===== 初期状態テスト =====

    @Test
    fun `初期状態_IDLE状態でエラーなし`() {
        // When & Then
        assertEquals("初期状態はIDLEであるべき", CameraState.IDLE, cameraViewModel.cameraState.value)
        assertNull("初期状態ではエラーはないべき", cameraViewModel.cameraError.value)
        assertNull("初期状態ではキャプチャ結果はないべき", cameraViewModel.captureResult.value)
    }

    // ===== カメラ初期化テスト =====

    @Test
    fun `initializeCamera_正常初期化_READY状態になる`() = runTest {
        // When
        cameraViewModel.initializeCamera(mockContext)

        // Then
        verify(mockCameraStateObserver).onChanged(CameraState.INITIALIZING)
        // 注意: 実際の実装では最終的にREADY状態になることを検証
    }

    @Test
    fun `initializeCamera_既に初期化済み_重複初期化しない`() = runTest {
        // Given
        cameraViewModel.initializeCamera(mockContext)
        reset(mockCameraStateObserver)

        // When
        cameraViewModel.initializeCamera(mockContext)

        // Then
        verifyNoInteractions(mockCameraStateObserver)
    }

    // ===== 画像キャプチャテスト =====

    @Test
    fun `captureImage_READY状態_キャプチャ実行`() = runTest {
        // Given
        cameraViewModel.initializeCamera(mockContext)
        // ViewModelの状態をREADYに設定（実装で行われる）

        // When
        cameraViewModel.captureImage()

        // Then
        verify(mockCameraStateObserver).onChanged(CameraState.CAPTURING)
    }

    @Test
    fun `captureImage_IDLE状態_キャプチャ不可`() = runTest {
        // When
        val result = cameraViewModel.captureImage()

        // Then
        assertFalse("IDLE状態ではキャプチャできないべき", result)
        verify(mockCameraStateObserver, never()).onChanged(CameraState.CAPTURING)
    }

    @Test
    fun `captureImage_CAPTURING状態_重複キャプチャ不可`() = runTest {
        // Given
        cameraViewModel.initializeCamera(mockContext)
        cameraViewModel.captureImage() // 最初のキャプチャ
        reset(mockCameraStateObserver)

        // When
        val result = cameraViewModel.captureImage() // 2回目のキャプチャ

        // Then
        assertFalse("処理中の重複キャプチャは不可", result)
    }

    // ===== 状態管理テスト =====

    @Test
    fun `isReady_READY状態_trueを返す`() {
        // Given - ViewModelをREADY状態に設定（実装で行われる）
        
        // When & Then
        // 注意: 実装後にアサーションを追加
    }

    @Test
    fun `isCapturing_CAPTURING状態_trueを返す`() {
        // Given - ViewModelをCAPTURING状態に設定
        
        // When & Then
        // 注意: 実装後にアサーションを追加
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

    // ===== リソース管理テスト =====

    @Test
    fun `releaseCamera_カメラ解放_IDLE状態に戻る`() {
        // Given
        cameraViewModel.initializeCamera(mockContext)

        // When
        cameraViewModel.releaseCamera()

        // Then
        verify(mockCameraStateObserver).onChanged(CameraState.IDLE)
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

    // ===== ViewModel ライフサイクルテスト =====

    @Test
    fun `releaseCamera_リソース解放_適切にクリーンアップ`() {
        // Given
        cameraViewModel.initializeCamera(mockContext)

        // When
        cameraViewModel.releaseCamera()

        // Then
        assertEquals("リソース解放後はIDLE状態になるべき", CameraState.IDLE, cameraViewModel.cameraState.value)
    }
}