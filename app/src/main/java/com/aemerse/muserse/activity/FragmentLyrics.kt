package com.aemerse.muserse.activity

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Html
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.adapter.LyricsViewAdapter
import com.aemerse.muserse.databinding.FragmentLyricsBinding
import com.aemerse.muserse.lyricCard.ActivityLyricCard
import com.aemerse.muserse.model.Constants
import com.aemerse.muserse.model.MusicLibrary
import com.aemerse.muserse.model.PlaylistManager
import com.aemerse.muserse.model.TrackItem
import com.aemerse.muserse.qlyrics.lyrics.Lyrics
import com.aemerse.muserse.qlyrics.lyrics.ViewLyrics
import com.aemerse.muserse.qlyrics.offlineStorage.OfflineStorageLyrics
import com.aemerse.muserse.qlyrics.tasks.DownloadLyricThread
import com.aemerse.muserse.service.PlayerService
import com.aemerse.muserse.uiElementHelper.BottomOffsetDecoration
import com.aemerse.muserse.utils.UtilityFun
import com.nshmura.snappysmoothscroller.SnapType
import com.nshmura.snappysmoothscroller.SnappyLayoutManager
import com.nshmura.snappysmoothscroller.SnappyLinearLayoutManager
import java.util.concurrent.Executors

class FragmentLyrics : Fragment(), RecyclerView.OnItemTouchListener, Lyrics.Callback, ActionMode.Callback, View.OnClickListener {
    private var item: TrackItem? = null
    private var mLyricChange: BroadcastReceiver? = null
    private var fIsStaticLyrics: Boolean = true
    private var isLyricsLoaded: Boolean = false
    private var fLyricUpdaterThreadCancelled: Boolean = false
    private var fIsLyricUpdaterThreadRunning: Boolean = false
    private var handler: Handler? = null
    private var adapter: LyricsViewAdapter? = null
    private var layoutManager: LinearLayoutManager? = null
    private var gestureDetector: GestureDetectorCompat? = null
    private var actionMode: ActionMode? = null
    private var actionModeActive: Boolean = false
    var playerService: PlayerService? = null
    private var lyricThread: DownloadLyricThread? = null
    private var _binding: FragmentLyricsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLyricsBinding.inflate(inflater, container, false)

