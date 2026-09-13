package com.nextos.launcher.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextos.launcher.R
import com.nextos.launcher.data.AppCatalog
import com.nextos.launcher.data.FolderModel
import com.nextos.launcher.data.HomeCell
import com.nextos.launcher.data.HomeLayoutManager
import com.nextos.launcher.data.LauncherPreferences
import com.nextos.launcher.data.WidgetKind
import com.nextos.launcher.ui.widgets.OsWidgetFactory

class HomePagerAdapter(
    private val layout: HomeLayoutManager,
    private val catalog: AppCatalog,
    private val prefs: LauncherPreferences,
    private val onClick: (page: Int, cell: HomeCell, view: View) -> Unit,
    private val onLong: (page: Int, cell: HomeCell, index: Int, view: View) -> Unit,
    private val onRemove: (page: Int, index: Int) -> Unit
) : RecyclerView.Adapter<HomePagerAdapter.PageHolder>() {

    var jiggle = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class PageHolder(v: View) : RecyclerView.ViewHolder(v) {
        val widgets: LinearLayout = v.findViewById(R.id.widgetStack)
        val grid: RecyclerView = v.findViewById(R.id.iconGrid)
        var adapter: AppGridAdapter? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_home_page, parent, false)
        return PageHolder(v)
    }

    override fun getItemCount(): Int = layout.pages.size

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val page = position
        holder.widgets.removeAllViews()
        layout.widgetsOn(page).forEach { kind ->
            val w = OsWidgetFactory.inflate(holder.itemView.context, kind)
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = (8 * holder.itemView.resources.displayMetrics.density).toInt()
            holder.widgets.addView(w, lp)
            if (jiggle) {
                w.setOnLongClickListener {
                    layout.removeWidget(page, kind)
                    notifyItemChanged(page)
                    true
                }
            }
        }
        val gridAdapter = AppGridAdapter(
            catalog,
            prefs,
            { layout.folders },
            onClick = { cell, view -> onClick(page, cell, view) },
            onLong = { cell, index, view -> onLong(page, cell, index, view) },
            onRemove = { index -> onRemove(page, index) }
        )
        gridAdapter.cells = layout.pages[page]
        gridAdapter.jiggle = jiggle
        holder.adapter = gridAdapter
        holder.grid.layoutManager = GridLayoutManager(holder.itemView.context, layout.columns)
        holder.grid.adapter = gridAdapter
        holder.grid.isNestedScrollingEnabled = false
        holder.grid.itemAnimator = null
    }
}
