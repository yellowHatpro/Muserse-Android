package com.aemerse.muserse.activity

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.databinding.ActivityTagEditorBinding
import com.aemerse.muserse.model.Constants
import com.aemerse.muserse.model.MusicLibrary
import com.aemerse.muserse.model.PlaylistManager
import com.aemerse.muserse.model.TrackItem
import com.aemerse.muserse.uiElementHelper.ColorHelper
import com.aemerse.muserse.utils.UtilityFun
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.io.File

class ActivityTagEditor : AppCompatActivity(), View.OnClickListener {
    private var song_id: Int = 0
    private var original_title: String? = null
    private var original_artist: String? = null
    private var original_album: String? = null
    private var edited_title: String = ""
    private var edited_artist: String = ""
    private var edited_album: String = ""
    private var track_title: String? = null
    private val SAVE: Int = 10
    var fChanged: Boolean = false
    private var item: TrackItem? = null
    private var ALBUM_ART_PATH: String = ""

    //file path where changed image file is stored
    private var new_artwork_path: String = ""
    private lateinit var binding: ActivityTagEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        //if player service not running, kill the app
        if (ApplicationClass.getService() == null) {
            val intent = Intent(this, ActivityPermissionSeek::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
        ColorHelper.setStatusBarGradiant(this)
        super.onCreate(savedInstanceState)
        when (ApplicationClass.getPref().getInt(getString(R.string.pref_theme), Constants.PRIMARY_COLOR.LIGHT)) {
            Constants.PRIMARY_COLOR.DARK -> setTheme(R.style.AppThemeDark)
            Constants.PRIMARY_COLOR.GLOSSY -> setTheme(R.style.AppThemeDark)
            Constants.PRIMARY_COLOR.LIGHT -> setTheme(R.style.AppThemeLight)
        }
        binding = ActivityTagEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //show info dialog
        showInfoDialog()
        ALBUM_ART_PATH = Environment.getExternalStorageDirectory()
            .absolutePath + "/" + getString(R.string.album_art_dir_name)

        //findViewById(R.id.root_view_tag_editor).setBackgroundDrawable(ColorHelper.GetGradientDrawableDark());
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        //get file path
        val file_path: String? = intent.getStringExtra("file_path")
        if (file_path == null) {
            finish()
        }
        track_title = intent.getStringExtra("track_title")
        song_id = intent.getIntExtra("id", 0)
        item = MusicLibrary.instance.getTrackItemFromId(song_id)
        val toolbar: Toolbar = findViewById<Toolbar>(R.id.toolbar_)
        setSupportActionBar(toolbar)

        // add back arrow to toolbar
        if (supportActionBar != null) {
            //getSupportActionBar().setBackgroundDrawable(ColorHelper.GetGradientDrawableToolbar());
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ColorHelper.GetStatusBarColor());
        }*/setTitle(getString(R.string.title_tag_editor))
        binding.albumArtTe.setOnClickListener(this)

        //get current tags from audio file and populate the fields
        setTagsFromContent()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    private fun setTagsFromContent() {
        if (item == null) {
            return
        }
        binding.titleTe.setText(item!!.title)
        song_id = item!!.id
        original_title = item!!.title
        binding.albumTe.setText(item!!.album)
        original_album = item!!.album
        binding.artistTe.setText(item!!.getArtist())
        original_artist = item!!.getArtist()
        when (ApplicationClass.getPref().getInt(getString(R.string.pref_default_album_art), 0)) {
            0 -> Glide.with(this)
                .load(MusicLibrary.instance.getAlbumArtUri(item!!.albumId))
                .signature(ObjectKey(System.currentTimeMillis().toString()))
                .placeholder(R.drawable.music)
                .into(binding.albumArtTe)
            1 -> Glide.with(this)
                .load(MusicLibrary.instance.getAlbumArtUri(item!!.albumId))
                .signature(ObjectKey(System.currentTimeMillis().toString()))
                .placeholder(UtilityFun.defaultAlbumArtDrawable)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.albumArtTe)
        }
    }

    override fun onPause() {
        super.onPause()
        ApplicationClass.isAppVisible = false
    }