        if (ApplicationClass.getService() == null) {
            UtilityFun.restartApp()
            return binding.root
        }
        playerService = ApplicationClass.getService()
        initializeListeners()
        return binding.root
    }

    private fun initializeListeners() {
        binding.buttonUpdateMetadata.setOnClickListener(this)
        binding.textViewLyricStatus.setOnClickListener(this)
        mLyricChange = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.v(Constants.TAG, "update lyrics please Jarvis")
                updateLyricsIfNeeded()
            }
        }
    }

    private fun updateLyricsIfNeeded() {
        item = playerService!!.getCurrentTrack()
        if (item == null) {
            binding.textViewLyricStatus.text = getString(R.string.no_music_found_lyrics)
            binding.textViewLyricStatus.visibility = View.VISIBLE
            binding.loadingLyricsAnimation.hide()
            return
        }
        if (mLyrics != null) {
            //if lyrics are already displayed for current song, skip this
            if (mLyrics!!.getOriginalTrack().equals(
                    item!!.title)
            ) {
                return
            }
        }
        if (isLyricsLoaded) {
            return
        }
        Log.v(Constants.TAG, "Intent Song playing " + playerService!!.getCurrentTrack()!!.title)
        updateLyrics()
    }

    private fun updateLyrics() {
        //hide edit metadata things
        Log.d("FragmentLyrics", "updateLyrics: ")
        if (!isAdded || activity == null) {
            return
        }
        item = playerService!!.getCurrentTrack()
        binding.trackArtistLyricFrag.visibility = View.GONE
        binding.trackTitleLyricFrag.visibility = View.GONE
        binding.updateTrackMetadata.visibility = View.GONE
        binding.buttonUpdateMetadata.visibility = View.GONE
        binding.buttonUpdateMetadata.isClickable = false

        //set loading animation
        binding.loadingLyricsAnimation.visibility = View.VISIBLE
        binding.loadingLyricsAnimation.show()

        binding.llDynamicLyricView.visibility = View.GONE
        fLyricUpdaterThreadCancelled = true
        binding.textViewLyricStatus.visibility = View.VISIBLE
        binding.textViewLyricStatus.text = getString(R.string.lyrics_loading)
        if (!ApplicationClass.getPref().getBoolean(getString(R.string.pref_disclaimer_accepted), false)) {
            binding.textViewLyricStatus.visibility = View.VISIBLE
            binding.textViewLyricStatus.text = getString(R.string.disclaimer_rejected)
            try {
                //some exceptions reported in play console, thats why
                binding.loadingLyricsAnimation.hide()
            } catch (ignored: Exception) {
            }
            // }
            return
        }
        if ((mLyrics != null) && (mLyrics!!.getFlag() == Lyrics.POSITIVE_RESULT) && (mLyrics!!.getTrackId() != -1) && (mLyrics!!.getTrackId() == item!!.id)) {
            onLyricsDownloaded(mLyrics)
            return
        }
        when {
            item != null -> {
                //check in offline storage
                mLyrics = OfflineStorageLyrics.getLyricsFromDB(item)
                if (mLyrics != null) {
                    onLyricsDownloaded(mLyrics)
                    return
                }
                when {
                    UtilityFun.isConnectedToInternet -> {
                        fetchLyrics((item!!.getArtist())!!, (item!!.title)!!, null)
                    }
                    else -> {
                        binding.textViewLyricStatus.text = getString(R.string.no_connection)
                        binding.loadingLyricsAnimation.hide()
                    }
                }
            }
            else -> {
                binding.textViewLyricStatus.text = getString(R.string.no_music_found_lyrics)
                binding.textViewLyricStatus.visibility = View.VISIBLE
                binding.loadingLyricsAnimation.hide()
            }
        }
    }

    private fun fetchLyrics(vararg params: String?) {
        if (activity == null) return
        val artist = params[0]
        val title = params[1]

        ///filter title string
        //title = filterTitleString(title);
        var url: String? = null
        if (params.size > 2) url = params[2]
        val d = Log.d("Fragment lyrics", "fetchLyrics: download lyric thread starting!")
        lyricThread = when (url) {
            null -> DownloadLyricThread(this, true, item, artist!!, title!!)
            else -> DownloadLyricThread(this, true, item, url, artist!!, title!!)
        }
        lyricThread!!.start()
    }

    override fun onLyricsDownloaded(lyrics: Lyrics?) {
        isLyricsLoaded = true
        //control comes here no matter where lyrics found, in db or online
        //so update the view here
        if ((lyrics == null) || (activity == null) || !isAdded) {
            return
        }

        //before lyrics getting displayed, song has been changed already, display loading lyrics and return,
        //background thread already working to fetch latest lyrics
        //track id is -1 if lyrics are downloaded from internet and have
        //id of track from content resolver if lyrics came from offline storage
        if (lyrics.getTrackId() != -1 && lyrics.getTrackId() != playerService!!.getCurrentTrack()!!.id) {
            return
        }
        binding.loadingLyricsAnimation.hide()
        mLyrics = lyrics
        when (Lyrics.POSITIVE_RESULT) {
            lyrics.getFlag() -> {
                //  lrcView.setVisibility(View.VISIBLE);
                //lrcView.setOriginalLyrics(lyrics);
                //lrcView.setSourceLrc(lyrics.getText());
                //((TextView)layout.findViewById(R.id.textView3)).setVisibility(View.GONE);
                //updateLRC();

                //see if timing information available and update view accordingly
                // if(lyrics.isLRC()){
                binding.textViewLyricStatus.visibility = View.GONE
                fIsStaticLyrics = !mLyrics!!.isLRC()
                fLyricUpdaterThreadCancelled = false
                binding.llDynamicLyricView.visibility = View.VISIBLE
                binding.textViewLyricStatus.visibility = View.GONE
                //lyricCopyRightText.setVisibility(View.VISIBLE);
                initializeLyricsView()
            }
            else -> {
                //in case no lyrics found, set staticLyric flag true as we start lyric thread based on its value
                //and we dont want our thread to run even if no lyrics found
                if (playerService!!.getCurrentTrack() != null) {
                    binding.trackArtistLyricFrag.visibility = View.VISIBLE
                    binding.trackTitleLyricFrag.visibility = View.VISIBLE
                    binding.updateTrackMetadata.visibility = View.VISIBLE
                    binding.buttonUpdateMetadata.visibility = View.VISIBLE
                    binding.buttonUpdateMetadata.isClickable = true
                    binding.trackTitleLyricFrag.setText(item!!.title)
                    binding.trackArtistLyricFrag.setText(item!!.getArtist())
                }
                fIsStaticLyrics = true
                binding.textViewLyricStatus.text = getString(R.string.tap_to_refresh_lyrics)
                binding.textViewLyricStatus.visibility = View.VISIBLE
                //lyricCopyRightText.setVisibility(View.GONE);
            }
        }
    }

    private fun initializeLyricsView() {
        if (mLyrics == null) {
            return
        }
        if (handler == null) {
            handler = Handler(Looper.getMainLooper())
        }
        adapter = LyricsViewAdapter(requireContext(), mLyrics)
        val snappyLinearLayoutManager: SnappyLayoutManager = SnappyLinearLayoutManager(context)
        snappyLinearLayoutManager.setSnapType(SnapType.CENTER)
        snappyLinearLayoutManager.setSnapDuration(1500)
        //layoutManager.setSnapInterpolator(new DecelerateInterpolator());

        // Attach layout manager to the RecyclerView:
        binding.dynamicLyricsRecyclerView.layoutManager = snappyLinearLayoutManager as RecyclerView.LayoutManager?
        val offsetPx: Float = resources.getDimension(R.dimen.bottom_offset_secondary_lib)
        val bottomOffsetDecoration = BottomOffsetDecoration(offsetPx.toInt())
        binding.dynamicLyricsRecyclerView.addItemDecoration(bottomOffsetDecoration)
        binding.dynamicLyricsRecyclerView.setHasFixedSize(true)
        binding.dynamicLyricsRecyclerView.adapter = adapter
        binding.dynamicLyricsRecyclerView.addOnItemTouchListener(this)
        gestureDetector = GestureDetectorCompat(context, RecyclerViewDemoOnGestureListener())
        layoutManager = binding.dynamicLyricsRecyclerView.layoutManager as LinearLayoutManager
        fLyricUpdaterThreadCancelled = false
        if (!fIsStaticLyrics && (playerService!!.getStatus() == playerService!!.PLAYING) && !fIsLyricUpdaterThreadRunning) {
            Executors.newSingleThreadExecutor().execute(lyricUpdater)
            scrollLyricsToCurrentLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (ApplicationClass.getService() == null) {
            UtilityFun.restartApp()
            return
        }

        /* This code together with the one in onDestroy()
         * will make the screen be always on until this Activity gets destroyed. */startLyricUpdater()
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mLyricChange!!, IntentFilter(Constants.ACTION.UPDATE_LYRIC_AND_INFO))
        /*LocalBroadcastManager.getInstance(getContext()).registerReceiver(mPlayPauseUpdateReceiver
                ,new IntentFilter(Constants.ACTION.PLAY_PAUSE_UI_UPDATE));*/
        //UpdateUI();
        updateLyrics()

        /*if(!fSeekbarRunning && playerService!!.getStatus()==playerService!!.PLAYING) {
            fSeekbarThreadCancelled = false;
            Executors.newSingleThreadExecutor().execute(seekbarUpdater);
        }*/
    }

    private fun startLyricUpdater() {
        if (!fIsStaticLyrics && !fIsLyricUpdaterThreadRunning && (playerService!!.getStatus() == playerService!!.PLAYING)) {
            Log.d("FragmentLyrics", "startLyricUpdater: starting lyric updater")
            fLyricUpdaterThreadCancelled = false
            Executors.newSingleThreadExecutor().execute(lyricUpdater)
        }
        try {
            if (!fIsStaticLyrics) scrollLyricsToCurrentLocation()
        } catch (e: Exception) {
            Log.d("FragmentLyrics", "startLyricUpdater: unable to scroll lyrics to latest position")
        }
    }

    private fun scrollLyricsToCurrentLocation() {
        adapter!!.changeCurrent(playerService!!.getCurrentTrackProgress().toLong())
        val index: Int = adapter!!.getCurrentTimeIndex()
        if (index != -1) {
            // without delay lyrics wont scroll to latest position when called from onResume for some reason
            Handler().postDelayed({ binding.dynamicLyricsRecyclerView.smoothScrollToPosition(index) }, 100)
        }
        Log.d("FragmentLyrics", "scrollLyricsToCurrentLocation: index $index")
        adapter!!.notifyDataSetChanged()
    }

    fun smoothScrollAfterSeekbarTouched(progress: Int) {
        if (adapter != null && !fIsStaticLyrics) {
            adapter!!.changeCurrent(UtilityFun.progressToTimer(progress, playerService!!.getCurrentTrackDuration()).toLong())
            val index: Int = adapter!!.getCurrentTimeIndex()
            if (index != -1) {
                binding.dynamicLyricsRecyclerView.smoothScrollToPosition(index)
                adapter!!.notifyDataSetChanged()
            }
            Log.d("FragmentLyrics", "scrollLyricsToCurrentLocation: index $index")
        }
    }

    override fun onPause() {
        Log.d("FragmentLyrics", "onPause: stopping lyric updater threads")
        if (actionMode != null) {
            actionMode!!.finish()
            actionMode = null
        }
        stopLyricUpdater()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mLyricChange!!)
        //LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mPlayPauseUpdateReceiver);

        //fSeekbarThreadCancelled = true;
        super.onPause()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (playerService == null) playerService = ApplicationClass.getService()
        if (isVisibleToUser) {
            startLyricUpdater()
        } else {
            stopLyricUpdater()
        }
    }

    private fun stopLyricUpdater() {
        if (fIsLyricUpdaterThreadRunning) {
            fLyricUpdaterThreadCancelled = true
            fIsLyricUpdaterThreadRunning = false
        }
    }

    fun runLyricThread() {
        isLyricsLoaded = false
        if (!fIsStaticLyrics && !fIsLyricUpdaterThreadRunning && (playerService!!.getStatus() == playerService!!.PLAYING)) {
            fLyricUpdaterThreadCancelled = false
            Executors.newSingleThreadExecutor().execute(lyricUpdater)
        } else {
            fLyricUpdaterThreadCancelled = true
        }
    }

    fun clearLyrics() {
        if (playerService == null) return
        if (playerService!!.getCurrentTrack() != null) {
            try {
                binding.llDynamicLyricView.visibility = View.GONE
                fIsStaticLyrics = true
                binding.textViewLyricStatus.text = getString(R.string.tap_to_refresh_lyrics)
                binding.textViewLyricStatus.visibility = View.VISIBLE
                binding.buttonUpdateMetadata.visibility = View.VISIBLE
                binding.buttonUpdateMetadata.isClickable = true
                binding.trackTitleLyricFrag.setText(item!!.title)
                binding.trackArtistLyricFrag.setText(item!!.getArtist())
                binding.trackArtistLyricFrag.visibility = View.VISIBLE
                binding.trackTitleLyricFrag.visibility = View.VISIBLE
                binding.updateTrackMetadata.visibility = View.VISIBLE
            } catch (ignored: Exception) {
            }
        }
    }

    //when clicked on this, lyrics are searched again from viewlyrics
    //but this time option is given to select lyrics
    fun wrongLyrics() {
        if (mLyrics == null || mLyrics!!.getFlag() != Lyrics.POSITIVE_RESULT) {
            if (isAdded && activity != null) Toast.makeText(activity,
                getString(R.string.error_no_lyrics),
                Toast.LENGTH_SHORT).show()
            return
        }
        if (mLyrics!!.getSource() == null || (!mLyrics!!.getSource().equals(ViewLyrics.clientUserAgent) && !mLyrics!!.getSource().equals("manual"))) {
            if (isAdded && activity != null) Toast.makeText(activity,
                "No lyrics from other sources available!",
                Toast.LENGTH_SHORT).show()
            return
        }
        item = playerService!!.getCurrentTrack()

        ///filter title string
        val title: String? = item!!.title
        val artist: String? = item!!.getArtist()
        Executors.newSingleThreadExecutor().execute {
            if (artist != null && title != null) {
                try {
                    ViewLyrics.fromMetaData(activity,
                        artist,
                        title,
                        item,
                        this@FragmentLyrics)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun shareLyrics() {
        if ((mLyrics == null) || (mLyrics!!.getFlag() != Lyrics.POSITIVE_RESULT) || (adapter == null)) {
            if (activity != null && isAdded) {
                Toast.makeText(activity,
                    getString(R.string.error_no_lyrics),
                    Toast.LENGTH_SHORT).show()
            }
            return
        }
        var shareBody: String = getString(R.string.lyrics_share_text)
        shareBody += "\n\nTrack : " + mLyrics!!.getTrack()
            .toString() + "\n".toString() + "Artist : " + mLyrics!!.getArtist().toString() + "\n\n"
        if (mLyrics!!.isLRC()) {
            shareBody += Html.fromHtml(adapter!!.getStaticLyrics()).toString()
        } else {
            shareBody += Html.fromHtml(mLyrics!!.getText())
        }
        shareTextIntent(shareBody)
    }

    private fun shareTextIntent(shareBody: String) {
        val sharingIntent: Intent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Lyrics")
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        if (isAdded) {
            startActivity(Intent.createChooser(sharingIntent, "Lyrics share!"))
        } else {
            Toast.makeText(activity,
                getString(R.string.error_sharing_lyrics),
                Toast.LENGTH_SHORT).show()
        }
    }

    fun disclaimerAccepted() {
        updateLyrics()
    }

    override fun onDestroy() {
        fLyricUpdaterThreadCancelled = true
        if (lyricThread != null) lyricThread!!.setCallback(null)
        super.onDestroy()
    }

    override fun onDestroyView() {
        fLyricUpdaterThreadCancelled = true
        super.onDestroyView()
    }

    private fun myToggleSelection(idx: Int) {
        adapter!!.toggleSelection(idx)
        if (adapter!!.getSelectedItemCount() == 0) {
            actionMode!!.finish()
            actionMode = null
            return
        }
        val numberOfItems: Int = adapter!!.getSelectedItemCount()
        val selectionString: String = when (numberOfItems) {
            1 -> " item selected"
            else -> " items selected"
        }
        val title: String = numberOfItems.toString() + selectionString
        actionMode!!.title = title
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        gestureDetector!!.onTouchEvent(e)
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        val inflater: MenuInflater = actionMode.menuInflater
        inflater.inflate(R.menu.menu_cab_recyclerview_lyrics, menu)
        return true
    }

    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_share -> try {
                shareTextIntent(getSelectedLyricString().toString())
                actionMode.finish()
                actionModeActive = false
            } catch (e: IndexOutOfBoundsException) {
                actionMode.finish()
                actionModeActive = false
                Toast.makeText(activity,
                    "Invalid selection, please try again",
                    Toast.LENGTH_SHORT).show()
            }
            R.id.menu_lyric_card -> {
                val intent = Intent(activity, ActivityLyricCard::class.java)
                intent.putExtra("lyric", getSelectedLyricString().toString())
                    .putExtra("artist", mLyrics!!.getArtist())
                    .putExtra("track", mLyrics!!.getTrack())
                startActivity(intent)
            }
        }
        return false
    }

    private fun getSelectedLyricString(): StringBuilder {
        val shareString: StringBuilder = StringBuilder()
        val selectedItemPositions: List<Int> = adapter!!.getSelectedItems()
        var currPos: Int
        for (i in selectedItemPositions.indices) {
            currPos = selectedItemPositions.get(i)
            val lyricLine = adapter!!.getLineAtPosition(currPos)
            shareString.append(lyricLine).append("\n")
        }
        return shareString
    }

    override fun onDestroyActionMode(actionMode: ActionMode) {
        actionMode.finish()
        actionModeActive = false
        adapter!!.clearSelections()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.lyrics_line -> {
                val idx: Int = binding.dynamicLyricsRecyclerView.getChildLayoutPosition(view)
                if (actionModeActive) {
                    myToggleSelection(idx)
                    return
                }
            }
            R.id.text_view_lyric_status -> {
                binding.textViewLyricStatus.text = getString(R.string.lyrics_loading)
                updateLyrics()
            }
            R.id.button_update_metadata -> {
                item = playerService!!.getCurrentTrack()
                if (item == null) {
                    return
                }
                val edited_title = binding.trackTitleLyricFrag.text.toString()
                val edited_artist = binding.trackArtistLyricFrag.text.toString()
                if (edited_title.isEmpty() || edited_artist.isEmpty()) {
                    Toast.makeText(context,
                        getString(R.string.te_error_empty_field),
                        Toast.LENGTH_SHORT).show()
                    return
                }
                if (edited_title != item!!.title ||
                    edited_artist != item!!.getArtist()
                ) {

                    //changes made, save those
                    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    val values = ContentValues()
                    values.put(MediaStore.Audio.Media.TITLE, edited_title)
                    values.put(MediaStore.Audio.Media.ARTIST, edited_artist)
                    requireContext().contentResolver
                        .update(uri,
                            values,
                            MediaStore.Audio.Media.TITLE + "=?",
                            arrayOf(item!!.title))
                    val d = MusicLibrary.instance.updateTrackNew(item!!.id, edited_title, edited_artist, item!!.album!!)
                    PlaylistManager.getInstance(ApplicationClass.getContext())!!.addEntryToMusicTable(d!!)
                    val intent = Intent(context, ActivityNowPlaying::class.java)
                    intent.putExtra("refresh", true)
                    intent.putExtra("position", playerService!!.getCurrentTrackPosition())
                    intent.putExtra("originalTitle", item!!.title)
                    intent.putExtra("title", edited_title)
                    intent.putExtra("artist", edited_artist)
                    intent.putExtra("album", item!!.album)
                    startActivity(intent)
                    binding.trackArtistLyricFrag.visibility = View.GONE
                    binding.trackTitleLyricFrag.visibility = View.GONE
                    binding.updateTrackMetadata.visibility = View.GONE
                    binding.buttonUpdateMetadata.visibility = View.GONE
                    binding.buttonUpdateMetadata.isClickable = false
                    if (activity != null) {
                        if (requireActivity().currentFocus != null) {
                            (requireActivity()
                                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)?.hideSoftInputFromWindow(
                                view.windowToken,
                                0)
                        }
                    }
                } else {
                    Toast.makeText(context,
                        getString(R.string.change_tags_to_update),
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private inner class RecyclerViewDemoOnGestureListener :
        GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val view = binding.dynamicLyricsRecyclerView.findChildViewUnder(e.x, e.y)
            if (view != null) {
                onClick(view)
            }
            return super.onSingleTapConfirmed(e)
        }

        override fun onLongPress(e: MotionEvent) {
            if (!isAdded || activity == null) return
            val view = binding.dynamicLyricsRecyclerView.findChildViewUnder(e.x, e.y)
            if (actionModeActive) {
                return
            }
            // Start the CAB using the ActionMode.Callback defined above
            actionMode = activity!!.startActionMode(this@FragmentLyrics)
            actionModeActive = true
            val idx = binding.dynamicLyricsRecyclerView.getChildPosition(view!!)
            myToggleSelection(idx)
            super.onLongPress(e)
        }
    }

    private val lyricUpdater: Runnable = Runnable {
        while (true) {
            if (fLyricUpdaterThreadCancelled) {
                break
            }
            fIsLyricUpdaterThreadRunning = true
            //Log.v("FragmentLyrics","Lyric thread running");
            if (activity != null) {
                handler!!.post {
                    val index: Int = adapter!!.changeCurrent(playerService!!.getCurrentTrackProgress().toLong())
                    val firstVisibleItem: Int = layoutManager!!.findFirstVisibleItemPosition()
                    val lastVisibleItem: Int = layoutManager!!.findLastVisibleItemPosition()
                    if ((index != -1) && (index > firstVisibleItem) && (index < lastVisibleItem)) {
                        binding.dynamicLyricsRecyclerView.smoothScrollToPosition(index)
                    }
                }
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        fIsLyricUpdaterThreadRunning = false
        Log.v("FragmentLyrics", "Lyric thread stopped")
    }

    companion object {
        private var mLyrics: Lyrics? = null
    }
}