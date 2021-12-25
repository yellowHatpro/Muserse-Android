package com.aemerse.muserse.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.uiElementHelper.FastScroller
import com.aemerse.muserse.adapter.AlbumLibraryAdapter
import com.aemerse.muserse.model.Constants
import com.aemerse.muserse.model.MusicLibrary
import java.util.concurrent.Executors

class FragmentAlbumLibrary : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private var mRecyclerView: RecyclerView? = null
    private var fastScroller: FastScroller? = null
    private var albumLibraryAdapter: AlbumLibraryAdapter? = null

    //private SwipeRefreshLayout swipeRefreshLayout;
    private var mRefreshLibraryReceiver: BroadcastReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRefreshLibraryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                albumLibraryAdapter = AlbumLibraryAdapter(requireContext(),
                    MusicLibrary.instance.getDataItemsForAlbums())
                mRecyclerView!!.adapter = albumLibraryAdapter
                //swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    override fun onDestroy() {
        mRecyclerView = null
        super.onDestroy()
    }

    fun filter(s: String?) {
        if (albumLibraryAdapter != null) {
            albumLibraryAdapter!!.filter(s!!)
        }
    }

    fun sort(sort_id: Int) {
        if (albumLibraryAdapter != null) {
            albumLibraryAdapter!!.sort(sort_id)
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mRefreshLibraryReceiver!!, IntentFilter(Constants.ACTION.REFRESH_LIB))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mRefreshLibraryReceiver!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout: View = inflater.inflate(R.layout.fragment_library, container, false)
        //swipeRefreshLayout = layout.findViewById(R.id.swipeRefreshLayout);
        //swipeRefreshLayout.setOnRefreshListener(this);
        mRecyclerView = layout.findViewById(R.id.recyclerviewList)
        fastScroller = layout.findViewById(R.id.fastscroller)
        fastScroller!!.setRecyclerView(mRecyclerView)

        /*mRecyclerView.setTrackColor(ColorHelper.getColor(R.color.colorTransparent));
        mRecyclerView.setThumbColor(ColorHelper.getAccentColor());
        mRecyclerView.setPopupBgColor(ColorHelper.getAccentColor());*/albumLibraryAdapter =
            AlbumLibraryAdapter(requireContext(), MusicLibrary.instance.getDataItemsForAlbums())
        albumLibraryAdapter!!.sort(ApplicationClass.getPref()
            .getInt(getString(R.string.pref_album_sort_by), Constants.SORT_BY.NAME))
        mRecyclerView!!.adapter = albumLibraryAdapter
        val mLayoutManager: RecyclerView.LayoutManager = GridLayoutManager(context, 3)
        mRecyclerView!!.layoutManager = mLayoutManager
        mRecyclerView!!.itemAnimator = DefaultItemAnimator()
        mRecyclerView!!.setHasFixedSize(true)
        mRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    (activity as ActivityMain?)!!.hideFab(true)
                } else (activity as ActivityMain?)!!.hideFab(false)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    (activity as ActivityMain?)!!.hideFab(false)
                }
                super.onScrollStateChanged(recyclerView, newState)
            }
        })
        return layout
    }

    override fun onRefresh() {
        Executors.newSingleThreadExecutor().execute {
            MusicLibrary.instance.RefreshLibrary()
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRecyclerView!!.adapter = null
    }
}