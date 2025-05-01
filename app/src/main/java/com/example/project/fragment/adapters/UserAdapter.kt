package com.example.project.fragment.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.User

class UserAdapter(
    private var userList: List<UserWithStatus>,
    private val onUserClick: (String) -> Unit // Pass UID as string
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.tvUserName)
        val userStatus: TextView = itemView.findViewById(R.id.tvUserStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(itemView)
    }

    override fun getItemCount(): Int = userList.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val userWithStatus = userList[position]
        val user = userWithStatus.user
        holder.userName.text = "${user.lastName} ${user.name}"
        holder.userStatus.text = userWithStatus.status

        val context = holder.itemView.context
        val statusColor = when (userWithStatus.status.lowercase()) {
            "present" -> ContextCompat.getColor(context, R.color.mainColor)
            "absent" -> ContextCompat.getColor(context, R.color.status_rejected)
            "late" -> ContextCompat.getColor(context, R.color.status_pending)
            "rest" -> ContextCompat.getColor(context, R.color.status_approved)
            else -> ContextCompat.getColor(context, android.R.color.black)
        }
        holder.userStatus.setTextColor(statusColor)

        holder.itemView.setOnClickListener {
            onUserClick(userWithStatus.uid)  // Pass UID here
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateUsers(newUserList: List<UserWithStatus>) {
        userList = newUserList
        notifyDataSetChanged()
    }
}

