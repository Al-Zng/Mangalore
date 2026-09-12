package com.mangalore.app

import android.app.Application
import com.mangalore.app.localization.LocalizationManager

class MangaloreApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalizationManager.init(this)
    }
}
