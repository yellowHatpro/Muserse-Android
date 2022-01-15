package com.aemerse.muserse.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import com.aemerse.muserse.dbHelper.DbHelperListOfPlaylist
import com.aemerse.muserse.dbHelper.DbHelperUserMusicData
import com.aemerse.muserse.R
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class PlaylistManager private constructor(
    /**
     * Attention
     * One of the ugliest piece of code I have ever written in my life resides in this file.
     * Please forgive me for this shit I have given to this world.
     *
     * Only way to make this code good is to rewrite it.
     *
     * In my defence, this is one of the very first coding project I have done in my life!
     */
    private var context: Context
) {
    fun populateUserMusicTable() {
        Executors.newSingleThreadExecutor().execute {
            try {
                Log.v(Constants.TAG, "populating")
                //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
                val db: SQLiteDatabase = dbHelperUserMusicData.writableDatabase
                var cur: Cursor
                //ArrayList<dataItem> dataItems = MusicLibrary.getInstance().getDataItemsForTracks();
                for (item in MusicLibrary.instance.getDataItemsForTracks()!!.values) {
                    //check if song already contains in db, if no, add it
                    val where: String = (DbHelperUserMusicData.KEY_ID.toString() + " = "
                            + item.id)
                    cur = db.query(DbHelperUserMusicData.TABLE_NAME,
                        arrayOf<String>(DbHelperUserMusicData.KEY_ID),
                        where,
                        null,
                        null,
                        null,
                        null,
                        null)
                    if (cur.count == 0) {
                        val c = ContentValues()
                        c.put(DbHelperUserMusicData.KEY_TITLE, item.title)
                        c.put(DbHelperUserMusicData.KEY_ID, item.id)
                        c.put(DbHelperUserMusicData.KEY_COUNT, 0)
                        c.put(DbHelperUserMusicData.KEY_FAV, 0)
                        c.put(DbHelperUserMusicData.KEY_TIME_STAMP, 0)
                        c.put(DbHelperUserMusicData.KEY_LAST_PLAYING_QUEUE, 0)
                        db.insert(DbHelperUserMusicData.TABLE_NAME, null, c)
                    }
                    cur.close()
                }
            } catch (e: Exception) {
                //igore any exception
                //concurrent modification exception
            }
        }
    }

    fun addEntryToMusicTable(d: dataItem) {
        try {
            val db: SQLiteDatabase = dbHelperUserMusicData.writableDatabase
            val cur: Cursor
            val where: String = (DbHelperUserMusicData.KEY_TITLE.toString() + "= '"
                    + d.title.replace("'", "''") + "'")
            cur = db.query(DbHelperUserMusicData.TABLE_NAME,
                arrayOf<String>(DbHelperUserMusicData.KEY_TITLE),
                where,
                null,
                null,
                null,
                null,
                null)
            if (cur.count == 0) {
                val c = ContentValues()
                c.put(DbHelperUserMusicData.KEY_TITLE, d.title)
                c.put(DbHelperUserMusicData.KEY_ID, d.id)
                c.put(DbHelperUserMusicData.KEY_COUNT, 0)
                c.put(DbHelperUserMusicData.KEY_FAV, 0)
                c.put(DbHelperUserMusicData.KEY_TIME_STAMP, 0)
                c.put(DbHelperUserMusicData.KEY_LAST_PLAYING_QUEUE, 0)
                db.insert(DbHelperUserMusicData.TABLE_NAME, null, c)
            }
            cur.close()
        } catch (e: Exception) {
            //igore any exception
            //concurrent modification exception
        }
    }

    /*
    user_addable = only playlist in which user can add songs
     */
    fun getPlaylistList(userAddable: Boolean): ArrayList<String> {
        if (listOfPlaylists.size == 0) {
            val db: SQLiteDatabase = dbHelperListOfPlaylist.readableDatabase
            dbHelperListOfPlaylist.onCreate(db)
            val cursor: Cursor = db.query(Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST,
                null,
                null,
                null,
                null,
                null,
                null)
            while (cursor.moveToNext()) {
                val s: String = cursor.getString(0).replace("_", " ")
                listOfPlaylists[s] = getTrackCount(s)
            }
            cursor.close()
        }
        Log.d("PlaylistManager", "GetPlaylistList: " + listOfPlaylists.keys)
        val temp = ArrayList<String>()
        for (name in listOfPlaylists.keys) {
            if (userAddable) {
                if (name.replace(" ", "_") != Constants.SYSTEM_PLAYLISTS.MOST_PLAYED
                    && name.replace(" ", "_") != Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED
                    && name.replace(" ", "_") != Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED
                ) temp.add(name)
            } else {
                temp.add(name)
            }
        }
        return temp
    }

    val systemPlaylistsList: ArrayList<String>
        get() {
            if (listOfPlaylists.size == 0) {
                val db: SQLiteDatabase = dbHelperListOfPlaylist.readableDatabase
                dbHelperListOfPlaylist.onCreate(db)
                val cursor: Cursor = db.query(Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null)
                while (cursor.moveToNext()) {
                    val s: String = cursor.getString(0).replace("_", " ")
                    listOfPlaylists[s] = getTrackCount(s)
                }
                cursor.close()
            }
            Log.d("PlaylistManager", "GetPlaylistList: " + listOfPlaylists.keys)
            val temp = ArrayList<String>()
            for (name in listOfPlaylists.keys) {
                if (name.replace(" ",
                        "_") == Constants.SYSTEM_PLAYLISTS.MOST_PLAYED || name.replace(" ",
                        "_") == Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED || name.replace(" ",
                        "_") == Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED || name.replace(" ",
                        "_") == Constants.SYSTEM_PLAYLISTS.MY_FAV
                ) temp.add(name)
            }
            return temp
        }
    val userCreatedPlaylistList: ArrayList<String>
        get() {
            if (listOfPlaylists.size == 0) {
                val db: SQLiteDatabase = dbHelperListOfPlaylist.readableDatabase
                dbHelperListOfPlaylist.onCreate(db)
                val cursor: Cursor = db.query(Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null)
                while (cursor.moveToNext()) {
                    val s: String = cursor.getString(0).replace("_", " ")
                    listOfPlaylists[s] = getTrackCount(s)
                }
                cursor.close()
            }
            Log.d("PlaylistManager", "GetPlaylistList: " + listOfPlaylists.keys)
            val temp = ArrayList<String>()
            for (name in listOfPlaylists.keys) {
                if (name.replace(" ", "_") != Constants.SYSTEM_PLAYLISTS.MOST_PLAYED
                    && name.replace(" ", "_") != Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED
                    && name.replace(" ", "_") != Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED
                    && name.replace(" ", "_") != Constants.SYSTEM_PLAYLISTS.MY_FAV
                ) temp.add(name)
            }
            return temp
        }

    fun createPlaylist(playlist_name: String): Boolean {
        var playlistName = playlist_name
        playlistName = playlistName.replace(" ", "_")
        //try adding column
        //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        var db: SQLiteDatabase = dbHelperUserMusicData.writableDatabase
        dbHelperUserMusicData.onCreate(db)
        //create column for newly created playlist
        val insertQuery =
            "ALTER TABLE " + DbHelperUserMusicData.TABLE_NAME.toString() + " ADD COLUMN " + playlistName + " INTEGER DEFAULT 0"
        try {
            db.execSQL(insertQuery)
        } catch (ignored: Exception) {
        }
        //try creating entry in playlist list
        //DbHelperListOfPlaylist dbHelperListOfPlaylist = new DbHelperListOfPlaylist(context);
        db = dbHelperListOfPlaylist.writableDatabase
        dbHelperListOfPlaylist.onCreate(db)
        val where = DbHelperListOfPlaylist.KEY_TITLE + "= '" + playlistName.replace("'",
                "''") + "'"
        if (db.query(Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST,
                arrayOf(DbHelperListOfPlaylist.KEY_TITLE),
                where,
                null,
                null,
                null,
                null).count == 0
        ) {
            val c = ContentValues()
            c.put(DbHelperListOfPlaylist.KEY_TITLE, playlistName)
            db.insert(Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST, null, c)

            //invalidate playlist cache @todo can be better I guess
            listOfPlaylists.clear() //it will be populated automatically on next db call
            return true
        }
        return false
    }

    fun deletePlaylist(playlist_name: String): Boolean {
        var playlistName = playlist_name
        playlistName = playlistName.replace(" ", "_")
        //delete column is not possible in sqlite
        //just a remove playlist entry from playlist list
        //and clear the column
        if (playlistName == Constants.SYSTEM_PLAYLISTS.MOST_PLAYED || playlistName == Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED || playlistName == Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED || playlistName == Constants.SYSTEM_PLAYLISTS.MY_FAV) {
            return false
        }


        //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        var db: SQLiteDatabase = dbHelperUserMusicData.writableDatabase
        dbHelperUserMusicData.onCreate(db)
        val c = ContentValues()
        c.put(playlistName, 0)
        db.update(DbHelperUserMusicData.TABLE_NAME, c, null, null)

        //DbHelperListOfPlaylist dbHelperListOfPlaylist
        //      = new DbHelperListOfPlaylist(context);
        db = dbHelperListOfPlaylist.writableDatabase
        dbHelperListOfPlaylist.onCreate(db)

        //invalidate playlist cache
        listOfPlaylists.clear() //it will be populated automatically on next db call
        return db.delete(Constants.SYSTEM_PLAYLISTS.PLAYLIST_LIST,
            DbHelperListOfPlaylist.KEY_TITLE.toString() + "= '" + playlistName.replace("'",
                "''") + "'",
            null) != 0
    }

    fun addSongToPlaylist(playlist_name_arg: String, song_ids: IntArray) {
        Executors.newSingleThreadExecutor().execute(Runnable {
            val playlistName = playlist_name_arg.replace(" ", "_")
            val hand: Handler = Handler(Looper.getMainLooper())
            val db: SQLiteDatabase = dbHelperUserMusicData.writableDatabase
            dbHelperUserMusicData.onCreate(db)

            //check if song exists
            if (song_ids.size == 1) {
                val where: String =
                    (DbHelperUserMusicData.KEY_ID.toString() + "= '" + song_ids[0] + "'"
                            + " AND \"" + playlistName + "\" != 0")
                if (db.query(DbHelperUserMusicData.TABLE_NAME,
                        arrayOf("\"" + playlistName + "\""),
                        where,
                        null,
                        null,
                        null,
                        null)
                        .count > 0
                ) {
                    hand.post {
                        Toast.makeText(context, context.getString(R.string.song_already_exists_in) + playlistName, Toast.LENGTH_SHORT).show()
                    }
                    return@Runnable
                }
            }
            val max = "MAX($playlistName)"
            val cursor: Cursor = db.query(DbHelperUserMusicData.TABLE_NAME,
                arrayOf(max),
                null,
                null,
                null,
                null,
                null)
            cursor.moveToFirst()
            var maxValue: Int = cursor.getInt(0)
            cursor.close()
            for (id in song_ids) {
                val c = ContentValues()
                c.put(playlistName, ++maxValue)
                db.update(DbHelperUserMusicData.TABLE_NAME,
                    c,
                    DbHelperUserMusicData.KEY_ID.toString() + "= ?",
                    arrayOf(id.toString() + ""))
            }
            val favPlayListName = playlistName.replace("_", " ")
            if (listOfPlaylists.containsKey(favPlayListName)) {
                listOfPlaylists[favPlayListName] = listOfPlaylists[favPlayListName]!! + 1
            }
            hand.post(Runnable {
                if (playlistName == Constants.SYSTEM_PLAYLISTS.MY_FAV) {
                    return@Runnable
                }
                val toast: Toast = Toast.makeText(context,
                    context.getString(R.string.songs_added_in) + playlistName.replace("_", " "),
                    Toast.LENGTH_SHORT)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
            })
        })
    }

    fun addSongToFav(id: Int) {
        val playlistName: String = DbHelperUserMusicData.KEY_FAV.replace(" ", "_")
        val db: SQLiteDatabase = dbHelperUserMusicData.writableDatabase
        dbHelperUserMusicData.onCreate(db)
        val max = "MAX($playlistName)"
        val cursor: Cursor =
            db.query(DbHelperUserMusicData.TABLE_NAME, arrayOf(max), null, null, null, null, null)
        cursor.moveToFirst()
        var maxValue: Int = cursor.getInt(0)
        cursor.close()
        val c = ContentValues()
        c.put(playlistName, ++maxValue)
        db.update(DbHelperUserMusicData.TABLE_NAME,
            c,
            DbHelperUserMusicData.KEY_ID + "= ?",
            arrayOf(id.toString() + ""))
        val favPlayListName = Constants.SYSTEM_PLAYLISTS.MY_FAV.replace("_", " ")
        if (listOfPlaylists.containsKey(favPlayListName)) {
            listOfPlaylists[favPlayListName] =
                listOfPlaylists[favPlayListName]!! + 1
        }
    }

    fun removeSongFromPlaylistNew(playlist_name: String, id: Int) {
        var playlistName = playlist_name
        playlistName = playlistName.replace(" ", "_")
        run {

            //user playlist
            //DbHelperUserMusicData dbHelperMusicData
            //      = new DbHelperUserMusicData(context);
            val db: SQLiteDatabase = dbHelperUserMusicData.readableDatabase
            dbHelperUserMusicData.onCreate(db)
            try {
                val where: String = DbHelperUserMusicData.KEY_ID + "='" + id + "'"
                val c = ContentValues()
                c.put(playlistName, 0)
                db.update(DbHelperUserMusicData.TABLE_NAME, c, where, null)
                if (listOfPlaylists.containsKey(playlistName.replace("_", " "))) {
                    listOfPlaylists[playlistName.replace("_", " ")] =
                        listOfPlaylists[playlistName.replace("_", " ")]!! - 1
                }
                Toast.makeText(context,
                    context.getString(R.string.removed_from_playlist),
                    Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context,
                    context.getString(R.string.error_removing),
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getPlaylist(playlist_name: String): ArrayList<dataItem> {
        var playlistName = playlist_name
        Log.d("PlaylistManager", "GetPlaylist: $playlistName")
        playlistName = playlistName.replace(" ", "_")
        val trackList: ArrayList<dataItem> = when (playlistName) {
            Constants.SYSTEM_PLAYLISTS.MOST_PLAYED -> GetMostPlayed()
            Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED -> getRecentlyPlayed()
            Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED -> GetRecentlyAdded()
            Constants.SYSTEM_PLAYLISTS.MY_FAV -> getFav()
            else -> getUserPlaylist(playlistName)
        }
        return trackList
    }

    fun addToRecentlyPlayedAndUpdateCount(_id: Int) {

        //thread for updating play numberOfTracks
        Executors.newSingleThreadExecutor()
            .execute { //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
                val db: SQLiteDatabase = dbHelperUserMusicData.writableDatabase
                dbHelperUserMusicData.onCreate(db)
                try {
                    val getCurrentCountQuery = ("SELECT " + DbHelperUserMusicData.KEY_COUNT + " FROM " + DbHelperUserMusicData.TABLE_NAME.toString() + " WHERE "
                            + DbHelperUserMusicData.KEY_ID + " = '" + _id.toString() + "'")
                    val getCurrentCountCursor: Cursor = db.rawQuery(getCurrentCountQuery, null)
                    getCurrentCountCursor.moveToFirst()
                    val currentCount: Int = getCurrentCountCursor.getInt(getCurrentCountCursor.getColumnIndex(DbHelperUserMusicData.KEY_COUNT))
                    getCurrentCountCursor.close()
                    val c = ContentValues()
                    c.put(DbHelperUserMusicData.KEY_COUNT, currentCount + 1)
                    db.update(DbHelperUserMusicData.TABLE_NAME,
                        c,
                        DbHelperUserMusicData.KEY_ID + "= ?",
                        arrayOf(_id.toString() + ""))
                } catch (e: Exception) {
                    Log.v("Serious", "Error" + e.stackTrace.toString())
                }
            }
        val recentPlayListName = Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED.replace("_", " ")
        if (listOfPlaylists.containsKey(recentPlayListName)) {
            if (listOfPlaylists[recentPlayListName]!! < Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED_MAX) listOfPlaylists[recentPlayListName] =
                listOfPlaylists[recentPlayListName]!! + 1
        }

        //thread for adding entry in recently played
        Executors.newSingleThreadExecutor()
            .execute { //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
                val db: SQLiteDatabase = dbHelperUserMusicData.writableDatabase
                dbHelperUserMusicData.onCreate(db)
                val c = ContentValues()
                c.put(DbHelperUserMusicData.KEY_TIME_STAMP, System.currentTimeMillis())
                db.update(DbHelperUserMusicData.TABLE_NAME,
                    c,
                    DbHelperUserMusicData.KEY_ID + "= ?",
                    arrayOf(_id.toString() + ""))
            }
    }

    fun isFavNew(id: Int): Boolean {
        var returnValue = false

        //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        val db: SQLiteDatabase = dbHelperUserMusicData.readableDatabase
        dbHelperUserMusicData.onCreate(db)
        val where: String = DbHelperUserMusicData.KEY_ID + "= '" + id + "'"
        val cursor: Cursor = db.query(DbHelperUserMusicData.TABLE_NAME,
            arrayOf<String>(DbHelperUserMusicData.KEY_FAV),
            where,
            null,
            null,
            null,
            null,
            null)
        if (cursor.count != 0) {
            cursor.moveToFirst()
            if (cursor.getInt(cursor.getColumnIndex(DbHelperUserMusicData.KEY_FAV)) > 0) {
                returnValue = true
            }
        }
        cursor.close()
        return returnValue
    }

    fun removeFromFavNew(id: Int) {
        val db: SQLiteDatabase = dbHelperUserMusicData.writableDatabase
        dbHelperUserMusicData.onCreate(db)
        val c = ContentValues()
        c.put(DbHelperUserMusicData.KEY_FAV, 0)
        db.update(DbHelperUserMusicData.TABLE_NAME,
            c,
            DbHelperUserMusicData.KEY_ID + "= ?",
            arrayOf(id.toString() + ""))
        val favPlayListName = Constants.SYSTEM_PLAYLISTS.MY_FAV.replace("_", " ")
        if (listOfPlaylists.containsKey(favPlayListName)) {
            listOfPlaylists[favPlayListName] =
                listOfPlaylists[favPlayListName]!! - 1
        }
    }

    fun storeLastPlayingQueueNew(tracklist: ArrayList<Int>) {
        Executors.newSingleThreadExecutor().execute {
            val db: SQLiteDatabase = dbHelperUserMusicData.writableDatabase
            dbHelperUserMusicData.onCreate(db)

            //clear column first
            var c = ContentValues()
            c.put(DbHelperUserMusicData.KEY_LAST_PLAYING_QUEUE, 0)
            db.update(DbHelperUserMusicData.TABLE_NAME, c, null, null)
            var count = 0
            try {
                for (id in tracklist) {
                    c = ContentValues()
                    c.put(DbHelperUserMusicData.KEY_LAST_PLAYING_QUEUE, 1)
                    db.update(DbHelperUserMusicData.TABLE_NAME,
                        c,
                        DbHelperUserMusicData.KEY_ID.toString() + "= ?",
                        arrayOf(id.toString() + ""))
                    count++
                }
            } catch (ignored: ConcurrentModificationException) {
                //other instance of this thread started, ignore this exception
                //let other thread save the queue, exit this thread
                Log.d("PlaylistManager", "run: Error storing queue : Concurrrent modification")
            } catch (ignored: Exception) {
            }
        }
    }

    fun restoreLastPlayingQueueNew(): ArrayList<Int> {
//        DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        val db: SQLiteDatabase = dbHelperUserMusicData.readableDatabase
        dbHelperUserMusicData.onCreate(db)
        val where: String = DbHelperUserMusicData.KEY_LAST_PLAYING_QUEUE.toString() + " = 1"
        val c: Cursor = db.query(DbHelperUserMusicData.TABLE_NAME,
            arrayOf<String>(DbHelperUserMusicData.KEY_ID),
            where,
            null,
            null,
            null,
            null)
        val tracklist = ArrayList<Int>()
        while (c.moveToNext()) {
            var id: Int
            try {
                id = Integer.valueOf(c.getString(0))
            } catch (e: Exception) {
                continue
            }
            tracklist.add(id)
        }
        c.close()
        Log.d("PlaylistManager",
            "RestoreLastPlayingQueue: restored queue count : " + tracklist.size)
        return tracklist
    }

    fun clearPlaylist(playlist_name: String): Boolean {
        var playlistName = playlist_name
        if (listOfPlaylists.containsKey(playlistName)) {
            listOfPlaylists[playlistName] = 0L
        }
        playlistName = playlistName.replace(" ", "_")
        when (playlistName) {
            Constants.SYSTEM_PLAYLISTS.MOST_PLAYED -> playlistName =
                DbHelperUserMusicData.KEY_COUNT
            Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED -> playlistName =
                DbHelperUserMusicData.KEY_TIME_STAMP
            Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED -> return false
            else -> {}
        }
        val db: SQLiteDatabase = dbHelperUserMusicData.writableDatabase
        dbHelperUserMusicData.onCreate(db)
        val query =
            "UPDATE `" + DbHelperUserMusicData.TABLE_NAME.toString() + "` SET `" + playlistName + "` = '0'"
        try {
            db.execSQL(query)
        } catch (e: Exception) {
            Log.d("PlaylistManager", "ClearPlaylist: error")
            return false
        }
        return true
    }

    fun getTrackCountFromCache(playlist_name: String): Long {
        return if (listOfPlaylists.containsKey(
                playlist_name)
        ) {
            listOfPlaylists[playlist_name]!!
        } else {
            0
        }
    }

    //private methods
    private fun getFav(): ArrayList<dataItem> {

        //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        val db: SQLiteDatabase = dbHelperUserMusicData.readableDatabase
        dbHelperUserMusicData.onCreate(db)
        val where: String = DbHelperUserMusicData.KEY_FAV.toString() + " != 0"
        val c: Cursor = db.query(DbHelperUserMusicData.TABLE_NAME,
            arrayOf(DbHelperUserMusicData.KEY_ID),
            where,
            null,
            null,
            null,
            DbHelperUserMusicData.KEY_FAV)
        val tracklist: ArrayList<dataItem> = ArrayList()
        while (c.moveToNext()) {
            for (d in MusicLibrary.instance.getDataItemsForTracks()!!.values) {
                if (d.id == c.getInt(0)) {
                    tracklist.add(d)
                    break
                }
            }
        }
        c.close()
        return tracklist
    }

    private fun getRecentlyPlayed(): ArrayList<dataItem> {
        // DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        val db: SQLiteDatabase = dbHelperUserMusicData.readableDatabase
        dbHelperUserMusicData.onCreate(db)
        val tracklist: ArrayList<dataItem> = ArrayList()
        val where: String = DbHelperUserMusicData.KEY_TIME_STAMP + " > 0 "
        val cursor: Cursor = db.query(DbHelperUserMusicData.TABLE_NAME,
            arrayOf<String>(DbHelperUserMusicData.KEY_ID),
            where,
            null,
            null,
            null,
            DbHelperUserMusicData.KEY_TIME_STAMP.toString() + " DESC",
            "" + Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED_MAX)
        while (cursor.moveToNext()) {
            for (d in MusicLibrary.instance.getDataItemsForTracks()!!.values) {
                if (d.id == cursor.getInt(0)) {
                    tracklist.add(d)
                    break
                }
            }
        }
        cursor.close()
        return tracklist
    }

    private fun GetRecentlyAdded(): ArrayList<dataItem> {
        val pathToId = HashMap<String?, Int>()
        val musicFiles: ArrayList<File> = ArrayList()
        for (item in MusicLibrary.instance.getDataItemsForTracks()!!.values) {
            var f: File
            //for console error log
            try {
                f = File(item.file_path)
            } catch (e: Exception) {
                continue
            }
            musicFiles.add(f)
            pathToId[item.file_path] = item.id
        }
        musicFiles.sortWith { o1, o2 ->
            when {
                o1!!.lastModified() > o2!!.lastModified() -> {
                    -1
                }
                o1.lastModified() < o2.lastModified() -> {
                    +1
                }
                else -> {
                    0
                }
            }
        }
        val tracklist: ArrayList<dataItem> = ArrayList()
        val lessThanCount = if (musicFiles.size > 50) 50 else musicFiles.size
        for (i in 0 until lessThanCount) {
            for (d in MusicLibrary.instance.getDataItemsForTracks()!!.values) {
                if (d.id == pathToId[musicFiles[i].absolutePath]) {
                    tracklist.add(d)
                    break
                }
            }
        }
        return tracklist
    }

    private fun getUserPlaylist(playlist_name: String): ArrayList<dataItem> {
        var playlistName = playlist_name
        playlistName = "\"" + playlistName + "\""

        //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        val db: SQLiteDatabase = dbHelperUserMusicData.readableDatabase
        dbHelperUserMusicData.onCreate(db)
        val where = "$playlistName != 0"
        val c: Cursor = db.query(DbHelperUserMusicData.TABLE_NAME,
            arrayOf<String>(DbHelperUserMusicData.KEY_ID),
            where,
            null,
            null,
            null,
            playlistName)
        val tracklist: ArrayList<dataItem> = ArrayList()
        while (c.moveToNext()) {
            for (d in MusicLibrary.instance.getDataItemsForTracks()!!.values) {
                if (d.id == c.getInt(0)) {
                    tracklist.add(d)
                    break
                }
            }
        }
        c.close()
        return tracklist
    }

    private fun GetMostPlayed(): ArrayList<dataItem> {
        //   DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        val db: SQLiteDatabase = dbHelperUserMusicData.readableDatabase
        dbHelperUserMusicData.onCreate(db)
        val tracklist: ArrayList<dataItem> = ArrayList<dataItem>()
        val where: String = DbHelperUserMusicData.KEY_COUNT.toString() + " > 0 "
        val cursor: Cursor = db.query(DbHelperUserMusicData.TABLE_NAME,
            arrayOf<String>(DbHelperUserMusicData.KEY_ID),
            where,
            null,
            null,
            null,
            DbHelperUserMusicData.KEY_COUNT.toString() + " DESC",
            "" + Constants.SYSTEM_PLAYLISTS.MOST_PLAYED_MAX)

        /*while (cursor.moveToNext()){
            tracklist.add(cursor.getString(0));
        }*/while (cursor.moveToNext()) {
            for (d in MusicLibrary.instance.getDataItemsForTracks()!!.values) {
                if (d.id == cursor.getInt(0)) {
                    tracklist.add(d)
                    break
                }
            }
        }
        cursor.close()
        return tracklist
    }

    //get track count for playlist from db
    private fun getTrackCount(playlist_name: String): Long {
        var playlistName = playlist_name
        playlistName = playlistName.replace(" ", "_")

        //DbHelperUserMusicData dbHelperUserMusicData = new DbHelperUserMusicData(context);
        val db: SQLiteDatabase = dbHelperUserMusicData.readableDatabase
        dbHelperUserMusicData.onCreate(db)
        if (playlistName == Constants.SYSTEM_PLAYLISTS.MOST_PLAYED) {
            val where: String = DbHelperUserMusicData.KEY_COUNT + " > 0 "
            val cursor: Cursor = db.query(DbHelperUserMusicData.TABLE_NAME,
                arrayOf(DbHelperUserMusicData.KEY_ID),
                where,
                null,
                null,
                null,
                DbHelperUserMusicData.KEY_COUNT + " DESC",
                "" + Constants.SYSTEM_PLAYLISTS.MOST_PLAYED_MAX)
            val count: Int = cursor.count
            cursor.close()
            return count.toLong()
        }
        if (playlistName == Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED) {
            val where: String = DbHelperUserMusicData.KEY_TIME_STAMP + " > 0 "
            val cursor: Cursor = db.query(DbHelperUserMusicData.TABLE_NAME,
                arrayOf(DbHelperUserMusicData.KEY_ID),
                where,
                null,
                null,
                null,
                DbHelperUserMusicData.KEY_TIME_STAMP + " DESC",
                "" + Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED_MAX)
            val count: Int = cursor.count
            cursor.close()
            return count.toLong()
        }
        if (playlistName == Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED) {
            return 50 //very ugly
        }
        playlistName = "\"" + playlistName + "\""
        val where = "$playlistName != 0"
        val count: Long = DatabaseUtils.queryNumEntries(db, DbHelperUserMusicData.TABLE_NAME, where)
        Log.d("PlaylistManager", "getTrackCount: $playlistName : $count")
        return count
    }

    companion object {
        private var playlistManager: PlaylistManager? = null
        private lateinit var dbHelperUserMusicData: DbHelperUserMusicData
        private lateinit var dbHelperListOfPlaylist: DbHelperListOfPlaylist
        private val trackCount: HashMap<String, Int>? = null
        private val listOfPlaylists =
            HashMap<String, Long>() //cache playlist list to avoid unnecessary db calls

        fun getInstance(context: Context): PlaylistManager? {
            if (playlistManager == null) {
                playlistManager = PlaylistManager(context)
            }
            return playlistManager
        }
    }

    init {
        dbHelperUserMusicData = DbHelperUserMusicData(context)
        dbHelperListOfPlaylist = DbHelperListOfPlaylist(context)
        getPlaylistList(false)
    }
}