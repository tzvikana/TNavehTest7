package com.drivenets.tnavehtest7.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.drivenets.tnavehtest7.data.CalendarEvent
import com.drivenets.tnavehtest7.receiver.MeetingAlarmReceiver
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class AlarmService(private val context: Context) {
    companion object {
        private const val TAG = "AlarmService"
        private const val ALARM_OFFSET_MINUTES = 1L // 1 minute before meeting
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(event: CalendarEvent) {
        val startTimeMillis = event.startTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        
        val alarmTimeMillis = startTimeMillis - TimeUnit.MINUTES.toMillis(ALARM_OFFSET_MINUTES)
        val currentTimeMillis = System.currentTimeMillis()

        if (alarmTimeMillis <= currentTimeMillis) {
            Log.d(TAG, "Skipping alarm for past event: ${event.title}")
            return
        }

        // Calculate alarm time in hours and minutes
        val alarmTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(alarmTimeMillis),
            ZoneId.systemDefault()
        )
        
        try {
            Log.d(TAG, """
                ===== Scheduling Alarm =====
                Event: ${event.title}
                Current time: ${LocalDateTime.now()}
                Meeting time: ${event.startTime}
                Alarm time: $alarmTime
                Time until alarm: ${(alarmTimeMillis - currentTimeMillis) / 1000 / 60} minutes
                Setting alarm for: ${alarmTime.hour}:${alarmTime.minute}
                ===========================
            """.trimIndent())

            // Set the alarm using AlarmClock API
            val alarmIntent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "Meeting: ${event.title}")
                putExtra(android.provider.AlarmClock.EXTRA_HOUR, alarmTime.hour)
                putExtra(android.provider.AlarmClock.EXTRA_MINUTES, alarmTime.minute)
                putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, true)
                putExtra(android.provider.AlarmClock.EXTRA_VIBRATE, true)
            }
            
            // Also set up a backup notification alarm
            val notificationIntent = Intent(context, MeetingAlarmReceiver::class.java).apply {
                putExtra("eventId", event.id)
                putExtra("eventTitle", event.title)
                putExtra("eventStartTime", startTimeMillis)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                event.id.toInt(),
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                context.startActivity(alarmIntent)
                Log.d(TAG, "Clock alarm set successfully for ${event.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set clock alarm, falling back to notification alarm", e)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(alarmTimeMillis, pendingIntent),
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTimeMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(alarmTimeMillis, pendingIntent),
                        pendingIntent
                    )
                }
            }
            Log.d(TAG, "Alarm scheduling completed for ${event.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm for event: ${event.title}", e)
        }

        val intent = Intent(context, MeetingAlarmReceiver::class.java).apply {
            putExtra("eventId", event.id)
            putExtra("eventTitle", event.title)
            putExtra("eventStartTime", startTimeMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Cancel any existing alarm for this event
            cancelAlarm(event.id)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    Log.d(TAG, "Setting exact alarm for: ${event.title}")
                    // Create a show activity intent
                    val showIntent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val showPendingIntent = PendingIntent.getActivity(
                        context,
                        event.id.toInt(),
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Set the alarm with the show operation
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(alarmTimeMillis, showPendingIntent),
                        pendingIntent
                    )
                } else {
                    Log.w(TAG, "Cannot schedule exact alarms - permission not granted. Using inexact alarm.")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                    )
                }
            } else {
                Log.d(TAG, "Setting exact alarm for: ${event.title} (pre-Android S)")
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(alarmTimeMillis, pendingIntent),
                    pendingIntent
                )
            }
            Log.d(TAG, "Successfully scheduled alarm for: ${event.title} at ${Instant.ofEpochMilli(alarmTimeMillis)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm for event: ${event.title}", e)
        }
    }

    fun cancelAlarm(eventId: Long) {
        val intent = Intent(context, MeetingAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Log.d(TAG, "Cancelled alarm for event ID: $eventId")
        }
    }
}
