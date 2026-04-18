package com.example.neuronexus.common.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuronexus.common.adapters.NotificationAdapter
import com.example.neuronexus.common.models.AppNotification
import com.example.neuronexus.common.viewmodel.NetworkViewModel
import com.example.neuronexus.databinding.ActivityNotificationsBinding
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.viewmodel.ext.android.viewModel

// FIXME: Verify these two imports match the exact location of your Dashboard Activities
import com.example.neuronexus.doctor.activities.DoctorDashboardActivity
import com.example.neuronexus.patient.activities.PatientDashboardActivity

class NotificationsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ACTION = "extra_action"
        const val EXTRA_BOOKING_ID = "extra_booking_id"

        const val ACTION_OPEN_BOOKING_DETAIL = "action_open_booking_detail"
        const val ACTION_OPEN_DOCTOR_BOOKING_DETAIL = "action_open_doctor_booking_detail"
        const val ACTION_OPEN_HISTORY = "action_open_history"
    }

    private var binding: ActivityNotificationsBinding? = null
    private val networkViewModel: NetworkViewModel by viewModel()

    private var notificationAdapter: NotificationAdapter? = null
    private var currentUid: String = ""
    private var userRole: String = "patients"
    private var currentNotificationList: List<AppNotification> = emptyList()
    private var currentAnnouncementList: List<AppNotification> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding?.root ?: return)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrEmpty()) {
            finish()
            return
        }
        currentUid = uid
        userRole = getUserRole()

        setupUI()
        setupObservers()
        startListeners()
    }

    private fun getUserRole(): String {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val role = prefs.getString("user_role", "patients") ?: "patients"
        return role
    }

    private fun setupUI() {
        val b = binding ?: return

        notificationAdapter = NotificationAdapter(
            notifications = mutableListOf(),
            onNotificationClick = { notification ->
                handleNotificationClick(notification)
            }
        )

        b.rvNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationAdapter
        }

        b.btnBack.setOnClickListener {
            if (userRole == "doctors") {
                startActivity(
                    Intent(
                        this@NotificationsActivity,
                        DoctorDashboardActivity::class.java
                    )
                )
                finish()
            } else {
                startActivity(
                    Intent(
                        this@NotificationsActivity,
                        PatientDashboardActivity::class.java
                    )
                )
                finish()
            }
        }
    }

    private fun setupObservers() {
        networkViewModel.notifications.observe(this) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                val list = result.getOrNull() ?: emptyList()
                mergeAndDisplay(
                    notifications = list,
                    announcements = currentAnnouncementList
                )
            } else {
                showEmptyState()
            }
        }

        networkViewModel.announcements.observe(this) { result ->
            result ?: return@observe
            if (result.isSuccess) {
                val announcementList = result.getOrNull() ?: emptyList()
                val converted = announcementList.map { announcement ->
                    AppNotification(
                        notificationId = announcement.announcementId,
                        type = "ANNOUNCEMENT",
                        title = announcement.title,
                        message = announcement.message,
                        createdAt = announcement.createdAt,
                        isRead = isAnnouncementRead(announcement.announcementId)
                    )
                }

                mergeAndDisplay(
                    notifications = currentNotificationList,
                    announcements = converted
                )
            }
        }

        networkViewModel.markReadResult.observe(this) { result ->
            result ?: return@observe
            if (result.isFailure) {
                Toast.makeText(
                    this,
                    result.exceptionOrNull()?.message ?: "Failed to mark as read",
                    Toast.LENGTH_SHORT
                ).show()
            }
            networkViewModel.resetMarkReadState()
        }
    }

    private fun startListeners() {
        if (currentUid.isEmpty()) return
        networkViewModel.startListeningToNotifications(currentUid)
        networkViewModel.startListeningToAnnouncements(userRole)
    }

    private fun mergeAndDisplay(
        notifications: List<AppNotification>,
        announcements: List<AppNotification>
    ) {
        currentNotificationList = notifications
        currentAnnouncementList = announcements

        val merged = (notifications + announcements).sortedByDescending { it.createdAt }
        val b = binding ?: return

        if (merged.isEmpty()) {
            b.rvNotifications.visibility = View.GONE
            b.tvEmptyState.visibility = View.VISIBLE
        } else {
            b.rvNotifications.visibility = View.VISIBLE
            b.tvEmptyState.visibility = View.GONE
            notificationAdapter?.updateList(merged)
        }
    }

    private fun showEmptyState() {
        val b = binding ?: return
        b.rvNotifications.visibility = View.GONE
        b.tvEmptyState.visibility = View.VISIBLE
    }

    private fun handleNotificationClick(notification: AppNotification) {
        // Mark as read first
        if (!notification.isRead) {
            if (notification.type == "ANNOUNCEMENT") {
                markAnnouncementAsRead(notification.notificationId)
                val updatedAnnouncements = currentAnnouncementList.map {
                    if (it.notificationId == notification.notificationId) it.copy(isRead = true) else it
                }
                mergeAndDisplay(currentNotificationList, updatedAnnouncements)
            } else {
                if (currentUid.isNotEmpty()) {
                    networkViewModel.markNotificationAsRead(
                        currentUid,
                        notification.notificationId
                    )
                }
            }
        }

        // Route navigation based on user role and notification type
        when (notification.type) {
            "ANNOUNCEMENT" -> {
                // No navigation — stay on notifications screen
                return
            }

            else -> {
                if (userRole == "doctors") {
                    handleDoctorNavigation(notification)
                } else {
                    handlePatientNavigation(notification)
                }
            }
        }
    }

    private fun handleDoctorNavigation(notification: AppNotification) {
        val destination = when (notification.type) {
            "NEW_BOOKING",
            "BOOKING_CANCELLED" -> ACTION_OPEN_DOCTOR_BOOKING_DETAIL

            "BOOKING_EXPIRED" -> ACTION_OPEN_HISTORY
            else -> ACTION_OPEN_DOCTOR_BOOKING_DETAIL
        }

        val intent = Intent(this, DoctorDashboardActivity::class.java).apply {
            putExtra(EXTRA_ACTION, destination)
            putExtra(EXTRA_BOOKING_ID, notification.bookingId ?: "")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun handlePatientNavigation(notification: AppNotification) {
        val bookingId = notification.bookingId
        if (bookingId.isNullOrEmpty()) {
            val intent = Intent(this, PatientDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            return
        }

        val intent = Intent(this, PatientDashboardActivity::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_OPEN_BOOKING_DETAIL)
            putExtra(EXTRA_BOOKING_ID, bookingId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun isAnnouncementRead(announcementId: String): Boolean {
        if (announcementId.isEmpty()) return false
        val prefs = getSharedPreferences("announcement_reads", Context.MODE_PRIVATE)
        return prefs.getBoolean(announcementId, false)
    }

    private fun markAnnouncementAsRead(announcementId: String) {
        if (announcementId.isEmpty()) return
        val prefs = getSharedPreferences("announcement_reads", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(announcementId, true).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
        networkViewModel.stopListeningToNotifications()
        networkViewModel.stopListeningToAnnouncements()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (userRole == "doctors") {
            startActivity(Intent(this@NotificationsActivity, DoctorDashboardActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this@NotificationsActivity, PatientDashboardActivity::class.java))
            finish()
        }

    }
}