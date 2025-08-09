package com.drivenets.tnavehtest7.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivenets.tnavehtest7.data.CalendarEvent
import com.drivenets.tnavehtest7.service.AlarmService
import com.drivenets.tnavehtest7.service.CalendarService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MeetingsViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "MeetingsViewModel"
    }

    private val calendarService = CalendarService(application)
    private val alarmService = AlarmService(application)

    private val _upcomingMeetings = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val upcomingMeetings: StateFlow<List<CalendarEvent>> = _upcomingMeetings.asStateFlow()

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<LocalDateTime?>(null)
    val lastSyncTime: StateFlow<LocalDateTime?> = _lastSyncTime.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun setPermissionGranted(granted: Boolean) {
        _permissionGranted.value = granted
        if (granted) {
            refreshMeetings()
        }
    }

    fun refreshMeetings() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                Log.d(TAG, "Starting calendar sync...")
                val events = calendarService.getUpcomingEvents()
                _upcomingMeetings.value = events
                scheduleAlarms(events)
                _lastSyncTime.value = LocalDateTime.now()
                Log.d(TAG, "Calendar sync completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync calendar", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun scheduleAlarms(events: List<CalendarEvent>) {
        Log.d(TAG, "Found ${events.size} events to schedule alarms for")
        events.forEach { event ->
            try {
                Log.d(TAG, "Attempting to schedule alarm for: ${event.title} at ${event.startTime}")
                alarmService.scheduleAlarm(event)
                Log.d(TAG, "Successfully scheduled alarm for: ${event.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule alarm for: ${event.title}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
