package com.aemerse.muserse.activity

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.uiElementHelper.BottomOffsetDecoration
import com.aemerse.muserse.uiElementHelper.ColorHelper
import com.aemerse.muserse.uiElementHelper.TypeFaceHelper
import com.aemerse.muserse.adapter.AlbumLibraryAdapter
import com.aemerse.muserse.adapter.SecondaryLibraryAdapter
import com.aemerse.muserse.customViews.ExpandableTextView
import com.aemerse.muserse.model.*
import com.aemerse.muserse.qlyrics.ArtistInfo.ArtistInfo
import com.aemerse.muserse.qlyrics.offlineStorage.OfflineStorageArtistBio
import com.aemerse.muserse.qlyrics.tasks.DownloadArtInfoThread
import com.aemerse.muserse.service.PlayerService
import com.aemerse.muserse.utils.UtilityFun
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

class ActivitySecondaryLibrary : AppCompatActivity(), View.OnClickListener, ArtistInfo.Callback {
    @JvmField @BindView(R.id.secondaryLibraryList)
    var mRecyclerView: RecyclerView? = null

    @JvmField @BindView(R.id.albumsInArtistFrag)
    var mAlbumsRecyclerView: RecyclerView? = null

    @JvmField @BindView(R.id.artistBio)
    var artistBio: ExpandableTextView? = null
    private var adapter: SecondaryLibraryAdapter? = null
    private var mReceiverForMiniPLayerUpdate: BroadcastReceiver? = null
    private var mReceiverForDataReady: BroadcastReceiver? = null

    @JvmField @BindView(R.id.song_name_mini_player)
    var songNameMiniPlayer: TextView? = null

    @JvmField @BindView(R.id.artist_mini_player)
    var artistNameMiniPlayer: TextView? = null

    @JvmField @BindView(R.id.play_pause_mini_player)
    var buttonPlay: ImageView? = null

    @JvmField @BindView(R.id.album_art_mini_player)
    var albumArtIv: ImageView? = null

    @JvmField @BindView(R.id.mini_player)
    var miniPlayer: LinearLayout? = null

    @JvmField @BindView(R.id.next_mini_plaayrer)
    var buttonNext: ImageView? = null

    @JvmField @BindView(R.id.main_backdrop)
    var mainBackdrop: ImageView? = null

    @JvmField @BindView(R.id.fab_right_side)
    var fab: FloatingActionButton? = null

    @JvmField @BindView(R.id.border_view)
    var border: View? = null

    @JvmField @BindView(R.id.progressBar)
    var progressBar: View? = null

    @JvmField @BindView(R.id.main_collapsing)
    var collapsingToolbarLayout: CollapsingToolbarLayout? = null

    @JvmField @BindView(R.id.root_view_secondary_lib)
    var rootView: View? = null

    private var mLastClickTime: Long = 0
    private var status: Int = 0
    private var key: Int = 0 //text view on which clicked
    private var title: String? = null
    private val handler: Handler = Handler(Looper.getMainLooper())
    var playerService: PlayerService? = null
    private val RC_LOGIN: Int = 100

    override fun onNewIntent(intent: Intent) {
        try {
            val b: Boolean = intent.getBooleanExtra("refresh", false)
            if (b) {
                val position: Int = intent.getIntExtra("position", -1)
                val title: String = intent.getStringExtra("title")!!
                val artist: String = intent.getStringExtra("artist")!!
                val album: String = intent.getStringExtra("album")!!
                val originalTitle: String = intent.getStringExtra("originalTitle")!!
                val currentItem: TrackItem = playerService!!.getCurrentTrack()!!
                if ((currentItem.title == originalTitle)) {
                    //current song is playing, update  track item
                    playerService!!.updateTrackItem(playerService!!.getCurrentTrackPosition(),
                        currentItem.id,
                        title,
                        artist,
                        album)
                    playerService!!.PostNotification()
                    updateMiniplayerUI()
                }

                //data changed in edit track info activity, update item
                adapter!!.updateItem(position, title, artist, album)
            }
        } catch (ignored: Exception) {
            Log.v(Constants.TAG, ignored.toString())
        }
        super.onNewIntent(intent)
    }

