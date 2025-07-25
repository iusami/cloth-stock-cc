package com.example.clothstock.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.CategoryCount
import com.example.clothstock.data.model.ColorCount
import com.example.clothstock.data.model.SizeCount

/**
 * ClothItem のデータアクセスオブジェクト (DAO)
 * 
 * データベースへのCRUD操作と検索クエリを定義
 */
@Dao
interface ClothDao {

    // ===== INSERT操作 =====

    /**
     * 新しい衣服アイテムを挿入
     * 
     * @param clothItem 挿入するアイテム
     * @return 生成されたアイテムのID
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(clothItem: ClothItem): Long

    /**
     * 複数の衣服アイテムを一括挿入
     * 
     * @param clothItems 挿入するアイテムのリスト
     * @return 生成されたアイテムIDのリスト
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(clothItems: List<ClothItem>): List<Long>

    // ===== UPDATE操作 =====

    /**
     * 既存の衣服アイテムを更新
     * 
     * @param clothItem 更新するアイテム
     * @return 更新された行数
     */
    @Update
    suspend fun update(clothItem: ClothItem): Int

    /**
     * 複数の衣服アイテムを一括更新
     * 
     * @param clothItems 更新するアイテムのリスト
     * @return 更新された行数
     */
    @Update
    suspend fun updateAll(clothItems: List<ClothItem>): Int

    // ===== DELETE操作 =====

    /**
     * 衣服アイテムを削除
     * 
     * @param clothItem 削除するアイテム
     * @return 削除された行数
     */
    @Delete
    suspend fun delete(clothItem: ClothItem): Int

    /**
     * IDで衣服アイテムを削除
     * 
     * @param id 削除するアイテムのID
     * @return 削除された行数
     */
    @Query("DELETE FROM cloth_items WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * すべての衣服アイテムを削除
     * 
     * @return 削除された行数
     */
    @Query("DELETE FROM cloth_items")
    suspend fun deleteAll(): Int

    // ===== SELECT操作 =====

    /**
     * すべての衣服アイテムを取得（作成日時の降順）
     * 
     * @return 衣服アイテムのFlow
     */
    @Query("SELECT * FROM cloth_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<ClothItem>>

    /**
     * IDで衣服アイテムを取得
     * 
     * @param id 取得するアイテムのID
     * @return 指定されたアイテム（存在しない場合はnull）
     */
    @Query("SELECT * FROM cloth_items WHERE id = :id")
    suspend fun getItemById(id: Long): ClothItem?

    /**
     * カテゴリで衣服アイテムを検索
     * 
     * @param category 検索するカテゴリ
     * @return 指定カテゴリのアイテムのFlow
     */
    @Query("SELECT * FROM cloth_items WHERE category = :category ORDER BY createdAt DESC")
    fun getItemsByCategory(category: String): Flow<List<ClothItem>>

    /**
     * 色で衣服アイテムを検索
     * 
     * @param color 検索する色
     * @return 指定色のアイテムのFlow
     */
    @Query("SELECT * FROM cloth_items WHERE color = :color ORDER BY createdAt DESC")
    fun getItemsByColor(color: String): Flow<List<ClothItem>>

    /**
     * サイズ範囲で衣服アイテムを検索
     * 
     * @param minSize 最小サイズ
     * @param maxSize 最大サイズ
     * @return 指定サイズ範囲のアイテムのFlow
     */
    @Query("SELECT * FROM cloth_items WHERE size BETWEEN :minSize AND :maxSize ORDER BY size ASC")
    fun getItemsBySizeRange(minSize: Int, maxSize: Int): Flow<List<ClothItem>>

    /**
     * 複合条件で衣服アイテムを検索
     * 
     * @param category カテゴリ（nullの場合は無視）
     * @param color 色（nullの場合は無視）
     * @param minSize 最小サイズ（nullの場合は無視）
     * @param maxSize 最大サイズ（nullの場合は無視）
     * @return 条件に合致するアイテムのFlow
     */
    @Query("""
        SELECT * FROM cloth_items 
        WHERE (:category IS NULL OR category = :category)
        AND (:color IS NULL OR color = :color)
        AND (:minSize IS NULL OR size >= :minSize)
        AND (:maxSize IS NULL OR size <= :maxSize)
        ORDER BY createdAt DESC
    """)
    fun searchItems(
        category: String? = null,
        color: String? = null,
        minSize: Int? = null,
        maxSize: Int? = null
    ): Flow<List<ClothItem>>

    // ===== 統計・集計操作 =====

    /**
     * 総アイテム数を取得
     * 
     * @return 総アイテム数
     */
    @Query("SELECT COUNT(*) FROM cloth_items")
    suspend fun getItemCount(): Int

    /**
     * カテゴリ別のアイテム数を取得
     * 
     * @return カテゴリ別のアイテム数リスト
     */
    @Query("SELECT category, COUNT(*) as count FROM cloth_items GROUP BY category")
    suspend fun getItemCountByCategory(): List<CategoryCount>

    /**
     * 色別のアイテム数を取得
     * 
     * @return 色別のアイテム数リスト
     */
    @Query("SELECT color, COUNT(*) as count FROM cloth_items GROUP BY color")
    suspend fun getItemCountByColor(): List<ColorCount>

    /**
     * サイズ別のアイテム数を取得
     * 
     * @return サイズ別のアイテム数リスト
     */
    @Query("SELECT size, COUNT(*) as count FROM cloth_items GROUP BY size ORDER BY size")
    suspend fun getItemCountBySize(): List<SizeCount>

    /**
     * 最近追加されたアイテムを取得
     * 
     * @param limit 取得する件数
     * @return 最近のアイテムのFlow
     */
    @Query("SELECT * FROM cloth_items ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentItems(limit: Int = 10): Flow<List<ClothItem>>

    /**
     * 指定期間内のアイテムを取得
     * 
     * @param startTime 開始時間（ミリ秒）
     * @param endTime 終了時間（ミリ秒）
     * @return 期間内のアイテムのFlow
     */
    @Query("SELECT * FROM cloth_items WHERE createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    fun getItemsByDateRange(startTime: Long, endTime: Long): Flow<List<ClothItem>>

    // ===== メンテナンス操作 =====

    /**
     * データベースの整合性をチェック
     * 
     * @return 整合性チェック結果
     */
    @Query("PRAGMA integrity_check")
    suspend fun checkIntegrity(): List<String>

    /**
     * データベースを最適化（VACUUM）
     * 注意: この操作は時間がかかる可能性があります
     */
    @Query("VACUUM")
    suspend fun vacuum()

    /**
     * 統計情報を更新（ANALYZE）
     */
    @Query("ANALYZE")
    suspend fun analyze()
}