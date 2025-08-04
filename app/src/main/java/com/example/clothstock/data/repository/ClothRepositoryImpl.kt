package com.example.clothstock.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import com.example.clothstock.data.database.ClothDao
import com.example.clothstock.data.database.ClothDatabase
import com.example.clothstock.data.model.ClothItem

/**
 * ClothRepository の実装クラス
 * 
 * データアクセス層の具体的な実装を提供
 * DAOを使用してデータベース操作を実行し、バリデーションやエラーハンドリングを行う
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
        return clothDao.searchItemsByText(searchText)
    }

    override fun searchItemsWithFilters(
        sizeFilters: List<Int>?,
        colorFilters: List<String>?,
        categoryFilters: List<String>?,
        searchText: String?
    ): Flow<List<ClothItem>> {
        return clothDao.searchItemsWithFilters(sizeFilters, colorFilters, categoryFilters, searchText)
    }

    override suspend fun getAvailableFilterOptions(): com.example.clothstock.data.model.FilterOptions {
        // 3つのDistinctクエリを並行実行してFilterOptionsオブジェクトを構築
        val sizes = clothDao.getDistinctSizes()
        val colors = clothDao.getDistinctColors()
        val categories = clothDao.getDistinctCategories()
        
        return com.example.clothstock.data.model.FilterOptions(
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