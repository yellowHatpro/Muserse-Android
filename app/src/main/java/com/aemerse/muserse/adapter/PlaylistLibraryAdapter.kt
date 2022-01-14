package com.aemerse.muserse.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.uiElementHelper.ColorHelper
import com.aemerse.muserse.activity.ActivitySecondaryLibrary
import com.aemerse.muserse.model.Constants
import com.aemerse.muserse.model.MusicLibrary
import com.aemerse.muserse.model.PlaylistManager
import com.aemerse.muserse.model.dataItem
import com.aemerse.muserse.service.PlayerService
import java.io.File

class PlaylistLibraryAdapter constructor(private val context: Context) : RecyclerView.Adapter<PlaylistLibraryAdapter.MyViewHolder?>(), PopupMenu.OnMenuItemClickListener {
    private var headers: ArrayList<String>
    private val inflater: LayoutInflater
    private var position: Int = 0
    private val playerService: PlayerService
    private var viewParent: View? = null

    fun clear() {}

    fun refreshPlaylistList() {
        headers = PlaylistManager.getInstance(ApplicationClass.getContext())!!.systemPlaylistsList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View = inflater.inflate(R.layout.fragment_playlist_item, parent, false)
        viewParent = parent
        val holder = MyViewHolder(view)
        //int color = ColorHelper.getBaseThemeTextColor() ;
        ((view.findViewById<View>(R.id.header)) as TextView).setTextColor(ColorHelper.getPrimaryTextColor())
        ((view.findViewById<View>(R.id.secondaryHeader)) as TextView).setTextColor(ColorHelper.getSecondaryTextColor())
        ((view.findViewById<View>(R.id.count)) as TextView).setTextColor(ColorHelper.getSecondaryTextColor())
        ((view.findViewById<View>(R.id.menuPopup)) as ImageView).setColorFilter(ColorHelper.getSecondaryTextColor())
        return holder
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.title.text = headers[position]
        holder.title.setPadding(20, 0, 0, 0)
        val count: Long = PlaylistManager.getInstance(ApplicationClass.Companion.getContext())!!.getTrackCountFromCache(headers.get(position))
        if (count != 0L) {
            holder.count.text = context.getString(R.string.track_count, count.toString())
        } else {
            holder.count.text = context.getString(R.string.empty_playlist)
        }
        holder.count.setPadding(20, 0, 0, 0)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_play -> {
                if (ApplicationClass.Companion.isLocked()) {
                    //Toast.makeText(context,"Music is Locked!",Toast.LENGTH_SHORT).show();
                    Snackbar.make(viewParent!!,
                        context.getString(R.string.music_is_locked),
                        Snackbar.LENGTH_SHORT).show()
                    return true
                }
                Play()
            }
            R.id.action_share -> share()
            R.id.action_delete -> delete()
            R.id.action_play_next -> AddToQ(Constants.ADD_TO_Q.IMMEDIATE_NEXT)
            R.id.action_add_to_q -> AddToQ(Constants.ADD_TO_Q.AT_LAST)
            R.id.action_clear_playlist -> when {
                PlaylistManager.getInstance(ApplicationClass.getContext())!!.clearPlaylist(headers[position]) -> {
                    Snackbar.make(viewParent!!,
                        context.getString(R.string.snack_cleared) + " " + headers[position],
                        Snackbar.LENGTH_SHORT).show()
                }
                else -> {
                    Snackbar.make(viewParent!!,
                        context.getString(R.string.snack_unable_to_Clear) + " " + headers.get(position),
                        Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    private fun Play() {
        val temp: ArrayList<dataItem> = PlaylistManager.getInstance(ApplicationClass.getContext())!!.getPlaylist(headers.get(position))
        val trackList: ArrayList<Int> = ArrayList()
        for (d: dataItem in temp) {
            trackList.add(d.id)
        }
        if (trackList.isNotEmpty()) {
            playerService.setTrackList(trackList)
            playerService.playAtPosition(0)
            /*
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent()
                    .setAction(Constants.ACTION.PLAY_AT_POSITION)
                    .putExtra("position",0));*/
        } else {
            //Toast.makeText(context,"empty playlist",Toast.LENGTH_SHORT).show();
            Snackbar.make(viewParent!!, context.getString(R.string.empty_play_list), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun share() {
        val files: ArrayList<Uri> = ArrayList() //for sending multiple files
        val temp: ArrayList<dataItem> = PlaylistManager.getInstance(ApplicationClass.getContext())!!.getPlaylist(headers[position])
        val trackList: ArrayList<Int> = ArrayList()
        for (d: dataItem in temp) {
            trackList.add(d.id)
        }
        for (id: Int in trackList) {
            try {
                val file = File(MusicLibrary.instance.getTrackItemFromId(id)!!.getFilePath())
                val fileUri: Uri = FileProvider.getUriForFile(context,
                    context.applicationContext
                        .packageName + "com.aemerse.music.provider",
                    file)
                files.add(fileUri)
            } catch (e: Exception) {
                //Toast.makeText(context,"Something wrong!",Toast.LENGTH_LONG).show();
                Snackbar.make(viewParent!!,
                    context.getString(R.string.error_something_wrong),
                    Snackbar.LENGTH_SHORT).show()
                return
            }
        }
        when {
            files.isNotEmpty() -> {
                val intent = Intent()
                intent.action = Intent.ACTION_SEND_MULTIPLE
                intent.type = "*/*"
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)
                context.startActivity(Intent.createChooser(intent, "multiple audio files"))
            }
            else -> {
                //Toast.makeText(context,"empty playlist",Toast.LENGTH_SHORT).show();
                Snackbar.make(viewParent!!,
                    context.getString(R.string.empty_play_list),
                    Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun AddToQ(positionToAdd: Int) {
        //we are using same function for adding to q and playing next
        // toastString is to identify which string to disokay as toast
        val toastString: String =
            (if (positionToAdd == Constants.ADD_TO_Q.AT_LAST) context.getString(R.string.added_to_q) else context.getString(
                R.string.playing_next))
        //when adding to playing next, order of songs should be desc
        //and asc for adding at last
        //this is how the function in player service is writte, deal with it
        val sortOrder: Int =
            (if (positionToAdd == Constants.ADD_TO_Q.AT_LAST) Constants.SORT_ORDER.ASC else Constants.SORT_ORDER.DESC)
        val temp: ArrayList<dataItem> =
            PlaylistManager.getInstance(context)!!.getPlaylist(headers.get(position))
        val trackList: ArrayList<Int> = ArrayList()
        for (d: dataItem in temp) {
            trackList.add(d.id)
        }
        if (trackList.isNotEmpty()) {
            for (id: Int in trackList) {
                playerService.addToQ(id, positionToAdd)
            }
            //to update the to be next field in notification
            ApplicationClass.getService()!!.PostNotification()

            /*Toast.makeText(context
                    , toastString + headers.get(position)
                    , Toast.LENGTH_SHORT).show();*/Snackbar.make(viewParent!!,
                toastString + headers[position],
                Snackbar.LENGTH_SHORT).show()
        } else {
            //Toast.makeText(context,"empty playlist",Toast.LENGTH_SHORT).show();
            Snackbar.make(viewParent!!,
                context.getString(R.string.empty_play_list),
                Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun delete() {
        MaterialDialog(context)
            .title(R.string.are_u_sure)
            .positiveButton(R.string.yes){
                if (((headers[position] == Constants.SYSTEM_PLAYLISTS.RECENTLY_ADDED) || (headers[position] == Constants.SYSTEM_PLAYLISTS.RECENTLY_PLAYED) || (headers[position] == Constants.SYSTEM_PLAYLISTS.MOST_PLAYED) || (headers[position] == Constants.SYSTEM_PLAYLISTS.MY_FAV))) {
                    //Toast.makeText(context,"Cannot delete "+headers.get(position),Toast.LENGTH_SHORT).show();
                    Snackbar.make(viewParent!!, context.getString(R.string.cannot_del) + headers.get(position), Snackbar.LENGTH_SHORT).show()
                    return@positiveButton
                }
                when {
                    PlaylistManager.getInstance(ApplicationClass.getContext())?.deletePlaylist(headers.get(position)) == true -> {
                        //Toast.makeText(context,"Deleted "+headers.get(position),Toast.LENGTH_SHORT).show();
                        Snackbar.make(viewParent!!, context.getString(R.string.deleted) + headers[position], Snackbar.LENGTH_SHORT).show()
                        headers.remove(headers[position])
                        notifyDataSetChanged()
                    }
                    else -> {
                        //Toast.makeText(context,"Cannot delete "+headers.get(position),Toast.LENGTH_SHORT).show();
                        Snackbar.make(viewParent!!,
                            context.getString(R.string.cannot_del) + headers[position],
                            Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            .negativeButton(R.string.no)
            .show()
    }

    override fun getItemCount(): Int {
        return headers.size
    }

    fun onClick(view: View, position: Int) {
        this.position = position
        when (view.id) {
            R.id.libraryItem -> {
                val intent: Intent = Intent(context, ActivitySecondaryLibrary::class.java)
                intent.putExtra("status", Constants.FRAGMENT_STATUS.PLAYLIST_FRAGMENT)
                intent.putExtra("title", headers.get(position).trim { it <= ' ' })
                context.startActivity(intent)
                (context as Activity).overridePendingTransition(R.anim.slide_in_right,
                    R.anim.slide_out_left)
            }
            R.id.menuPopup -> {
                val popup = PopupMenu(context, view)
                val inflater: MenuInflater = popup.menuInflater
                inflater.inflate(R.menu.system_playlist_menu, popup.menu)
                popup.show()
                popup.setOnMenuItemClickListener(this)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class MyViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var title: TextView = itemView.findViewById<TextView>(R.id.header)
        var count: TextView = itemView.findViewById<TextView>(R.id.secondaryHeader)
        override fun onClick(view: View) {
            this@PlaylistLibraryAdapter.onClick(view, layoutPosition)
        }

        init {
            itemView.findViewById<View>(R.id.album_art_wrapper).visibility = View.GONE
            itemView.setOnClickListener(this)
            itemView.findViewById<View>(R.id.menuPopup).setOnClickListener(this)
        }
    }

    init {
        //create first page for folder fragment
        PreferenceManager.getDefaultSharedPreferences(context)
        inflater = LayoutInflater.from(context)
        headers = PlaylistManager.getInstance(ApplicationClass.getContext())!!.systemPlaylistsList
        headers.addAll(PlaylistManager.getInstance(ApplicationClass.getContext())!!.userCreatedPlaylistList)
        playerService = ApplicationClass.getService()!!
        setHasStableIds(true)
    }
}