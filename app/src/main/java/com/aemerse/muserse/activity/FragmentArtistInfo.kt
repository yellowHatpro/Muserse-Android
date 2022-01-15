package com.aemerse.muserse.activity

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.databinding.FragmentArtistInfoBinding
import com.aemerse.muserse.interfaces.DoubleClickListener
import com.aemerse.muserse.model.Constants
import com.aemerse.muserse.model.TrackItem
import com.aemerse.muserse.qlyrics.ArtistInfo.ArtistInfo
import com.aemerse.muserse.qlyrics.offlineStorage.OfflineStorageArtistBio
import com.aemerse.muserse.qlyrics.tasks.DownloadArtInfoThread
import com.aemerse.muserse.service.PlayerService
import com.aemerse.muserse.utils.UtilityFun
import java.io.*
import java.net.URL

class FragmentArtistInfo : Fragment(), ArtistInfo.Callback {
    private var mArtistUpdateReceiver: BroadcastReceiver? = null
    private var mArtistInfo: ArtistInfo? = null

    private var playerService: PlayerService? = null

    private var _binding: FragmentArtistInfoBinding? = null
    private val binding get() = _binding!!

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        Log.v("frag", isVisibleToUser.toString() + "")
        super.setUserVisibleHint(isVisibleToUser)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArtistInfoBinding.inflate(inflater, container, false)
        playerService = ApplicationClass.getService()
        if (ApplicationClass.getService() == null) {
            UtilityFun.restartApp()
            return binding.root
        }
        playerService = ApplicationClass.getService()
        binding.buttonUpdateMetadata.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val item: TrackItem = playerService!!.getCurrentTrack() ?: return
                val editedArtist: String = binding.trackArtistArtsiBioFrag.text.toString().trim()
                if (editedArtist.isEmpty()) {
                    Toast.makeText(context,
                        getString(R.string.te_error_empty_field),
                        Toast.LENGTH_SHORT).show()
                    return
                }
                when {
                    editedArtist != item.getArtist() -> {

                        //changes made, save those
                        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        val values = ContentValues()
                        values.put(MediaStore.Audio.Media.ARTIST, editedArtist)
                        context!!.contentResolver
                            .update(uri,
                                values,
                                MediaStore.Audio.Media.TITLE + "=?",
                                arrayOf(item.title))
                        val intent: Intent = Intent(context, ActivityNowPlaying::class.java)
                        intent.putExtra("refresh", true)
                        intent.putExtra("position", playerService!!.getCurrentTrackPosition())
                        intent.putExtra("originalTitle", item.title)
                        intent.putExtra("title", item.title)
                        intent.putExtra("artist", editedArtist)
                        intent.putExtra("album", item.album)
                        startActivity(intent)
                        binding.trackArtistArtsiBioFrag.visibility = View.GONE
                        binding.updateTrackMetadata.visibility = View.GONE
                        binding.buttonUpdateMetadata.visibility = View.GONE
                        binding.buttonUpdateMetadata.isClickable = false
                        if (activity != null) {
                            val view: View? = activity!!.currentFocus
                            if (view != null) {
                                (activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?)?.hideSoftInputFromWindow(
                                    view.windowToken,
                                    0)
                            }
                        }
                        downloadArtInfo()
                    }
                    else -> {
                        Toast.makeText(context,
                            getString(R.string.change_tags_to_update),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        //retry click listener
        binding.root.findViewById<View>(R.id.ll_art_bio)
            .setOnClickListener(object : DoubleClickListener() {
                override fun onSingleClick(v: View?) {
                    if (binding.retryTextView.visibility == View.VISIBLE) {
                        binding.retryTextView.visibility = View.GONE
                        binding.textViewArtBioFrag.visibility = View.VISIBLE
                        binding.trackArtistArtsiBioFrag.visibility = View.GONE
                        binding.updateTrackMetadata.visibility = View.GONE
                        binding.buttonUpdateMetadata.visibility = View.GONE
                        binding.buttonUpdateMetadata.isClickable = false
                        binding.loadingLyricsAnimation.visibility = View.GONE
                        downloadArtInfo()
                    }
                }

                override fun onDoubleClick(v: View?) {

                    //if no connection text, do not hide artist content
                    if ((binding.retryTextView.text.toString() == getString(R.string.no_connection))) {
                        return
                    }
                    when (binding.textViewArtBioFrag.visibility) {
                        View.VISIBLE -> {
                            binding.textViewArtBioFrag.visibility = View.GONE
                        }
                        else -> {
                            binding.textViewArtBioFrag.visibility = View.VISIBLE
                        }
                    }
                }
            })

        //downloadArtInfo();
        mArtistUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                //already displayed, skip
                updateArtistInfoIfNeeded()
            }
        }
        return binding.root
    }

