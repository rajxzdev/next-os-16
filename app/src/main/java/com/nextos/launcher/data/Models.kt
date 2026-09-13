package com.nextos.launcher.data

import android.content.Intent
import android.graphics.drawable.Drawable

data class AppEntry(
    val packageName: String,
    val activityName: String?,
    val label: String,
    val shortLabel: String,
    val launchIntent: Intent?,
    val lastUpdate: Long = 0L,
    val category: AppCategory = AppCategory.OTHER,
    val customPlate: Int? = null,
    val locked: Boolean = false,
    val hidden: Boolean = false,
    val isFolder: Boolean = false,
    val folderId: String? = null
)

enum class AppCategory {
    SUGGESTED, RECENT, SOCIAL, CREATIVITY, ENTERTAINMENT, INFORMATION, UTILITIES, SYSTEM, OTHER
}

enum class WidgetKind {
    CLOCK, WEATHER, CALENDAR, MUSIC, BATTERY
}

data class HomeCell(
    val type: CellType,
    val packageName: String? = null,
    val folderId: String? = null
)

enum class CellType { EMPTY, APP, FOLDER }

data class FolderModel(
    val id: String,
    var name: String,
    val packages: MutableList<String>
)

data class PageWidgets(
    val page: Int,
    val kinds: MutableList<WidgetKind>
)

data class SearchHit(
    val entry: AppEntry,
    val icon: Drawable?
)
