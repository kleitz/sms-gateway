package com.terminuslabs.smsmessenger.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.terminuslabs.smsmessenger.R
import com.terminuslabs.smsmessenger.works.Tasks

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FCM"

    var notificationId = 4319

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO: Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.
        Log.d(TAG, "From: " + remoteMessage.from)
        try{
            Tasks.syncRemoteMessages(this);
        }catch (e: Throwable){
            Log.e(TAG, "Error al sincronizar mensajes", e)
        }
        try{
            Tasks.sendScheduledMessages(this);
        }catch (e: Throwable){
            Log.e(TAG, "Error al enviar mensajes ajendados", e)
        }


        //si no es accion y tiene contenido se muestra el mensaje
        if (remoteMessage.notification != null && !TextUtils.isEmpty(remoteMessage.notification!!.body)  && !TextUtils.isEmpty(remoteMessage.notification!!.title)
            && !"sync".equals(remoteMessage.notification!!.title)) {
            val mensaje = remoteMessage.notification!!.body
            val titulo = remoteMessage.notification!!.title
            if (!TextUtils.isEmpty(mensaje) || !TextUtils.isEmpty(titulo)) {
                notificar(this, titulo!!, mensaje!!)
            }
        }

    }


    //Notification.InboxStyle inboxStyle = new Notification.InboxStyle();
    fun notificar(context: Context, title: String, body: String) {
        try {
            notificationId++
            val NOTIFICATION_CHANNEL_ID: String = getNotificationChannelId(context)!!
            val mBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                mBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis()) //.setTicker("Hearty365")
                //     .setPriority(Notification.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(title)
                .setContentText(body )
                .setAutoCancel(true)
                .setGroup("firebase_notifications")

            // Sets an ID for the notification
            val mNotificationId: Int = notificationId
            // Gets an instance of the NotificationManager service
            val nManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Builds the notification and issues it.
            nManager.notify(mNotificationId, mBuilder.build())

        } catch (e: Throwable) {
        }
    }


    fun getNotificationChannelId(context: Context): String {
        var NOTIFICATION_CHANNEL_ID = ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NOTIFICATION_CHANNEL_ID = "sms_gateway_01"
            val channelName = "Mensajes desde firebase"
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )

            // Configure the notification channel.
            notificationChannel.description = "Notificaciones desde firebase"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.BLUE
            //notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true)
            val nManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nManager.createNotificationChannel(notificationChannel)
        }
        return NOTIFICATION_CHANNEL_ID
    }

}
