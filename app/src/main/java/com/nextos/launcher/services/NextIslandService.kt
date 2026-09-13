package com.nextos.launcher.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nextos.launcher.R
import com.nextos.launcher.ui.island.NextIslandBus

class NextIslandService : Service() {

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CH, "Next Island", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val n: Notification = NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.next_island))
            .setContentText(NextIslandBus.last?.title ?: getString(R.string.next_island))
            .setOngoing(true)
            .build()
        startForeground(16, n)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CH = "next_island"
    }
}
