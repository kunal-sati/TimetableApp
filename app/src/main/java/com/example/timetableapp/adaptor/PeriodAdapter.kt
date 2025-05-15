package com.example.timetableapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.timetableapp.R
import com.example.timetableapp.data.Period

class PeriodAdapter(private val onItemClick: (Period) -> Unit) :
    ListAdapter<Period, PeriodAdapter.PeriodViewHolder>(PeriodDiffCallback()) {

    class PeriodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSubject: TextView = itemView.findViewById(R.id.tvSubject)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvNotes: TextView = itemView.findViewById(R.id.tvNotes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeriodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_period, parent, false)
        return PeriodViewHolder(view)
    }

    override fun onBindViewHolder(holder: PeriodViewHolder, position: Int) {
        val period = getItem(position)
        holder.tvSubject.text = period.subject
        holder.tvTime.text = "${period.startTime} - ${period.endTime}"
        holder.tvLocation.text = period.location

        if (period.notes.isNotEmpty()) {
            holder.tvNotes.visibility = View.VISIBLE
            holder.tvNotes.text = period.notes
        } else {
            holder.tvNotes.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(period)
        }
    }

    private class PeriodDiffCallback : DiffUtil.ItemCallback<Period>() {
        override fun areItemsTheSame(oldItem: Period, newItem: Period): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Period, newItem: Period): Boolean {
            return oldItem == newItem
        }
    }
}
