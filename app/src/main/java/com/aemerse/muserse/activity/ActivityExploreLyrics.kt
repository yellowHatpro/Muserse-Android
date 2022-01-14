package com.aemerse.muserse.activity

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.adapter.TopTracksAdapter
import com.aemerse.muserse.databinding.ActivityTrackInfoBinding
import com.aemerse.muserse.lyricsExplore.OnPopularTracksReady
import com.aemerse.muserse.lyricsExplore.PopularTrackRepo
import com.aemerse.muserse.lyricsExplore.Track
import com.aemerse.muserse.model.Constants
import com.aemerse.muserse.uiElementHelper.ColorHelper
import com.aemerse.muserse.utils.UtilityFun
import io.github.inflationx.viewpump.ViewPumpContextWrapper

class ActivityExploreLyrics : AppCompatActivity(), OnPopularTracksReady,
    View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {
    @JvmField @BindView(R.id.root_view_lyrics_explore)
    var rootView: View? = null

    @JvmField @BindView(R.id.recyclerView)
    var recyclerView: RecyclerView? = null

    @JvmField @BindView(R.id.recycler_view_wrapper)
    var rvWrapper: View? = null

    @JvmField @BindView(R.id.progressBar)
    var progressBar: ProgressBar? = null

    @JvmField @BindView(R.id.statusTextView)
    var statusText: TextView? = null

    @JvmField @BindView(R.id.swipeRefreshLayout)
    var swipeRefreshLayout: SwipeRefreshLayout? = null

    @JvmField @BindView(R.id.fab_right_side)
    var fab: FloatingActionButton? = null

    @JvmField @BindView(R.id.trending_now_text)
    var trendingNow: TextView? = null
    private var handler: Handler? = null

    private lateinit var binding: ActivityTrackInfoBinding

    private lateinit var artist: EditText
    private lateinit var trackTitle: EditText

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, ActivityMain::class.java))
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_in_right)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackInfoBinding.inflate(layoutInflater)

        ColorHelper.setStatusBarGradiant(this)
        when (ApplicationClass.getPref().getInt(getString(R.string.pref_theme), Constants.PRIMARY_COLOR.LIGHT)) {
            Constants.PRIMARY_COLOR.DARK -> setTheme(R.style.AppThemeDark)
            Constants.PRIMARY_COLOR.GLOSSY -> setTheme(R.style.AppThemeDark)
            Constants.PRIMARY_COLOR.LIGHT -> setTheme(R.style.AppThemeLight)
        }
        setContentView(R.layout.activity_lyrics_explore)
        ButterKnife.bind(this)
        growShrinkAnimate()
        handler = Handler(Looper.getMainLooper())
        val toolbar: Toolbar = findViewById(R.id.toolbar_)
        toolbar.setTitle(R.string.lyrics_explore)
        setSupportActionBar(toolbar)

        // add back ar
        // row to toolbar
        if (supportActionBar != null) {
            //getSupportActionBar().setBackgroundDrawable(ColorHelper.GetGradientDrawableToolbar());
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }

        //rootView.setBackgroundDrawable(ColorHelper.GetGradientDrawableDark());
        swipeRefreshLayout!!.setOnRefreshListener(this)

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ColorHelper.GetStatusBarColor());
        }*/
        fab!!.backgroundTintList = ColorStateList.valueOf(ColorHelper.getWidgetColor())
        fab!!.setOnClickListener(this)
    }

    private fun growShrinkAnimate() {
        val growAnim: ScaleAnimation = ScaleAnimation(1.0f,
            1.15f,
            1.0f,
            1.15f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f)
        val shrinkAnim: ScaleAnimation = ScaleAnimation(1.15f,
            1.0f,
            1.15f,
            1.0f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f)
        growAnim.duration = 500
        shrinkAnim.duration = 500
        fab!!.animation = growAnim
        growAnim.start()
        growAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                fab!!.animation = shrinkAnim
                shrinkAnim.start()
            }
        })
        shrinkAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                fab!!.animation = growAnim
                growAnim.start()
            }
        })
    }

    private fun loadPopularTracks(lookInCache: Boolean) {
        val country =
            ApplicationClass.getPref().getString(getString(R.string.pref_user_country), "")!!
        PopularTrackRepo().fetchPopularTracks(country, this, lookInCache)
    }

    @OnClick(R.id.statusTextView)
    fun retryLoading() {
        progressBar!!.visibility = View.VISIBLE
        statusText!!.visibility = View.GONE
        handler!!.postDelayed({ loadPopularTracks(false) }, 1000)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("ActivityExploreLyrics", "onNewIntent: ")
    }

    override fun popularTracksReady(tracks: List<Track>?, region: String) {
        handler!!.post {
            swipeRefreshLayout!!.isRefreshing = false
            progressBar!!.visibility = View.GONE
            if (tracks!!.isEmpty()) {
                statusText!!.setText(R.string.error_fetching_popular_tracks)
                statusText!!.visibility = View.VISIBLE
            } else {
                val adapter = TopTracksAdapter(this@ActivityExploreLyrics, tracks)
                recyclerView!!.adapter = adapter
                recyclerView!!.layoutManager = WrapContentLinearLayoutManager(this@ActivityExploreLyrics)
                rvWrapper!!.visibility = View.VISIBLE
                trendingNow!!.text = getString(R.string.trending_now_in, region)
            }
        }
    }

    override fun error() {
        handler!!.post {
            progressBar!!.visibility = View.GONE
            statusText!!.setText(R.string.error_fetching_popular_tracks)
            statusText!!.visibility = View.VISIBLE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }

    override fun onResume() {
        super.onResume()
        ApplicationClass.Companion.isAppVisible = true
        if (intent.extras != null && intent.extras!!
                .getBoolean("fresh_load", false)
        ) {
            loadPopularTracks(false)
        } else {
            loadPopularTracks(true)
        }
        if (intent.extras != null && intent.extras!!
                .getBoolean("search_on_launch", false)
        ) {
            Log.d("ActivityExploreLyrics", "onCreate: search lyric dialog on startup")
            searchLyricDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        ApplicationClass.Companion.isAppVisible = false
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.fab_right_side -> searchLyricDialog()
        }
    }

    private fun searchLyricDialog() {
        val builder = MaterialDialog(this)
            .title(R.string.title_search_lyrics)
            .customView(R.layout.lyric_search_dialog, scrollable = true)
            .positiveButton(R.string.pos_search_lyric){
                if ((trackTitle.text.toString() == "")) {
                    trackTitle.error = getString(R.string.error_empty_title_lyric_search)
                    return@positiveButton
                }
                var artistName: String = artist.text.toString()
                if ((artistName == "")) {
                    artistName = getString(R.string.unknown_artist)
                }
                progressBar!!.visibility = View.VISIBLE
                val finalArtistName: String = artistName
                handler!!.postDelayed({
                    val intent: Intent =
                        Intent(this@ActivityExploreLyrics, ActivityLyricView::class.java)
                    intent.putExtra("track_title", trackTitle.text.toString())
                    intent.putExtra("artist", finalArtistName)
                    startActivity(intent)
                }, 1000)
            }
            .negativeButton(R.string.cancel)

        val layout: View = builder.getCustomView()
        trackTitle = layout.findViewById(R.id.track_title_edit)
        artist = layout.findViewById(R.id.artist_edit)
        progressBar = layout.findViewById(R.id.progressBar)
        handler!!.postDelayed({
            binding.trackTitle.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_DOWN,
                0f,
                0f,
                0))
            binding.trackTitle.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                MotionEvent.ACTION_UP,
                0f,
                0f,
                0))
        }, 200)
        builder.show()
    }

    override fun onRefresh() {
        if (!UtilityFun.isConnectedToInternet) {
            Toast.makeText(this, "No Connection!", Toast.LENGTH_SHORT).show()
            swipeRefreshLayout!!.isRefreshing = false
            return
        }
        rvWrapper!!.visibility = View.GONE
        progressBar!!.visibility = View.VISIBLE
        statusText!!.visibility = View.GONE
        loadPopularTracks(false)
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