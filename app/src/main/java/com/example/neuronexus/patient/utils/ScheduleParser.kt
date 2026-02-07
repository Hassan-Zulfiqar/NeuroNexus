package com.example.neuronexus.patient.utils

import com.example.neuronexus.patient.model.DoctorSchedule
import com.example.neuronexus.patient.model.TimeRange
import java.util.Locale

object ScheduleParser {

    // Day mapping: Mon=1 ... Sun=7
    private val DAY_MAP = mapOf(
        "mon" to 1, "tue" to 2, "wed" to 3, "thu" to 4, "fri" to 5, "sat" to 6, "sun" to 7,
        "monday" to 1, "tuesday" to 2, "wednesday" to 3, "thursday" to 4, "friday" to 5, "saturday" to 6, "sunday" to 7
    )

    /**
     * Parses a raw schedule string into a structured DoctorSchedule.
     * Example Input: "Mon-Wed: 5pm-9pm Sat: 10am-2pm"
     */
    fun parseSchedule(rawSchedule: String): DoctorSchedule {
        if (rawSchedule.isBlank()) return DoctorSchedule()

        val weeklyMap = mutableMapOf<Int, MutableList<TimeRange>>()
        val segmentRegex = Regex("([a-zA-Z,\\s\\-]+):?\\s*(\\d{1,2}(?::\\d{2})?\\s*[apAP]?[mM]?\\s*-\\s*\\d{1,2}(?::\\d{2})?\\s*[apAP]?[mM]?)")
        val matches = segmentRegex.findAll(rawSchedule)

        for (match in matches) {
            val dayPart = match.groupValues[1].trim()
            val timePart = match.groupValues[2].trim()

            val days = parseDayPart(dayPart)
            val timeRange = parseTimePart(timePart)

            if (days.isNotEmpty() && timeRange != null) {
                for (day in days) {
                    val ranges = weeklyMap.getOrPut(day) { mutableListOf() }
                    ranges.add(timeRange)
                }
            }
        }

        return DoctorSchedule(weeklyMap)
    }

    // Helper: "Mon-Wed" -> [1, 2, 3]
    private fun parseDayPart(dayPart: String): List<Int> {
        val days = mutableSetOf<Int>()
        val lower = dayPart.lowercase(Locale.ROOT)

        if (lower.contains("-")) {
            // Range: "Mon-Wed"
            val parts = lower.split("-")
            if (parts.size == 2) {
                val start = getDayIndex(parts[0].trim())
                val end = getDayIndex(parts[1].trim())
                if (start != -1 && end != -1) {
                    if (start <= end) {
                        for (i in start..end) days.add(i)
                    } else {
                        // Wrap around (e.g. Fri-Mon -> 5,6,7,1)
                        for (i in start..7) days.add(i)
                        for (i in 1..end) days.add(i)
                    }
                }
            }
        } else if (lower.contains(",")) {
            // List: "Mon, Wed, Fri"
            lower.split(",").forEach {
                val idx = getDayIndex(it.trim())
                if (idx != -1) days.add(idx)
            }
        } else {
            // Single Day: "Mon"
            val idx = getDayIndex(lower.trim())
            if (idx != -1) days.add(idx)
        }
        return days.toList()
    }

    // Helper: "5pm-9pm" -> TimeRange(1020, 1260)
    private fun parseTimePart(timePart: String): TimeRange? {
        val parts = timePart.split("-")
        if (parts.size != 2) return null

        val startMin = parseTime(parts[0].trim())
        val endMin = parseTime(parts[1].trim())

        return if (startMin != -1 && endMin != -1) {
            TimeRange(startMin, endMin)
        } else null
    }

    // Helper: "5pm" -> 1020 minutes
    private fun parseTime(rawTime: String): Int {
        val time = rawTime.lowercase(Locale.ROOT).replace(" ", "")

        // Handle "5pm" or "05:00pm"
        val isPm = time.contains("pm")
        val isAm = time.contains("am")

        val cleanTime = time.replace("am", "").replace("pm", "")
        val timeParts = cleanTime.split(":")

        var hour = timeParts[0].toIntOrNull() ?: return -1
        val minute = if (timeParts.size > 1) timeParts[1].toIntOrNull() ?: 0 else 0

        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0 // 12am is 00:00

        return (hour * 60) + minute
    }

    private fun getDayIndex(dayStr: String): Int {
        // Find key that starts with the input string (e.g. "mon" matches "monday")
        return DAY_MAP.entries.firstOrNull { it.key.startsWith(dayStr) || dayStr.startsWith(it.key) }?.value ?: -1
    }
}