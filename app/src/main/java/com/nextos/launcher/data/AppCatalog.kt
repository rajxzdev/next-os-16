package com.nextos.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import com.nextos.launcher.util.CategoryHelper
import com.nextos.launcher.util.LabelShortener
import com.nextos.launcher.util.UsageStatsHelper

class AppCatalog(private val context: Context) {

    private val prefs = LauncherPreferences(context)
    private val lock = AppLockStore(context)
    private val pm = context.packageManager
    @Volatile private var cache: List<AppEntry>? = null

    fun invalidate() {
        cache = null
    }

    fun allLaunchable(includeHidden: Boolean = false): List<AppEntry> {
        cache?.let { cached ->
            return if (includeHidden) cached else cached.filter { !it.hidden }
        }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
        val hidden = prefs.hiddenPackages()
        val names = prefs.customNames()
        val plates = prefs.customPlates()
        val self = context.packageName
        val all = resolved.mapNotNull { ri ->
            val pkg = ri.activityInfo.packageName
            if (pkg == self) return@mapNotNull null
            val raw = names[pkg] ?: ri.loadLabel(pm).toString()
            val ai = ri.activityInfo.applicationInfo
            val launch = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(pkg, ri.activityInfo.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            AppEntry(
                packageName = pkg,
                activityName = ri.activityInfo.name,
                label = raw,
                shortLabel = LabelShortener.short(raw),
                launchIntent = launch,
                lastUpdate = ai.lastUpdateTime,
                category = CategoryHelper.of(pkg, raw, ai.flags),
                customPlate = plates[pkg],
                locked = lock.isLocked(pkg),
                hidden = hidden.contains(pkg)
            )
        }.sortedBy { it.label.lowercase() }
        cache = all
        return if (includeHidden) all else all.filter { !it.hidden }
    }

    fun byPackage(pkg: String): AppEntry? = allLaunchable(true).firstOrNull { it.packageName == pkg }

    fun recent(limit: Int = 8): List<AppEntry> {
        val map = allLaunchable().associateBy { it.packageName }
        return UsageStatsHelper.recentPackages(context, limit).mapNotNull { map[it] }
    }

    fun resolveDockDefaults(): List<String> {
        val candidates = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://")),
            Intent("android.media.action.IMAGE_CAPTURE")
        )
        val out = ArrayList<String>(4)
        for (i in candidates) {
            val ri = i.resolveActivity(pm) ?: continue
            if (!out.contains(ri.packageName) && ri.packageName != context.packageName) {
                out.add(ri.packageName)
            }
        }
        if (out.size < 4) {
            for (app in allLaunchable()) {
                if (!out.contains(app.packageName)) out.add(app.packageName)
                if (out.size >= 4) break
            }
        }
        return out.take(4)
    }
}
