package com.terminuslabs.smsmessenger.activities

import android.annotation.TargetApi
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.textfield.TextInputEditText
import com.simplemobiletools.commons.activities.ManageBlockedNumbersActivity
import com.simplemobiletools.commons.dialogs.ChangeDateTimeFormatDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getBlockedNumbers
import com.simplemobiletools.commons.extensions.getFontSizeText
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import com.terminuslabs.smsmessenger.R
import com.terminuslabs.smsmessenger.extensions.config
import com.terminuslabs.smsmessenger.helpers.refreshMessages
import com.terminuslabs.smsmessenger.sync.ServiceBuilder
import com.terminuslabs.smsmessenger.works.DownloadScheduledMessagesWorker
import com.terminuslabs.smsmessenger.works.SendScheduledSMSWorker
import kotlinx.android.synthetic.main.activity_settings.*
import java.util.*


class SettingsActivity : SimpleActivity() {
    private var blockedNumbersAtPause = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onResume() {
        super.onResume()

        setupServerConfig()
        setupCustomizeColors()
        setupUseEnglish()
        setupManageBlockedNumbers()
        setupChangeDateTimeFormat()
        setupFontSize()
        setupShowCharacterCounter()
        updateTextColors(settings_scrollview)

        if (blockedNumbersAtPause != -1 && blockedNumbersAtPause != getBlockedNumbers().hashCode()) {
            refreshMessages()
        }
    }

    override fun onPause() {
        super.onPause()
        blockedNumbersAtPause = getBlockedNumbers().hashCode()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setupServerConfig() {
        settings_change_server_config_holder.setOnClickListener {
            startServerConfig()
        }
    }


    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf(config.wasUseEnglishToggled || Locale.getDefault().language != "en")
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            System.exit(0)
        }
    }

    // support for device-wise blocking came on Android 7, rely only on that
    @TargetApi(Build.VERSION_CODES.N)
    private fun setupManageBlockedNumbers() {
        settings_manage_blocked_numbers_holder.beVisibleIf(isNougatPlus())
        settings_manage_blocked_numbers_holder.setOnClickListener {
            startActivity(Intent(this, ManageBlockedNumbersActivity::class.java))
        }
    }

    private fun setupChangeDateTimeFormat() {
        settings_change_date_time_format_holder.setOnClickListener {
            ChangeDateTimeFormatDialog(this) {
                refreshMessages()
            }
        }
    }

    private fun setupFontSize() {
        settings_font_size.text = getFontSizeText()
        settings_font_size_holder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_SMALL, getString(R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, getString(R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, getString(R.string.large)),
                RadioItem(FONT_SIZE_EXTRA_LARGE, getString(R.string.extra_large)))

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                settings_font_size.text = getFontSizeText()
            }
        }
    }

    private fun setupShowCharacterCounter() {
        settings_show_character_counter.isChecked = config.showCharacterCounter
        settings_show_character_counter_holder.setOnClickListener {
            settings_show_character_counter.toggle()
            config.showCharacterCounter = settings_show_character_counter.isChecked
        }
    }


    private fun startServerConfig(){
       val factory = LayoutInflater.from(this)
        val textEntryView: View = factory.inflate(R.layout.alert_add_server, null)


        val input1 = textEntryView.findViewById<TextInputEditText>(R.id.urlServer)
        val input2 = textEntryView.findViewById<TextInputEditText>(R.id.passwrodServer)

        input1.setText(ServiceBuilder.getServerUrl(this), TextView.BufferType.EDITABLE)
        input2.setText(ServiceBuilder.getAuthHeader(this), TextView.BufferType.EDITABLE)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.server_config_desc))
            .setView(textEntryView)
            .setPositiveButton("Save",
                DialogInterface.OnClickListener { dialog, whichButton ->
                    ServiceBuilder.setUrl(this, input1.text.toString())
                    ServiceBuilder.setAuthHeader(this, input2.text.toString())
                    scheduleWorksNow()
                })
            .setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialog, whichButton -> })
            .show()
    }



    fun scheduleWorksNow(){
        val workDownloadSms = OneTimeWorkRequestBuilder<DownloadScheduledMessagesWorker>().build()
        val workSendSMS = OneTimeWorkRequestBuilder<SendScheduledSMSWorker>().build()
        WorkManager.getInstance (this).enqueue(Arrays.asList(workDownloadSms, workSendSMS))
    }

}
