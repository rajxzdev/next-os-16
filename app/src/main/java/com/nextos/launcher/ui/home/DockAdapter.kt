package com.nextos.launcher.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.nextos.launcher.R
import com.nextos.launcher.cache.IconCache
import com.nextos.launcher.data.AppCatalog
import com.nextos.launcher.util.SpringMotion

class DockAdapter(
    private val catalog: AppCatalog,
    private val onClick: (String, View) -> Unit,
    private val onLong: (String, Int) -> Unit
) : RecyclerView.Adapter<DockAdapter.Holder>() {

    var packages: List<String> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var jiggle = false

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.dockIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_dock_icon, parent, false)
        val w = parent.measuredWidth.coerceAtLeast(1) / packages.size.coerceAtLeast(4)
        v.layoutParams = RecyclerView.LayoutParams(w, ViewGroup.LayoutParams.MATCH_PARENT)
        return Holder(v)
    }

    override fun getItemCount(): Int = packages.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val pkg = packages[position]
        JiggleController.stop(holder.icon)
        if (pkg.isEmpty()) {
            holder.icon.setImageDrawable(null)
            holder.icon.setOnClickListener(null)
            return
        }
        val app = catalog.byPackage(pkg)
        holder.icon.setImageDrawable(
            IconCache.get(holder.itemView.context).load(pkg, app?.customPlate)
        )
        holder.icon.setOnClickListener {
            SpringMotion.press(holder.icon)
            holder.icon.postDelayed({
                SpringMotion.release(holder.icon)
                onClick(pkg, holder.icon)
            }, 80)
        }
        holder.icon.setOnLongClickListener {
            onLong(pkg, holder.bindingAdapterPosition)
            true
        }
        if (jiggle) JiggleController.start(holder.icon)
    }
}
