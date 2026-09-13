package com.nextos.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nextos.launcher.cache.IconCache

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.data?.schemeSpecificPart ?: return
        IconCache.get(context).evictPackage(pkg)
        context.sendBroadcast(
            Intent(ACTION_APPS_CHANGED).setPackage(context.packageName)
        )
    }

    companion object {
        const val ACTION_APPS_CHANGED = "com.nextos.launcher.APPS_CHANGED"
    }
}
