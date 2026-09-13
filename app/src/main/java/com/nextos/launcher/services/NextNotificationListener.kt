package com.nextos.launcher.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nextos.launcher.ui.island.NextIslandBus

class NextNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.isOngoing && sbn.notification.extras.getCharSequence("android.title") == null) {
            return
        }
        NextIslandBus.fromNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val remaining = try {
            activeNotifications
        } catch (_: Exception) {
            emptyArray()
        }
        if (remaining.isNullOrEmpty()) {
            NextIslandBus.idle()
        } else {
            NextIslandBus.fromNotification(remaining.last())
        }
    }
}
