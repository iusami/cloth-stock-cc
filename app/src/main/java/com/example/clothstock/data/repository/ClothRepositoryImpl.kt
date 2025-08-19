package com.example.clothstock.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import com.example.clothstock.data.database.ClothDao
import com.example.clothstock.data.database.ClothDatabase
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.FilterOptions
import com.example.clothstock.data.model.PaginationSearchParameters
import com.example.clothstock.data.model.DeletionResult
import com.example.clothstock.data.model.DeletionFailure

/**
 * ClothRepository の実装クラス
 * 
 * データアクセス層の具体的な実装を提供
 * DAOを使用してデータベース操作を実行し、バリデーションやエラーハンドリングを行う
 * 
 * 主要実装特徴:
 * - ClothDaoへの委譲による効率的なデータアクセス
 * - ClothItemのValidatableインターフェースを活用したバリデーション
 * - エラーハンドリングとレスポンス形式の統一
 * 
 * メモ検索機能統合:
 * Task 2でClothDaoにメモ検索機能が実装されたため、本リポジトリクラスも
 * 自動的にメモ検索機能を持つ。追加の実装変更は不要で、DAOの機能を活用。
 */
class ClothRepositoryImpl(
    private val clothDao: ClothDao
) : ClothRepository {

    // ===== CREATE操作 =====

    override suspend fun insertItem(clothItem: ClothItem): Long {
        // バリデーション実行
        validateClothItem(clothItem)
        
        return clothDao.insert(clothItem)
    }

    override suspend fun insertItems(clothItems: List<ClothItem>): List<Long> {
        // 全アイテムのバリデーション実行
        clothItems.forEach { validateClothItem(it) }
        
        return clothDao.insertAll(clothItems)
    }

    // ===== READ操作 =====

    override fun getAllItems(): Flow<List<ClothItem>> {
        return clothDao.getAllItems()
    }

    override suspend fun getItemById(id: Long): ClothItem? {
        return clothDao.getItemById(id)
    }

    override fun getItemsByCategory(category: String): Flow<List<ClothItem>> {
        return clothDao.getItemsByCategory(category)
    }

    override fun getItemsByColor(color: String): Flow<List<ClothItem>> {
        return clothDao.getItemsByColor(color)
    }

    override fun getItemsBySizeRange(minSize: Int, maxSize: Int): Flow<List<ClothItem>> {
        return clothDao.getItemsBySizeRange(minSize, maxSize)
    }

    override fun searchItems(
        category: String?,
        color: String?,
        minSize: Int?,
        maxSize: Int?
    ): Flow<List<ClothItem>> {
        return clothDao.searchItems(category, color, minSize, maxSize)
    }

    override fun getRecentItems(limit: Int): Flow<List<ClothItem>> {
        return clothDao.getRecentItems(limit)
    }

    override fun getItemsByDateRange(startTime: Long, endTime: Long): Flow<List<ClothItem>> {
        return clothDao.getItemsByDateRange(startTime, endTime)
    }

    // ===== UPDATE操作 =====

    override suspend fun updateItem(clothItem: ClothItem): Boolean {
        // バリデーション実行
        validateClothItem(clothItem)
        
        val updatedRows = clothDao.update(clothItem)
        return updatedRows > 0
    }

    override suspend fun updateItems(clothItems: List<ClothItem>): Int {
        // 全アイテムのバリデーション実行
        clothItems.forEach { validateClothItem(it) }
        
        return clothDao.updateAll(clothItems)
    }

    // ===== DELETE操作 =====

    override suspend fun deleteItem(clothItem: ClothItem): Boolean {
        val deletedRows = clothDao.delete(clothItem)
        return deletedRows > 0
    }

    override suspend fun deleteItemById(id: Long): Boolean {
        val deletedRows = clothDao.deleteById(id)
        return deletedRows > 0
    }

    override suspend fun deleteAllItems(): Int {
        return clothDao.deleteAll()
    }

    override suspend fun deleteItems(items: List<ClothItem>): DeletionResult {
        // 🟢 TDD Green Phase: 例外処理とバリデーション対応を追加
        if (items.isEmpty()) {
            return DeletionResult(
                totalRequested = 0,
                successfulDeletions = 0,
                failedDeletions = 0,
                failedItems = emptyList()
            )
        }
        
        // 改良された実装：例外処理とバリデーションエラーを適切に処理
        var successCount = 0
        val failures = mutableListOf<DeletionFailure>()
        
        for (item in items) {
            try {
                // バリデーション実行
                validateClothItem(item)
                
                // データベースから削除
                val deletedRows = clothDao.delete(item)
                if (deletedRows > 0) {
                    successCount++
                } else {
                    failures.add(
                        DeletionFailure(
                            itemId = item.id,
                            reason = "削除に失敗しました（対象が見つかりません）",
                            exception = null
                        )
                    )
                }
            } catch (validationException: IllegalArgumentException) {
                // バリデーションエラーの場合
                failures.add(
                    DeletionFailure(
                        itemId = item.id,
                        reason = "バリデーションエラー: ${validationException.message}",
                        exception = validationException
                    )
                )
            } catch (databaseException: RuntimeException) {
                // データベースエラーやその他のランタイムエラーの場合
                failures.add(
                    DeletionFailure(
                        itemId = item.id,
                        reason = "削除に失敗しました: ${databaseException.message ?: "不明なエラー"}",
                        exception = databaseException
                    )
                )
            }
        }
        
        return DeletionResult(
            totalRequested = items.size,
            successfulDeletions = successCount,
            failedDeletions = failures.size,
            failedItems = failures
        )
    }

    override suspend fun deleteItemWithFileCleanup(item: ClothItem): Boolean {
        // 削除処理では空画像パスを許可するため、カスタムバリデーションを実行
        validateClothItemForDeletion(item)
        
        // Step 1: データベースから削除を試行
        val deletedRows = clothDao.delete(item)
        if (deletedRows <= 0) {
            return false
        }
        
        // Step 2: 空の画像パスの場合はデータベース削除のみで成功
        if (item.imagePath.isBlank()) {
            return true
        }
        
        // Step 3: ファイル削除とロールバック処理
        return deleteFileWithRollback(item)
    }

    // ===== 統計・集計操作 =====

    override suspend fun getItemCount(): Int {
        return clothDao.getItemCount()
    }

    override suspend fun getItemCountByCategory(): Map<String, Int> {
        return clothDao.getItemCountByCategory().associate { it.category to it.count }
    }

    override suspend fun getItemCountByColor(): Map<String, Int> {
        return clothDao.getItemCountByColor().associate { it.color to it.count }
    }

    override suspend fun getItemCountBySize(): Map<Int, Int> {
        return clothDao.getItemCountBySize().associate { it.size to it.count }
    }

    // ===== Task4: フィルター・検索機能 =====

    override fun searchItemsByText(searchText: String?): Flow<List<ClothItem>> {
        // DAOレイヤーのメモ検索機能を活用
        // 色、カテゴリ、メモフィールドの部分一致検索を実行
        return clothDao.searchItemsByText(searchText)
    }

    override fun searchItemsWithFilters(
        sizeFilters: List<Int>?,
        colorFilters: List<String>?,
        categoryFilters: List<String>?,
        searchText: String?
    ): Flow<List<ClothItem>> {
        // 複合フィルター検索でメモ検索機能を統合活用
        // サイズ・色・カテゴリフィルター + テキスト検索（メモ含む）
        return clothDao.searchItemsWithFilters(sizeFilters, colorFilters, categoryFilters, searchText)
    }

    override suspend fun getAvailableFilterOptions(): FilterOptions {
        // 3つのDistinctクエリを並行実行してFilterOptionsオブジェクトを構築
        val sizes = clothDao.getDistinctSizes()
        val colors = clothDao.getDistinctColors()
        val categories = clothDao.getDistinctCategories()
        
        return FilterOptions(
            availableSizes = sizes,
            availableColors = colors,
            availableCategories = categories
        )
    }

    // ===== メンテナンス操作 =====

    override suspend fun checkDataIntegrity(): Boolean {
        return try {
            // 簡単な整合性チェックとしてデータ数を取得
            // 実際のPRAGMA integrity_checkはRoomでサポートされていないため
            clothDao.getItemCount()
            true // データアクセスが成功すれば基本的な整合性はOK
        } catch (e: Exception) {
            // データベースアクセスエラーの場合は整合性チェック失敗とみなす
            false
        }
    }

    override suspend fun optimizeDatabase() {
        // VACUUM、ANALYZEはRoomでサポートされていないため現在は何もしない
        // 必要に応じて将来的にSQLiteDatabaseを直接使用して実装可能
        // 
        // 現時点では以下のような基本的な最適化のみ実行:
        // - 不要な一時データの削除（将来実装予定）
        // - キャッシュクリア（将来実装予定）
    }

    override suspend fun clearCache() {
        // 将来のキャッシュ機能拡張用
        // 現在は何もしない
    }

    // ===== Task 12: プログレッシブローディング対応 =====

    override fun searchItemsWithPagination(
        parameters: PaginationSearchParameters
    ): Flow<List<ClothItem>> {
        return clothDao.searchItemsWithPagination(parameters)
    }

    override suspend fun getFilteredItemCount(
        sizeFilters: List<Int>?,
        colorFilters: List<String>?,
        categoryFilters: List<String>?,
        searchText: String?
    ): Int {
        // searchItemsWithFiltersと同じ条件でアイテム数をカウント
        // メモ検索対応により、searchTextは色・カテゴリ・メモフィールドを対象
        return clothDao.getFilteredItemCount(
            sizeFilters = sizeFilters,
            colorFilters = colorFilters,
            categoryFilters = categoryFilters,
            searchText = searchText
        )
    }

    // ===== プライベートヘルパーメソッド =====

    /**
     * ClothItemのバリデーションを実行
     * 
     * Validatableインターフェースで定義されたvalidate()メソッドを使用して
     * ClothItemとその埋め込まれたTagDataの整合性をチェックする
     * 
     * @param clothItem バリデーション対象のアイテム（Validatableを実装）
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    private fun validateClothItem(clothItem: ClothItem) {
        val validationResult = clothItem.validate() // Validatable.validate()を呼び出し
        if (!validationResult.isSuccess()) {
            throw IllegalArgumentException("バリデーションエラー: ${validationResult.getErrorMessage()}")
        }
    }
    
    /**
     * ファイル削除とロールバック処理を実行
     * 
     * @param item 削除対象のアイテム（データベースからは既に削除済み）
     * @return ファイル削除が成功した場合true、失敗の場合false
     */
    private suspend fun deleteFileWithRollback(item: ClothItem): Boolean {
        return try {
            val fileDeleted = com.example.clothstock.util.FileUtils.deleteImageFile(item.imagePath)
            
            if (!fileDeleted) {
                // ファイル削除失敗 - データベースロールバックを試行
                rollbackDatabaseDeletion(item)
                false
            } else {
                // ファイル削除成功
                true
            }
        } catch (e: java.io.IOException) {
            // ファイル削除で例外が発生 - データベースロールバックを試行
            rollbackDatabaseDeletion(item)
            false
        }
    }
    
    /**
     * データベース削除のロールバックを試行
     * 
     * @param item ロールバック対象のアイテム
     */
    private suspend fun rollbackDatabaseDeletion(item: ClothItem) {
        try {
            clothDao.insert(item)
        } catch (rollbackException: RuntimeException) {
            // ロールバック失敗 - 実環境では適切なロギング・アラート機能が必要
        }
    }
    
    /**
     * 削除処理用のClothItemバリデーションを実行
     * 
     * 通常のvalidateClothItem()と異なり、空の画像パスを許可する
     * 削除処理では画像パスが空でも問題ないため
     * 
     * @param clothItem バリデーション対象のアイテム
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    private fun validateClothItemForDeletion(clothItem: ClothItem) {
        // TagDataのバリデーションのみ実行（画像パスのチェックはスキップ）
        val tagValidation = clothItem.tagData.validate()
        require(tagValidation.isSuccess()) { 
            "バリデーションエラー: ${tagValidation.getErrorMessage()}" 
        }
        
        // メモの文字数制限チェック
        require(clothItem.memo.length <= ClothItem.MAX_MEMO_LENGTH) {
            "バリデーションエラー: メモが${ClothItem.MAX_MEMO_LENGTH}文字を超えています（現在: ${clothItem.memo.length}文字）"
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ClothRepositoryImpl? = null

        /**
         * ClothRepositoryImplのシングルトンインスタンスを取得
         * 
         * @param context アプリケーションコンテキスト
         * @return ClothRepositoryImplインスタンス
         */
        fun getInstance(context: Context): ClothRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                val database = ClothDatabase.getInstance(context)
                val instance = ClothRepositoryImpl(database.clothDao())
                INSTANCE = instance
                instance
            }
        }
    }
}