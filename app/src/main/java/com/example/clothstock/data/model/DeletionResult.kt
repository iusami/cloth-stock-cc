package com.example.clothstock.data.model

/**
 * 削除操作の結果を追跡するためのデータクラス
 * 
 * バッチ削除操作の成功・失敗状況を管理し、
 * 詳細なエラー情報と統計情報を提供する
 * 
 * @property totalRequested 削除を要求されたアイテムの総数
 * @property successfulDeletions 正常に削除されたアイテム数
 * @property failedDeletions 削除に失敗したアイテム数
 * @property failedItems 削除に失敗したアイテムの詳細情報リスト
 */
data class DeletionResult(
    val totalRequested: Int,
    val successfulDeletions: Int,
    val failedDeletions: Int,
    val failedItems: List<DeletionFailure> = emptyList()
) {
    
    /**
     * 全ての削除操作が成功したかどうか
     * 
     * @return 失敗したアイテムが0個の場合true、そうでなければfalse
     */
    val isCompleteSuccess: Boolean = failedDeletions == 0
    
    /**
     * 一部のアイテムが成功し、一部が失敗したかどうか
     * 
     * @return 成功と失敗の両方が存在する場合true、そうでなければfalse
     */
    val isPartialSuccess: Boolean = successfulDeletions > 0 && failedDeletions > 0
    
    /**
     * 全ての削除操作が失敗したかどうか
     * 
     * @return 成功したアイテムが0個で、かつ要求された削除があった場合true、そうでなければfalse
     */
    val isCompleteFailure: Boolean = successfulDeletions == 0 && totalRequested > 0
}

/**
 * 個別アイテムの削除失敗情報を表すデータクラス
 * 
 * 削除に失敗したアイテムの詳細情報を保持し、
 * エラーの原因と例外情報を提供する
 * 
 * @property itemId 削除に失敗したアイテムのID
 * @property reason 削除失敗の理由（人間が読める形式）
 * @property exception 発生した例外（存在する場合）
 */
data class DeletionFailure(
    val itemId: Long,
    val reason: String,
    val exception: Throwable?
)
