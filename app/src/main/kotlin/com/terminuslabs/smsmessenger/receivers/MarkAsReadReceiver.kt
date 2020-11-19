package com.terminuslabs.smsmessenger.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.commons.extensions.notificationManager
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.terminuslabs.smsmessenger.extensions.conversationsDB
import com.terminuslabs.smsmessenger.extensions.markThreadMessagesRead
import com.terminuslabs.smsmessenger.extensions.updateUnreadCountBadge
import com.terminuslabs.smsmessenger.helpers.MARK_AS_READ
import com.terminuslabs.smsmessenger.helpers.THREAD_ID

class MarkAsReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MARK_AS_READ -> {
                val threadId = intent.getIntExtra(THREAD_ID, 0)
                context.notificationManager.cancel(threadId)
                ensureBackgroundThread {
                    context.markThreadMessagesRead(threadId)
                    context.conversationsDB.markRead(threadId.toLong())
                    context.updateUnreadCountBadge(context.conversationsDB.getUnreadConversations())
                }
            }
        }
    }
}
