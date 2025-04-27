package com.example.project.fragment.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.Leave
import com.example.project.data.User

class LeaveAdapter( private var leaveList: List<LeaveWithUser>,
    private val onLeaveClick: (LeaveWithUser) -> Unit
) : RecyclerView.Adapter<LeaveAdapter.LeaveViewHolder>() {


    class LeaveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFullName: TextView = itemView.findViewById(R.id.tvFullName)
        val tvLeaveType: TextView = itemView.findViewById(R.id.tvType)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaveViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leave_request, parent, false)
        return LeaveViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LeaveViewHolder, position: Int) {
        val leave = leaveList[position]
        val user = leave.user
        holder.tvFullName.text = if (user != null) {
            "${user.lastName} ${user.name}"
        } else {
            "Unknown"
        }
        holder.tvLeaveType.text = "${leave.leave.type}"
        holder.tvStatus.text = "${leave.leave.status}"

        val context = holder.itemView.context
        val statusColor = when (leave.leave.status.lowercase()) {
            "approved" -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
            "rejected" -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
            "pending" -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(context, android.R.color.black)
        }
        holder.tvStatus.setTextColor(statusColor)

        holder.itemView.setOnClickListener {
            onLeaveClick(leave)
        }
    }

    override fun getItemCount(): Int {
        return leaveList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newLeaveList: List<LeaveWithUser>) {
        leaveList = newLeaveList
        notifyDataSetChanged()

    }

}
