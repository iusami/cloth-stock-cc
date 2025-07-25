package com.example.clothstock.data.database

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room データベース用のTypeConverter
 * 
 * カスタムデータ型をデータベースで扱えるプリミティブ型に変換する
 */
class Converters {
    
    /**
     * Date から Long への変換（保存時）
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    /**
     * Long から Date への変換（読み込み時）
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}