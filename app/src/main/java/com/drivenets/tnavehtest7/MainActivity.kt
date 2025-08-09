package com.drivenets.tnavehtest7

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import com.drivenets.tnavehtest7.ui.MeetingsScreen
import com.drivenets.tnavehtest7.ui.theme.TNavehTest7Theme
import com.drivenets.tnavehtest7.viewmodel.MeetingsViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MeetingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request exact alarm permission on Android 12 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
        
        setContent {
            TNavehTest7Theme {
                val meetings by viewModel.upcomingMeetings.collectAsStateWithLifecycle()
                val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
                val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MeetingsScreen(
                        upcomingMeetings = meetings,
                        onPermissionGranted = viewModel::setPermissionGranted,
                        onRefresh = viewModel::refreshMeetings,
                        lastSyncTime = lastSyncTime,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}