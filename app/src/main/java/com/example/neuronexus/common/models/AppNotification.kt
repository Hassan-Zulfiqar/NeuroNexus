package com.example.neuronexus.common.models

import com.google.firebase.database.PropertyName
import java.io.Serializable

data class AppNotification(
    val notificationId: String = "",
    val recipientUid: String = "",
    val senderName: String = "",
    val type: String = "",           // Determines meaning of referenceId
    val title: String = "",
    val message: String = "",
    val bookingId: String? = null,   // Primary link — present on most notifications
    val referenceId: String? = null, // Secondary link — meaning determined by type

    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,

    val createdAt: Long = System.currentTimeMillis()
) : Serializable

data class AppAnnouncement(
    val announcementId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "ANNOUNCEMENT",
    val createdAt: Long = 0L,
    val createdBy: String = "",
    val targetAudience: String = "all", // "all", "doctors", "patients", "labs"
    val isActive: Boolean = true
) : Serializable