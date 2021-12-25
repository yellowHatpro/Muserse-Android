package com.aemerse.muserse.qlyrics.offlineStorage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class DbHelperArtistBio constructor(context: Context?) :
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
        private val DATABASE_NAME: String = "artist_bio"
        val KEY_ARTIST: String = "song_artist"
        val ARTIST_ID: String = "_id"
        val ARTIST_BIO: String = "art_bio"
        val TABLE_NAME: String = "offline_artist_bio"
        private val TABLE_CREATE: String = ("CREATE TABLE IF NOT EXISTS "
                + TABLE_NAME + " (" + ARTIST_ID + " INTEGER, " + KEY_ARTIST + " TEXT, "
                + ARTIST_BIO + " TEXT);")
    }
}