package com.aemerse.muserse.dbHelper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.aemerse.muserse.model.MusicLibrary
import java.util.concurrent.Executors

class DbHelperUserMusicData constructor(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(TABLE_CREATE_NEW)
    }

    override fun onUpgrade(db: SQLiteDatabase, arg1: Int, arg2: Int) {
        if (arg2 == 3) {
            val insertQuery: String = ("ALTER TABLE " + TABLE_NAME + " ADD COLUMN "
                    + KEY_ID + " INTEGER DEFAULT 0")
            db.execSQL(insertQuery)
            updateIdField(db)
        } else {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            db.execSQL(TABLE_CREATE_NEW)
        }
        Log.d("DbHelperUserMusicData", "onUpgrade: from to :$arg1 : $arg2")
    }

    private fun updateIdField(db: SQLiteDatabase) {
        Executors.newSingleThreadExecutor().execute(object : Runnable {
            override fun run() {
                try {
                    val cursor: Cursor? =
                        db.query(TABLE_NAME, arrayOf(KEY_TITLE), null, null, null, null, null)
                    if (cursor != null && cursor.count != 0) {
                        while (cursor.moveToNext()) {
                            val title = cursor.getString(0)
                            val item = MusicLibrary.instance.getTrackItemFromTitle(title) ?: continue
                            val c = ContentValues()
                            c.put(KEY_ID, item.id)
                            db.update(TABLE_NAME, c, "$KEY_TITLE= ?", arrayOf(title))
                            Log.d("DbHelperUserMusicData", "run: $title")
                        }
                    }
                    cursor?.close()
                } catch (ignored: Exception) {
                }
            }
        })
    }

    companion object {
        private val DATABASE_VERSION: Int = 3
        private val DATABASE_NAME: String = "beta_player"

        //columns
        val KEY_TITLE: String = "song_title"
        val KEY_ID: String = "song_id"
        val KEY_TIME_STAMP: String = "last_time_played"
        val KEY_COUNT: String = "number_of_times_played"
        val KEY_FAV: String = "My_Fav"
        val KEY_LAST_PLAYING_QUEUE: String = "last_playing_queue"
        val TABLE_NAME: String = "user_music_data"
        private val TABLE_CREATE: String = ("CREATE TABLE IF NOT EXISTS "
                + TABLE_NAME + " (" + KEY_TITLE + " TEXT, " + KEY_COUNT + " INTEGER, " + KEY_COUNT + " INTEGER, " + KEY_TIME_STAMP + " INTEGER, "
                + KEY_LAST_PLAYING_QUEUE + " INTEGER, " + KEY_FAV + " INTEGER);")

        //TABLE CREATE with song id
        private val TABLE_CREATE_NEW: String = ("CREATE TABLE IF NOT EXISTS "
                + TABLE_NAME + " (" + KEY_TITLE + " TEXT, " + KEY_ID + " INTEGER, " + KEY_COUNT + " INTEGER, " + KEY_TIME_STAMP + " INTEGER, "
                + KEY_LAST_PLAYING_QUEUE + " INTEGER, " + KEY_FAV + " INTEGER);")
    }
}