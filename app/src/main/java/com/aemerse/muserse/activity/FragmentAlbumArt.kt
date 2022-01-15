package com.aemerse.muserse.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.model.Constants
import com.aemerse.muserse.model.MusicLibrary
import com.aemerse.muserse.service.PlayerService
import com.aemerse.muserse.utils.UtilityFun

class FragmentAlbumArt : Fragment() {
    private var playerService: PlayerService? = null
    private var mUIUpdate: BroadcastReceiver? = null

    private lateinit var albumArt: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout: View = inflater.inflate(R.layout.fragment_album_art, container, false)
        albumArt = layout.findViewById(R.id.album_art_now_playing)
        playerService = ApplicationClass.getService()
        mUIUpdate = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.v(Constants.TAG, "update disc please Jarvis")
                updateUI()
            }
        }
        return layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isAdded && activity != null) {
            //exit animation
            requireActivity().startPostponedEnterTransition()

            //place album art view properly in center
            albumArt.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        albumArt.viewTreeObserver.removeOnGlobalLayoutListener(this)

                        //y position of control buttons
                        val yControl: Float = (activity as ActivityNowPlaying?)!!.yControl

                        //height of toolbar
                        val toolbarHeight: Float =
                            (activity as ActivityNowPlaying?)!!.toolbarHeight
                        if (toolbarHeight != 0f || yControl != 0f) {
                            //centre the album art
                            albumArt.y = ((yControl - toolbarHeight) / 2) - albumArt.height / 2
                        }
                    }
                })
        }
    }

    override fun onPause() {
        Log.v(Constants.TAG, "Disc paused........")
        if (context != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mUIUpdate!!)
        }
        super.onPause()
    }

    override fun onResume() {
        Log.v(Constants.TAG, "Disc resumed........")
        if (context != null) {
            LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(mUIUpdate!!, IntentFilter(Constants.ACTION.COMPLETE_UI_UPDATE))
        }
        updateUI()
        super.onResume()
    }

    private fun updateUI() {
        if ((activity == null) || !isAdded || (playerService!!.getCurrentTrack() == null)) {
            return
        }
        val currentNowPlayingBackPref: Int = ApplicationClass.getPref()
            .getInt(getString(R.string.pref_now_playing_back), 1)
        //if album art selected, hide small album art
        when (currentNowPlayingBackPref) {
            2 -> {
                albumArt.setImageBitmap(null)
            }
            else -> {
                val request: RequestBuilder<Drawable> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Glide.with(this)
                        .load(MusicLibrary.instance.getAlbumArtFromTrack(playerService!!.getCurrentTrack()!!.id))
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                } else {
                    TODO("VERSION.SDK_INT < Q")
                }
                when (ApplicationClass.getPref().getInt(getString(R.string.pref_default_album_art), 0)) {
                    0 -> request.listener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable?>?, isFirstResource: Boolean): Boolean {
                            //Log.d("AlbumLibraryAdapter", "onException: ");
                            if (UtilityFun.isConnectedToInternet && !ApplicationClass.getPref().getBoolean(getString(R.string.pref_data_saver), false)) {
                                val url = MusicLibrary.instance.artistUrls[playerService!!.getCurrentTrack()!!.getArtist()]
//                                if (url != null && url.isNotEmpty()) albumArt?.let {
//                                    request.load(Uri.parse(url))
//                                        .into(it)
//                                }
                                return true
                            }
                            return false
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable?>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            return false
                        }
                    }).placeholder(R.drawable.music)
                    1 -> request.listener(object : RequestListener<Drawable?> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable?>?, isFirstResource: Boolean): Boolean {
                            if (UtilityFun.isConnectedToInternet && !ApplicationClass.getPref().getBoolean(getString(R.string.pref_data_saver), false)) {
                                val url: String? = MusicLibrary.instance.artistUrls[playerService!!.getCurrentTrack()!!.getArtist()]
                                if (url != null && url.isNotEmpty()) request.load(Uri.parse(url))
                                    .into(albumArt)
                                return true
                            }
                            return false
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable?>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            return false
                        }
                    }).placeholder(UtilityFun.defaultAlbumArtDrawable)
                }
                albumArt.let { request.into(it) }
            }
        }
    }
}