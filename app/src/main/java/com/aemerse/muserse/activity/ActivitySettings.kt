package com.aemerse.muserse.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MotionEventCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.ItemListener
import com.afollestad.materialdialogs.list.listItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.uiElementHelper.ColorHelper
import com.aemerse.muserse.uiElementHelper.TypeFaceHelper
import com.aemerse.muserse.uiElementHelper.recyclerviewHelper.ItemTouchHelperAdapter
import com.aemerse.muserse.uiElementHelper.recyclerviewHelper.OnStartDragListener
import com.aemerse.muserse.uiElementHelper.recyclerviewHelper.SimpleItemTouchHelperCallback
import com.aemerse.muserse.model.Constants
import com.aemerse.muserse.model.MusicLibrary
import com.aemerse.muserse.service.BatchDownloaderService
import com.aemerse.muserse.service.NotificationListenerService
import com.aemerse.muserse.service.PlayerService
import com.aemerse.muserse.utils.UtilityFun
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.io.File
import java.util.*
import java.util.concurrent.Executors

class ActivitySettings : AppCompatActivity() {
    private var launchedFrom: Int = 0
    private var playerService: PlayerService? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        //if player service not running, kill the app
        if (ApplicationClass.getService() == null) {
            UtilityFun.restartApp()
            finish()
            return
        }
        playerService = ApplicationClass.getService()
        ColorHelper.setStatusBarGradiant(this)
        when (ApplicationClass.getPref().getInt(getString(R.string.pref_theme), Constants.PRIMARY_COLOR.LIGHT)) {
            Constants.PRIMARY_COLOR.DARK -> setTheme(R.style.AppThemeDarkPref)
            Constants.PRIMARY_COLOR.GLOSSY -> setTheme(R.style.AppThemeDarkPref)
            Constants.PRIMARY_COLOR.LIGHT -> setTheme(R.style.AppThemeLight)
        }
        super.onCreate(savedInstanceState)
        launchedFrom = intent.getIntExtra("launchedFrom", 0)
        setContentView(R.layout.acitivty_settings)

