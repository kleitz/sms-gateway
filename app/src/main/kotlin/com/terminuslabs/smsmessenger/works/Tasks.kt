package com.terminuslabs.smsmessenger.works

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.getIntent
import android.os.Build
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction
import com.terminuslabs.smsmessenger.brodcastrecivers.CustomAlarmReceiver
import com.terminuslabs.smsmessenger.extensions.scheduledMessageDB
import com.terminuslabs.smsmessenger.interfaces.RemoteServerEndpoint
import com.terminuslabs.smsmessenger.models.RemoteModel
import com.terminuslabs.smsmessenger.models.ScheduledMessage
import com.terminuslabs.smsmessenger.receivers.SmsSentReceiver
import com.terminuslabs.smsmessenger.sync.ServiceBuilder
import java.util.*


object Tasks {

    fun syncRemoteMessages(ctx: Context) {
        val messsagesIds :  MutableList<Long> = LinkedList()
        val requestBody :  MutableList<RemoteModel.RequestMessageInfo> = LinkedList()
        val sended = ctx.scheduledMessageDB.getByDistinctState(ScheduledMessage.STATE_PENDING)
        for ( msg in sended){
            requestBody.add(RemoteModel.RequestMessageInfo(msg.id, msg.state, msg.resultCode))
            messsagesIds.add(msg.id)
        }

        val request =
            ServiceBuilder.buildService(
                RemoteServerEndpoint::class.java
            )

        val url = ServiceBuilder.getServerUrl(ctx)
        val autorizationH = ServiceBuilder.getAuthHeader(ctx)
        if (url == null){
            return
        }
        val respose = request.syncMessageStates(url, autorizationH, requestBody).execute()


        val alarmsDates :  MutableSet<Calendar> = HashSet()

        if (respose.isSuccessful){
            val newMessages :  MutableList<ScheduledMessage> = LinkedList()
            val responseBody =  respose.body()
            if (responseBody!= null){
                for (msg in responseBody){
                    val newMessage = ScheduledMessage()
                    newMessage.id = msg.id
                    newMessage.body = msg.body
                    newMessage.phoneNumber = msg.phoneNumber
                    newMessage.scheduledDate = msg.scheduledDate
                    newMessage.creationDate = Date()
                    newMessages.add(newMessage)


                }
            }
            //se guardan los mensajes
            ctx.scheduledMessageDB.delete(messsagesIds)
            ctx.scheduledMessageDB.insertOrUpdateAll(newMessages)

            var correrAhora = false

            //se agendan alarmas para que se envie de forma exacta
            for (message in newMessages) {
                if (message.scheduledDate!= null &&  (message.scheduledDate!!.compareTo(Date()) > 0 )){
                    agendarAlarmaHoraExacta(ctx, message)
                }else{
                    correrAhora = true
                }
            }
            if (correrAhora){
                val workSendSMSNow = OneTimeWorkRequestBuilder<SendScheduledSMSWorker>().build()
                val workManager = WorkManager.getInstance (ctx)
                WorkManager.getInstance (ctx).enqueue(workSendSMSNow)
            }
        } else{
            throw java.lang.Exception("Response code: " + respose.code())
        }
    }

    private fun agendarAlarmaHoraExacta(ctx: Context, message : ScheduledMessage){
        try{
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val alarmIntent = Intent(ctx, CustomAlarmReceiver::class.java).let { intent ->
                PendingIntent.getBroadcast(ctx, 0, intent, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    message.scheduledDate!!.time,
                    alarmIntent
                )
            }else{
                alarmManager?.set(
                    AlarmManager.RTC_WAKEUP,
                    message.scheduledDate!!.time,
                    alarmIntent
                )
            }

        }catch ( t: Throwable){
            Log.e("AppError", "Error", t)
        }
    }




    fun sendScheduledMessages(ctx: Context) {
        val toSend = ctx.scheduledMessageDB.getToSend(ScheduledMessage.STATE_PENDING, Date())
        for ( msg in toSend){
            try{
                val settings = Settings()
                settings.useSystemSending = true
                settings.deliveryReports = true
                val transaction = Transaction(ctx, settings)

                val i1 = Intent(ctx, SmsSentReceiver::class.java)
                i1.putExtra("scheduled_id", msg.id)
                transaction.setExplicitBroadcastForDeliveredSms(i1)

                val i2 = Intent(ctx, SmsSentReceiver::class.java)
                i2.putExtra("scheduled_id", msg.id)
                transaction.setExplicitBroadcastForSentSms(i2)


                val message = com.klinker.android.send_message.Message(msg.body, msg.phoneNumber)
                transaction.sendNewMessage(message, Transaction.NO_THREAD_ID)

                ctx.scheduledMessageDB.changeState(msg.id, ScheduledMessage.STATE_SENT)
            }catch (e: Exception){
                ctx.scheduledMessageDB.changeState(msg.id, ScheduledMessage.STATE_ERROR)
            }
        }
    }

}
