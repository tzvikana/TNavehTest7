package com.drivenets.tnavehtest7.service

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import com.drivenets.tnavehtest7.data.CalendarEvent
import com.drivenets.tnavehtest7.data.AttendeeStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class CalendarService(private val context: Context) {
    companion object {
        private const val TAG = "CalendarService"
        private val PROJECTION = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.ORGANIZER,
            CalendarContract.Events.CUSTOM_APP_PACKAGE,
            CalendarContract.Events.CUSTOM_APP_URI,
            CalendarContract.Events.ACCOUNT_TYPE,
            CalendarContract.Events.SELF_ATTENDEE_STATUS
        )
    }

    fun getUpcomingEvents(minutesAhead: Long = TimeUnit.HOURS.toMinutes(24)): List<CalendarEvent> {
        val contentResolver: ContentResolver = context.contentResolver
        
        // Get available calendars
        val calendars = getAvailableCalendars()
        Log.d(TAG, "Found ${calendars.size} calendars: $calendars")
        
        if (calendars.isEmpty()) {
            Log.w(TAG, "No calendars found. Make sure Outlook calendar is synced.")
            return emptyList()
        }

        val now = System.currentTimeMillis()
        // Look back 1 hour to catch recently added meetings
        val start = now - TimeUnit.HOURS.toMillis(1)
        val end = now + TimeUnit.MINUTES.toMillis(minutesAhead)

        val selection = "${CalendarContract.Events.CALENDAR_ID} IN (${calendars.joinToString(",")}) AND " +
                       "${CalendarContract.Events.DTEND} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(start.toString(), end.toString())
        
        Log.d(TAG, "Querying for events between ${Instant.ofEpochMilli(start)} and ${Instant.ofEpochMilli(end)}")

        val events = mutableListOf<CalendarEvent>()

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                PROJECTION,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val event = cursor.toCalendarEvent()
                    events.add(event)
                } while (cursor.moveToNext())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied while accessing calendar", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error while fetching calendar events", e)
        } finally {
            cursor?.close()
        }

        return events
    }

    private fun Cursor.toCalendarEvent(): CalendarEvent {
        val id = getLong(getColumnIndexOrThrow(CalendarContract.Events._ID))
        val title = getString(getColumnIndexOrThrow(CalendarContract.Events.TITLE))
        val description = getString(getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION))
        val startMillis = getLong(getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
        val endMillis = getLong(getColumnIndexOrThrow(CalendarContract.Events.DTEND))
        val location = getString(getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION))
        val organizer = getString(getColumnIndexOrThrow(CalendarContract.Events.ORGANIZER))
        val customAppPackage = getString(getColumnIndexOrThrow(CalendarContract.Events.CUSTOM_APP_PACKAGE))
        val customAppUri = getString(getColumnIndexOrThrow(CalendarContract.Events.CUSTOM_APP_URI))
        val accountType = getString(getColumnIndexOrThrow(CalendarContract.Events.ACCOUNT_TYPE))
        val attendeeStatus = getInt(getColumnIndexOrThrow(CalendarContract.Events.SELF_ATTENDEE_STATUS))

        val startTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(startMillis),
            ZoneId.systemDefault()
        )
        val endTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(endMillis),
            ZoneId.systemDefault()
        )

        // Check if it's an online meeting (Teams/Skype)
        val isOnlineMeeting = when {
            customAppPackage?.contains("teams", ignoreCase = true) == true -> true
            customAppPackage?.contains("skype", ignoreCase = true) == true -> true
            description?.contains("teams.microsoft.com", ignoreCase = true) == true -> true
            description?.contains("join.skype.com", ignoreCase = true) == true -> true
            location?.contains("teams.microsoft.com", ignoreCase = true) == true -> true
            location?.contains("join.skype.com", ignoreCase = true) == true -> true
            else -> false
        }

        // Extract meeting URL
        val meetingUrl = when {
            customAppUri != null -> customAppUri
            isOnlineMeeting -> extractMeetingUrl(description, location)
            else -> null
        }

        return CalendarEvent(
            id = id,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            location = location,
            organizer = organizer,
            meetingUrl = meetingUrl,
            isOnlineMeeting = isOnlineMeeting,
            accountType = accountType,
            attendeeStatus = when (attendeeStatus) {
                CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED -> AttendeeStatus.ACCEPTED
                CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED -> AttendeeStatus.DECLINED
                CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE -> AttendeeStatus.TENTATIVE
                CalendarContract.Attendees.ATTENDEE_STATUS_NONE -> AttendeeStatus.NOT_RESPONDED
                else -> AttendeeStatus.NONE
            }
        )
    }

    private fun getAvailableCalendars(): List<String> {
        val calendarIds = mutableListOf<String>()
        var cursor: Cursor? = null
        
        try {
            cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.ACCOUNT_NAME,
                    CalendarContract.Calendars.OWNER_ACCOUNT
                ),
                null,
                null,
                null
            )

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val calendarId = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars._ID))
                    val displayName = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                    val accountName = cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME))
                    
                    calendarIds.add(calendarId)
                    Log.d(TAG, "Found calendar: ID=$calendarId, Name=$displayName, Account=$accountName")
                } while (cursor.moveToNext())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied while accessing calendars", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error while fetching calendars", e)
        } finally {
            cursor?.close()
        }

        return calendarIds
    }

    private fun extractMeetingUrl(description: String?, location: String?): String? {
        val urlPattern = "(https?://[^\\s]+)".toRegex()
        val descriptionMatch = description?.let { urlPattern.find(it) }
        val locationMatch = location?.let { urlPattern.find(it) }
        
        return when {
            descriptionMatch != null && (
                descriptionMatch.value.contains("teams.microsoft.com") ||
                descriptionMatch.value.contains("join.skype.com")
            ) -> descriptionMatch.value
            locationMatch != null && (
                locationMatch.value.contains("teams.microsoft.com") ||
                locationMatch.value.contains("join.skype.com")
            ) -> locationMatch.value
            else -> null
        }
    }
}
