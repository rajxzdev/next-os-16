package com.nextos.launcher

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.nextos.launcher.cache.IconCache
import com.nextos.launcher.data.LauncherPreferences

class NextOSApplication : Application() {

    lateinit var prefs: LauncherPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = LauncherPreferences(this)
        applyDarkMode(prefs.darkMode())
        IconCache.get(this)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        IconCache.get(this).clear()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            IconCache.get(this).clear()
        }
    }

    companion object {
        lateinit var instance: NextOSApplication
            private set

        fun applyDarkMode(mode: Int) {
            val night = when (mode) {
                LauncherPreferences.DARK_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                LauncherPreferences.DARK_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(night)
        }
    }
}
