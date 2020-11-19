package com.terminuslabs.smsmessenger.receivers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.klinker.android.send_message.SentReceiver
import com.terminuslabs.smsmessenger.extensions.scheduledMessageDB
import com.terminuslabs.smsmessenger.helpers.refreshMessages
import java.lang.Exception

class SmsSentReceiver : SentReceiver() {
    override fun onMessageStatusUpdated(context: Context, intent: Intent, receiverResultCode: Int) {
        refreshMessages()
        setUriForMessage(context, intent, receiverResultCode)
    }


    fun setUriForMessage(context: Context, intent: Intent, receiverResultCode: Int){
        try{
            if (intent!= null && intent.extras!= null && intent.hasExtra("scheduled_id")){
                val msgId = intent.extras?.getLong("scheduled_id")
                context.scheduledMessageDB.changeResult(msgId!!, receiverResultCode)
            }
        }catch (e : Exception){
            Log.e("AppError", "error al obtener status", e)
        }
    }

}
