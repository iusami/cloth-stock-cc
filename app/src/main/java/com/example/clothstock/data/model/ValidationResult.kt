package com.example.clothstock.data.model

/**
 * バリデーション結果を表現するクラス
 * 
 * 成功/失敗の状態とエラーメッセージを保持する
 */
sealed class ValidationResult {
    
    /**
     * バリデーション成功
     */
    object Success : ValidationResult()
    
    /**
     * バリデーション失敗
     * 
     * @param message エラーメッセージ
     * @param field エラーが発生したフィールド名（オプション）
     */
    data class Error(
        val message: String,
        val field: String? = null
    ) : ValidationResult()
    
    /**
     * 成功かどうかを判定
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * 失敗かどうかを判定
     */
    fun isError(): Boolean = this is Error
    
    /**
     * エラーメッセージを取得（成功時はnull）
     */
    fun getErrorMessage(): String? {
        return when (this) {
            is Success -> null
            is Error -> message
        }
    }
    
    companion object {
        /**
         * 成功結果を作成
         */
        fun success(): ValidationResult = Success
        
        /**
         * エラー結果を作成
         */
        fun error(message: String, field: String? = null): ValidationResult {
            return Error(message, field)
        }
    }
}