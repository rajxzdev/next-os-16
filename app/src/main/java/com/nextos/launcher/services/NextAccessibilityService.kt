package com.nextos.launcher.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.nextos.launcher.data.AppLockStore
import com.nextos.launcher.ui.island.IslandEvent
import com.nextos.launcher.ui.island.NextIslandBus
import com.nextos.launcher.ui.lock.AppLockGateActivity

class NextAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
        NextIslandBus.emit(
            IslandEvent("Next Island", "Akses aktif", packageName, expanded = true)
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val store = AppLockStore(this)
        if (store.needsGate(pkg)) {
            val gate = Intent(this, AppLockGateActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(AppLockGateActivity.EXTRA_PACKAGE, pkg)
            startActivity(gate)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: NextAccessibilityService? = null
    }
}
