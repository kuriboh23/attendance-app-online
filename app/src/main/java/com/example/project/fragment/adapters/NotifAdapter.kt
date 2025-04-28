package com.example.project.fragment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.Notif
import com.example.project.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotifAdapter(private val notifList: List<Notif>) :
    RecyclerView.Adapter<NotifAdapter.NotifViewHolder>() {

    inner class NotifViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotifViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        val notif = notifList[position]

        holder.binding.apply {
//            tvTitle.text = notif.title
            tvMessage.text = notif.message
            tvTime.text = formatTimeAgo(notif.timestamp)

            // Optional: Change icon based on type
            when (notif.type) {
                "leave_approved" -> ivIcon.setImageResource(R.drawable.approve_v0)
                "leave_rejected" -> ivIcon.setImageResource(R.drawable.reject_v0) // example rejected icon
                else -> ivIcon.setImageResource(R.drawable.info)   // default notification icon
            }
        }
    }

    override fun getItemCount(): Int = notifList.size

    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            days == 1L -> "Yesterday"
            days < 6 -> "$days days ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
