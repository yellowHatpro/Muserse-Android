package com.aemerse.muserse.fcm

import android.app.IntentService
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.activity.ActivityExploreLyrics
import com.aemerse.muserse.activity.ActivityLyricView
import com.aemerse.muserse.model.Constants
import com.aemerse.muserse.uiElementHelper.ColorHelper
import com.aemerse.muserse.utils.AppLaunchCountManager
import com.aemerse.muserse.utils.UtilityFun
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("MyFirebaseMessaging", "onMessageReceived: $remoteMessage")
        val map: Map<String, String> = remoteMessage.data
        for (keys in map.keys) {
            Log.d("MyFirebaseMessaging", "onMessageReceived: $keys")
        }

        //if opt out of notifications in settings, this part should never be executed
        //in case it does, ignore the message
        if (!ApplicationClass.getPref().getBoolean(getString(R.string.pref_notifications), true)) {
            return
        }

        //do not show notification to users with ads removed
        if (map["type"] == "discount" && UtilityFun.isAdsRemoved) {
            return
        }
        if (map["type"] == "review" && (!AppLaunchCountManager.isEligibleForRatingAsk || ApplicationClass.Companion.getPref().getBoolean(getString(R.string.pref_already_rated), false))) {
            return
        }
        val handler = Handler(Looper.getMainLooper())
        handler.post { GeneratePictureStyleNotification(map).execute() }
    }

    class GeneratePictureStyleNotification internal constructor(private val map: Map<String, String>) : AsyncTask<String?, Void?, Bitmap?>() {
        private var builder: NotificationCompat.Builder? = null
        override fun doInBackground(vararg params: String?): Bitmap? {
            val `in`: InputStream
            try {
                val url = URL(map["image_link"])
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                `in` = connection.inputStream
                return BitmapFactory.decodeStream(`in`)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            try {
                val contentTitle = map["title"]
                val contentText = map["body"]
                val subText = map["subtitle"]
                builder = NotificationCompat.Builder(ApplicationClass.getContext(),
                    ApplicationClass.getContext().getString(R.string.notification_channel))
                    .setColor(ColorHelper.getColor(R.color.notification_color))
                    .setSmallIcon(R.drawable.ic_batman_kitkat)
                    .setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setSubText(subText)
                    .setAutoCancel(true)
                    .setStyle(NotificationCompat.BigPictureStyle().bigPicture(result)
                        .setSummaryText(contentText))
                builder!!.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                builder!!.priority = Notification.PRIORITY_MAX
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    builder!!.setChannelId(ApplicationClass.getContext()
                        .getString(R.string.notification_channel))
                }
                when (map["type"]) {
                    "discount" -> discountNotif()
                    "trending_tracks" -> trendingTracksNotif()
                    "check_out_lyric" -> checkOutLyricNotif(map)
                    "unknown" -> unknownNotif(map)
                    "search_lyric" -> searchLyricNotif()
                    "review" -> reviewNotif(map)
                }
                val notification = builder!!.build()
                val mNotificationManager = ApplicationClass.getContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.notify(Random().nextInt(), notification)
                try {
                    val bundle = Bundle()
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "notification_displayed")
                    UtilityFun.logEvent(bundle)
                }
                catch (ignored: Exception) { }
            } catch (e: Exception) {
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "notification_crashed_app")
                UtilityFun.logEvent(bundle)
            }
        }

        //helper methods to build specific notifications
        private fun discountNotif() {}
        private fun trendingTracksNotif() {
            val requestCode = Random().nextInt()
            val notificationIntent = Intent(ApplicationClass.getContext(), ActivityExploreLyrics::class.java)
            notificationIntent.action = Constants.ACTION.MAIN_ACTION
            notificationIntent.putExtra("fresh_load", true)
            notificationIntent.putExtra("from_notif", true)
            notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val contentIntent = PendingIntent.getActivity(ApplicationClass.getContext(), requestCode, notificationIntent, 0)
            builder!!.setContentIntent(contentIntent)
        }

        private fun checkOutLyricNotif(map: Map<String, String>) {
            val trackTitle = map["trackname"]
            val artist = map["artist"]
            val requestCode = Random().nextInt()
            val notificationIntent = Intent(ApplicationClass.getContext(), ActivityLyricView::class.java)
            notificationIntent.action = Constants.ACTION.MAIN_ACTION
            notificationIntent.putExtra("track_title", trackTitle)
            notificationIntent.putExtra("artist", artist)
            notificationIntent.putExtra("from_notif", true)
            notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val contentIntent: PendingIntent = PendingIntent.getActivity(ApplicationClass.Companion.getContext(), requestCode, notificationIntent, 0)
            builder!!.setContentIntent(contentIntent)
        }

        private fun unknownNotif(map: Map<String, String>) {
            val notificationIntent = Intent(Intent.ACTION_VIEW)
            notificationIntent.data = Uri.parse(map["link"])
            val contentIntent = PendingIntent.getActivity(ApplicationClass.getContext(), 0, notificationIntent, 0)
            builder!!.setContentIntent(contentIntent)
        }

        private fun searchLyricNotif() {
            val requestCode = Random().nextInt()
            val notificationIntent = Intent(ApplicationClass.getContext(), ActivityExploreLyrics::class.java)
            notificationIntent.action = Constants.ACTION.MAIN_ACTION
            notificationIntent.putExtra("search_on_launch", true)
            notificationIntent.putExtra("from_notif", true)
            notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            val contentIntent = PendingIntent.getActivity(ApplicationClass.getContext(), requestCode, notificationIntent, 0)
            builder!!.setContentIntent(contentIntent)
        }

        private fun reviewNotif(map: Map<String, String>) {
            val action1Intent = Intent(ApplicationClass.getContext(), NotificationActionService::class.java).setAction(ALREADY_RATED)
            action1Intent.putExtra("from_notif", true)
            val alreadyRatedIntent = PendingIntent.getService(ApplicationClass.getContext(), 20, action1Intent, PendingIntent.FLAG_ONE_SHOT)
            val action2Intent = Intent(ApplicationClass.getContext(), NotificationActionService::class.java).setAction(RATE_NOW)
            action2Intent.putExtra("from_notif", true)
            val rateNowIntent = PendingIntent.getService(ApplicationClass.getContext(), 20, action2Intent, PendingIntent.FLAG_ONE_SHOT)
            builder!!.addAction(NotificationCompat.Action(R.drawable.ic_close_white_24dp, "Rate now!", rateNowIntent))
            builder!!.addAction(NotificationCompat.Action(R.drawable.ic_close_white_24dp, "Already rated", alreadyRatedIntent))
            builder!!.setContentIntent(rateNowIntent)
        }

        class NotificationActionService :
            IntentService(NotificationActionService::class.java.simpleName) {
            override fun onHandleIntent(intent: Intent?) {
                val action: String = intent!!.action ?: return
                if (intent.extras != null && intent.extras!!.getBoolean("from_notif")) {
                    try {
                        val bundle = Bundle()
                        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE,
                            "notification_clicked")
                        UtilityFun.logEvent(bundle)
                    } catch (ignored: Exception) {
                    }
                }
                Log.d("NotificationAction", "onHandleIntent: $action")
                when (action) {
                    ALREADY_RATED -> ApplicationClass.getPref().edit()
                        .putBoolean(getString(R.string.pref_already_rated), true).apply()
                    RATE_NOW -> {
                        val appPackageName: String =
                            packageName // getPackageName() from Context or Activity object
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$appPackageName")))
                        } catch (anfe: ActivityNotFoundException) {
                            startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
                        }
                        try {
                            val bundle = Bundle()
                            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE,
                                "rate_on_notif_click")
                            UtilityFun.logEvent(bundle)
                        } catch (ignored: Exception) {
                        }
                    }
                }
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager?)?.cancel(Constants.NOTIFICATION_ID.FCM)
            }
        }

        companion object {
            private const val ALREADY_RATED = "already_rated"
            private const val RATE_NOW = "rate_now"
        }
    }
}