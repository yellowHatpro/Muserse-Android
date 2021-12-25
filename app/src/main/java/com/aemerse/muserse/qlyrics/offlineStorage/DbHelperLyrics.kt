package com.aemerse.muserse.qlyrics.offlineStorage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class DbHelperLyrics internal constructor(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(TABLE_CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    companion object {
        private val DATABASE_VERSION: Int = 2
        private val DATABASE_NAME: String = "lyrics"
        val KEY_TITLE: String = "song_title"
        val _ID: String = "_id"
        val LYRICS: String = "lyric"
        val TABLE_NAME: String = "offline_lyrics"
        private val TABLE_CREATE: String = ("CREATE TABLE IF NOT EXISTS "
                + TABLE_NAME + " (" + _ID + " INTEGER, " + KEY_TITLE + " TEXT, "
                + LYRICS + " TEXT);")
    }
}