package com.example.project.fragment.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.fragment.adapters.UserWithUid

class TeamUserAdapter(private var teamUsers: List<UserWithUid>,
                      private val status: String,
                      private val onUserClick: (UserWithUid) -> Unit) :
    RecyclerView.Adapter<TeamUserAdapter.ViewHolder>() {

    private var fullUserList: List<UserWithUid> = teamUsers

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.tvUserName)
        val userStatus: TextView = itemView.findViewById(R.id.tvUserStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount() = teamUsers.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userWithUid = teamUsers[position]
        val user = userWithUid.user
        holder.userName.text = "${user.lastName} ${user.name}"
        holder.userStatus.text = status
        // Set click listener
        holder.itemView.setOnClickListener {
            onUserClick(userWithUid)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateUsers(newUsers: List<UserWithUid>) {
        fullUserList = newUsers
        teamUsers = newUsers
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filterUsers(query: String) {
        teamUsers = if (query.isEmpty()) {
            fullUserList
        } else {
            fullUserList.filter {
                val user = it.user
                val fullName = "${user.lastName} ${user.name}".lowercase()
                fullName.contains(query.lowercase().trim())
            }
        }
        notifyDataSetChanged()
    }
}

