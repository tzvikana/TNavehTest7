package com.drivenets.tnavehtest7.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.drivenets.tnavehtest7.service.AlarmService
import com.drivenets.tnavehtest7.service.CalendarService

class CalendarSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val calendarService = CalendarService(context)
    private val alarmService = AlarmService(context)

    override suspend fun doWork(): Result {
        return try {
            val events = calendarService.getUpcomingEvents()
            events.forEach { event ->
                alarmService.scheduleAlarm(event)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}


