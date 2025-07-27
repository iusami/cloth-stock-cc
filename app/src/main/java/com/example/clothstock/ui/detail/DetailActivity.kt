package com.example.clothstock.ui.detail

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.clothstock.R

/**
 * 衣服アイテム詳細表示Activity
 * 
 * TDD Redフェーズ - 最小限実装
 * フルサイズ画像とタグ情報を表示する
 */
class DetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CLOTH_ITEM_ID = "extra_cloth_item_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        
        // TODO: TDD Greenフェーズで実装
        // 現在はレイアウトエラーでテストが失敗する状態（Red）
    }
}