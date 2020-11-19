package com.terminuslabs.smsmessenger.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.SimpleContactsHelper
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.terminuslabs.smsmessenger.R
import com.terminuslabs.smsmessenger.adapters.ScheduledAdapter
import com.terminuslabs.smsmessenger.extensions.*
import com.terminuslabs.smsmessenger.models.ScheduledMessage
import com.terminuslabs.smsmessenger.works.Tasks
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.conversations_fastscroller
import kotlinx.android.synthetic.main.activity_main.conversations_list
import kotlinx.android.synthetic.main.activity_main.no_conversations_placeholder
import kotlinx.android.synthetic.main.activity_scheduled.*
import java.util.ArrayList

class ScheduledActivity : SimpleActivity()  {


    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheduled)
        title = getString(R.string.scheduled)


        conversations_fab2.setOnClickListener {
            forceSyncSms()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStoredScheduledMessages()
        //loadRemoteScheduledMessages()
        /*if (storedTextColor != config.textColor) {
            (conversations_list.adapter as? ConversationsAdapter)?.updateTextColor(config.textColor)
        }

        if (storedFontSize != config.fontSize) {
            (conversations_list.adapter as? ConversationsAdapter)?.updateFontSize()
        }*/

        /*updateTextColors(main_coordinator)
        no_conversations_placeholder_2.setTextColor(getAdjustedPrimaryColor())
        no_conversations_placeholder_2.underlineText()*/
        //checkShortcut()
    }





    fun updateList(){
        loadStoredScheduledMessages()
        //siempre correr mAdapter.notifyDataSetChanged();
    }


    private fun forceSyncSms() {
        ensureBackgroundThread {
            //se tren los mensajes remotos
            try{
                Tasks.syncRemoteMessages(this)
                runOnUiThread { toast("Mensajes remotos actualizados")}
            }catch (e: Exception){
                Log.e("AppError", "Error", e)
                runOnUiThread {
                    showErrorToast(e)
                }
            }

            //se envian los pendientes
            try{
                Tasks.sendScheduledMessages(this)
                runOnUiThread { toast("Mensajes pendientes enviados")}
            }catch (e: Exception){
                Log.e("AppError", "Error", e)
                runOnUiThread {
                    showErrorToast(e)
                }
            }


            loadStoredScheduledMessages()
        }
    }


    private fun loadStoredScheduledMessages() {
        ensureBackgroundThread {
            val conversations = try {
                scheduledMessageDB.getByState(ScheduledMessage.STATE_PENDING).toMutableList() as ArrayList<ScheduledMessage>
            } catch (e: Exception) {
                ArrayList()
            }

            for (it in conversations){
                it.contactName = SimpleContactsHelper(this).getNameFromPhoneNumber(it.phoneNumber)
            }

            runOnUiThread {
                setupConversations(conversations)
            }
        }
    }



    private fun setupConversations(conversations: ArrayList<ScheduledMessage>) {
        val hasConversations = conversations.isNotEmpty()
        conversations_list.beVisibleIf(hasConversations)
        no_conversations_placeholder.beVisibleIf(!hasConversations)
        //no_conversations_placeholder_2.beVisibleIf(!hasConversations)

        val currAdapter = conversations_list.adapter
        if (currAdapter == null) {
            ScheduledAdapter(
                this,
                conversations,
                conversations_list,
                conversations_fastscroller
            ) {}.apply {
                conversations_list.adapter = this
            }
        } else {
            try {
                (currAdapter as ScheduledAdapter).updateConversations(conversations)

            } catch (ignored: Exception) {
                Log.e("AppError", "Error al actualizar adapter", ignored)
            }
        }
    }







}
