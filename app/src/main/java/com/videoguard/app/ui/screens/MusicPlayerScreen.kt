package com.videoguard.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.videoguard.app.data.VideoRepository
import com.videoguard.app.model.MusicPlayerViewMode
import com.videoguard.app.model.VideoItem
import com.videoguard.app.ui.components.YouTubeVideoPlayer
import com.videoguard.app.ui.theme.YTMusicBlack
import com.videoguard.app.ui.theme.YTMusicRedAccent

enum class MusicSheetTab {
    UP_NEXT,
    LYRICS,
    RELATED
}

@Composable
fun MusicPlayerScreen(
    track: VideoItem,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onCollapse: () -> Unit,
    onNextTrack: () -> Unit,
    onPrevTrack: () -> Unit,
    onTrackSelected: (VideoItem) -> Unit,
    isBackgroundPlay: Boolean = true,
    onToggleBackgroundPlay: () -> Unit = {},
    viewMode: MusicPlayerViewMode = MusicPlayerViewMode.SONG,
    onViewModeChanged: (MusicPlayerViewMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(MusicSheetTab.UP_NEXT) }
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0.35f) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val liveQueue by com.videoguard.app.service.PlaybackController.playbackQueue.collectAsState()
    var dynamicRelated by remember(track.id) { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoadingRelated by remember(track.id) { mutableStateOf(false) }
    var isSheetExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(track.id) {
        if (liveQueue.size <= 1) {
            isLoadingRelated = true
            val results = com.videoguard.app.data.YouTubeSearchEngine.getRelatedVideos(track, isMusicOnly = true)
            if (results.isNotEmpty()) {
                dynamicRelated = results
                com.videoguard.app.service.PlaybackController.setQueue(listOf(track) + results)
            }
            isLoadingRelated = false
        }
    }

    val queueTracks = remember(liveQueue, dynamicRelated, track.id) {
        if (liveQueue.size > 1) {
            val currentIdx = liveQueue.indexOfFirst { it.id == track.id }
            if (currentIdx != -1) {
                val upcoming = liveQueue.drop(currentIdx + 1)
                val previous = liveQueue.take(currentIdx)
                upcoming + previous
            } else {
                liveQueue.filter { it.id != track.id }
            }
        } else if (dynamicRelated.isNotEmpty()) {
            dynamicRelated
        } else {
            VideoRepository.getMusicTracks().filter { it.id != track.id }
        }
    }

    val sheetHeight by animateDpAsState(
        targetValue = if (isSheetExpanded) 360.dp else 180.dp,
        label = "sheetHeight"
    )

    // Audio Visualizer waveform animation
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "w3"
    )

    val currentTime by com.videoguard.app.service.PlaybackController.currentTimeSeconds.collectAsState()
    val duration by com.videoguard.app.service.PlaybackController.durationSeconds.collectAsState()
    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    val currentSeconds = if (isUserScrubbing) scrubPosition else currentTime
    val maxSeconds = if (duration > 0f) duration else 100f
    val sliderProgress = (currentSeconds / maxSeconds).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { /* Absorb background taps */ }
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2B0C16),
                        Color(0xFF14060B),
                        Color(0xFF070707)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        // Top Bar: Collapse Icon | [ Song | Video ] Toggle (Top Center) | Background Play & More
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            IconButton(
                onClick = onCollapse,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Iconic YT Music [ Song | Video ] Pill Switcher in Top Centre
            Box(modifier = Modifier.align(Alignment.Center)) {
                SongVideoToggle(
                    viewMode = viewMode,
                    onModeSelected = onViewModeChanged
                )
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleBackgroundPlay,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Background Play",
                        tint = if (isBackgroundPlay) Color(0xFF00E676) else Color(0xFF888888),
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Center Area: Either Square Album Art (Song mode) OR Embedded Video Player (Video mode)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
                .pointerInput(Unit) {
                    detectTapGestures { /* Absorb center taps */ }
                },
            contentAlignment = Alignment.Center
        ) {
            if (viewMode == MusicPlayerViewMode.SONG) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .aspectRatio(1f)
                            .shadow(24.dp, RoundedCornerShape(16.dp), spotColor = Color(0x66FF1744))
                            .clip(RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectTapGestures { /* Do not toggle play/pause on album tap */ }
                            }
                    ) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Audio visualizer bar when playing
                    if (isPlaying) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(18.dp)
                        ) {
                            listOf(wave1, wave2, wave3, wave1 * 0.7f, wave2 * 0.9f).forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .width(3.5.dp)
                                        .fillMaxHeight(h)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(YTMusicRedAccent)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "High Quality Audio",
                                color = Color(0xFFAAAAAA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // Video Mode: Transparent placeholder so persistent player displays here
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { /* Do not toggle play/pause on video tap in music mode */ }
                        }
                )
            }
        }

        // Track Info & Like / Dislike
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist.ifEmpty { track.channelName },
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(
                    onClick = {
                        isDisliked = !isDisliked
                        if (isDisliked) isLiked = false
                    }
                ) {
                    Icon(
                        imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (isDisliked) YTMusicRedAccent else Color.White
                    )
                }
                IconButton(
                    onClick = {
                        isLiked = !isLiked
                        if (isLiked) isDisliked = false
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        tint = if (isLiked) YTMusicRedAccent else Color.White
                    )
                }
            }
        }

        // Seekbar / Slider (Live synchronized)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 2.dp)
        ) {
            Slider(
                value = currentSeconds.coerceIn(0f, maxSeconds),
                onValueChange = { newVal ->
                    isUserScrubbing = true
                    scrubPosition = newVal
                },
                onValueChangeFinished = {
                    isUserScrubbing = false
                    com.videoguard.app.service.PlaybackController.seekTo(scrubPosition)
                },
                valueRange = 0f..maxSeconds,
                colors = SliderDefaults.colors(
                    thumbColor = YTMusicRedAccent,
                    activeTrackColor = YTMusicRedAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatMusicTime(currentSeconds), color = Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(if (duration > 0f) formatMusicTime(duration) else track.duration, color = Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Playback Controls: Shuffle | Prev | Play/Pause | Next | Repeat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { isShuffle = !isShuffle },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) YTMusicRedAccent else Color(0xFFAAAAAA),
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onPrevTrack, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Big Modern Play/Pause Button with subtle ring
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onPlayPauseToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(38.dp)
                )
            }

            IconButton(onClick = onNextTrack, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(
                onClick = { isRepeat = !isRepeat },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (isRepeat) YTMusicRedAccent else Color(0xFFAAAAAA),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Bottom Sheet Tabs: [ UP NEXT | LYRICS | RELATED ]
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Sleek handle bar
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF4A4A4A))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Tab Buttons + Expand Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabPill(
                            title = "UP NEXT",
                            isActive = activeTab == MusicSheetTab.UP_NEXT,
                            onClick = { activeTab = MusicSheetTab.UP_NEXT }
                        )
                        TabPill(
                            title = "LYRICS",
                            isActive = activeTab == MusicSheetTab.LYRICS,
                            onClick = { activeTab = MusicSheetTab.LYRICS }
                        )
                        TabPill(
                            title = "RELATED",
                            isActive = activeTab == MusicSheetTab.RELATED,
                            onClick = { activeTab = MusicSheetTab.RELATED }
                        )
                    }

                    IconButton(
                        onClick = { isSheetExpanded = !isSheetExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSheetExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isSheetExpanded) "Collapse Queue" else "Expand Queue",
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    when (activeTab) {
                        MusicSheetTab.UP_NEXT -> {
                            if (isLoadingRelated && queueTracks.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            } else {
                                LazyColumn {
                                    items(queueTracks, key = { it.id }) { qTrack ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val activeQueue = if (liveQueue.size > 1) liveQueue else listOf(track) + queueTracks
                                                    com.videoguard.app.service.PlaybackController.playTrack(qTrack, activeQueue)
                                                }
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = qTrack.thumbnailUrl,
                                                contentDescription = qTrack.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = qTrack.title,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = qTrack.artist.ifEmpty { qTrack.channelName },
                                                    color = Color(0xFFAAAAAA),
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = qTrack.duration,
                                                color = Color(0xFF888888),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        MusicSheetTab.LYRICS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = track.lyrics.ifEmpty {
                                        "Lyrics are not currently available for this track."
                                    },
                                    color = Color(0xFFEEEEEE),
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                        MusicSheetTab.RELATED -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = "About the Artist",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${track.artist.ifEmpty { track.channelName }} has over ${track.views} across YouTube and YouTube Music. Album: ${track.album.ifEmpty { "Single" }}.",
                                    color = Color(0xFFCCCCCC),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        com.videoguard.app.ui.components.PlayerSettingsBottomSheet(onDismiss = { showSettingsSheet = false })
    }
}

@Composable
private fun SongVideoToggle(
    viewMode: MusicPlayerViewMode,
    onModeSelected: (MusicPlayerViewMode) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF222222))
            .padding(2.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (viewMode == MusicPlayerViewMode.SONG) Color(0xFF383838) else Color.Transparent
                    )
                    .clickable { onModeSelected(MusicPlayerViewMode.SONG) }
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Song",
                    color = if (viewMode == MusicPlayerViewMode.SONG) Color.White else Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (viewMode == MusicPlayerViewMode.VIDEO) Color(0xFF383838) else Color.Transparent
                    )
                    .clickable { onModeSelected(MusicPlayerViewMode.VIDEO) }
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Video",
                    color = if (viewMode == MusicPlayerViewMode.VIDEO) Color.White else Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TabPill(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) Color(0xFF282828) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            color = if (isActive) Color.White else Color(0xFF888888),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
        )
    }
}

private fun formatMusicTime(seconds: Float): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%d:%02d", m, s)
}
