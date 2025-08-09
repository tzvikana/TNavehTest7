package com.drivenets.tnavehtest7.data

import java.time.LocalDateTime

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val location: String?,
    val organizer: String?,
    val meetingUrl: String?,
    val isOnlineMeeting: Boolean,
    val accountType: String?,
    val attendeeStatus: AttendeeStatus
)

enum class AttendeeStatus {
    NONE,
    ACCEPTED,
    DECLINED,
    TENTATIVE,
    NOT_RESPONDED
}
