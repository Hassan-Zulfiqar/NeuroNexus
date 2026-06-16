package com.example.neuronexus.common.workers

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.neuronexus.common.utils.NotificationHelper
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val REMINDER_LEAD_TIME_MS = 30L * 60 * 1000
    private const val ONE_DAY_MS = 24L * 60 * 60 * 1000

    fun scheduleAppointmentReminder(
        context: Context,
        bookingId: String,
        exactTimeInMillis: Long,
        title: String,
        message: String
    ) {
        if (bookingId.isBlank() || exactTimeInMillis <= 0L) return

        val reminderTime = exactTimeInMillis - REMINDER_LEAD_TIME_MS
        val delay = reminderTime - System.currentTimeMillis()

        if (delay <= 0L) return

        val data = workDataOf(
            AppointmentReminderWorker.KEY_TITLE to title,
            AppointmentReminderWorker.KEY_MESSAGE to message,
            AppointmentReminderWorker.KEY_BOOKING_ID to bookingId
        )

        val request = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(bookingId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelAppointmentReminder(context: Context, bookingId: String) {
        if (bookingId.isBlank()) return
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(bookingId))
    }

    fun scheduleInstallmentReminder(
        context: Context,
        bookingId: String,
        installmentNumber: Int,
        dueDateMillis: Long,
        title: String,
        message: String
    ) {
        if (bookingId.isBlank() || dueDateMillis <= 0L) return

        val reminderTime = dueDateMillis - ONE_DAY_MS
        val delay = reminderTime - System.currentTimeMillis()
        if (delay <= 0L) return

        val data = workDataOf(
            AppointmentReminderWorker.KEY_TITLE to title,
            AppointmentReminderWorker.KEY_MESSAGE to message,
            AppointmentReminderWorker.KEY_BOOKING_ID to "${bookingId}_inst$installmentNumber",
            AppointmentReminderWorker.KEY_CHANNEL_ID to NotificationHelper.CHANNEL_PAYMENT_REMINDERS
        )

        val request = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            installmentWorkName(bookingId, installmentNumber),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelInstallmentReminders(context: Context, bookingId: String, maxInstallments: Int) {
        if (bookingId.isBlank() || maxInstallments < 2) return
        for (i in 2..maxInstallments) {
            WorkManager.getInstance(context).cancelUniqueWork(installmentWorkName(bookingId, i))
        }
    }

    private fun uniqueWorkName(bookingId: String) = "appointment_reminder_$bookingId"

    private fun installmentWorkName(bookingId: String, installmentNumber: Int) =
        "installment_reminder_${bookingId}_$installmentNumber"
}
