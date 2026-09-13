package com.nextos.launcher.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextos.launcher.R
import com.nextos.launcher.cache.IconCache
import com.nextos.launcher.data.AppCategory
import com.nextos.launcher.data.AppEntry
import com.nextos.launcher.data.LauncherPreferences

data class LibraryGroup(val title: String, val apps: List<AppEntry>)

class LibraryAdapter(
    private val prefs: LauncherPreferences,
    private val onApp: (AppEntry, View) -> Unit,
    private val onAppLong: (AppEntry) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.Holder>() {

    var groups: List<LibraryGroup> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.categoryTitle)
        val grid: RecyclerView = v.findViewById(R.id.categoryGrid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_library_category, parent, false)
        return Holder(v)
    }

    override fun getItemCount() = groups.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val g = groups[position]
        holder.title.text = g.title
        holder.grid.layoutManager = GridLayoutManager(holder.itemView.context, 4)
        holder.grid.adapter = MiniAppAdapter(g.apps, prefs, onApp, onAppLong)
        holder.grid.isNestedScrollingEnabled = false
    }
}

class MiniAppAdapter(
    private val apps: List<AppEntry>,
    private val prefs: LauncherPreferences,
    private val onApp: (AppEntry, View) -> Unit,
    private val onAppLong: (AppEntry) -> Unit
) : RecyclerView.Adapter<MiniAppAdapter.Holder>() {

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val image: ImageView = v.findViewById(R.id.iconImage)
        val label: TextView = v.findViewById(R.id.iconLabel)
        val lock: ImageView = v.findViewById(R.id.lockBadge)
        val remove: TextView = v.findViewById(R.id.removeBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app_icon, parent, false)
        return Holder(v)
    }

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = apps[position]
        holder.remove.visibility = View.GONE
        holder.lock.visibility = if (app.locked) View.VISIBLE else View.GONE
        holder.image.setImageDrawable(
            IconCache.get(holder.itemView.context).load(app.packageName, app.customPlate)
        )
        holder.label.text = if (prefs.labels()) app.shortLabel else ""
        holder.itemView.setOnClickListener { onApp(app, holder.image) }
        holder.itemView.setOnLongClickListener {
            onAppLong(app)
            true
        }
    }
}

object LibraryBuilder {
    fun build(
        context: android.content.Context,
        apps: List<AppEntry>,
        recent: List<AppEntry>
    ): List<LibraryGroup> {
        val out = mutableListOf<LibraryGroup>()
        if (recent.isNotEmpty()) {
            out += LibraryGroup(context.getString(R.string.recent), recent)
        }
        val order = listOf(
            AppCategory.SOCIAL to R.string.social,
            AppCategory.CREATIVITY to R.string.creativity,
            AppCategory.ENTERTAINMENT to R.string.entertainment,
            AppCategory.INFORMATION to R.string.information,
            AppCategory.UTILITIES to R.string.utilities,
            AppCategory.SYSTEM to R.string.system,
            AppCategory.OTHER to R.string.other
        )
        for ((cat, res) in order) {
            val slice = apps.filter { it.category == cat }
            if (slice.isNotEmpty()) out += LibraryGroup(context.getString(res), slice)
        }
        return out
    }
}
