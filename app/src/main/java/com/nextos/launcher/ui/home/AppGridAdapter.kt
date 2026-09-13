package com.nextos.launcher.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nextos.launcher.R
import com.nextos.launcher.cache.IconCache
import com.nextos.launcher.data.AppCatalog
import com.nextos.launcher.data.CellType
import com.nextos.launcher.data.FolderModel
import com.nextos.launcher.data.HomeCell
import com.nextos.launcher.data.LauncherPreferences
import com.nextos.launcher.util.SpringMotion

class AppGridAdapter(
    private val catalog: AppCatalog,
    private val prefs: LauncherPreferences,
    private val folders: () -> Map<String, FolderModel>,
    private val onClick: (HomeCell, View) -> Unit,
    private val onLong: (HomeCell, Int, View) -> Unit,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.Holder>() {

    var cells: List<HomeCell> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var jiggle: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val root: View = v.findViewById(R.id.iconRoot)
        val image: ImageView = v.findViewById(R.id.iconImage)
        val label: TextView = v.findViewById(R.id.iconLabel)
        val lock: ImageView = v.findViewById(R.id.lockBadge)
        val remove: TextView = v.findViewById(R.id.removeBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app_icon, parent, false)
        return Holder(v)
    }

    override fun getItemCount(): Int = cells.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val cell = cells[position]
        JiggleController.stop(holder.root)
        if (cell.type == CellType.EMPTY) {
            holder.image.setImageDrawable(null)
            holder.label.text = ""
            holder.lock.visibility = View.GONE
            holder.remove.visibility = View.GONE
            holder.root.setOnClickListener(null)
            holder.root.setOnLongClickListener {
                onLong(cell, holder.bindingAdapterPosition, holder.root)
                true
            }
            return
        }
        val ctx = holder.itemView.context
        val cache = IconCache.get(ctx)
        if (cell.type == CellType.FOLDER) {
            val folder = folders()[cell.folderId]
            holder.image.setImageDrawable(
                cache.fromVector(R.drawable.ic_folder, R.drawable.plate_peach)
            )
            holder.label.text = if (prefs.labels()) folder?.name ?: ctx.getString(R.string.folder_name) else ""
            holder.lock.visibility = View.GONE
        } else {
            val app = catalog.byPackage(cell.packageName ?: "")
            val plate = app?.customPlate
            holder.image.setImageDrawable(cache.load(cell.packageName ?: "", plate))
            holder.label.text = if (prefs.labels()) app?.shortLabel ?: "" else ""
            holder.lock.visibility = if (app?.locked == true) View.VISIBLE else View.GONE
        }
        holder.label.visibility = if (prefs.labels()) View.VISIBLE else View.GONE
        holder.remove.visibility = if (jiggle) View.VISIBLE else View.GONE
        holder.root.setOnClickListener {
            SpringMotion.press(holder.image)
            holder.image.postDelayed({
                SpringMotion.release(holder.image)
                onClick(cell, holder.image)
            }, 90)
        }
        holder.root.setOnLongClickListener {
            onLong(cell, holder.bindingAdapterPosition, holder.root)
            true
        }
        holder.remove.setOnClickListener { onRemove(holder.bindingAdapterPosition) }
        if (jiggle) JiggleController.start(holder.root)
    }

    override fun onViewRecycled(holder: Holder) {
        JiggleController.stop(holder.root)
    }
}
