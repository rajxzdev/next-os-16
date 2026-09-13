package com.nextos.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nextos.launcher.data.AppLockStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppLockStore(context).clearSession()
    }
}
