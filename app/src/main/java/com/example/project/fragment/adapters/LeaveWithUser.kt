package com.example.project.fragment.adapters

import com.example.project.data.Leave
import com.example.project.data.User

data class LeaveWithUser(
    val leaveKey: String,
    val uid: String,
    val user: User,
    val leave: Leave
)
