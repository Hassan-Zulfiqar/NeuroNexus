package com.example.neuronexus.patient.model

// 1. The Container for a Doctor's Week
// Key: DayOfWeek (1=Mon ... 7=Sun)
data class DoctorSchedule(
    val weeklyAvailability: Map<Int, List<TimeRange>> = emptyMap()
)

data class TimeRange(
    val startMinute: Int,
    val endMinute: Int
)

data class TimeSlot(
    val timeLabel: String,
    val startMinute: Int,
    var isAvailable: Boolean = true,
    var isSelected: Boolean = false
)