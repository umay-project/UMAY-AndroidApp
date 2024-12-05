package com.example.rescueapp.ui.controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rescueapp.R
import com.example.rescueapp.ui.models.DebrisSite


class DebrisSiteAdapter(private val debrisSites: List<DebrisSite>) :
    RecyclerView.Adapter<DebrisSiteAdapter.DebrisSiteViewHolder>() {

    class DebrisSiteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        val audioFileTextView: TextView = itemView.findViewById(R.id.audioFileTextView)
        val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebrisSiteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_debris_site, parent, false)
        return DebrisSiteViewHolder(view)
    }

    override fun onBindViewHolder(holder: DebrisSiteViewHolder, position: Int) {
        val debrisSite = debrisSites[position]
        holder.timestampTextView.text = "Timestamp: ${debrisSite.timestamp}"
        holder.audioFileTextView.text = "Audio File: ${debrisSite.audioFileName}"
        holder.locationTextView.text = "Location: ${debrisSite.latitude}, ${debrisSite.longitude}"
    }

    override fun getItemCount(): Int = debrisSites.size
}
