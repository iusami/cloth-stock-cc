package com.example.clothstock.data.model

/**
 * バリデーション可能なオブジェクトを表すインターフェース
 * 
 * データモデルにバリデーション機能を統一的に提供する
 */
interface Validatable {
    
    /**
     * オブジェクトが有効かどうかを判定
     * 
     * @return バリデーション結果
     */
    fun validate(): ValidationResult
    
    /**
     * 簡単な有効性チェック（従来のisValid()と互換性維持）
     * 
     * @return 有効な場合true
     */
    fun isValid(): Boolean {
        return validate().isSuccess()
    }
    
    /**
     * バリデーションエラーメッセージを取得
     * 
     * @return エラーメッセージ（有効な場合はnull）
     */
    fun getValidationError(): String? {
        return validate().getErrorMessage()
    }
}