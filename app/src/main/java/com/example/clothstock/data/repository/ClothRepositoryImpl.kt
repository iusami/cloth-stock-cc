package com.example.clothstock.data.repository

import kotlinx.coroutines.flow.Flow
import com.example.clothstock.data.database.ClothDao
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

    // ===== メンテナンス操作 =====

    override suspend fun checkDataIntegrity(): Boolean {
        return try {
            val integrityResults = clothDao.checkIntegrity()
            // "ok" が返されれば整合性に問題なし
            integrityResults.isNotEmpty() && integrityResults[0].equals("ok", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun optimizeDatabase() {
        try {
            clothDao.vacuum()
            clothDao.analyze()
        } catch (e: Exception) {
            // ログ出力やエラー通知を行う（実装は後で追加）
            throw e
        }
    }

    override suspend fun clearCache() {
        // 将来のキャッシュ機能拡張用
        // 現在は何もしない
    }

    // ===== プライベートヘルパーメソッド =====

    /**
     * ClothItemのバリデーションを実行
     * 
     * @param clothItem バリデーション対象のアイテム
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    private fun validateClothItem(clothItem: ClothItem) {
        val validationResult = clothItem.validate()
        if (!validationResult.isValid) {
            throw IllegalArgumentException("バリデーションエラー: ${validationResult.errors.joinToString(", ")}")
        }
    }
}