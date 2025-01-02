package com.example.rescueapp.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rescueapp.R
import com.example.rescueapp.ui.models.DebrisSite
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class FalseTaggedAdapter(
    private val onPlayClick: (String) -> Unit,
    private val onMarkAsTrueClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<FalseTaggedAdapter.ViewHolder>() {

    private var entries = listOf<DebrisSite>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timestampText: TextView = view.findViewById(R.id.timestampText)
        val locationText: TextView = view.findViewById(R.id.locationText)
        val playButton: FloatingActionButton = view.findViewById(R.id.playButton)
        val markTrueButton: MaterialButton = view.findViewById(R.id.markTrueButton)
        val deleteButton: MaterialButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_false_tagged, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]

        holder.timestampText.text = formatTimestamp(entry.timestamp)
        holder.locationText.text = "Lat: ${entry.latitude}, Long: ${entry.longitude}"

        holder.playButton.setOnClickListener {
            onPlayClick(entry.audioFileName)
        }

        holder.markTrueButton.setOnClickListener {
            onMarkAsTrueClick(entry.audioFileName)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(entry.audioFileName)
        }
    }

    override fun getItemCount() = entries.size

    fun updateEntries(newEntries: List<DebrisSite>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    private fun formatTimestamp(timestamp: String): String {
        return try {
            val date = Date(timestamp.toLong())
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            "Invalid Timestamp"
        }
    }
}