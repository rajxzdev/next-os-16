package com.nextos.launcher.ui.widgets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nextos.launcher.R
import com.nextos.launcher.data.WidgetKind

class WidgetPickerAdapter(
    private val onPick: (WidgetKind) -> Unit
) : RecyclerView.Adapter<WidgetPickerAdapter.Holder>() {

    private val items = listOf(
        Triple(WidgetKind.CLOCK, R.drawable.ic_clock, R.string.clock_widget),
        Triple(WidgetKind.WEATHER, R.drawable.ic_weather, R.string.weather_widget),
        Triple(WidgetKind.CALENDAR, R.drawable.ic_calendar, R.string.calendar_widget),
        Triple(WidgetKind.MUSIC, R.drawable.ic_music, R.string.music_widget),
        Triple(WidgetKind.BATTERY, R.drawable.ic_battery, R.string.battery_widget)
    )

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.pickIcon)
        val label: TextView = v.findViewById(R.id.pickLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_widget_pick, parent, false)
        return Holder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val (kind, icon, label) = items[position]
        holder.icon.setImageResource(icon)
        holder.label.setText(label)
        holder.itemView.setOnClickListener { onPick(kind) }
    }
}
