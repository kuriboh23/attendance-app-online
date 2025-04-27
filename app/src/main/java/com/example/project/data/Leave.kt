package com.example.project.data

data class Leave(
    val date: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val type: String = "",
    val note: String = "",
    var status: String = "",
    val attachmentUrl: String? = null
)
