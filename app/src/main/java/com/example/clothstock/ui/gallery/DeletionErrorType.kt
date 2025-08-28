package com.example.clothstock.ui.gallery

/**
 * 削除エラーの種類を分類するenum
 * 
 * Requirements 5.1-5.3に対応した削除エラーカテゴリー
 * 各エラータイプに対して適切なユーザーメッセージと対処法を提供
 */
enum class DeletionErrorType {
    /**
     * ファイル削除権限エラー (Requirement 5.1)
     * ファイルシステムへのアクセス権限が不足している場合
     */
    FILE_PERMISSION_DENIED,
    
    /**
     * データベーストランザクション失敗 (Requirement 5.2)
     * データベース削除処理で整合性エラーが発生した場合
     */
    DATABASE_TRANSACTION_FAILED,
    
    /**
     * ネットワーク・ストレージアクセス不可 (Requirement 5.3)
     * ネットワーク接続やストレージアクセスに問題がある場合
     */
    NETWORK_STORAGE_UNAVAILABLE,
    
    /**
     * 部分削除発生 (Requirement 5.4)
     * マルチアイテム操作で一部のみ成功した場合
     */
    PARTIAL_DELETION_OCCURRED,
    
    /**
     * 不明な削除エラー
     * 上記以外の予期しないエラーが発生した場合
     */
    UNKNOWN_DELETION_ERROR;
    
    /**
     * エラータイプに基づく適切なリトライ可能性判定
     * 
     * @return リトライが有効かどうか
     */
    fun isRetryable(): Boolean {
        return when (this) {
            NETWORK_STORAGE_UNAVAILABLE -> true  // ネットワーク問題は一時的な可能性
            DATABASE_TRANSACTION_FAILED -> true  // DB問題も再試行で解決する可能性
            FILE_PERMISSION_DENIED -> false      // 権限問題は設定変更が必要
            PARTIAL_DELETION_OCCURRED -> true    // 失敗分のみ再試行可能
            UNKNOWN_DELETION_ERROR -> true       // 不明なエラーは再試行を許可
        }
    }
    
    /**
     * エラータイプに基づくメトリクスキー取得
     * 
     * @return エラーメトリクス追跡用キー
     */
    fun getMetricsKey(): String {
        return when (this) {
            FILE_PERMISSION_DENIED -> "FILE_PERMISSION_ERROR"
            DATABASE_TRANSACTION_FAILED -> "DATABASE_DELETION_ERROR"
            NETWORK_STORAGE_UNAVAILABLE -> "NETWORK_STORAGE_ERROR"
            PARTIAL_DELETION_OCCURRED -> "PARTIAL_DELETION_ERROR"
            UNKNOWN_DELETION_ERROR -> "UNKNOWN_DELETION_ERROR"
        }
    }
}
