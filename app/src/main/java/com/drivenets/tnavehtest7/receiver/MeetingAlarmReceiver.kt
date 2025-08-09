package com.drivenets.tnavehtest7.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.drivenets.tnavehtest7.MainActivity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MeetingAlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "meeting_notifications"
        private const val NOTIFICATION_ID = 1
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra("eventId", -1)
        val eventTitle = intent.getStringExtra("eventTitle") ?: "Upcoming Meeting"
        val eventStartTime = intent.getLongExtra("eventStartTime", -1L)

        if (eventId == -1L || eventStartTime == -1L) return

        val startTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(eventStartTime),
            ZoneId.systemDefault()
        )
        
        val formattedTime = startTime.format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        )

        createNotificationChannel(context)
        showNotification(context, eventTitle, formattedTime)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Meeting Notifications"
            val descriptionText = "Notifications for upcoming meetings"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, title: String, time: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Meeting Reminder")
            .setContentText("$title starts at $time")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}


