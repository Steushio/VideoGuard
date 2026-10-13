package com.videoguard.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.videoguard.app.model.AppMode
import com.videoguard.app.model.VideoItem
import com.videoguard.app.ui.theme.YouTubeRed
import com.videoguard.app.ui.theme.YTMusicRedAccent

@Composable
fun MiniPlayerBar(
    video: VideoItem?,
    isPlaying: Boolean,
    currentMode: AppMode,
    onExpand: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit = { com.videoguard.app.service.PlaybackController.nextTrack() },
    modifier: Modifier = Modifier
) {
    if (video == null) return

    val currentTime by com.videoguard.app.service.PlaybackController.currentTimeSeconds.collectAsState()
    val duration by com.videoguard.app.service.PlaybackController.durationSeconds.collectAsState()
    val progressFraction = remember(currentTime, duration) {
        if (duration > 0f) (currentTime / duration).coerceIn(0f, 1f) else 0f
    }

    val isMusic = currentMode == AppMode.YOUTUBE_MUSIC
    val barBg = if (isMusic) Color(0xFF1E1E1E) else Color(0xFF222222)
    val accentColor = if (isMusic) YTMusicRedAccent else YouTubeRed

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .shadow(10.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(barBg)
            .clickable(onClick = onExpand)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail / Album Art (Rounded & proportioned)
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(if (isMusic) 42.dp else 52.dp, if (isMusic) 42.dp else 34.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Title & Channel/Artist
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = video.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isMusic && video.artist.isNotEmpty()) video.artist else video.channelName,
                        color = Color(0xFFAAAAAA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play / Pause Toggle
                IconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Skip Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Close
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Sleek accent progress line along bottom
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = accentColor,
                trackColor = Color(0xFF333333)
            )
        }
    }
}
