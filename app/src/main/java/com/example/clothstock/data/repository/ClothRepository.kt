package com.example.clothstock.data.repository

import kotlinx.coroutines.flow.Flow
import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.FilterOptions
import com.example.clothstock.data.model.PaginationSearchParameters
import com.example.clothstock.data.model.DeletionResult

/**
 * 衣服データアクセスのリポジトリインターフェース
 * 
 * データアクセス層の抽象化を提供し、ビジネスロジックとデータソースを分離
 * ViewModelや他の上位層はこのインターフェースを通してデータアクセスを行う
 * 
 * 主要機能:
 * - CRUD操作（作成・読取・更新・削除）
 * - 高度な検索機能（色・カテゴリ・メモフィールド対象）
 * - フィルター検索とテキスト検索の組み合わせ
 * - プログレッシブローディング対応
 * - データ統計・集計機能
 * 
 * メモ検索機能統合:
 * Task 2で実装されたメモ検索機能により、全ての検索メソッドでメモ内容を対象とした
 * 部分一致検索が可能。パフォーマンス最適化のため専用インデックスを活用。
 */
interface ClothRepository {

    // ===== CREATE操作 =====

    /**
     * 新しい衣服アイテムを挿入
     * 
     * @param clothItem 挿入するアイテム（バリデーション済み）
     * @return 生成されたアイテムのID
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    suspend fun insertItem(clothItem: ClothItem): Long

    /**
     * 複数の衣服アイテムを一括挿入
     * 
     * @param clothItems 挿入するアイテムのリスト
     * @return 生成されたアイテムIDのリスト
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    suspend fun insertItems(clothItems: List<ClothItem>): List<Long>

    // ===== READ操作 =====

    /**
     * すべての衣服アイテムを取得（作成日時の降順）
     * 
     * @return 衣服アイテムのFlow
     */
    fun getAllItems(): Flow<List<ClothItem>>

    /**
     * IDで衣服アイテムを取得
     * 
     * @param id 取得するアイテムのID
     * @return 指定されたアイテム（存在しない場合はnull）
     */
    suspend fun getItemById(id: Long): ClothItem?

    /**
     * カテゴリで衣服アイテムを検索
     * 
     * @param category 検索するカテゴリ
     * @return 指定カテゴリのアイテムのFlow
     */
    fun getItemsByCategory(category: String): Flow<List<ClothItem>>

    /**
     * 色で衣服アイテムを検索
     * 
     * @param color 検索する色
     * @return 指定色のアイテムのFlow
     */
    fun getItemsByColor(color: String): Flow<List<ClothItem>>

    /**
     * サイズ範囲で衣服アイテムを検索
     * 
     * @param minSize 最小サイズ
     * @param maxSize 最大サイズ
     * @return 指定サイズ範囲のアイテムのFlow
     */
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
    fun searchItems(
        category: String? = null,
        color: String? = null,
        minSize: Int? = null,
        maxSize: Int? = null
    ): Flow<List<ClothItem>>

    /**
     * 最近追加されたアイテムを取得
     * 
     * @param limit 取得する件数（デフォルト10件）
     * @return 最近のアイテムのFlow
     */
    fun getRecentItems(limit: Int = 10): Flow<List<ClothItem>>

    /**
     * 指定期間内のアイテムを取得
     * 
     * @param startTime 開始時間（ミリ秒）
     * @param endTime 終了時間（ミリ秒）
     * @return 期間内のアイテムのFlow
     */
    fun getItemsByDateRange(startTime: Long, endTime: Long): Flow<List<ClothItem>>

    // ===== UPDATE操作 =====

    /**
     * 既存の衣服アイテムを更新
     * 
     * @param clothItem 更新するアイテム（バリデーション済み）
     * @return 更新が成功した場合true、失敗した場合false
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    suspend fun updateItem(clothItem: ClothItem): Boolean

    /**
     * 複数の衣服アイテムを一括更新
     * 
     * @param clothItems 更新するアイテムのリスト
     * @return 更新された行数
     * @throws IllegalArgumentException バリデーションエラーの場合
     */
    suspend fun updateItems(clothItems: List<ClothItem>): Int

    // ===== DELETE操作 =====

    /**
     * 衣服アイテムを削除
     * 
     * @param clothItem 削除するアイテム
     * @return 削除が成功した場合true、失敗した場合false
     */
    suspend fun deleteItem(clothItem: ClothItem): Boolean

    /**
     * IDで衣服アイテムを削除
     * 
     * @param id 削除するアイテムのID
     * @return 削除が成功した場合true、失敗した場合false
     */
    suspend fun deleteItemById(id: Long): Boolean

    /**
     * すべての衣服アイテムを削除
     * 
     * @return 削除された行数
     */
    suspend fun deleteAllItems(): Int

    /**
     * 複数の衣服アイテムを一括削除（バッチ削除）
     * 
     * データベースとファイルシステムの両方から削除を行い、
     * 削除結果の詳細情報を提供する
     * 
     * @param items 削除するアイテムのリスト
     * @return 削除操作の結果（成功・失敗の詳細情報）
     */
    suspend fun deleteItems(items: List<ClothItem>): DeletionResult

