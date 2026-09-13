package com.nextos.launcher.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.view.View
import com.nextos.launcher.R
import com.nextos.launcher.data.AppLockStore
import com.nextos.launcher.ui.lock.AppLockGateActivity

object AppLauncher {

    fun launch(context: Context, intent: Intent?, packageName: String, origin: View? = null) {
        val store = AppLockStore(context)
        if (store.needsGate(packageName)) {
            val gate = Intent(context, AppLockGateActivity::class.java)
                .putExtra(AppLockGateActivity.EXTRA_PACKAGE, packageName)
                .putExtra(AppLockGateActivity.EXTRA_LAUNCH, intent)
            if (context !is android.app.Activity) {
                gate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(gate)
            return
        }
        if (intent == null) return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            if (origin != null) {
                val opts = ActivityOptions.makeScaleUpAnimation(
                    origin, 0, 0, origin.width, origin.height
                )
                context.startActivity(intent, opts.toBundle())
            } else {
                context.startActivity(intent)
            }
        } catch (_: Exception) {
            try {
                val launch = context.packageManager.getLaunchIntentForPackage(packageName)
                launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (launch != null) context.startActivity(launch)
            } catch (_: Exception) {
            }
        }
    }

    fun openSettings(context: Context) {
        context.startActivity(
            Intent(context, com.nextos.launcher.ui.settings.SettingsActivity::class.java)
        )
    }

    fun elasticOverride(): Int = R.anim.next_open_enter
}
