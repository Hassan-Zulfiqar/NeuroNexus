package com.example.neuronexus.common.models

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val dateTime: String,
    val iconResId: Int,
    val isRead: Boolean = false
)

