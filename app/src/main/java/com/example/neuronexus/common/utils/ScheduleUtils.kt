package com.example.neuronexus.common.utils

import java.util.Locale

object ScheduleUtils {

    const val SCHEDULE_HINT = "Example: Mon-Fri: 8am-5pm or Mon-Fri: 8am-12pm, Sat: 9am-1pm"


    fun isValidSchedule(schedule: String): Boolean {
        return getScheduleValidationError(schedule) == null
    }

    /**
     * Validates the schedule and returns a specific, user-friendly error message if invalid.
     * Returns null if the schedule is fully valid.
     */
    fun getScheduleValidationError(schedule: String): String? {
        val trimmed = schedule.trim()
        if (trimmed.isEmpty()) {
            return "Schedule cannot be empty"
        }

        // Split multiple ranges safely (e.g., "Mon-Fri: 8am-12pm, Sat-Sun: 9am-1pm")
        // This regex splits ONLY at commas that immediately follow 'am' or 'pm' (case-insensitive).
        // This prevents accidentally splitting day lists like "Mon, Wed, Fri: 8am-12pm".
        val blocks = trimmed.split(Regex("(?i)(?<=[am|pm])\\s*,\\s*"))

        // Regex to match a single valid block: "Days: StartTime - EndTime"
        val blockRegex = Regex("^([a-zA-Z\\s,-]+):\\s*(\\d{1,2}(?::\\d{2})?\\s*[a-zA-Z]{2})\\s*-\\s*(\\d{1,2}(?::\\d{2})?\\s*[a-zA-Z]{2})$")

        for (block in blocks) {
            val matchResult = blockRegex.matchEntire(block.trim())
                ?: return "Invalid schedule format. $SCHEDULE_HINT"

            val startTimeStr = matchResult.groupValues[2]
            val endTimeStr = matchResult.groupValues[3]

            val startMins = parseTime(startTimeStr)
            val endMins = parseTime(endTimeStr)

            // If time parsing failed (e.g., invalid hour like 15am)
            if (startMins == null || endMins == null) {
                return "Invalid schedule format. $SCHEDULE_HINT"
            }

            if (startMins == endMins) {
                return "Start and end time cannot be the same"
            }

            if (endMins < startMins) {
                return "Invalid time range: end time must be after start time. Example: 8am-5pm not 5pm-8am"
            }
        }

        return null // All checks passed
    }

    /**
     * Converts a 12-hour formatted time string (e.g., "8am", "08:30 PM") into total minutes since midnight.
     * Returns null if parsing fails.
     */
    private fun parseTime(timeStr: String): Int? {
        val timeRegex = Regex("(?i)^(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)$")
        val match = timeRegex.matchEntire(timeStr.trim()) ?: return null

        var hours = match.groupValues[1].toIntOrNull() ?: return null
        val minutes = if (match.groupValues[2].isNotEmpty()) match.groupValues[2].toIntOrNull() ?: 0 else 0
        val period = match.groupValues[3].lowercase(Locale.getDefault())

        // Validate typical 12-hour clock constraints
        if (hours < 1 || hours > 12) return null
        if (minutes < 0 || minutes > 59) return null

        // Convert to 24-hour format logic
        if (period == "pm" && hours != 12) {
            hours += 12
        } else if (period == "am" && hours == 12) {
            hours = 0
        }

        return (hours * 60) + minutes
    }
}

