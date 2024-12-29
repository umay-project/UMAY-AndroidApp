package com.example.rescueapp.ui.controller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rescueapp.R
import com.example.rescueapp.ui.models.DebrisClusterItem

class ClusterDetailsAdapter(
    private val items: List<DebrisClusterItem>,
    private val onPlayAudio: (String) -> Unit,
    private val onReportAudio: (String) -> Unit
) : RecyclerView.Adapter<ClusterDetailsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        val playButton: Button = itemView.findViewById(R.id.playButton)
        val reportButton: Button = itemView.findViewById(R.id.reportButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cluster_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.titleTextView.text = item.clusterTitle
        holder.timestampTextView.text = item.clusterSnippet
        holder.playButton.setOnClickListener {
            onPlayAudio(item.audioFileName)
        }

        holder.reportButton.setOnClickListener {
            onReportAudio(item.audioFileName)
        }
    }

    override fun getItemCount(): Int = items.size
}
