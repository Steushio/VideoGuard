package com.videoguard.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videoguard.app.service.PlaybackController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsBottomSheet(
    onDismiss: () -> Unit
) {
    val isBackgroundPlay by PlaybackController.isBackgroundPlayEnabled.collectAsState()
    val isLoopEnabled by PlaybackController.isLoopEnabled.collectAsState()
    val currentSpeed by PlaybackController.playbackSpeed.collectAsState()
    val currentQuality by PlaybackController.playbackQuality.collectAsState()
    val sleepTimer by PlaybackController.sleepTimerMinutes.collectAsState()
    val currentTrack by PlaybackController.currentTrack.collectAsState()

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF212121),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF666666)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Playback settings",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF333333), thickness = 0.8.dp)

            Spacer(modifier = Modifier.height(4.dp))

            // 1. Quality
            SettingsRow(
                icon = Icons.Default.HighQuality,
                title = "Quality",
                subtitle = currentQuality,
                onClick = { showQualityDialog = true }
            )

            // 2. Playback Speed
            val speedText = if (currentSpeed == 1.0f) "Normal (1.0x)" else "${currentSpeed}x"
            SettingsRow(
                icon = Icons.Default.Speed,
                title = "Playback speed",
                subtitle = speedText,
                onClick = { showSpeedDialog = true }
            )

            // 3. Background Playback Toggle
            SettingsSwitchRow(
                icon = Icons.Default.Headphones,
                title = "Background playback",
                subtitle = "Keep audio playing when screen is off",
                checked = isBackgroundPlay,
                onCheckedChange = { PlaybackController.toggleBackgroundPlay() }
            )

            // 4. Loop Video Toggle
            SettingsSwitchRow(
                icon = Icons.Default.Repeat,
                title = "Loop video",
                subtitle = "Automatically replay when finished",
                checked = isLoopEnabled,
                onCheckedChange = { PlaybackController.toggleLoop() }
            )

            // 5. Sleep Timer
            val timerText = if (sleepTimer != null) "$sleepTimer minutes remaining" else "Off"
            SettingsRow(
                icon = Icons.Default.Timer,
                title = "Sleep timer",
                subtitle = timerText,
                onClick = { showSleepTimerDialog = true }
            )

            // 6. Stats & Video Info
            SettingsRow(
                icon = Icons.Default.Info,
                title = "Stats & video info",
                subtitle = "Resolution, ID, codec & channel",
                onClick = { showStatsDialog = true }
            )
        }
    }

    // Quality Selection Dialog
    if (showQualityDialog) {
        val qualities = listOf("Auto (1080p)", "1080p", "720p", "480p", "360p")
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Select Quality", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    qualities.forEach { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    PlaybackController.setPlaybackQuality(q)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(q, color = Color.White, fontSize = 16.sp)
                            if (currentQuality == q) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF3EA6FF))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Close", color = Color(0xFFAAAAAA))
                }
            },
            containerColor = Color(0xFF282828),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Speed Selection Dialog
    if (showSpeedDialog) {
        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    speeds.forEach { sp ->
                        val label = if (sp == 1.0f) "Normal" else "${sp}x"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    PlaybackController.setPlaybackSpeed(sp)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = Color.White, fontSize = 16.sp)
                            if (currentSpeed == sp) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF3EA6FF))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close", color = Color(0xFFAAAAAA))
                }
            },
            containerColor = Color(0xFF282828),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        val options = listOf(null to "Off", 15 to "15 minutes", 30 to "30 minutes", 45 to "45 minutes", 60 to "60 minutes")
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { (mins, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    PlaybackController.setSleepTimer(mins)
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = Color.White, fontSize = 16.sp)
                            if (sleepTimer == mins) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF3EA6FF))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Close", color = Color(0xFFAAAAAA))
                }
            },
            containerColor = Color(0xFF282828),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Stats Dialog
    if (showStatsDialog) {
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            title = { Text("Video Stats & Info", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Title: ${currentTrack?.title ?: "N/A"}", color = Color.White, fontSize = 13.sp)
                    Text("Channel: ${currentTrack?.channelName ?: "N/A"}", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                    Text("Video ID: ${currentTrack?.id ?: "N/A"}", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                    Text("Resolution: $currentQuality", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                    Text("Audio: High Bitrate WebM/Opus", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                    Text("Background Play: ${if (isBackgroundPlay) "Active" else "Disabled"}", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsDialog = false }) {
                    Text("Done", color = Color(0xFF3EA6FF), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF282828),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFFAAAAAA), fontSize = 12.sp)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF888888),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFFAAAAAA), fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF3EA6FF),
                uncheckedThumbColor = Color(0xFFAAAAAA),
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}
