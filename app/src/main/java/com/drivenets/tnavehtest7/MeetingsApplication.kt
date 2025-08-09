package com.drivenets.tnavehtest7

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.drivenets.tnavehtest7.worker.CalendarSyncWorker
import java.util.concurrent.TimeUnit

class MeetingsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupCalendarSync()
    }

    private fun setupCalendarSync() {
        val calendarSyncRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "calendar_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            calendarSyncRequest
        )
    }
}


