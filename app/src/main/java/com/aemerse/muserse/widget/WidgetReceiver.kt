package com.aemerse.muserse.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.aemerse.muserse.activity.ActivityPermissionSeek
import com.aemerse.muserse.model.Constants
import com.aemerse.muserse.model.MusicLibrary
import com.aemerse.muserse.service.PlayerService

class WidgetReceiver : AppWidgetProvider() {
    var TAG = "Widget"
    var action: String? = null
    var context: Context? = null

    private val playerServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, service: IBinder) {
            val playerBinder = service as PlayerService.PlayerBinder
            val playerService: PlayerService = playerBinder.getService()
            ApplicationClass.setService(playerService)
            context!!.startService(Intent(context, PlayerService::class.java)
                .setAction(action))
        }

        override fun onServiceDisconnected(arg0: ComponentName) {}
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.v(TAG, "Intent " + intent.action)
        this.context = context
        action = intent.action
        when (intent.action) {
            null -> {
                //launch player
                when {
                    ApplicationClass.getService() == null -> {
                        MusicLibrary.instance
                        when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                                context.startForegroundService(Intent(context,
                                    PlayerService::class.java).setAction(Constants.ACTION.LAUNCH_PLAYER_FROM_WIDGET))
                            }
                            else -> {
                                context.startService(Intent(context, PlayerService::class.java)
                                    .setAction(Constants.ACTION.LAUNCH_PLAYER_FROM_WIDGET))
                            }
                        }
                    }
                    else -> {
                        //permission seek activity is used here to show splash screen
                        context.startActivity(Intent(context, ActivityPermissionSeek::class.java).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
            else -> {
                when {
                    ApplicationClass.getService() == null -> {
                        Log.v(TAG, "Widget " + "Service is null")
                        when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                                context.startForegroundService(Intent(context,
                                    PlayerService::class.java).setAction(intent.action))
                            }
                            else -> {
                                context.startService(Intent(context, PlayerService::class.java))
                            }
                        }

                        /*try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }*/

                        /*new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        IBinder service = peekService(context, new Intent(context, playerService!!.class));

                                        if (service != null){
                                            playerService!!.PlayerBinder playerBinder = (playerService!!.PlayerBinder) service;
                                            PlayerService playerService = playerBinder.getTrackInfoService();
                                            MyApp.setService(playerService);
                                            context.startService(new Intent(context, playerService!!.class)
                                                    .setAction(action));
                                            Log.v(TAG,"Widget "+ action);
                                            Log.v(TAG,"Widget "+ "Service started");
                                        }else {
                                            Log.v(TAG,"Widget "+ "Service null");
                                        }
                                    }
                                }, 500);*/
                    }
                    else -> {
                        context.startService(Intent(context, PlayerService::class.java)
                            .setAction(intent.action))
                    }
                }
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("WidgetReceiver", "onUpdate: called")

        //if player service is null, start the service
        //current song info will be updated in widget from the service itself
        if (ApplicationClass.getService() == null) {
            Log.d("WidgetReceiver", "onUpdate: Music service is null")
            MusicLibrary.instance
            try {
                val playerServiceIntent = Intent(context, PlayerService::class.java)
                playerServiceIntent.action = Constants.ACTION.WIDGET_UPDATE
                context.startService(playerServiceIntent)
                //context.bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
            } catch (e: Exception) {
                Log.d("WidgetReceiver", "onUpdate: Error in creating widget")
                e.printStackTrace()
            }
        }

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (appWidgetId in appWidgetIds) {
            val intent = Intent(context, WidgetReceiver::class.java)
            val activityP = PendingIntent.getBroadcast(context, 0, intent, 0)
            val previousIntent = Intent(context, WidgetReceiver::class.java)
            previousIntent.action = Constants.ACTION.PREV_ACTION
            val prevP = PendingIntent.getBroadcast(context, 0, previousIntent, 0)
            val playIntent = Intent(context, WidgetReceiver::class.java)
            playIntent.action = Constants.ACTION.PLAY_PAUSE_ACTION
            val playPauseP = PendingIntent.getBroadcast(context, 0, playIntent, 0)
            val nextIntent = Intent(context, WidgetReceiver::class.java)
            nextIntent.action = Constants.ACTION.NEXT_ACTION
            val nextP = PendingIntent.getBroadcast(context, 0, nextIntent, 0)
            val shuffleIntent = Intent(context, WidgetReceiver::class.java)
            shuffleIntent.action = Constants.ACTION.SHUFFLE_WIDGET
            val shuffleP = PendingIntent.getBroadcast(context, 0, shuffleIntent, 0)
            val repeatIntent = Intent(context, WidgetReceiver::class.java)
            repeatIntent.action = Constants.ACTION.REPEAT_WIDGET
            val repeatP = PendingIntent.getBroadcast(context, 0, repeatIntent, 0)
            val favIntent = Intent(context, WidgetReceiver::class.java)
            favIntent.action = Constants.ACTION.FAV_WIDGET
            val favP = PendingIntent.getBroadcast(context, 0, favIntent, 0)
            val views = RemoteViews(context.packageName, R.layout.wigdet)
            views.setOnClickPendingIntent(R.id.root_view_widget, activityP)
            views.setOnClickPendingIntent(R.id.widget_Play, playPauseP)
            views.setOnClickPendingIntent(R.id.widget_Skip_back, prevP)
            views.setOnClickPendingIntent(R.id.widget_Skip_forward, nextP)
            views.setOnClickPendingIntent(R.id.repeat_wrapper, repeatP)
            views.setOnClickPendingIntent(R.id.widget_shuffle, shuffleP)
            views.setOnClickPendingIntent(R.id.widget_fav, favP)
            if (ApplicationClass.getService() != null) {
                ApplicationClass.getService()?.updateWidget(true)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}