    /**
     * ファイルクリーンアップ付きアイテム削除
     * 
     * データベースからのアイテム削除と関連するファイル削除を
     * 協調して実行する。どちらかが失敗した場合は適切にロールバックする
     * 
     * @param item 削除するアイテム
     * @return 削除が成功した場合true、失敗した場合false
     */
    suspend fun deleteItemWithFileCleanup(item: ClothItem): Boolean

    // ===== 統計・集計操作 =====

    /**
     * 総アイテム数を取得
     * 
     * @return 総アイテム数
     */
    suspend fun getItemCount(): Int

    /**
     * カテゴリ別のアイテム数を取得
     * 
     * @return カテゴリ名をキー、アイテム数を値とするMap
     */
    suspend fun getItemCountByCategory(): Map<String, Int>

    /**
     * 色別のアイテム数を取得
     * 
     * @return 色名をキー、アイテム数を値とするMap
     */
    suspend fun getItemCountByColor(): Map<String, Int>

    /**
     * サイズ別のアイテム数を取得
     * 
     * @return サイズをキー、アイテム数を値とするMap
     */
    suspend fun getItemCountBySize(): Map<Int, Int>

    // ===== メンテナンス操作 =====

    /**
     * データベースの整合性をチェック
     * 
     * @return 整合性チェック結果
     */
    suspend fun checkDataIntegrity(): Boolean

    /**
     * データベースを最適化
     * 注意: この操作は時間がかかる可能性があります
     */
    suspend fun optimizeDatabase()

    /**
     * キャッシュをクリア（将来のキャッシュ機能拡張用）
     */
    suspend fun clearCache()

    // ===== Task4: フィルター・検索機能 =====

    /**
     * テキスト検索で衣服アイテムを検索
     * 色、カテゴリ、メモフィールドを対象に部分一致検索を実行
     * 
     * 検索対象フィールド:
     * - color: 色フィールドでの部分一致
     * - category: カテゴリフィールドでの部分一致  
     * - memo: メモフィールドでの部分一致（Task 2で追加）
     * 
     * パフォーマンス考慮:
     * - OR条件による複数フィールド検索（短絡評価活用）
     * - memoフィールド専用インデックスを活用（migration v1→v2で追加）
     * 
     * @param searchText 検索テキスト（空の場合は全件取得）
     * @return 検索条件に合致するアイテムのFlow
     */
    fun searchItemsByText(searchText: String?): Flow<List<ClothItem>>

    /**
     * 複合フィルター条件で衣服アイテムを検索
     * サイズ、色、カテゴリの複数条件とテキスト検索（メモ含む）を組み合わせ
     * 
     * フィルター処理:
     * - 各フィルターはAND条件で組み合わせ
     * - searchTextは色、カテゴリ、メモフィールドをOR条件で検索
     * - nullまたは空のフィルターは無視
     * 
     * 検索順序最適化:
     * - 高頻度検索フィールド（color, category）を優先配置
     * - memo検索は専用インデックスを活用
     * 
     * @param sizeFilters サイズフィルター（nullまたは空の場合は無視）
     * @param colorFilters 色フィルター（nullまたは空の場合は無視）
     * @param categoryFilters カテゴリフィルター（nullまたは空の場合は無視）
     * @param searchText 検索テキスト（色・カテゴリ・メモ対象、nullまたは空の場合は無視）
     * @return フィルター条件に合致するアイテムのFlow
     */
    fun searchItemsWithFilters(
        sizeFilters: List<Int>?,
        colorFilters: List<String>?,
        categoryFilters: List<String>?,
        searchText: String?
    ): Flow<List<ClothItem>>

    /**
     * 利用可能なフィルターオプションを取得
     * データベースに存在する重複なしのサイズ、色、カテゴリリストを取得
     * 
     * @return フィルターオプション情報
     * @throws RuntimeException データアクセスエラーの場合
     */
    suspend fun getAvailableFilterOptions(): FilterOptions

    // ===== Task 12: プログレッシブローディング対応 =====

    /**
     * 複合フィルター条件で衣服アイテムをページネーションで検索
     * プログレッシブローディング機能で使用される
     * 
     * @param parameters 検索パラメーター（フィルターとページネーション情報を含む）
     * @return フィルター条件に合致するアイテムのFlow（ページネーション適用済み）
     */
    fun searchItemsWithPagination(
        parameters: PaginationSearchParameters
    ): Flow<List<ClothItem>>

    /**
     * 複合フィルター条件に合致する総アイテム数を取得
     * プログレッシブローディングでの「残りデータ有無」判定に使用（メモ検索対応）
     * 
     * 検索条件:
     * - searchItemsWithFilters()と同じ条件でカウント実行
     * - searchTextは色、カテゴリ、メモフィールドを対象
     * - フィルターとテキスト検索の組み合わせに対応
     * 
     * @param sizeFilters サイズフィルター（nullまたは空の場合は無視）
     * @param colorFilters 色フィルター（nullまたは空の場合は無視）
     * @param categoryFilters カテゴリフィルター（nullまたは空の場合は無視）
     * @param searchText 検索テキスト（色・カテゴリ・メモ対象、nullまたは空の場合は無視）
     * @return 条件に合致する総アイテム数
     */
    suspend fun getFilteredItemCount(
        sizeFilters: List<Int>?,
        colorFilters: List<String>?,
        categoryFilters: List<String>?,
        searchText: String?
    ): Int
}