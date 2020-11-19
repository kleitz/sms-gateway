package com.terminuslabs.smsmessenger.brodcastrecivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.terminuslabs.smsmessenger.works.Tasks

class CustomAlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try{
            ensureBackgroundThread {
                Tasks.sendScheduledMessages(context)
            }
        } catch (e:Exception) {
            Toast.makeText(context, "ERROR", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }


}