    @SuppressLint("RestrictedApi")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //if player service not running, kill the app
        if (ApplicationClass.getService() == null) {
            UtilityFun.restartApp()
            finish()
            return
        }
        playerService = ApplicationClass.getService()
        ColorHelper.setStatusBarGradiant(this)
        when (ApplicationClass.getPref().getInt(getString(R.string.pref_theme), Constants.PRIMARY_COLOR.LIGHT)) {
            Constants.PRIMARY_COLOR.DARK -> setTheme(R.style.AppThemeDark)
            Constants.PRIMARY_COLOR.GLOSSY -> setTheme(R.style.AppThemeDark)
            Constants.PRIMARY_COLOR.LIGHT -> setTheme(R.style.AppThemeLight)
        }
        setContentView(R.layout.activity_secondary_library)

        val toolbar: Toolbar = findViewById<Toolbar>(R.id.toolbar_)
        try {
            toolbar.setCollapsible(false)
        } catch (ignored: Exception) {
        }
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
        if (intent != null) {
            status = intent.getIntExtra("status", 0)
            key = intent.getIntExtra("key", 0)
            title = intent.getStringExtra("title")
        }
        //remove _ from playlist name
        if (status == Constants.FRAGMENT_STATUS.PLAYLIST_FRAGMENT) {
            setTitle(title!!.replace("_", " "))
        } else {
            setTitle(title)
        }
        if (ApplicationClass.isLocked()) {
            //border.setVisibility(View.VISIBLE);
            border!!.setBackgroundResource(R.drawable.border_2dp)
        } else {
            border!!.setBackgroundResource(0)
        }
        Executors.newSingleThreadExecutor().execute(object : Runnable {
            override fun run() {
                when (status) {
                    Constants.FRAGMENT_STATUS.ARTIST_FRAGMENT -> {
                        adapter = SecondaryLibraryAdapter(
                            this@ActivitySecondaryLibrary,
                            MusicLibrary.instance.getSongListFromArtistIdNew(key, Constants.SORT_ORDER.ASC)!!
                        )
                        if (adapter!!.getList()!!.isEmpty()) {
                            return
                        }

                        //get album list for artist
                        val data: ArrayList<dataItem> = ArrayList<dataItem>()
                        for (d: dataItem in MusicLibrary.instance.getDataItemsForAlbums()) {
                            if (d.artist_id == key) data.add(d)
                        }
                        handler.post { mAlbumsRecyclerView!!.visibility = View.VISIBLE }
                        mAlbumsRecyclerView!!.adapter = AlbumLibraryAdapter(this@ActivitySecondaryLibrary,
                            data)
                        mAlbumsRecyclerView!!.layoutManager = LinearLayoutManager(this@ActivitySecondaryLibrary,
                            LinearLayoutManager.HORIZONTAL,
                            false)
                        mAlbumsRecyclerView!!.isNestedScrollingEnabled = false
                        val item = TrackItem()
                        item.artist_id = key
                        item.setArtist(title)
                        val mArtistInfo: ArtistInfo? =
                            OfflineStorageArtistBio.getArtistBioFromTrackItem(item)
                        //second check is added to make sure internet call will happen
                        //when user manually changes artist tag
                        if (mArtistInfo != null && (item.getArtist()!!
                                .trim { it <= ' ' } == mArtistInfo.getOriginalArtist()!!.trim())
                        ) {
                            handler.post { onArtInfoDownloaded(mArtistInfo) }
                        } else if (UtilityFun.isConnectedToInternet) {
                            var artist: String? = item.getArtist()
                            artist = UtilityFun.filterArtistString(artist!!)
                            DownloadArtInfoThread(this@ActivitySecondaryLibrary,
                                artist,
                                item).start()
                        }
                    }
                    Constants.FRAGMENT_STATUS.ALBUM_FRAGMENT -> {
                        adapter = SecondaryLibraryAdapter(this@ActivitySecondaryLibrary,
                            MusicLibrary.instance.getSongListFromAlbumIdNew(key, Constants.SORT_ORDER.ASC)!!)
                        Collections.sort(adapter!!.getList(), object : Comparator<dataItem> {
                            override fun compare(dataItem: dataItem, t1: dataItem): Int {
                                if (dataItem.trackNumber > t1.trackNumber) return 1 else if (dataItem.trackNumber < t1.trackNumber) return -1 else return 0
                            }
                        })
                    }
                    Constants.FRAGMENT_STATUS.GENRE_FRAGMENT -> {
                        adapter = SecondaryLibraryAdapter(this@ActivitySecondaryLibrary,
                            MusicLibrary.instance.getSongListFromGenreIdNew(key, Constants.SORT_ORDER.ASC)!!)
                        if (adapter!!.getList()!!.isEmpty()) {
                            return
                        }
                    }
                    Constants.FRAGMENT_STATUS.PLAYLIST_FRAGMENT -> {
                        val trackList: ArrayList<dataItem>
                        title = title!!.replace(" ", "_")
                        when (title) {
                            Constants.SYSTEM_PLAYLISTS.MOST_PLAYED -> {
                                trackList = PlaylistManager.getInstance(applicationContext)!!.getPlaylist(Constants.SYSTEM_PLAYLISTS.MOST_PLAYED)
                                adapter = SecondaryLibraryAdapter(this@ActivitySecondaryLibrary,
                                    trackList,
                                    status,
                                    Constants.SYSTEM_PLAYLISTS.MOST_PLAYED)
                            }
                            Constants.SYSTEM_PLAYLISTS.MY_FAV -> {
                                trackList = PlaylistManager.getInstance(applicationContext)!!.getPlaylist(Constants.SYSTEM_PLAYLISTS.MY_FAV)
                                adapter = SecondaryLibraryAdapter(this@ActivitySecondaryLibrary,
                                    trackList,
                                    status,
                                    Constants.SYSTEM_PLAYLISTS.MY_FAV)
                            }
                            Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED -> {
                                trackList = PlaylistManager.getInstance(applicationContext)!!.getPlaylist(Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED)
                                adapter = SecondaryLibraryAdapter(this@ActivitySecondaryLibrary,
                                    trackList,
                                    status,
                                    Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED)
                            }
                            Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED -> {
                                trackList = PlaylistManager.getInstance(applicationContext)!!.getPlaylist(Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED)
                                adapter = SecondaryLibraryAdapter(this@ActivitySecondaryLibrary,
                                    trackList,
                                    status,
                                    Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED)
                            }
                            else -> {
                                trackList = PlaylistManager.getInstance(applicationContext)!!.getPlaylist(title!!)
                                adapter = SecondaryLibraryAdapter(this@ActivitySecondaryLibrary,
                                    trackList,
                                    status,
                                    title)
                            }
                        }
                        if (trackList.isEmpty()) {
                            handler.post {
                                fab!!.setImageDrawable(ContextCompat.getDrawable(this@ActivitySecondaryLibrary,
                                    R.drawable.ic_add_black_24dp))
                            }
                        }
                    }
                }
                handler.post {
                    if (adapter != null) {
                        mRecyclerView!!.adapter = adapter
                    }
                    mRecyclerView!!.layoutManager = WrapContentLinearLayoutManager(this@ActivitySecondaryLibrary)
                    mRecyclerView!!.isNestedScrollingEnabled = false
                    val offsetPx: Float =
                        resources.getDimension(R.dimen.bottom_offset_secondary_lib)
                    val bottomOffsetDecoration: BottomOffsetDecoration = BottomOffsetDecoration(
                        offsetPx.toInt())
                    mRecyclerView!!.addItemDecoration(bottomOffsetDecoration)
                    border!!.visibility = View.VISIBLE
                    progressBar!!.visibility = View.INVISIBLE
                    var item: TrackItem? = null
                    if ((adapter != null) && (adapter!!.getList() != null) && (adapter!!.getList()!!.size > 0)) {
                        item = MusicLibrary.instance.getTrackItemFromId(adapter!!.getList()!![0].id)
                    }
                    Log.d("SecondaryLibraryActivi", "onCreate: item $item")
                    if (item != null) {
                        val url: String? =
                            MusicLibrary.instance.artistUrls[item.getArtist()]
                        Log.d("SecondaryLibraryActivi", "onCreate: url $url")
                        if (UtilityFun.isConnectedToInternet && url != null) {
                            setArtistImage(url)
                        } else {
                            val defaultAlbumArtSetting: Int = ApplicationClass.getPref()
                                .getInt(getString(R.string.pref_default_album_art), 0)
                            when (defaultAlbumArtSetting) {
                                0 -> Glide.with(this@ActivitySecondaryLibrary)
                                    .load(MusicLibrary.instance
                                        .getAlbumArtUri(item.albumId))
                                    .centerCrop()
                                    .placeholder(R.drawable.music)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .into(mainBackdrop!!)
                                1 -> Glide.with(this@ActivitySecondaryLibrary)
                                    .load(MusicLibrary.instance.getAlbumArtUri(item.albumId))
                                    .centerCrop()
                                    .placeholder(UtilityFun.defaultAlbumArtDrawable)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(mainBackdrop!!)
                            }
                        }
                    }
                }
            }
        })
        mReceiverForMiniPLayerUpdate = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateMiniplayerUI()
            }
        }
        mReceiverForDataReady = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                //updateMiniplayerUI();
                border!!.visibility = View.VISIBLE
            }
        }
        miniPlayer!!.setOnClickListener(this)
        buttonPlay!!.setOnClickListener(this)
        buttonNext!!.setOnClickListener(this)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        miniPlayer!!.setBackgroundColor(ColorHelper.getWidgetColor())
        //collapsingToolbarLayout.setContentScrimColor(ColorHelper.Ge());
        fab!!.setOnClickListener { view: View? ->
            if (status == Constants.FRAGMENT_STATUS.PLAYLIST_FRAGMENT && adapter!!.itemCount <= 2) {
                startActivity(Intent(this@ActivitySecondaryLibrary, ActivityMain::class.java)
                    .putExtra("move_to_tab", Constants.TABS.TRACKS))
            } else {
                if (adapter!!.itemCount <= 0) {
                    Toast.makeText(this@ActivitySecondaryLibrary,
                        "Empty Track List",
                        Toast.LENGTH_SHORT).show()
                } else {
                    adapter!!.shuffleAll()
                }
            }
        }
        fab!!.backgroundTintList = ColorStateList.valueOf(ColorHelper.getWidgetColor())
        collapsingToolbarLayout!!.statusBarScrim = ColorHelper.getGradientDrawable()
        setTextAndIconColor()
    }

    private fun setTextAndIconColor() {
        songNameMiniPlayer!!.setTextColor(ColorHelper.getPrimaryTextColor())
        artistNameMiniPlayer!!.setTextColor(ColorHelper.getSecondaryTextColor())
        artistBio!!.setTextColor(ColorHelper.getPrimaryTextColor())
        /*buttonPlay.setColorFilter(ColorHelper.getPrimaryTextColor());
        buttonNext.setColorFilter(ColorHelper.getPrimaryTextColor());*/
    }

    private fun setArtistImage(url: String) {
        Glide
            .with(applicationContext)
            .load(url)
            .transition(DrawableTransitionOptions.withCrossFade())
            .placeholder(R.drawable.music)
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .dontAnimate()
            .into(mainBackdrop!!)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    private fun updateMiniplayerUI() {
        try {
            if (playerService != null) {
                if (playerService!!.getCurrentTrack() != null) {
                    val request: RequestBuilder<Drawable> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Glide.with(this)
                            .load(MusicLibrary.instance.getAlbumArtFromTrack(playerService!!.getCurrentTrack()!!.id))
                            .centerCrop()
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                    } else {
                        TODO("VERSION.SDK_INT < Q")
                    }
                    var builder: RequestBuilder<Drawable>? = null
                    val url: String? =
                        MusicLibrary.instance.artistUrls[playerService!!.getCurrentTrack()!!.getArtist()]
                    if (url != null) {
                        when (ApplicationClass.getPref().getInt(getString(R.string.pref_default_album_art), 0)) {
                            0 -> builder = Glide.with(this)
                                .load(Uri.parse(url))
                                .placeholder(R.drawable.music)
                            1 -> builder = Glide.with(this)
                                .load(Uri.parse(url))
                                .placeholder(UtilityFun.defaultAlbumArtDrawable)
                        }
                    }
                    request.error(builder)
                    request.into(albumArtIv!!)

                    //albumArtIv.setImageBitmap(playerService!!.getAlbumArt());
                    if (playerService!!.getStatus() === playerService!!.PLAYING) {
                        buttonPlay!!.setImageDrawable(ContextCompat.getDrawable(this,
                            R.drawable.ic_pause_black_24dp))
                    } else {
                        buttonPlay!!.setImageDrawable(ContextCompat.getDrawable(this,
                            R.drawable.ic_play_arrow_black_24dp))
                    }
                    songNameMiniPlayer!!.text = playerService!!.getCurrentTrack()!!.title
                    artistNameMiniPlayer!!.text = playerService!!.getCurrentTrack()!!.getArtist()
                    (findViewById<View>(R.id.app_bar_layout) as AppBarLayout).setExpanded(true)
                    //mHandler.post(getDominantColorRunnable());
                }
            } else {
                //this should not happen
                //restart app
                exitProcess(0)
            }
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
    }

    override fun onClick(view: View) {
        if (ApplicationClass.Companion.getService() == null) {
            UtilityFun.restartApp()
            finish()
            return
        }
        when (view.id) {
            R.id.mini_player -> {
                val intent = Intent(applicationContext, ActivityNowPlaying::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                val options: ActivityOptions = ActivityOptions.makeSceneTransitionAnimation(this,
                    albumArtIv,
                    getString(R.string.transition))
                ActivityCompat.startActivityForResult(this,
                    intent,
                    RC_LOGIN,
                    options.toBundle())
                Log.v(Constants.TAG, "Launch now playing Jarvis")
            }
            R.id.play_pause_mini_player -> {
                if (playerService!!.getCurrentTrack() == null) {
                    Toast.makeText(this, getString(R.string.nothing_to_play), Toast.LENGTH_LONG)
                        .show()
                    return
                }
                if (SystemClock.elapsedRealtime() - mLastClickTime < 100) {
                    return
                }
                mLastClickTime = SystemClock.elapsedRealtime()
                playerService!!.play()
                playerService!!.PostNotification()
                if (playerService!!.getStatus() === playerService!!.PLAYING) {
                    buttonPlay!!.setImageDrawable(ContextCompat.getDrawable(this,
                        R.drawable.ic_pause_black_24dp))
                } else {
                    buttonPlay!!.setImageDrawable(ContextCompat.getDrawable(this,
                        R.drawable.ic_play_arrow_black_24dp))
                }
            }
            R.id.next_mini_plaayrer -> {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 100) {
                    return
                }
                mLastClickTime = SystemClock.elapsedRealtime()
                playerService!!.nextTrack()
                updateMiniplayerUI()
                /*
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent()
                        .setAction(Constants.ACTION.NEXT_ACTION));*/Log.v(Constants.TAG,
                    "next track please Jarvis")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        for (i in 0 until menu.size()) {
            if ((R.id.action_search == menu.getItem(i).itemId
                        || R.id.action_sort == menu.getItem(i).itemId)
            ) {
                menu.getItem(i).isVisible = false
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_settings -> startActivity(Intent(this,
                ActivitySettings::class.java).putExtra("ad", true))
            R.id.action_sleep_timer -> setSleepTimerDialog(this)
            R.id.action_equ -> {
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                if ((ApplicationClass.getPref().getBoolean(getString(R.string.pref_prefer_system_equ), true)
                            && (intent.resolveActivity(packageManager) != null))
                ) {
                    try {
                        //show system equalizer
                        startActivityForResult(intent, 0)
                    } catch (ignored: Exception) {
                    }
                } else {
                    //show app equalizer
                    if (playerService!!.getEqualizerHelper().isEqualizerSupported()) {
                        startActivity(Intent(this, ActivityEqualizer::class.java))
                    } else {
                        Snackbar.make(rootView!!,
                            R.string.error_equ_not_supported,
                            Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setSleepTimerDialog(context: Context) {
        val builder = MaterialDialog(context)
        val linear = LinearLayout(context)
        linear.orientation = LinearLayout.VERTICAL
        val text = TextView(context)
        val timer = ApplicationClass.getPref().getInt(context.getString(R.string.pref_sleep_timer), 0)
        if (timer == 0) {
            val tempString: String =
                "0 " + context.getString(R.string.main_act_sleep_timer_status_minutes)
            text.text = tempString
        }
        else {
            val stringTemp: String =
                (context.getString(R.string.main_act_sleep_timer_status_part1) +
                        timer +
                        context.getString(R.string.main_act_sleep_timer_status_part2))
            text.text = stringTemp
            builder.neutralButton(R.string.main_act_sleep_timer_neu){
                ApplicationClass.getPref().edit().putInt(context.getString(R.string.pref_sleep_timer), 0).apply()
                playerService!!.setSleepTimer(0, false)
                //Toast.makeText(context, "Sleep timer discarded", Toast.LENGTH_LONG).show();
                Snackbar.make(rootView!!, context.getString(R.string.sleep_timer_discarded), Snackbar.LENGTH_SHORT).show()
            }
        }
        text.setPadding(0, 10, 0, 0)
        text.gravity = Gravity.CENTER
        text.typeface = TypeFaceHelper.getTypeFace(this)
        val seek = SeekBar(context)
        seek.setPadding(40, 10, 40, 10)
        seek.max = 100
        seek.progress = 0
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val tempString = progress.toString() + context.getString(R.string.main_act_sleep_timer_status_minutes)
                text.text = tempString
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        linear.addView(seek)
        linear.addView(text)
        builder
            .title(R.string.main_act_sleep_timer_title)
            .positiveButton(R.string.okay){
                if (seek.progress != 0) {
                    ApplicationClass.getPref().edit()
                        .putInt(context.getString(R.string.pref_sleep_timer),
                            seek.progress).apply()
                    playerService!!.setSleepTimer(seek.progress, true)
                    val temp: String = (context.getString(R.string.sleep_timer_successfully_set)
                            + seek.progress
                            + context.getString(R.string.main_act_sleep_timer_status_minutes))
                    //Toast.makeText(context, temp, Toast.LENGTH_LONG).show();
                    Snackbar.make(rootView!!, temp, Snackbar.LENGTH_SHORT).show()
                }
            }
            .negativeButton(R.string.cancel)
            .customView(view = linear, scrollable = true)
            .show()
    }

    public override fun onDestroy() {
        mRecyclerView = null
        super.onDestroy() //get search icon back on action bar
    }

    public override fun onResume() {
        super.onResume()
        ApplicationClass.isAppVisible = true
        if (ApplicationClass.getService() == null) {
            UtilityFun.restartApp()
            finish()
            return
        } else {
            playerService = ApplicationClass.Companion.getService()
        }
        if (adapter != null) {
            adapter!!.bindService()
        }
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            mReceiverForMiniPLayerUpdate!!,
            IntentFilter(Constants.ACTION.COMPLETE_UI_UPDATE))
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            mReceiverForMiniPLayerUpdate!!,
            IntentFilter(Constants.ACTION.SECONDARY_ADAPTER_DATA_READY))
        updateMiniplayerUI()
    }

    override fun onPause() {
        ApplicationClass.isAppVisible = false
        LocalBroadcastManager.getInstance(applicationContext)
            .unregisterReceiver(mReceiverForMiniPLayerUpdate!!)
        LocalBroadcastManager.getInstance(applicationContext)
            .unregisterReceiver(mReceiverForDataReady!!)
        super.onPause()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_in_right)
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

    override fun onArtInfoDownloaded(artistInfo: ArtistInfo?) {
        if (artistInfo == null) return
        artistBio!!.visibility = View.VISIBLE
        artistBio!!.text = artistInfo.getArtistContent()
        setArtistImage(artistInfo.getImageUrl()!!)
    }

    //for catching exception generated by recycler view which was causing abend, no other way to handle this
    internal inner class WrapContentLinearLayoutManager constructor(context: Context?) :
        LinearLayoutManager(context) {
        //... constructor
        override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
            try {
                super.onLayoutChildren(recycler, state)
            } catch (ignored: IndexOutOfBoundsException) {
            }
        }
    }
}