    override fun onResume() {
        super.onResume()
        ApplicationClass.isAppVisible = true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.add(0, SAVE, 0, getString(R.string.action_save))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                readValues()
                if (fChanged) {
                    unsavedDataAlert()
                } else {
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_in_right)
                    finish()
                }
            }
            SAVE -> {
                readValues()
                if (fChanged) {
                    try {
                        save()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this,
                            getString(R.string.te_error_saving_tags),
                            Toast.LENGTH_LONG).show()
                    }
                    Log.v(Constants.TAG, edited_title)
                    Log.v(Constants.TAG, edited_artist)
                    Log.v(Constants.TAG, edited_album)
                    val edited_genre = ""
                    Log.v(Constants.TAG, edited_genre)
                } else {
                    val intent: Intent = when (intent.getIntExtra("from", Constants.TAG_EDITOR_LAUNCHED_FROM.MAIN_LIB)) {
                        Constants.TAG_EDITOR_LAUNCHED_FROM.MAIN_LIB -> {
                            Intent(this, ActivityMain::class.java)
                        }
                        Constants.TAG_EDITOR_LAUNCHED_FROM.NOW_PLAYING -> {
                            Intent(this, ActivityNowPlaying::class.java)
                        }
                        else -> {
                            Intent(this, ActivitySecondaryLibrary::class.java)
                        }
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun readValues() {
        edited_title = binding.titleTe.text.toString()
        edited_artist = binding.artistTe.text.toString()
        edited_album = binding.albumTe.text.toString()
        if (edited_title != original_title || edited_artist != original_artist || edited_album != original_album || new_artwork_path != "") {
            fChanged = true
        }
    }

    private fun unsavedDataAlert() {
        MaterialDialog(this)
            .title(R.string.te_unsaved_data_title)
            .message(R.string.changes_discard_alert_te)
            .positiveButton(R.string.te_unsaved_data_pos){
                try {
                    save()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@ActivityTagEditor,
                        "Error while saving tags!",
                        Toast.LENGTH_LONG).show()
                }
                finish()
            }
            .negativeButton(R.string.te_unsaved_data_new){
                finish()
            }
            .show()
    }

    private fun save() {
        if (edited_title.isEmpty() || edited_album.isEmpty() || edited_artist.isEmpty()) {
            Toast.makeText(applicationContext,
                getString(R.string.te_error_empty_field),
                Toast.LENGTH_SHORT).show()
            return
        }

        //change content in android
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        var values = ContentValues()
        values.put(MediaStore.Audio.Media.TITLE, edited_title)
        values.put(MediaStore.Audio.Media.ARTIST, edited_artist)
        values.put(MediaStore.Audio.Media.ALBUM, edited_album)
        contentResolver.update(uri,
            values,
            MediaStore.Audio.Media.TITLE + "=?",
            arrayOf(track_title))
        if (!(new_artwork_path == "")) {
            val sArtworkUri: Uri = Uri.parse("content://media/external/audio/albumart")
            val deleted: Int =
                contentResolver.delete(ContentUris.withAppendedId(sArtworkUri, item!!.albumId.toLong()),
                    null,
                    null)
            Log.v(Constants.TAG, "delete $deleted")
            values = ContentValues()
            values.put("album_id", item!!.albumId)
            values.put("_data", new_artwork_path)
            contentResolver.insert(sArtworkUri, values)
        }
        val d = MusicLibrary.instance.updateTrackNew(song_id, edited_title, edited_artist, edited_album)
        PlaylistManager.getInstance(ApplicationClass.getContext())!!.addEntryToMusicTable(d!!)
        //   PlaylistManager.getInstance(MyApp.getContext()).PopulateUserMusicTable();
        val intent: Intent = when (intent.getIntExtra("from", Constants.TAG_EDITOR_LAUNCHED_FROM.MAIN_LIB)) {
            Constants.TAG_EDITOR_LAUNCHED_FROM.MAIN_LIB -> {
                Intent(this, ActivityMain::class.java)
            }
            Constants.TAG_EDITOR_LAUNCHED_FROM.NOW_PLAYING -> {
                Intent(this, ActivityNowPlaying::class.java)
            }
            else -> {
                Intent(this, ActivitySecondaryLibrary::class.java)
            }
        }
        intent.putExtra("refresh", true)
        intent.putExtra("position", getIntent().getIntExtra("position", -1))
        intent.putExtra("originalTitle", original_title)
        intent.putExtra("title", edited_title)
        intent.putExtra("artist", edited_artist)
        intent.putExtra("album", edited_album)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_in_right)
        finish()
    }

    override fun onClick(v: View) {
        if (v.id == R.id.album_art_te) {
            pickImage()
        }
    }

    fun pickImage() {
        val intent: Intent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (data == null) {
            return
        }
        if (requestCode == 1) {
            //dumpIntent(data);
            checkAndCreateAlbumArtDirectory()
            val uri: Uri? = data.data
            if (uri != null) {
                Log.v(Constants.TAG, data.toString())
                val filePathArtwork = getRealPathFromURI(uri)
                if (filePathArtwork == null) {
                    Toast.makeText(this,
                        getString(R.string.te_error_image_load),
                        Toast.LENGTH_SHORT).show()
                    return
                }
                Glide.with(this)
                    .load(File(filePathArtwork)) // Uri of the picture
                    .into(binding.albumArtTe)
                new_artwork_path = filePathArtwork
            }
        }
    }

    private fun checkAndCreateAlbumArtDirectory() {
        val f = File(ALBUM_ART_PATH)
        if (f.exists()) {
            return
        }
        try {
            f.mkdir()
        } catch (ignored: Exception) {
        }
    }

    fun getRealPathFromURI(uri: Uri?): String? {
        val projection: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = managedQuery(uri, projection, null, null, null)
        if (cursor == null || cursor.count == 0) {
            return null
        }
        val column_index: Int = cursor
            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(column_index)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY -> ApplicationClass.getService()!!.play()
            KeyEvent.KEYCODE_MEDIA_NEXT -> ApplicationClass.getService()!!.nextTrack()
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> ApplicationClass.getService()!!.prevTrack()
            KeyEvent.KEYCODE_MEDIA_STOP -> ApplicationClass.getService()!!.stop()
            KeyEvent.KEYCODE_BACK -> onBackPressed()
        }
        return false
    }

    override fun onBackPressed() {
        readValues()
        if (fChanged) {
            unsavedDataAlert()
        } else {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_in_right)
        }
    }

    private fun showInfoDialog() {
        if (!ApplicationClass.getPref().getBoolean(getString(R.string.pref_show_edit_track_info_dialog), true)) {
            return
        }
        MaterialDialog(this)
            .title(R.string.te_show_info_title)
            .message(R.string.te_show_info_content)
            .positiveButton(R.string.te_show_info_pos){
                ApplicationClass.getPref().edit()
                    .putBoolean(getString(R.string.pref_show_edit_track_info_dialog), false)
                    .apply()
            }
            .negativeButton(R.string.te_show_info_neg)
            .show()
    }

    companion object {
        /*private void deletePhoto(){
        if(album_art!=null){
            album_art.setImageDrawable(getResources().getDrawable(R.drawable.music));
        }
        Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        getContentResolver().delete(ContentUris.withAppendedId(sArtworkUri, item.getAlbumId()), null, null);
        String customAlbumArt = Environment.getExternalStorageDirectory().getAbsolutePath()
                +"/"+getString(R.string.album_art_dir_name)+"/"
                +item.getAlbumId();
        File f = new File(customAlbumArt);
        if(f.exists()){
            try {
                f.delete();
            }catch (Exception ignored){

            }
        }
    }*/
        fun dumpIntent(i: Intent) {
            val bundle: Bundle? = i.extras
            if (bundle != null) {
                val keys: Set<String> = bundle.keySet()
                val it: Iterator<String> = keys.iterator()
                Log.e(Constants.TAG, "Dumping Intent start")
                while (it.hasNext()) {
                    val key: String = it.next()
                    Log.e(Constants.TAG, "[" + key + "=" + bundle.get(key) + "]")
                }
                Log.e(Constants.TAG, "Dumping Intent end")
            }
        }
    }
}