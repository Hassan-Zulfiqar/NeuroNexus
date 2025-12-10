package com.example.neuronexus.common.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.neuronexus.R
import com.example.neuronexus.common.models.NotificationItem
import com.example.neuronexus.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val notificationList: List<NotificationItem>,
    private val onItemClick: (NotificationItem) -> Unit
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
        val notification = notificationList[position]

        holder.binding.tvNotificationTitle.text = notification.title
        holder.binding.tvNotificationMessage.text = notification.message
        holder.binding.tvNotificationDateTime.text = notification.dateTime
        holder.binding.imgNotificationIcon.setImageResource(notification.iconResId)

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

        holder.itemView.setOnClickListener {
            onItemClick(notification)
        }
    }

    override fun getItemCount(): Int = notificationList.size
}

