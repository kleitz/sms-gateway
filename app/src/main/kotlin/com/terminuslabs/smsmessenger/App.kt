package com.terminuslabs.smsmessenger

import android.app.Application
import com.simplemobiletools.commons.extensions.baseConfig
import com.simplemobiletools.commons.extensions.checkUseEnglish

class App : Application() {
    override fun onCreate() {
        baseConfig.wasAppRated = true

        super.onCreate()
        checkUseEnglish()
    }
}
