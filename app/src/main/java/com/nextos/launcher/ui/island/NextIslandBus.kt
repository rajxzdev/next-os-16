package com.nextos.launcher.ui.island

import android.service.notification.StatusBarNotification
import java.util.concurrent.CopyOnWriteArrayList

data class IslandEvent(
    val title: String,
    val body: String,
    val packageName: String,
    val expanded: Boolean = false,
    val playing: Boolean = false
)

object NextIslandBus {
    private val listeners = CopyOnWriteArrayList<(IslandEvent) -> Unit>()
    @Volatile
    var last: IslandEvent? = null
        private set

    fun add(l: (IslandEvent) -> Unit) {
        listeners.add(l)
        last?.let { l(it) }
    }

    fun remove(l: (IslandEvent) -> Unit) {
        listeners.remove(l)
    }

    fun emit(event: IslandEvent) {
        last = event
        listeners.forEach { it(event) }
    }

    fun fromNotification(sbn: StatusBarNotification) {
        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: sbn.packageName
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        emit(IslandEvent(title, text, sbn.packageName, expanded = text.isNotBlank()))
    }

    fun idle() {
        emit(IslandEvent("Next Island", "", "com.nextos.launcher", expanded = false))
    }
}
