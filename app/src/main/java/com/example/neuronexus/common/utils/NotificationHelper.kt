package com.example.neuronexus.common.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.neuronexus.R

object NotificationHelper {

    const val CHANNEL_APPOINTMENT_REMINDERS = "appointment_reminders"
    const val CHANNEL_PAYMENT_REMINDERS = "payment_reminders"
    const val CHANNEL_BOOKING_UPDATES = "booking_updates"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)

        val appointmentChannel = NotificationChannel(
            CHANNEL_APPOINTMENT_REMINDERS,
            "Appointment Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders before your scheduled appointments and lab tests"
        }

        val paymentChannel = NotificationChannel(
            CHANNEL_PAYMENT_REMINDERS,
            "Payment Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Installment payment due date reminders"
        }

        val bookingChannel = NotificationChannel(
            CHANNEL_BOOKING_UPDATES,
            "Booking Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Updates about your bookings and appointment requests"
        }

        manager?.createNotificationChannel(appointmentChannel)
        manager?.createNotificationChannel(paymentChannel)
        manager?.createNotificationChannel(bookingChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String
    ) {
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(notificationId, notification)
    }
}
