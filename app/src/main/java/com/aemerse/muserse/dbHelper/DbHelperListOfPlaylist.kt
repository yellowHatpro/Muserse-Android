package com.aemerse.muserse.dbHelper

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aemerse.muserse.model.Constants

class DbHelperListOfPlaylist constructor(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(TABLE_CREATE)
        //populate playlist list with system playlist
        if ((TABLE_NAME == Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST)) {
            val cursor: Cursor = db.query(Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST,
                null,
                null,
                null,
                null,
                null,
                null)
            // if playlists list is empty
            if (cursor.count == 0) {
                for (playlistName: String? in Constants.SYSTEM_PLAYLISTS.listOfSystemPlaylist) {
                    val c = ContentValues()
                    c.put(KEY_TITLE, playlistName)
                    db.insert(Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST, null, c)
                }
            }
            cursor.close()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, arg1: Int, arg2: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    companion object {
        private val DATABASE_VERSION: Int = 2
        private val DATABASE_NAME: String = "player"
        val KEY_TITLE: String = "file"
        private val TABLE_NAME: String = Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST
        private var TABLE_CREATE: String = ""
    }

    init {
        TABLE_CREATE = "CREATE TABLE IF NOT EXISTS $TABLE_NAME ($KEY_TITLE TEXT);"
    }
}