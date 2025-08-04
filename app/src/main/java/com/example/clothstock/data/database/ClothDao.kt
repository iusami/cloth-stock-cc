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

    // ===== Task3: フィルター・検索機能 =====

    /**
     * テキスト検索で衣服アイテムを検索
     * 色、カテゴリフィールドを対象に部分一致検索を実行
     * 
     * パフォーマンス考慮:
     * - LIKE演算子の前方一致を優先することでインデックス活用を促進
     * - OR条件の短絡評価を活用
     * - NULL値と空文字列の効率的なハンドリング
     * 
     * @param searchText 検索テキスト（空の場合は全件取得）
     * @return 検索条件に合致するアイテムのFlow（作成日時降順）
     */
    @Query("""
        SELECT * FROM cloth_items 
        WHERE (:searchText IS NULL OR :searchText = '' OR 
               color LIKE '%' || :searchText || '%' OR 
               category LIKE '%' || :searchText || '%')
        ORDER BY createdAt DESC
    """)
    fun searchItemsByText(searchText: String?): Flow<List<ClothItem>>

    /**
     * 複合フィルター条件で衣服アイテムを検索
     * サイズ、色、カテゴリの複数条件とテキスト検索を組み合わせ
     * 
     * 注意: Roomの制約により、空のリストはnullに変換して渡すこと
     * 
     * @param sizeFilters サイズフィルター（nullまたは空の場合は無視）
     * @param colorFilters 色フィルター（nullまたは空の場合は無視）
     * @param categoryFilters カテゴリフィルター（nullまたは空の場合は無視）
     * @param searchText 検索テキスト（nullまたは空の場合は無視）
     * @return フィルター条件に合致するアイテムのFlow
     */
    @Query("""
        SELECT * FROM cloth_items 
        WHERE (:sizeFilters IS NULL OR size IN (:sizeFilters))
        AND (:colorFilters IS NULL OR color IN (:colorFilters))
        AND (:categoryFilters IS NULL OR category IN (:categoryFilters))
        AND (:searchText IS NULL OR :searchText = '' OR 
             color LIKE '%' || :searchText || '%' OR 
             category LIKE '%' || :searchText || '%')
        ORDER BY createdAt DESC
    """)
    fun searchItemsWithFiltersInternal(
        sizeFilters: List<Int>?,
        colorFilters: List<String>?,
        categoryFilters: List<String>?,
        searchText: String?
    ): Flow<List<ClothItem>>

    /**
     * 複合フィルター条件で衣服アイテムを検索（空リスト対応版）
     * 空のリストを適切にnullに変換してから内部メソッドを呼び出す
     * 
     * @param sizeFilters サイズフィルター（nullまたは空の場合は無視）
     * @param colorFilters 色フィルター（nullまたは空の場合は無視）
     * @param categoryFilters カテゴリフィルター（nullまたは空の場合は無視）
     * @param searchText 検索テキスト（nullまたは空の場合は無視）
     * @return フィルター条件に合致するアイテムのFlow
     */
    fun searchItemsWithFilters(
        sizeFilters: List<Int>?,
        colorFilters: List<String>?,
        categoryFilters: List<String>?,
        searchText: String?
    ): Flow<List<ClothItem>> {
        return searchItemsWithFiltersInternal(
            sizeFilters = sizeFilters?.takeIf { it.isNotEmpty() },
            colorFilters = colorFilters?.takeIf { it.isNotEmpty() },
            categoryFilters = categoryFilters?.takeIf { it.isNotEmpty() },
            searchText = searchText?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * データベースに存在する重複なしのサイズリストを取得
     * フィルターオプション生成に使用
     * 
     * @return 昇順ソートされたサイズリスト
     */
    @Query("SELECT DISTINCT size FROM cloth_items ORDER BY size")
    suspend fun getDistinctSizes(): List<Int>

    /**
     * データベースに存在する重複なしの色リストを取得
     * フィルターオプション生成に使用
     * 
     * @return アルファベット順ソートされた色リスト
     */
    @Query("SELECT DISTINCT color FROM cloth_items ORDER BY color")
    suspend fun getDistinctColors(): List<String>

    /**
     * データベースに存在する重複なしのカテゴリリストを取得
     * フィルターオプション生成に使用
     * 
     * @return アルファベット順ソートされたカテゴリリスト
     */
    @Query("SELECT DISTINCT category FROM cloth_items ORDER BY category")
    suspend fun getDistinctCategories(): List<String>

    // ===== メンテナンス操作 =====
    // 注意: PRAGMA、VACUUM、ANALYZEはRoomでサポートされていないため
    // これらの操作は上位層で直接SQLiteDatabaseインスタンスを使用して実装する
}