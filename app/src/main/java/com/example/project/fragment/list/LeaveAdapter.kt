package com.example.project.fragment.list

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.Leave
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class LeaveAdapter(
    private val onItemClick: (Leave) -> Unit
) : RecyclerView.Adapter<LeaveAdapter.LeaveViewHolder>() {

    private var leaveList: List<Leave> = emptyList()

    class LeaveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvLeaveDate)
        val tvType: TextView = view.findViewById(R.id.tvLeaveType)
        val tvStatus: TextView = view.findViewById(R.id.tvLeaveStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaveViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leave, parent, false)
        return LeaveViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: LeaveViewHolder, position: Int) {
        val leave = leaveList[position]
        holder.tvDate.text = getDayAbbreviation(leave.date)
        holder.tvType.text = leave.type
        holder.tvStatus.text = leave.status

        val statusColor = when (leave.status.lowercase()) {
            "approved" -> R.color.status_approved
            "rejected" -> R.color.status_rejected
            "pending" -> R.color.status_pending
            else -> R.color.black
        }

        holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, statusColor))

        holder.itemView.setOnClickListener { onItemClick(leave) }
    }

    override fun getItemCount(): Int = leaveList.size

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDayAbbreviation(dateString: String?): String {
        return try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)
            val date = LocalDate.parse(dateString, inputFormatter)
            val dayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
            date.format(dayFormatter)
        } catch (e: Exception) {
            "Invalid Date"
        }
    }

    fun setData(newLeaveList: List<Leave>) {
        leaveList = newLeaveList
        notifyDataSetChanged()
    }
}
