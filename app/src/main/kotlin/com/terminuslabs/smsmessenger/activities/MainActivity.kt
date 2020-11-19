package com.terminuslabs.smsmessenger.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.provider.Telephony
import android.view.Menu
import android.view.MenuItem
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.FAQItem
import com.terminuslabs.smsmessenger.BuildConfig
import com.terminuslabs.smsmessenger.R
import com.terminuslabs.smsmessenger.adapters.ConversationsAdapter
import com.terminuslabs.smsmessenger.extensions.config
import com.terminuslabs.smsmessenger.extensions.conversationsDB
import com.terminuslabs.smsmessenger.extensions.getConversations
import com.terminuslabs.smsmessenger.extensions.updateUnreadCountBadge
import com.terminuslabs.smsmessenger.helpers.THREAD_ID
import com.terminuslabs.smsmessenger.helpers.THREAD_TITLE
import com.terminuslabs.smsmessenger.models.Conversation
import com.terminuslabs.smsmessenger.models.Events
import com.terminuslabs.smsmessenger.works.DownloadScheduledMessagesWorker
import com.terminuslabs.smsmessenger.works.SendScheduledSMSWorker
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : SimpleActivity() {
    private val MAKE_DEFAULT_APP_REQUEST = 1

    private var storedTextColor = 0
    private var storedFontSize = 0
    private var bus: EventBus? = null

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appLaunched(BuildConfig.APPLICATION_ID)

        if (checkAppSideloading()) {
            return
        }

        if (isQPlus()) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager!!.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    askPermissions()
                } else {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
                }
            } else {
                toast(R.string.unknown_error_occurred)
                finish()
            }
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) {
                askPermissions()
            } else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                startActivityForResult(intent, MAKE_DEFAULT_APP_REQUEST)
            }
        }

        scheduleWorks()
    }

    fun scheduleWorks(){
        val workDownloadSms = PeriodicWorkRequestBuilder<DownloadScheduledMessagesWorker>(1, TimeUnit.DAYS).build()
        val workSendSMS = PeriodicWorkRequestBuilder<SendScheduledSMSWorker>(2, TimeUnit.HOURS).build()
        val workSendSMSNow = OneTimeWorkRequestBuilder<SendScheduledSMSWorker>().build()
        val workDownloadSmsMSNow = OneTimeWorkRequestBuilder<DownloadScheduledMessagesWorker>().build()

        val workManager = WorkManager.getInstance (this.applicationContext)
        WorkManager.getInstance (this).enqueue(workDownloadSmsMSNow)
        WorkManager.getInstance (this).enqueue(workSendSMSNow)
        workManager.enqueueUniquePeriodicWork ("workDownloadSms", ExistingPeriodicWorkPolicy.KEEP, workDownloadSms)
        workManager.enqueueUniquePeriodicWork ("workSendSMS", ExistingPeriodicWorkPolicy.KEEP, workSendSMS)
    }

    override fun onResume() {
        super.onResume()
        if (storedTextColor != config.textColor) {
            (conversations_list.adapter as? ConversationsAdapter)?.updateTextColor(config.textColor)
        }

        if (storedFontSize != config.fontSize) {
            (conversations_list.adapter as? ConversationsAdapter)?.updateFontSize()
        }

        updateTextColors(main_coordinator)
        no_conversations_placeholder_2.setTextColor(getAdjustedPrimaryColor())
        no_conversations_placeholder_2.underlineText()
        checkShortcut()
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> launchSettings()
            //R.id.about -> launchAbout()
            R.id.scheduled -> launchScheduled()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == MAKE_DEFAULT_APP_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                askPermissions()
            } else {
                finish()
            }
        }
    }

    private fun storeStateVariables() {
        storedTextColor = config.textColor
        storedFontSize = config.fontSize
    }

    // while SEND_SMS and READ_SMS permissions are mandatory, READ_CONTACTS is optional. If we don't have it, we just won't be able to show the contact name in some cases
    private fun askPermissions() {
        handlePermission(PERMISSION_READ_SMS) {
            if (it) {
                handlePermission(PERMISSION_SEND_SMS) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CONTACTS) {
                            initMessenger()
                            bus = EventBus.getDefault()
                            try {
                                bus!!.register(this)
                            } catch (e: Exception) {
                            }
                        }
                    } else {
                        finish()
                    }
                }
            } else {
                finish()
            }
        }
    }

    private fun initMessenger() {
        storeStateVariables()
        getCachedConversations()

        no_conversations_placeholder_2.setOnClickListener {
            launchNewConversation()
        }

        conversations_fab.setOnClickListener {
            launchNewConversation()
        }
    }

    private fun getCachedConversations() {
        ensureBackgroundThread {
            val conversations = try {
                conversationsDB.getAll().sortedByDescending { it.date }.toMutableList() as ArrayList<Conversation>
            } catch (e: Exception) {
                ArrayList()
            }

            updateUnreadCountBadge(conversations)
            runOnUiThread {
                setupConversations(conversations)
                getNewConversations(conversations)
            }
        }
    }

    private fun getNewConversations(cachedConversations: ArrayList<Conversation>) {
        val privateCursor = getMyContactsCursor().loadInBackground()
        ensureBackgroundThread {
            val conversations = getConversations()

            // check if no message came from a privately stored contact in Simple Contacts
            val privateContacts = MyContactsContentProvider.getSimpleContacts(this, privateCursor)
            if (privateContacts.isNotEmpty()) {
                conversations.filter { it.title == it.phoneNumber }.forEach { conversation ->
                    privateContacts.forEach { contact ->
                        if (contact.doesContainPhoneNumber(conversation.phoneNumber)) {
                            conversation.title = contact.name
                            conversation.photoUri = contact.photoUri
                        }
                    }
                }
            }

            runOnUiThread {
                setupConversations(conversations)
            }

            conversations.forEach { clonedConversation ->
                if (!cachedConversations.map { it.thread_id }.contains(clonedConversation.thread_id)) {
                    conversationsDB.insertOrUpdate(clonedConversation)
                    cachedConversations.add(clonedConversation)
                }
            }

            cachedConversations.forEach { cachedConversation ->
                if (!conversations.map { it.thread_id }.contains(cachedConversation.thread_id)) {
                    conversationsDB.delete(cachedConversation.id!!)
                }
            }

            cachedConversations.forEach { cachedConversation ->
                val conv = conversations.firstOrNull { it.thread_id == cachedConversation.thread_id && it.getStringToCompare() != cachedConversation.getStringToCompare() }
                if (conv != null) {
                    conversationsDB.insertOrUpdate(conv)
                }
            }
        }
    }

    private fun setupConversations(conversations: ArrayList<Conversation>) {
        val hasConversations = conversations.isNotEmpty()
        conversations_list.beVisibleIf(hasConversations)
        no_conversations_placeholder.beVisibleIf(!hasConversations)
        no_conversations_placeholder_2.beVisibleIf(!hasConversations)

        val currAdapter = conversations_list.adapter
        if (currAdapter == null) {
            ConversationsAdapter(
                this,
                conversations,
                conversations_list,
                conversations_fastscroller
            ) {
                Intent(
                    this,
                    ThreadActivity::class.java
                ).apply {
                    putExtra(THREAD_ID, (it as Conversation).thread_id)
                    putExtra(THREAD_TITLE, it.title)
                    startActivity(this)
                }
            }.apply {
                conversations_list.adapter = this
            }
        } else {
            try {
                (currAdapter as ConversationsAdapter).updateConversations(conversations)
            } catch (ignored: Exception) {
            }
        }
    }


    private fun launchScheduled() {
        startActivity(Intent(applicationContext, ScheduledActivity::class.java))
    }

    private fun launchNewConversation() {
        Intent(this, NewConversationActivity::class.java).apply {
            startActivity(this)
        }
    }

    @SuppressLint("NewApi")
    private fun checkShortcut() {
        val appIconColor = config.appIconColor
        if (isNougatMR1Plus() && config.lastHandledShortcutColor != appIconColor) {
            val newConversation = getCreateNewContactShortcut(appIconColor)

            val manager = getSystemService(ShortcutManager::class.java)
            try {
                manager.dynamicShortcuts = Arrays.asList(newConversation)
                config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getCreateNewContactShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.new_conversation)
        val drawable = resources.getDrawable(R.drawable.shortcut_plus)
        (drawable as LayerDrawable).findDrawableByLayerId(R.id.shortcut_plus_background).applyColorFilter(appIconColor)
        val bmp = drawable.convertToBitmap()

        val intent = Intent(this, NewConversationActivity::class.java)
        intent.action = Intent.ACTION_VIEW
        return ShortcutInfo.Builder(this, "new_conversation")
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(bmp))
            .setIntent(intent)
            .build()
    }

    private fun launchSettings() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        val licenses = LICENSE_EVENT_BUS or LICENSE_SMS_MMS or LICENSE_INDICATOR_FAST_SCROLL

        val faqItems = arrayListOf(
            FAQItem(R.string.faq_2_title_commons, R.string.faq_2_text_commons),
            FAQItem(R.string.faq_6_title_commons, R.string.faq_6_text_commons),
            FAQItem(R.string.faq_9_title_commons, R.string.faq_9_text_commons)
        )

        startAboutActivity(R.string.app_name, licenses, BuildConfig.VERSION_NAME, faqItems, true)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshMessages(event: Events.RefreshMessages) {
        initMessenger()
    }
}