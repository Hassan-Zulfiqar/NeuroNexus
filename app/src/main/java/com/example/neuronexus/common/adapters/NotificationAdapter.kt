package com.example.neuronexus.common.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.R
import com.example.neuronexus.common.models.AppNotification
import com.example.neuronexus.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private var notifications: MutableList<AppNotification>,
    private val onNotificationClick: (AppNotification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        // Text Bindings
        holder.binding.tvNotificationTitle.text = notification.title
        holder.binding.tvNotificationMessage.text = notification.message
        holder.binding.tvNotificationDateTime.text = getRelativeTimeString(notification.createdAt)

        // Read/Unread Styling
        if (!notification.isRead) {
            holder.binding.viewUnreadIndicator.visibility = android.view.View.VISIBLE
            holder.binding.cardNotification.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.colorCard)
            )
        } else {
            holder.binding.viewUnreadIndicator.visibility = android.view.View.GONE
            holder.binding.cardNotification.setCardBackgroundColor(
                holder.itemView.context.getColor(android.R.color.white)
            )
        }

        // Icon Mapping (Using safely confirmed project drawables)
        val iconRes = when (notification.type) {
            "NEW_BOOKING"          -> R.drawable.calendar
            "BOOKING_CONFIRMED"    -> R.drawable.ic_check
            "BOOKING_REJECTED"     -> R.drawable.ic_close
            "APPOINTMENT_COMPLETED"-> R.drawable.ic_check
            "NO_SHOW"              -> R.drawable.ic_warning
            "BOOKING_CANCELLED"    -> R.drawable.ic_close
            "PRESCRIPTION_ADDED"   -> R.drawable.plus
            "LAB_BOOKING_CONFIRMED"-> R.drawable.ic_check
            "LAB_BOOKING_REJECTED" -> R.drawable.ic_close
            "LAB_REPORT_READY"     -> R.drawable.research
            "ANNOUNCEMENT"         -> R.drawable.info
            else                   -> R.drawable.notifications
        }
        holder.binding.imgNotificationIcon.setImageResource(iconRes)

        // Click Listener
        holder.itemView.setOnClickListener {
            onNotificationClick(notification)
        }
    }

    override fun getItemCount(): Int = notifications.size

    // Helper: Update List
    fun updateList(newNotifications: List<AppNotification>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }

    // Helper: Unread Count
    fun getUnreadCount(): Int {
        return notifications.count { !it.isRead }
    }

    // Helper: Timestamp Formatting
    private fun getRelativeTimeString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> "${diff / 60_000}m ago"
            diff < 86_400_000L -> "${diff / 3_600_000}h ago"
            diff < 604_800_000L -> "${diff / 86_400_000}d ago"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}