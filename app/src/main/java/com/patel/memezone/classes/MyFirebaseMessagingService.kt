package com.patel.memezone.classes

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.patel.memezone.R
import com.patel.memezone.activitys.HomeActivity

@SuppressLint("MissingFirebaseInstanceTokenRefresh")

const val channelId = "notification_channel"
const val channelName = "com.patel.memezone.classes"
class MyFirebaseMessagingService : FirebaseMessagingService(){


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.notification != null) {
            generateNotification(remoteMessage.notification!!.title!!,remoteMessage.notification!!.body!!)
        }
    }
        @SuppressLint("UnspecifiedImmutableFlag", "RemoteViewLayout")
        fun getRemoteView(title : String, message : String) : RemoteViews{
            val remoteViews = RemoteViews("com.patel.memezone.classes",R.layout.notification)
            remoteViews.setTextViewText(R.id.noti_Title,title)
            remoteViews.setTextViewText(R.id.noti_Content,message)
            remoteViews.setImageViewResource(R.id.noti_Logo,R.drawable.logo_de4c6ce7)

            return remoteViews
        }
        fun generateNotification(title : String, message : String) {
            val intent = Intent(this,HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pandingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            var builder: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext,
                channelId)
                .setSmallIcon(R.drawable.logo_de4c6ce7)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000,1000,1000,1000))
                .setOnlyAlertOnce(true)
                .setContentIntent(pandingIntent)

            builder = builder.setContent(getRemoteView(title,message))

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(channelId, channelName,NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(notificationChannel)
            }
            notificationManager.notify(0,builder.build())
        }
}