    private fun downloadArtInfo() {
        val item: TrackItem? = playerService!!.getCurrentTrack()
        if (item?.getArtist() == null) {
            return
        }
        binding.textViewArtBioFrag.text = getString(R.string.artist_info_loading)

        //set loading animation
        binding.loadingLyricsAnimation.visibility = View.VISIBLE
        binding.loadingLyricsAnimation.show()

        //see in offlinne db first
        mArtistInfo = OfflineStorageArtistBio.getArtistBioFromTrackItem(item)
        //second check is added to make sure internet call will happen
        //when user manually changes artist tag
        if (mArtistInfo != null && (item.getArtist()!!
                .trim { it <= ' ' } == mArtistInfo!!.getOriginalArtist()!!.trim())
        ) {
            onArtInfoDownloaded(mArtistInfo)
            return
        }
        if (UtilityFun.isConnectedToInternet) {
            var artist: String? = item.getArtist()
            artist = UtilityFun.filterArtistString(artist!!)
            DownloadArtInfoThread(this, artist, item).start()
        } else {
            binding.textViewArtBioFrag.visibility = View.GONE
            binding.retryTextView.text = getString(R.string.no_connection)
            binding.retryTextView.visibility = View.VISIBLE
            binding.loadingLyricsAnimation.hide()
            binding.loadingLyricsAnimation.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mArtistUpdateReceiver!!)
    }

