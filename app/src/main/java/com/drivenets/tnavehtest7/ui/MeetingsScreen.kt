package com.drivenets.tnavehtest7.ui

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drivenets.tnavehtest7.data.CalendarEvent
import com.drivenets.tnavehtest7.data.AttendeeStatus
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MeetingsScreen(
    upcomingMeetings: List<CalendarEvent>,
    onPermissionGranted: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    lastSyncTime: LocalDateTime?,
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val calendarPermissionState = rememberPermissionState(
        Manifest.permission.READ_CALENDAR
    )

    LaunchedEffect(calendarPermissionState.status) {
        onPermissionGranted(calendarPermissionState.status.isGranted)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!calendarPermissionState.status.isGranted) {
            PermissionRequest(
                onRequestPermission = { calendarPermissionState.launchPermissionRequest() }
            )
        } else {
            MeetingsList(
                meetings = upcomingMeetings,
                onRefresh = onRefresh,
                lastSyncTime = lastSyncTime,
                isRefreshing = isRefreshing
            )
        }
    }
}

@Composable
private fun PermissionRequest(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Calendar permission is required to show your upcoming meetings",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun MeetingsList(
    meetings: List<CalendarEvent>,
    onRefresh: () -> Unit,
    lastSyncTime: LocalDateTime?,
    isRefreshing: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Upcoming Meetings",
                    style = MaterialTheme.typography.headlineMedium
                )
                lastSyncTime?.let { syncTime ->
                    Text(
                        text = "Last synced: ${syncTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh meetings"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (meetings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No upcoming meetings",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(meetings) { meeting ->
                    MeetingCard(meeting = meeting)
                }
            }
        }
    }
}

@Composable
private fun MeetingCard(meeting: CalendarEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = meeting.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = formatDateTime(meeting),
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    meeting.location?.let { location ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    meeting.organizer?.let { organizer ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Organizer: $organizer",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                if (meeting.isOnlineMeeting) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.VideoCall,
                        contentDescription = "Online Meeting",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                when (meeting.attendeeStatus) {
                    AttendeeStatus.ACCEPTED -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Accepted",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    AttendeeStatus.DECLINED -> Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Declined",
                        tint = MaterialTheme.colorScheme.error
                    )
                    AttendeeStatus.TENTATIVE -> Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = "Tentative",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    else -> {}
                }
            }
        }
    }
}

private fun formatDateTime(meeting: CalendarEvent): String {
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    
    return "${meeting.startTime.format(dateFormatter)} at ${meeting.startTime.format(timeFormatter)}"
}
