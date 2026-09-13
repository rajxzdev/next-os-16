package com.nextos.launcher.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.nextos.launcher.R
import com.nextos.launcher.cache.IconCache
import com.nextos.launcher.data.AppEntry

class AppToggleAdapter(
    private val apps: List<AppEntry>,
    private val checked: (AppEntry) -> Boolean,
    private val onToggle: (AppEntry, Boolean) -> Unit,
    private val editableName: Boolean = false,
    private val onRename: ((AppEntry, String) -> Unit)? = null,
    private val onIcon: ((AppEntry) -> Unit)? = null
) : RecyclerView.Adapter<AppToggleAdapter.Holder>() {

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.rowIcon)
        val name: EditText = v.findViewById(R.id.rowName)
        val sw: MaterialSwitch = v.findViewById(R.id.rowSwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app_toggle, parent, false)
        return Holder(v)
    }

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(
            IconCache.get(holder.itemView.context).load(app.packageName, app.customPlate)
        )
        holder.name.setText(app.label)
        holder.name.isEnabled = editableName
        holder.sw.setOnCheckedChangeListener(null)
        holder.sw.isChecked = checked(app)
        holder.sw.setOnCheckedChangeListener { _, on -> onToggle(app, on) }
        holder.name.setOnFocusChangeListener { _, has ->
            if (!has && editableName) onRename?.invoke(app, holder.name.text.toString())
        }
        holder.icon.setOnClickListener { onIcon?.invoke(app) }
    }
}
