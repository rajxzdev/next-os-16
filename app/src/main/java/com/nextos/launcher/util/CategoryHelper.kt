package com.nextos.launcher.util

import android.content.pm.ApplicationInfo
import com.nextos.launcher.data.AppCategory

object CategoryHelper {

    private val social = listOf(
        "whatsapp", "telegram", "instagram", "facebook", "messenger", "twitter",
        "tiktok", "discord", "line", "snapchat", "wechat", "kakao", "signal"
    )
    private val creative = listOf(
        "camera", "gallery", "photos", "lightroom", "canva", "photoshop",
        "snapseed", "vsco", "capcut"
    )
    private val funApps = listOf(
        "youtube", "netflix", "spotify", "music", "game", "twitch", "vlc", "mxplayer"
    )
    private val info = listOf(
        "chrome", "browser", "maps", "news", "weather", "gmail", "outlook", "drive"
    )
    private val util = listOf(
        "file", "clock", "calendar", "notes", "calculator", "settings", "files", "keeper"
    )

    fun of(packageName: String, label: String, flags: Int): AppCategory {
        val blob = (packageName + " " + label).lowercase()
        if (flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
            (blob.contains("settings") || blob.contains("launcher"))
        ) return AppCategory.SYSTEM
        return when {
            social.any { blob.contains(it) } -> AppCategory.SOCIAL
            creative.any { blob.contains(it) } -> AppCategory.CREATIVITY
            funApps.any { blob.contains(it) } -> AppCategory.ENTERTAINMENT
            info.any { blob.contains(it) } -> AppCategory.INFORMATION
            util.any { blob.contains(it) } -> AppCategory.UTILITIES
            flags and ApplicationInfo.FLAG_SYSTEM != 0 -> AppCategory.SYSTEM
            else -> AppCategory.OTHER
        }
    }
}