    override fun onResume() {
        super.onResume()
        if (ApplicationClass.getService() != null) {
            updateArtistInfoIfNeeded()
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mArtistUpdateReceiver!!,
                IntentFilter(Constants.ACTION.UPDATE_LYRIC_AND_INFO))
        } else {
            UtilityFun.restartApp()
        }
    }

    private fun updateArtistInfoIfNeeded() {
        val item: TrackItem? = playerService!!.getCurrentTrack()
        if (item == null) {
            binding.textViewArtBioFrag.visibility = View.GONE
            binding.retryTextView.text = getString(R.string.no_music_found)
            //retryText.setVisibility(View.GONE);
            binding.retryTextView.visibility = View.VISIBLE
            binding.loadingLyricsAnimation.hide()
            return
        }
        if (mArtistInfo != null && mArtistInfo!!.getOriginalArtist().equals(item.getArtist())) {
            return
        }

        //set loading  text and animation
        //set loading  text and animation
        downloadArtInfo()
    }

    override fun onArtInfoDownloaded(artistInfo: ArtistInfo?) {
        mArtistInfo = artistInfo
        if ((artistInfo == null) || (activity == null) || !isAdded) {
            return
        }
        val item: TrackItem? = playerService!!.getCurrentTrack()
        //if song is already changed , return
        if (item != null && !(item.getArtist()!!
                .trim { it <= ' ' } == artistInfo.getOriginalArtist()!!.trim())
        ) {
            //artBioText.setText(getString(R.string.artist_info_loading));
            return
        }
        //hide loading animation
        binding.loadingLyricsAnimation.hide()
        binding.loadingLyricsAnimation.visibility = View.GONE
        if (artistInfo.getArtistContent() == null) {
            binding.retryTextView.text = getString(R.string.artist_info_no_result)
            binding.retryTextView.visibility = View.VISIBLE
            binding.textViewArtBioFrag.visibility = View.GONE
            val tempItem: TrackItem? = playerService!!.getCurrentTrack()
            if (tempItem != null) {
                binding.trackArtistArtsiBioFrag.visibility = View.VISIBLE
                binding.updateTrackMetadata.visibility = View.VISIBLE
                binding.buttonUpdateMetadata.visibility = View.VISIBLE
                binding.buttonUpdateMetadata.isClickable = true
                binding.trackArtistArtsiBioFrag.setText(tempItem.getArtist())
            }
            return
        }
        if ((activity != null) && (artistInfo.getArtistContent() != null)) {
            Log.d("onArtInfoDownloaded", "onArtInfoDownloaded: " + artistInfo.getCorrectedArtist())
            val content = artistInfo.getArtistContent()
            val index: Int = content!!.indexOf("Read more")
            val ss = SpannableString(content)
            val clickableSpan: ClickableSpan = object : ClickableSpan() {
                override fun onClick(textView: View) {
                    if (mArtistInfo!!.getArtistUrl() == null) {
                        Toast.makeText(context,
                            getString(R.string.error_invalid_url),
                            Toast.LENGTH_SHORT).show()
                    } else {
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(
                                mArtistInfo!!.getArtistUrl()))
                            startActivity(browserIntent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context,
                                "No supporting application found for opening the link.",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true
                    ds.typeface = Typeface.create(ds.typeface, Typeface.BOLD)
                }
            }
            if (index != -1) {
                ss.setSpan(clickableSpan, index, index + 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            when {
                content != "" -> {
                    binding.textViewArtBioFrag.visibility = View.VISIBLE
                    binding.retryTextView.visibility = View.GONE
                    binding.textViewArtBioFrag.text = ss
                    binding.textViewArtBioFrag.movementMethod = LinkMovementMethod.getInstance()
                    binding.trackArtistArtsiBioFrag.visibility = View.GONE
                    binding.updateTrackMetadata.visibility = View.GONE
                    binding.buttonUpdateMetadata.visibility = View.GONE
                    binding.buttonUpdateMetadata.isClickable = false
                    binding.trackArtistArtsiBioFrag.setText("")
                }
                else -> {
                    binding.textViewArtBioFrag.visibility = View.GONE
                    binding.retryTextView.text = getString(R.string.artist_info_no_result)
                    binding.retryTextView.visibility = View.VISIBLE
                    val tempItem: TrackItem? = playerService!!.getCurrentTrack()
                    if (tempItem != null) {
                        binding.trackArtistArtsiBioFrag.visibility = View.VISIBLE
                        binding.updateTrackMetadata.visibility = View.VISIBLE
                        binding.buttonUpdateMetadata.visibility = View.VISIBLE
                        binding.buttonUpdateMetadata.isClickable = true
                        binding.trackArtistArtsiBioFrag.setText(tempItem.getArtist())
                    }
                }
            }

            //check current now playing background setting
            ///get current setting
            // 0 - System default   1 - artist image  2 - custom
            val currentNowPlayingBackPref: Int =
                ApplicationClass.getPref().getInt(getString(R.string.pref_now_playing_back), 1)
            if (currentNowPlayingBackPref == 1 && !artistInfo.getCorrectedArtist()
                    .equals("[unknown]")
            ) {
                if (!(activity as ActivityNowPlaying?)!!.isArtistLoadedInBack()) {
                    SetBlurryImagetask().execute(artistInfo)
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class SetBlurryImagetask :
        AsyncTask<ArtistInfo?, String?, Bitmap?>() {
        var b: Bitmap? = null
        override fun doInBackground(vararg params: ArtistInfo?): Bitmap? {

            //store file in cache with artist id as name
            //create folder in cache for artist images
            val CACHE_ART_THUMBS: String =
                ApplicationClass.getContext().cacheDir.toString() + "/art_thumbs/"
            val actual_file_path: String = CACHE_ART_THUMBS + params[0]!!.getOriginalArtist()
            val f = File(CACHE_ART_THUMBS)
            if (!f.exists()) {
                f.mkdir()
            }
            if (!File(actual_file_path).exists()) {
                //create file
                val fos: FileOutputStream?
                try {
                    fos = FileOutputStream(File(actual_file_path))
                    val url = URL(params[0]!!.getImageUrl())
                    val inputStream: InputStream = url.openConnection().getInputStream()
                    val buffer = ByteArray(1024)
                    var bufferLength: Int
                    while ((inputStream.read(buffer).also { bufferLength = it }) > 0) {
                        fos.write(buffer, 0, bufferLength)
                    }
                    fos.close()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            b = BitmapFactory.decodeFile(actual_file_path)
            return b
        }

        override fun onPostExecute(b: Bitmap?) {

            //set background image
            if (b != null && activity != null) {
                (activity as ActivityNowPlaying?)!!.setBlurryBackground(b)
            }
        }
    }
}