        //findViewById(R.id.root_view_settings).setBackgroundDrawable(ColorHelper.GetGradientDrawableDark());
        val toolbar: Toolbar = findViewById(R.id.toolbar_)
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
        }*/
        title = "Settings"
        fragmentManager.beginTransaction().replace(R.id.linear_layout_fragment, MyPreferenceFragment()).commit()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onBackPressed() {
        when (launchedFrom) {
            Constants.PREF_LAUNCHED_FROM.MAIN -> startActivity(Intent(this,
                ActivityMain::class.java))
            Constants.PREF_LAUNCHED_FROM.DRAWER -> startActivity(Intent(this,
                ActivityMain::class.java))
            Constants.PREF_LAUNCHED_FROM.NOW_PLAYING -> startActivity(Intent(this,
                ActivityNowPlaying::class.java))
            else -> startActivity(Intent(this, ActivityMain::class.java))
        }
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_in_right)
        super.onBackPressed()
    }

    public override fun onPause() {
        ApplicationClass.isAppVisible = false
        super.onPause()
    }

    public override fun onResume() {
        if (ApplicationClass.getService() == null) {
            UtilityFun.restartApp()
            finish()
        }
        ApplicationClass.isAppVisible = true
        super.onResume()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY -> playerService!!.play()
            KeyEvent.KEYCODE_MEDIA_NEXT -> playerService!!.nextTrack()
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> playerService!!.prevTrack()
            KeyEvent.KEYCODE_MEDIA_STOP -> playerService!!.stop()
            KeyEvent.KEYCODE_BACK -> onBackPressed()
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result: CropImage.ActivityResult = CropImage.getActivityResult(data)
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val resultUri: Uri = result.uri
                    val fromFile = File(resultUri.path)
                    val savePath: String = when (backgroundSelectionStatus) {
                        MAIN_LIB -> ApplicationClass.getContext().filesDir
                            .toString() + getString(R.string.main_lib_back_custom_image)
                        NOW_PLAYING -> ApplicationClass.getContext().filesDir
                            .toString() + getString(R.string.now_playing_back_custom_image)
                        DEFAULT_ALBUM_ART -> ApplicationClass.getContext().filesDir
                            .toString() + getString(R.string.def_album_art_custom_image)
                        NAVIGATION_DRAWER -> ApplicationClass.getContext().filesDir
                            .toString() + getString(R.string.nav_back_custom_image)
                        else -> ApplicationClass.getContext().filesDir
                            .toString() + getString(R.string.nav_back_custom_image)
                    }
                    val toFile = File(savePath)
                    val b: Boolean = fromFile.renameTo(toFile)
                    Log.d(Constants.TAG,
                        "onActivityResult: saved custom image size : " + toFile.length() / (1024))
                    if (b) {
                        when (backgroundSelectionStatus) {
                            MAIN_LIB -> ApplicationClass.getPref().edit()
                                .putInt(getString(R.string.pref_main_library_back), 1).apply()
                            NOW_PLAYING -> ApplicationClass.getPref().edit()
                                .putInt(getString(R.string.pref_now_playing_back), 3).apply()
                            DEFAULT_ALBUM_ART -> ApplicationClass.getPref().edit()
                                .putInt(getString(R.string.pref_default_album_art), 1).apply()
                            NAVIGATION_DRAWER -> ApplicationClass.getPref().edit()
                                .putInt(getString(R.string.pref_nav_library_back), 1).apply()
                            else -> ApplicationClass.getPref().edit()
                                .putInt(getString(R.string.pref_nav_library_back), 1).apply()
                        }
                        Toast.makeText(this, "Background successfully updated!", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(this,
                            "Failed to save file, try some different image!",
                            Toast.LENGTH_SHORT).show()
                    }
                    Log.d(Constants.TAG, "onActivityResult: $result")
                }
                CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE -> {
                    Toast.makeText(this, "Failed to select image, try again!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    @SuppressLint("validFragment")
    class MyPreferenceFragment : PreferenceFragment(), OnStartDragListener {
        val PLAY_PAUSE: String = "Play/Pause Current Track"
        val NEXT: String = "Play Next Track"
        val PREVIOUS: String = "Play Previous Track"
        val MONOSPACE: String = "Monospace"
        val SOFIA: String = "Sofia"
        val MANROPE: String = "Manrope (Recommended)"
        val ASAP: String = "Asap"
        val SYSTEM_DEFAULT: String = "System Default"
        val ROBOTO: String = "Roboto"
        val LIST: String = "List View"
        val GRID: String = "Grid View"
        private var instantLyricStatus: CheckBoxPreference? = null
        private var mItemTouchHelper: ItemTouchHelper? = null
        override fun onResume() {
            super.onResume()
            setInstantLyricStatus()
        }

        private fun setInstantLyricStatus() {
            if (instantLyricStatus != null) {
                if (NotificationListenerService.isListeningAuthorized(ApplicationClass.getContext())) {
                    ApplicationClass.getPref().edit()
                        .putBoolean(getString(R.string.pref_instant_lyric), true).apply()
                    instantLyricStatus!!.isChecked = true
                } else {
                    ApplicationClass.getPref().edit()
                        .putBoolean(getString(R.string.pref_instant_lyric), false).apply()
                    instantLyricStatus!!.isChecked = false
                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)

            //Theme color
            val primaryColorPref = findPreference(getString(R.string.pref_theme_color))
            primaryColorPref!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    //open browser or intent here
                    //PrimarySelectionDialog();
                    themeSelectionDialog()
                    true
                }

            //now playing back
            val nowPlayingBackPref: Preference =
                findPreference(getString(R.string.pref_now_playing_back))
            nowPlayingBackPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    nowPlayingBackDialog()
                    true
                }

            //Main library back
            val mainLibBackPref: Preference =
                findPreference(getString(R.string.pref_main_library_back))
            mainLibBackPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    mainLibBackDialog()
                    true
                }

            //Main library back
            val navLibBackPref: Preference =
                findPreference(getString(R.string.pref_nav_library_back))
            navLibBackPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    navBackDialog()
                    true
                }

            //Main library back
            val defAlbumArtPref: Preference =
                findPreference(getString(R.string.pref_default_album_art))
            defAlbumArtPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    defAlbumArtDialog()
                    true
                }

            //text font
            val fontPref: Preference = findPreference(getString(R.string.pref_text_font))
            when (ApplicationClass.getPref().getInt(getString(R.string.pref_text_font), Constants.TYPEFACE.MANROPE)) {
                Constants.TYPEFACE.MONOSPACE -> findPreference(getString(R.string.pref_text_font)).summary =
                    MONOSPACE
                Constants.TYPEFACE.SOFIA -> findPreference(getString(R.string.pref_text_font)).summary =
                    SOFIA
                Constants.TYPEFACE.SYSTEM_DEFAULT -> findPreference(getString(R.string.pref_text_font)).summary =
                    SYSTEM_DEFAULT
                Constants.TYPEFACE.MANROPE -> findPreference(getString(R.string.pref_text_font)).summary =
                    MANROPE
                Constants.TYPEFACE.ASAP -> findPreference(getString(R.string.pref_text_font)).summary =
                    ASAP
                Constants.TYPEFACE.ROBOTO -> findPreference(getString(R.string.pref_text_font)).summary =
                    ROBOTO
            }
            fontPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    //open browser or intent here
                    fontPrefSelectionDialog()
                    true
                }

            //lockscreen albumName art
            val lockScreenArt: CheckBoxPreference =
                findPreference(getString(R.string.pref_lock_screen_album_Art)) as CheckBoxPreference
            lockScreenArt.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    if ((newValue as Boolean)) {
                        ApplicationClass.getPref().edit()
                            .putBoolean(getString(R.string.pref_lock_screen_album_Art), true)
                            .apply()
                        ApplicationClass.getService()!!.setMediaSessionMetadata(true)
                    } else {
                        ApplicationClass.getPref().edit()
                            .putBoolean(getString(R.string.pref_lock_screen_album_Art), false)
                            .apply()
                        ApplicationClass.getService()!!.setMediaSessionMetadata(false)
                    }
                    true
                }

            //prefer system equalizer
            val albumLibView: Preference = findPreference(getString(R.string.pref_album_lib_view))
            if (ApplicationClass.getPref().getBoolean(getString(R.string.pref_album_lib_view), true)) {
                albumLibView.summary = GRID
            } else {
                albumLibView.summary = LIST
            }
            albumLibView.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    albumViewDialog()
                    true
                }

            //prefer system equalizer
            val prefPrefSystemEqu: CheckBoxPreference =
                findPreference(getString(R.string.pref_prefer_system_equ)) as CheckBoxPreference
            prefPrefSystemEqu.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    if ((newValue as Boolean)) {
                        ApplicationClass.getPref().edit()
                            .putBoolean(getString(R.string.pref_prefer_system_equ), true).apply()
                    } else {
                        ApplicationClass.getPref().edit()
                            .putBoolean(getString(R.string.pref_prefer_system_equ), false).apply()
                    }
                    true
                }

            //notifcations
            val notifications: CheckBoxPreference =
                findPreference(getString(R.string.pref_notifications)) as CheckBoxPreference
            notifications.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    val pos_text: String
                    if ((newValue as Boolean)) {
                        pos_text = getString(R.string.turn_on)
                    } else {
                        pos_text = getString(R.string.turn_off)
                    }
                    MaterialDialog(activity)
                        .title(R.string.notifications_title)
                        .message(R.string.notification_content)
                        .positiveButton(text = pos_text){
                            val country = ApplicationClass.getPref().getString(ApplicationClass.getContext().getString(R.string.pref_user_country), "")
                            when {
                                ApplicationClass.getPref().getBoolean(getString(R.string.pref_notifications), true) -> {
                                    ApplicationClass.getPref().edit()
                                        .putBoolean(getString(R.string.pref_notifications), false)
                                        .apply()
                                    notifications.isChecked = false
                                }
                                else -> {
                                    ApplicationClass.getPref().edit()
                                        .putBoolean(getString(R.string.pref_notifications), true)
                                        .apply()
                                    notifications.isChecked = true
                                }
                            }
                        }
                        .negativeButton(R.string.cancel)
                        .show()
                    false
                }

            //shake
            val shakeStatus: CheckBoxPreference =
                findPreference(getString(R.string.pref_shake)) as CheckBoxPreference
            shakeStatus.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    if ((newValue as Boolean)) {
                        ApplicationClass.getPref().edit()
                            .putBoolean(getString(R.string.pref_shake), true).apply()
                        //playerService!!.setShakeListener(true)
                    } else {
                        ApplicationClass.getPref().edit()
                            .putBoolean(getString(R.string.pref_shake), false).apply()
                       // playerService!!.setShakeListener(false)
                    }
                    true
                }
            val continuousPlaybackPref: CheckBoxPreference =
                findPreference(getString(R.string.pref_continuous_playback)) as CheckBoxPreference
            continuousPlaybackPref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    val pos_text: String = if ((newValue as Boolean)) {
                        getString(R.string.turn_on)
                    } else {
                        getString(R.string.turn_off)
                    }
                    val dialog: MaterialDialog = MaterialDialog(activity)
                        .title(R.string.title_continous_playback)
                        .message(R.string.cont_playback_content)
                        .positiveButton(text = pos_text){
                            if (newValue) {
                                ApplicationClass.getPref().edit()
                                    .putBoolean(getString(R.string.pref_continuous_playback), true)
                                    .apply()
                                continuousPlaybackPref.isChecked = true
                            } else {
                                ApplicationClass.getPref().edit()
                                    .putBoolean(getString(R.string.pref_continuous_playback), false)
                                    .apply()
                                continuousPlaybackPref.isChecked = false
                            }
                        }
                        .negativeButton(R.string.cancel)
                    dialog.show()
                    false
                }
            val dataSaverPref: CheckBoxPreference =
                findPreference(getString(R.string.pref_data_saver)) as CheckBoxPreference
            dataSaverPref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    val pos_text: String = if ((newValue as Boolean)) {
                        getString(R.string.turn_on)
                    } else {
                        getString(R.string.turn_off)
                    }
                    val dialog: MaterialDialog = MaterialDialog(activity)
                        .title(R.string.title_data_Saver)
                        .message(R.string.data_saver_content)
                        .positiveButton(text = pos_text){
                            if (newValue) {
                                ApplicationClass.getPref().edit()
                                    .putBoolean(getString(R.string.pref_data_saver), true).apply()
                                dataSaverPref.isChecked = true
                            } else {
                                ApplicationClass.getPref().edit()
                                    .putBoolean(getString(R.string.pref_data_saver), false).apply()
                                dataSaverPref.isChecked = false
                            }
                        }
                        .negativeButton(R.string.cancel)

                    dialog.show()
                    false
                }
            instantLyricStatus =
                findPreference(getString(R.string.pref_instant_lyric)) as CheckBoxPreference?
            //instant lyric
            instantLyricStatus!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    val pos_text: String = when {
                        newValue as Boolean -> {
                            getString(R.string.turn_on)
                        }
                        else -> {
                            getString(R.string.turn_off)
                        }
                    }
                    MaterialDialog(activity)
                        .title(R.string.instant_lyrics_title)
                        .message(R.string.instant_lyrics_content)
                        .positiveButton(text = pos_text){
                            when {
                                newValue -> {
                                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                    startActivity(intent)
                                    Toast.makeText(ApplicationClass.getContext(),
                                        "Click on Muserse to enable!",
                                        Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    val intent: Intent =
                                        Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                    startActivity(intent)
                                    Toast.makeText(ApplicationClass.getContext(),
                                        "Click on Muserse to disable!",
                                        Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        .negativeButton(R.string.cancel)
                        .show()
                    false
                }

            //shake
            val shakeAction: Preference = findPreference(getString(R.string.pref_shake_action))
            when (ApplicationClass.getPref().getInt(getString(R.string.pref_shake_action), Constants.SHAKE_ACTIONS.NEXT)) {
                Constants.SHAKE_ACTIONS.NEXT -> {
                    shakeAction.summary = NEXT
                }
                Constants.SHAKE_ACTIONS.PLAY_PAUSE -> {
                    shakeAction.summary = PLAY_PAUSE
                }
                else -> {
                    shakeAction.summary = PREVIOUS
                }
            }
            shakeAction.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    //open browser or intent here
                    ShakeActionDialog()
                    true
                }


            //hide short clips preference
            val hideShortClipsPref: Preference =
                findPreference(getString(R.string.pref_hide_short_clips))
            val summary = ApplicationClass.getPref().getInt(getString(R.string.pref_hide_short_clips), 10).toString() + " seconds"
            hideShortClipsPref.summary = summary
            hideShortClipsPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    //open browser or intent here
                    shortClipDialog()
                    true
                }

            //excluded folders preference
            val excludedFoldersPref: Preference =
                findPreference(getString(R.string.pref_excluded_folders))
            excludedFoldersPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    //open browser or intent here
                    displayExcludedFolders()
                    true
                }
            val hideByStartPref: Preference = findPreference(getString(R.string.pref_hide_tracks_starting_with))
            val text1 = ApplicationClass.getPref()
                .getString(getString(R.string.pref_hide_tracks_starting_with_1), "")
            val text2 = ApplicationClass.getPref()
                .getString(getString(R.string.pref_hide_tracks_starting_with_2), "")
            val text3 = ApplicationClass.getPref()
                .getString(getString(R.string.pref_hide_tracks_starting_with_3), "")
            hideByStartPref.summary = "$text1, $text2, $text3"
            hideByStartPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    //open browser or intent here
                    hideByStartDialog()
                    true
                }


            //opening tab preference
            val openingTabPref: Preference = findPreference(getString(R.string.pref_opening_tab))
            openingTabPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    //open browser or intent here
                    tabSeqDialog()
                    true
                }


            //about us  preference
            val aboutUs: Preference = findPreference(getString(R.string.pref_about_us))
            aboutUs.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    //open browser or intent here
                    activity.startActivity(Intent(activity, ActivityAboutUs::class.java))
                    true
                }

            //cache artist data
            /*Preference cacheArtistDataPref = findPreference(getString(R.string.pref_cache_artist_data));
            Long lastTimeDidAt = MyApp.getPref().getLong(getString(R.string.pref_artist_cache_manual),0);
            if (System.currentTimeMillis() >= lastTimeDidAt +
                    (2 * 60 * 60 * 1000)) {
                cacheArtistDatapref!!.setEnabled(true);
            }
            cacheArtistDatapref!!.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    preference.setEnabled(false);
                    MyApp.getPref().edit().putLong(getString(R.string.pref_artist_cache_manual), System.currentTimeMillis()).apply();
                    new BulkArtInfoGrabber().start();
                    Toast.makeText(MyApp.getContext(), "Artist info local caching started in background, will be finished shortly!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });*/

            //batch download  preference
            val batchDownload: Preference = findPreference(getString(R.string.pref_batch_download))
            batchDownload.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    if (ApplicationClass.isBatchServiceRunning) {
                        Toast.makeText(activity,
                            getString(R.string.error_batch_download_running),
                            Toast.LENGTH_LONG).show()
                        return@OnPreferenceClickListener false
                    }
                    activity.startService(Intent(activity,
                        BatchDownloaderService::class.java))
                    Toast.makeText(activity,
                        getString(R.string.batch_download_started),
                        Toast.LENGTH_LONG).show()
                    true
                }

            //reset  preference
            val resetPref: Preference = findPreference(getString(R.string.pref_reset_pref))
            resetPref.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    resetPrefDialog()
                    true
                }
        }

        private fun albumViewDialog() {
            val dialog: MaterialDialog = MaterialDialog(activity)
                .title(R.string.title_album_lib_view)
                .listItems(items = listOf(LIST, GRID), selection = object : ItemListener{
                    override fun invoke(dialog: MaterialDialog, index: Int, text: CharSequence) {
                        when (text.toString()) {
                            LIST -> {
                                ApplicationClass.getPref().edit()
                                    .putBoolean(getString(R.string.pref_album_lib_view), false).apply()
                                findPreference(getString(R.string.pref_album_lib_view)).summary = LIST
                            }
                            GRID -> {
                                ApplicationClass.getPref().edit()
                                    .putBoolean(getString(R.string.pref_album_lib_view), true).apply()
                                findPreference(getString(R.string.pref_album_lib_view)).summary = GRID
                            }
                        }
                    }
                })

            dialog.show()
        }

        private fun navBackDialog() {
            ///get current setting
            // 0 - System default   2 - custom
            val currentSelection: Int = ApplicationClass.getPref().getInt(getString(R.string.pref_nav_library_back), 0)
            val dialog: MaterialDialog = MaterialDialog(activity)
                .title(R.string.title_nav_back)
                .listItems(R.array.nav_back_pref_array, selection = object : ItemListener{
                    override fun invoke(dialog: MaterialDialog, index: Int, text: CharSequence) {
                        when (index) {
                            0 -> ApplicationClass.getPref().edit()
                                .putInt(getString(R.string.pref_nav_library_back), index).apply()
                            1 -> {
                                backgroundSelectionStatus = NAVIGATION_DRAWER
                                CropImage.activity()
                                    .setGuidelines(CropImageView.Guidelines.ON)
                                    .setAspectRatio(11, 16)
                                    .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                                    .setOutputCompressQuality(80)
                                    .start(activity)
                            }
                        }
                        true
                    }
                })
                .positiveButton(R.string.okay)

            dialog.show()
        }

        private fun defAlbumArtDialog() {
            ///get current setting
            // 0 - System default   2 - custom
            val currentSelection: Int =
                ApplicationClass.getPref().getInt(getString(R.string.pref_default_album_art), 0)
            val dialog: MaterialDialog = MaterialDialog(activity)
                .title(R.string.nav_default_album_art)
                .listItems(R.array.def_album_art_pref_array, selection = object : ItemListener{
                    override fun invoke(dialog: MaterialDialog, index: Int, text: CharSequence) {
                        when (index) {
                            0 -> ApplicationClass.getPref().edit()
                                .putInt(getString(R.string.pref_default_album_art), index).apply()
                            1 -> {
                                backgroundSelectionStatus = DEFAULT_ALBUM_ART
                                CropImage.activity()
                                    .setGuidelines(CropImageView.Guidelines.ON)
                                    .setAspectRatio(1, 1)
                                    .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                                    .setOutputCompressQuality(80)
                                    .start(activity)
                            }
                        }
                        true
                    }
                })
                .positiveButton(R.string.okay)

            dialog.show()
        }

        private fun mainLibBackDialog() {
            ///get current setting
            // 0 - System default   2 - custom
            val currentSelection: Int =
                ApplicationClass.getPref().getInt(getString(R.string.pref_main_library_back), 0)
            val dialog: MaterialDialog = MaterialDialog(activity)
                .title(R.string.title_main_library_back)
                .listItems(R.array.main_lib_back_pref_array, selection = object : ItemListener {
                    override fun invoke(dialog: MaterialDialog, index: Int, text: CharSequence) {

                        when (index) {
                        0 -> ApplicationClass.getPref().edit()
                        .putInt(getString(R.string.pref_main_library_back), index).apply()
                        1 -> {
                        backgroundSelectionStatus = MAIN_LIB
                        CropImage.activity()
                            .setGuidelines(CropImageView.Guidelines.ON)
                            .setAspectRatio(11, 16)
                            .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                            .setOutputCompressQuality(50)
                            .start(activity)
                    }
                    }
                    true
                }
                })
                .positiveButton(R.string.okay)

            dialog.show()
        }

        private fun nowPlayingBackDialog() {

            ///get current setting
            // 0 - System default   1 - artist image  2 - album art 3 - custom  4- custom (if Artist image unavailable)
            val currentSelection: Int =
                ApplicationClass.getPref().getInt(getString(R.string.pref_now_playing_back), 1)
            val dialog = MaterialDialog(activity)
                .title(R.string.title_now_playing_back)
                .listItems(R.array.now_playing_back_pref_array, selection = object : ItemListener{
                    override fun invoke(dialog: MaterialDialog, index: Int, text: CharSequence) {
                        when (index) {
                            0, 1, 2 -> ApplicationClass.getPref().edit()
                                .putInt(getString(R.string.pref_now_playing_back), index).apply()
                            3 -> {
                                backgroundSelectionStatus = NOW_PLAYING
                                CropImage.activity()
                                    .setGuidelines(CropImageView.Guidelines.ON)
                                    .setAspectRatio(9, 16)
                                    .setOutputCompressFormat(Bitmap.CompressFormat.JPEG)
                                    .setOutputCompressQuality(50)
                                    .start(activity)
                            }
                        }
                        true
                    }
                })
                .positiveButton(R.string.okay)
            dialog.show()
        }

        private fun displayExcludedFolders() {
            val excludedFoldersString = ApplicationClass.getPref().getString(getString(R.string.pref_excluded_folders), "")
            val excludedFolders = excludedFoldersString!!.split(",".toRegex()).toTypedArray().toList()
            val dialog: MaterialDialog = MaterialDialog(activity)
                .title(R.string.title_excluded_folders)
                .listItems(items = excludedFolders)
                .positiveButton(R.string.add){
                    MaterialDialog(activity)
                        .title(R.string.title_how_to_add)
                        .message(R.string.content_how_to_add)
                        .positiveButton(R.string.pos_how_to_add)
                        .show()
                }
                .negativeButton(R.string.reset){
                    ApplicationClass.getPref().edit().putString(getString(R.string.pref_excluded_folders), "").apply()
                    MusicLibrary.instance.RefreshLibrary()
                    Toast.makeText(activity, "Excluded folders reset, refreshing Music Library..", Toast.LENGTH_SHORT).show()
                }

            dialog.show()
        }

        private fun tabSeqDialog() {
            val inflater: LayoutInflater = activity.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.tab_sequence_preference, null)
            val rv: RecyclerView = dialogView.findViewById(R.id.rv_for_tab_sequence)
            val tsa = TabSequenceAdapter(this)
            rv.adapter = tsa
            rv.layoutManager = WrapContentLinearLayoutManager(ApplicationClass.getContext())
            val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(tsa)
            mItemTouchHelper = ItemTouchHelper(callback)
            mItemTouchHelper!!.attachToRecyclerView(rv)
            val dialog = MaterialDialog(activity)
                .title(R.string.setting_tab_seqe_title)
                .customView(view = dialogView, scrollable =  false)

            dialog.setOnDismissListener {
                    val temp: IntArray = tsa.getData()
                    val str: StringBuilder = StringBuilder()
                    for (aTemp: Int in temp) {
                        str.append(aTemp).append(",")
                    }
                    ApplicationClass.getPref().edit()
                        .putString(getString(R.string.pref_tab_seq), str.toString()).apply()
                }
            dialog.show()
        }

        private fun themeSelectionDialog() {
            val inflater: LayoutInflater = activity.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.theme_selector_dialog, null)
            val rv: RecyclerView = dialogView.findViewById(R.id.rv_for_theme_selector)
            val tsa = ThemeSelectorAdapter()
            rv.adapter = tsa
            val layoutManager = FlexboxLayoutManager(activity)
            layoutManager.flexDirection = FlexDirection.ROW
            layoutManager.justifyContent = JustifyContent.SPACE_EVENLY
            rv.layoutManager = layoutManager
            //rv.setLayoutManager(new GridLayoutManager(getActivity(), 4));
            val dialog: MaterialDialog = MaterialDialog(activity)
                .title(text = "Select theme")
                .customView(view = dialogView, scrollable = false)
                .positiveButton(text = "Apply"){
                    restartSettingsActivity()
                }

            dialog.show()
        }

        private fun rescanLibrary() {
            MusicLibrary.instance.RefreshLibrary()
            val dialog: ProgressDialog = ProgressDialog.show(activity, "",
                getString(R.string.library_rescan), true)
            Executors.newSingleThreadExecutor().execute {
                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                dialog.dismiss()
                activity.runOnUiThread {
                    Toast.makeText(activity,
                        getString(R.string.main_act_lib_refreshed),
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun shortClipDialog() {
            val linear = LinearLayout(activity)
            linear.orientation = LinearLayout.VERTICAL
            val text = TextView(activity)
            val summary = ApplicationClass.getPref().getInt(getString(R.string.pref_hide_short_clips), 10).toString() + " seconds"
            text.text = summary
            text.setTypeface(TypeFaceHelper.getTypeFace(ApplicationClass.getContext()))
            text.setPadding(0, 10, 0, 0)
            text.gravity = Gravity.CENTER
            val seek = SeekBar(activity)
            seek.setPadding(40, 10, 40, 10)
            seek.max = 100
            seek.progress = ApplicationClass.getPref().getInt(getString(R.string.pref_hide_short_clips), 10)
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    text.text = "$progress seconds"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val progress: Int = seekBar.progress
                    ApplicationClass.getPref().edit().putInt(getString(R.string.pref_hide_short_clips), progress).apply()
                    findPreference(getString(R.string.pref_hide_short_clips)).summary = progress.toString() + " seconds"
                }
            })
            linear.addView(seek)
            linear.addView(text)
            val dialog: MaterialDialog = MaterialDialog(activity)
                .title(R.string.title_hide_short_clips)
                .positiveButton(R.string.okay){
                    rescanLibrary()
                }
                .negativeButton(R.string.cancel)
                .customView(view = linear, scrollable = false)

            dialog.show()
        }

        private fun hideByStartDialog() {
            val text1 = ApplicationClass.getPref().getString(getString(R.string.pref_hide_tracks_starting_with_1), "")
            val text2 = ApplicationClass.getPref().getString(getString(R.string.pref_hide_tracks_starting_with_2), "")
            val text3 = ApplicationClass.getPref().getString(getString(R.string.pref_hide_tracks_starting_with_3), "")
            findPreference(getString(R.string.pref_hide_tracks_starting_with)).summary = text1 + ", " + text2 + ", " + text3
            val linear: LinearLayout = LinearLayout(activity)
            linear.setPadding(10, 10, 10, 0)
            val myEditText1 = EditText(activity) // Pass it an Activity or Context
            myEditText1.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT) // Pass two args; must be LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, or an integer pixel value.
            myEditText1.setText(text1)
            //myEditText1.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            myEditText1.inputType = InputType.TYPE_CLASS_TEXT
            myEditText1.maxLines = 1
            linear.addView(myEditText1)
            val myEditText2 = EditText(activity) // Pass it an Activity or Context
            myEditText2.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT) // Pass two args; must be LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, or an integer pixel value.
            myEditText2.setText(text2)
            // myEditText2.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            myEditText2.maxLines = 1
            myEditText2.inputType = InputType.TYPE_CLASS_TEXT
            linear.addView(myEditText2)
            val myEditText3 = EditText(activity) // Pass it an Activity or Context
            myEditText3.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT) // Pass two args; must be LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, or an integer pixel value.
            myEditText3.setText(text3)
            //myEditText3.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            myEditText3.inputType = InputType.TYPE_CLASS_TEXT
            myEditText3.maxLines = 1
            linear.addView(myEditText3)
            val tv: TextView = TextView(activity)
            tv.text = getString(R.string.case_sensitive_text)
            tv.setTypeface(TypeFaceHelper.getTypeFace(ApplicationClass.Companion.getContext()))
            tv.setPadding(0, 10, 0, 0)
            linear.addView(tv)
            linear.orientation = LinearLayout.VERTICAL
            val dialog: MaterialDialog = MaterialDialog(activity)
                .title(R.string.title_hide_tracks_starting_with)
                .positiveButton(R.string.okay){
                    val text11: String = myEditText1.text.toString().trim { it <= ' ' }
                    ApplicationClass.getPref().edit()
                        .putString(getString(R.string.pref_hide_tracks_starting_with_1), text11)
                        .apply()
                    val text21: String = myEditText2.text.toString().trim { it <= ' ' }
                    ApplicationClass.getPref().edit()
                        .putString(getString(R.string.pref_hide_tracks_starting_with_2), text21)
                        .apply()
                    val text31: String = myEditText3.text.toString().trim { it <= ' ' }
                    ApplicationClass.getPref().edit()
                        .putString(getString(R.string.pref_hide_tracks_starting_with_3), text31)
                        .apply()
                    findPreference(getString(R.string.pref_hide_tracks_starting_with)).summary = "$text11, $text21, $text31"
                    rescanLibrary()
                }
                .negativeButton(R.string.cancel)
                .customView(view = linear, scrollable = true)

            dialog.show()
        }

        private fun ShakeActionDialog() {
            val dialog: MaterialDialog = MaterialDialog(activity)
                .title(R.string.title_shake_action)
                .listItems(items = listOf(NEXT, PLAY_PAUSE, PREVIOUS), selection = object : ItemListener{
                    override fun invoke(dialog: MaterialDialog, index: Int, text: CharSequence) {
                        when (text.toString()) {
                            NEXT -> {
                                ApplicationClass.getPref().edit()
                                    .putInt(getString(R.string.pref_shake_action),
                                        Constants.SHAKE_ACTIONS.NEXT).apply()
                                findPreference(getString(R.string.pref_shake_action)).summary = NEXT
                            }
                            PLAY_PAUSE -> {
                                ApplicationClass.getPref().edit()
                                    .putInt(getString(R.string.pref_shake_action),
                                        Constants.SHAKE_ACTIONS.PLAY_PAUSE).apply()
                                findPreference(getString(R.string.pref_shake_action)).summary =
                                    PLAY_PAUSE
                            }
                            PREVIOUS -> {
                                ApplicationClass.getPref().edit()
                                    .putInt(getString(R.string.pref_shake_action),
                                        Constants.SHAKE_ACTIONS.PREVIOUS).apply()
                                findPreference(getString(R.string.pref_shake_action)).summary = PREVIOUS
                            }
                        }
                    }

                })

            dialog.show()
        }

        private fun resetPrefDialog() {
            val dialog: MaterialDialog = MaterialDialog(activity)
                .title(text = getString(R.string.title_reset_pref) + " ?") // .content(getString(R.string.lyric_art_info_content))
                .positiveButton(R.string.yes){
                    val editor: SharedPreferences.Editor = ApplicationClass.getPref().edit()
                    editor.putInt(getString(R.string.pref_theme), Constants.PRIMARY_COLOR.GLOSSY)
                    editor.putInt(getString(R.string.pref_text_font), Constants.TYPEFACE.SOFIA)
                    editor.remove(getString(R.string.pref_tab_seq))
                    editor.putBoolean(getString(R.string.pref_lock_screen_album_Art), true)
                    editor.putBoolean(getString(R.string.pref_shake), false)
                    editor.putInt(getString(R.string.pref_hide_short_clips), 10)
                    editor.putString(getString(R.string.pref_hide_tracks_starting_with_1), "")
                    editor.putString(getString(R.string.pref_hide_tracks_starting_with_2), "")
                    editor.putString(getString(R.string.pref_hide_tracks_starting_with_3), "")
                    editor.putString(getString(R.string.pref_excluded_folders), "")
                    editor.putBoolean(getString(R.string.pref_prefer_system_equ), true)
                    editor.putInt(getString(R.string.pref_main_library_back), 0)
                    editor.putInt(getString(R.string.pref_now_playing_back), 0)
                    editor.putBoolean(getString(R.string.pref_hide_lock_button), false)
                    editor.putBoolean(getString(R.string.pref_notifications), true)
                    editor.putBoolean(getString(R.string.pref_continuous_playback), false)
                    editor.putBoolean(getString(R.string.pref_data_saver), false)
                    editor.apply()
                    restartSettingsActivity()
                }
                .negativeButton(R.string.cancel)

            dialog.show()
        }

        private fun fontPrefSelectionDialog() {
            val dialog: MaterialDialog = MaterialDialog(activity)
                .title(R.string.title_text_font)
                .listItems(items = listOf(MANROPE,
                    ROBOTO,
                    ASAP,
                    SOFIA,
                    MONOSPACE,
                    SYSTEM_DEFAULT)
                , selection = object : ItemListener{
                        override fun invoke(dialog: MaterialDialog, index: Int, text: CharSequence, ) {
                            when (text.toString()) {
                                MANROPE -> {
                                    ApplicationClass.getPref().edit()
                                        .putInt(getString(R.string.pref_text_font),
                                            Constants.TYPEFACE.MANROPE).apply()
                                    findPreference(getString(R.string.pref_text_font)).summary = MANROPE
                                }
                                ROBOTO -> {
                                    ApplicationClass.getPref().edit()
                                        .putInt(getString(R.string.pref_text_font),
                                            Constants.TYPEFACE.ROBOTO).apply()
                                    findPreference(getString(R.string.pref_text_font)).summary = ROBOTO
                                }
                                MONOSPACE -> {
                                    ApplicationClass.getPref().edit()
                                        .putInt(getString(R.string.pref_text_font),
                                            Constants.TYPEFACE.MONOSPACE).apply()
                                    findPreference(getString(R.string.pref_text_font)).summary = MONOSPACE
                                }
                                ASAP -> {
                                    ApplicationClass.getPref().edit()
                                        .putInt(getString(R.string.pref_text_font), Constants.TYPEFACE.ASAP)
                                        .apply()
                                    findPreference(getString(R.string.pref_text_font)).summary = ASAP
                                }
                                SOFIA -> {
                                    ApplicationClass.getPref().edit()
                                        .putInt(getString(R.string.pref_text_font),
                                            Constants.TYPEFACE.SOFIA).apply()
                                    findPreference(getString(R.string.pref_text_font)).summary = SOFIA
                                }
                                SYSTEM_DEFAULT -> {
                                    ApplicationClass.getPref().edit()
                                        .putInt(getString(R.string.pref_text_font),
                                            Constants.TYPEFACE.SYSTEM_DEFAULT).apply()
                                    findPreference(getString(R.string.pref_text_font)).summary =
                                        SYSTEM_DEFAULT
                                }
                            }
                            ApplicationClass.getPref().edit()
                                .putBoolean(getString(R.string.pref_font_already_logged), false).apply()
                            val path = TypeFaceHelper.getTypeFacePath()
                            ViewPump.init(ViewPump.builder()
                                .addInterceptor(CalligraphyInterceptor(
                                    CalligraphyConfig.Builder()
                                        .setDefaultFontPath(path)
                                        .setFontAttrId(R.attr.fontPath)
                                        .build()))
                                .build())
                            restartSettingsActivity()
                        }
                    }
                )

            dialog.show()
        }

        private fun restartSettingsActivity() {
            val intent: Intent = activity.intent
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("ad", false)
            activity.finish()
            startActivity(intent)
        }

        override fun onStartDrag(viewHolder: RecyclerView.ViewHolder?) {
            mItemTouchHelper!!.startDrag(viewHolder!!)
        }
    }

    private class TabSequenceAdapter(dragStartListener: OnStartDragListener) :
        RecyclerView.Adapter<TabSequenceAdapter.MyViewHolder?>(), ItemTouchHelperAdapter {
        var data: IntArray = IntArray(Constants.TABS.NUMBER_OF_TABS)
        private val mDragStartListener: OnStartDragListener = dragStartListener

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val inflater: LayoutInflater = LayoutInflater.from(ApplicationClass.getContext())
            val view: View = inflater.inflate(R.layout.tab_sequence_item, parent, false)
            return MyViewHolder(view)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            //holder.title.setText(data.get(0));
            when (data[position]) {
                Constants.TABS.ALBUMS -> holder.title.text = ApplicationClass.getContext().getString(R.string.tab_album)
                Constants.TABS.ARTIST -> holder.title.text = ApplicationClass.getContext().getString(R.string.tab_artist)
                Constants.TABS.FOLDER -> holder.title.text = ApplicationClass.getContext().getString(R.string.tab_folder)
                Constants.TABS.GENRE -> holder.title.text = ApplicationClass.getContext().getString(R.string.tab_genre)
                Constants.TABS.PLAYLIST -> holder.title.text = ApplicationClass.getContext().getString(R.string.tab_playlist)
                Constants.TABS.TRACKS -> holder.title.text = ApplicationClass.getContext().getString(R.string.tab_track)
            }
            holder.handle.setOnTouchListener { view: View?, motionEvent: MotionEvent? ->
                if (MotionEventCompat.getActionMasked(motionEvent) == MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onStartDrag(holder)
                }
                false
            }
        }

        override fun getItemCount(): Int {
            return data.size
        }

        override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
            val temp: Int = data[fromPosition]
            data[fromPosition] = data[toPosition]
            data[toPosition] = temp
            notifyItemMoved(fromPosition, toPosition)
            return true
        }

        override fun onItemDismiss(position: Int) {
            notifyItemChanged(position)
        }

        @JvmName("getData1")
        fun getData(): IntArray {
            return data
        }

        class MyViewHolder constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            var title: TextView = itemView.findViewById(R.id.tab_name)
            var handle: ImageView

            init {
                title.typeface = TypeFaceHelper.getTypeFace(ApplicationClass.getContext())
                //title.setTypeface(TypeFaceHelper.getTypeFace());
                handle = itemView.findViewById<ImageView>(R.id.handle_for_drag)
            }
        }

        init {
            val savedTabSeq = ApplicationClass.getPref()
                .getString(ApplicationClass.getContext().getString(R.string.pref_tab_seq), Constants.TABS.DEFAULT_SEQ)
            val st: StringTokenizer = StringTokenizer(savedTabSeq, ",")
            for (i in 0 until Constants.TABS.NUMBER_OF_TABS) {
                data[i] = st.nextToken().toInt()
            }
        }
    }

    private class ThemeSelectorAdapter :
        RecyclerView.Adapter<ThemeSelectorAdapter.MyViewHolder?>() {
        private var currentSelectedItem: Int
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val inflater: LayoutInflater = LayoutInflater.from(ApplicationClass.getContext())
            val view: View = inflater.inflate(R.layout.theme_selection_item, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.view.setBackgroundDrawable(ColorHelper.getGradientDrawable(position))
            holder.view.setOnClickListener { view: View? ->
                currentSelectedItem = holder.adapterPosition
                ApplicationClass.setSelectedThemeId(holder.adapterPosition)
                notifyDataSetChanged()
            }
            if (currentSelectedItem == position) {
                holder.tick.visibility = View.VISIBLE
            } else {
                holder.tick.visibility = View.INVISIBLE
            }
        }

        override fun getItemCount(): Int {
            return ColorHelper.getNumberOfThemes()
        }

        class MyViewHolder constructor(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            var view: View = itemView.findViewById(R.id.themeView)
            var tick: View = itemView.findViewById(R.id.tick)
        }

        init {
            currentSelectedItem = ApplicationClass.getSelectedThemeId()
        }
    }

    private class WrapContentLinearLayoutManager(context: Context?) :
        LinearLayoutManager(context) {
        //... constructor
        override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
            try {
                super.onLayoutChildren(recycler, state)
            } catch (e: IndexOutOfBoundsException) {
                Log.e("probe", "meet a IOOBE in RecyclerView")
            }
        }
    }

    companion object {
        private val MAIN_LIB: Int = 0
        private val NOW_PLAYING: Int = 1
        private val NAVIGATION_DRAWER: Int = 2
        private val DEFAULT_ALBUM_ART: Int = 3
        private var backgroundSelectionStatus: Int = -1
    }
}