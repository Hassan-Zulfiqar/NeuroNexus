package com.example.neuronexus.common.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.neuronexus.common.utils.NotificationHelper

class AppointmentReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Reminder"
        val message = inputData.getString(KEY_MESSAGE) ?: ""
        val bookingId = inputData.getString(KEY_BOOKING_ID) ?: ""
        val channelId = inputData.getString(KEY_CHANNEL_ID)
            ?: NotificationHelper.CHANNEL_APPOINTMENT_REMINDERS

        NotificationHelper.showNotification(
            context = applicationContext,
            channelId = channelId,
            notificationId = bookingId.hashCode(),
            title = title,
            message = message
        )

        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val KEY_BOOKING_ID = "bookingId"
        const val KEY_CHANNEL_ID = "channelId"
    }
}
