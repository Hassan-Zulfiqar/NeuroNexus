package com.example.neuronexus.common.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.R
import com.example.neuronexus.common.adapters.NotificationAdapter
import com.example.neuronexus.common.models.NotificationItem
import com.example.neuronexus.databinding.ActivityNotificationsBinding

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        val notificationList = getDummyNotifications()

        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        val adapter = NotificationAdapter(notificationList) { notification ->
        }
        binding.rvNotifications.adapter = adapter

        if (notificationList.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvNotifications.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvNotifications.visibility = View.VISIBLE
        }
    }

    private fun getDummyNotifications(): List<NotificationItem> {
        return listOf(
            NotificationItem(
                id = "1",
                title = "Appointment Reminder",
                message = "You have an appointment with Dr. Tahir tomorrow at 10:00 AM",
                dateTime = "Today, 2:30 PM",
                iconResId = R.drawable.calendar,
                isRead = false
            ),
            NotificationItem(
                id = "2",
                title = "Lab Results Ready",
                message = "Your blood test results are now available. Please check your reports.",
                dateTime = "Yesterday, 4:15 PM",
                iconResId = R.drawable.info,
                isRead = false
            ),
            NotificationItem(
                id = "3",
                title = "Prescription Update",
                message = "Dr. Ehsan has updated your prescription. Please review the changes.",
                dateTime = "2 days ago, 11:20 AM",
                iconResId = R.drawable.ic_badge,
                isRead = true
            ),
            NotificationItem(
                id = "4",
                title = "Payment Successful",
                message = "Your payment of Rs.150 for consultation has been processed successfully.",
                dateTime = "3 days ago, 3:45 PM",
                iconResId = R.drawable.coin,
                isRead = true
            ),
            NotificationItem(
                id = "6",
                title = "Appointment Confirmed",
                message = "Your appointment with Dr. Emily Davis on Dec 25, 2024 at 2:00 PM has been confirmed.",
                dateTime = "5 days ago, 1:30 PM",
                iconResId = R.drawable.calendar,
                isRead = true
            )
        )
    }
}

