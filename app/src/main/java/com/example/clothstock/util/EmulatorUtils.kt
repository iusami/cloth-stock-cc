package com.example.clothstock.util

import android.os.Build
import android.util.Log

/**
 * エミュレーター環境検出とアクセシビリティ音声制御のユーティリティクラス
 * 
 * Androidエミュレーター（特にranchu）での音声エラー対策
 * - TalkBackとの音声競合回避
 * - PCM音声I/Oエラー対策
 */
object EmulatorUtils {
    private const val TAG = "EmulatorUtils"
    
    /**
     * エミュレーター環境かどうかを判定
     * 
     * @return エミュレーター環境の場合true
     */
    fun isEmulator(): Boolean {
        return try {
            // 複数の方法でエミュレーター環境を検出
            val isQemu = isQemuProperty()
            val isRanchu = isRanchuKernel()
            val isGoldfish = isGoldfishProperty()
            val isEmulatorBuild = isEmulatorBuild()
            
            val result = isQemu || isRanchu || isGoldfish || isEmulatorBuild
            
            Log.d(TAG, "Emulator detection results:")
            Log.d(TAG, "  - QEMU property: $isQemu")
            Log.d(TAG, "  - Ranchu kernel: $isRanchu")
            Log.d(TAG, "  - Goldfish property: $isGoldfish")
            Log.d(TAG, "  - Emulator build: $isEmulatorBuild")
            Log.d(TAG, "  - Final result: $result")
            
            result
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException detecting emulator environment", e)
            false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException detecting emulator environment", e)
            false
        }
    }
    
    /**
     * QEMUプロパティによる検出
     */
    private fun isQemuProperty(): Boolean {
        return try {
            val qemuProperty = getSystemProperty("ro.kernel.qemu", "0")
            qemuProperty == "1"
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException checking QEMU property", e)
            false
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "ClassNotFoundException checking QEMU property", e)
            false
        }
    }
    
    /**
     * ranchuカーネルによる検出
     */
    private fun isRanchuKernel(): Boolean {
        return try {
            val kernelVersion = getSystemProperty("ro.kernel.version", "")
            kernelVersion.contains("ranchu") || kernelVersion.contains("goldfish")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException checking ranchu kernel", e)
            false
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "ClassNotFoundException checking ranchu kernel", e)
            false
        }
    }
    
    /**
     * goldfishプロパティによる検出
     */
    private fun isGoldfishProperty(): Boolean {
        return try {
            val hardware = getSystemProperty("ro.hardware", "")
            hardware.contains("goldfish") || hardware.contains("ranchu")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException checking goldfish property", e)
            false
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "ClassNotFoundException checking goldfish property", e)
            false
        }
    }
    
    /**
     * ビルド情報による検出
     */
    private fun isEmulatorBuild(): Boolean {
        return try {
            val fingerprint = Build.FINGERPRINT
            val product = Build.PRODUCT
            val model = Build.MODEL
            val brand = Build.BRAND
            
            val result = fingerprint.startsWith("generic") ||
                    fingerprint.startsWith("unknown") ||
                    model.contains("google_sdk") ||
                    model.contains("Emulator") ||
                    model.contains("Android SDK") ||
                    product.contains("sdk") ||
                    product.contains("emulator") ||
                    brand.contains("generic")
            
            Log.d(TAG, "Build detection - Fingerprint: $fingerprint, Product: $product, Model: $model, Brand: $brand")
            
            result
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException checking build properties", e)
            false
        } catch (e: IllegalAccessException) {
            Log.w(TAG, "IllegalAccessException checking build properties", e)
            false
        }
    }
    
    /**
     * システムプロパティを安全に取得
     */
    private fun getSystemProperty(key: String, defaultValue: String): String {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java, String::class.java)
            method.invoke(null, key, defaultValue) as String
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "ClassNotFoundException getting system property $key", e)
            defaultValue
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "NoSuchMethodException getting system property $key", e)
            defaultValue
        }
    }
    
    /**
     * アクセシビリティ音声アナウンスが安全に実行可能かチェック
     * 
     * @return 音声アナウンスを実行しても安全な場合true
     */
    fun isAccessibilityAudioSafe(): Boolean {
        val isEmulatorEnv = isEmulator()
        val result = !isEmulatorEnv
        
        Log.d(TAG, "Accessibility audio safety check: isEmulator=$isEmulatorEnv, safe=$result")
        
        return result
    }
}
