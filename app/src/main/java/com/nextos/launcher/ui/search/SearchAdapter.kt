package com.nextos.launcher.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nextos.launcher.R
import com.nextos.launcher.data.SearchHit

class SearchAdapter(
    private val onHit: (SearchHit, View) -> Unit
) : RecyclerView.Adapter<SearchAdapter.Holder>() {

    var hits: List<SearchHit> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.searchIcon)
        val label: TextView = v.findViewById(R.id.searchLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_search_row, parent, false)
        return Holder(v)
    }

    override fun getItemCount() = hits.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val h = hits[position]
        holder.icon.setImageDrawable(h.icon)
        holder.label.text = h.entry.label
        holder.itemView.setOnClickListener { onHit(h, holder.icon) }
    }
}
