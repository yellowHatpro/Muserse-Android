package com.aemerse.muserse.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.aemerse.muserse.R
import com.aemerse.muserse.activity.ActivityLyricView
import com.aemerse.muserse.lyricsExplore.Track
import com.aemerse.muserse.utils.UtilityFun

class TopTracksAdapter(private val context: Context, private val trackList: List<Track>) : RecyclerView.Adapter<TopTracksAdapter.MyViewHolder?>(), PopupMenu.OnMenuItemClickListener {

    private val inflater = LayoutInflater.from(context)
    private var clickedPosition = 0

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View = inflater.inflate(R.layout.track_item_square_image, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val trackInfo = trackList[position].artist + " - " + trackList[position].title
        val playcount = "Playcount - " + trackList[position].playCount
        holder.trackName!!.text = trackInfo
        holder.playCount!!.text = playcount
    }

    override fun getItemCount(): Int {
        return trackList.size
    }

    private fun onClick(v: View, position: Int) {
        /**/
        clickedPosition = position
        when (v.id) {
            R.id.more -> {
                val popup = PopupMenu(context, v)
                val inflater = popup.menuInflater
                inflater.inflate(R.menu.menu_explore_lyric_item, popup.menu)
                popup.show()
                popup.setOnMenuItemClickListener(this)
            }
            R.id.root_view_item_explore_lyrics -> {
                val intent = Intent(context, ActivityLyricView::class.java)
                intent.putExtra("track_title", trackList[position].title)
                intent.putExtra("artist", trackList[position].artist)
                context.startActivity(intent)
                //if (context instanceof ActivityExploreLyrics) {
                (context as Activity).overridePendingTransition(R.anim.slide_in_right,
                    R.anim.slide_out_left)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search_youtube -> UtilityFun.launchYoutube(context,
                trackList[clickedPosition].artist + " - " + trackList[clickedPosition].title)
        }
        return true
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        @JvmField @BindView(R.id.trackInfo)
        var trackName: TextView? = null

        @JvmField @BindView(R.id.playCount)
        var playCount: TextView? = null

        @JvmField @BindView(R.id.imageView)
        var imageView: ImageView? = null

        @JvmField @BindView(R.id.more)
        var overflow: ImageView? = null
        override fun onClick(v: View) {
            this@TopTracksAdapter.onClick(v, layoutPosition)
        }

        init {
            ButterKnife.bind(this, itemView)
            itemView.setOnClickListener(this)
            overflow!!.setOnClickListener(this)
        }
    }

    init {
        setHasStableIds(true)
    }
}