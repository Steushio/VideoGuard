package com.videoguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.videoguard.app.data.VideoRepository
import com.videoguard.app.model.VideoItem
import com.videoguard.app.ui.components.YouTubeVideoPlayer
import com.videoguard.app.ui.theme.YouTubeDarkBg
import com.videoguard.app.ui.theme.YTMusicRedAccent

@Composable
fun VideoDetailScreen(
    video: VideoItem,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onCollapse: () -> Unit,
    onSwitchToMusicMode: (VideoItem) -> Unit,
    onVideoSelected: (VideoItem) -> Unit,
    onChannelClick: (VideoItem) -> Unit = {},
    onNextTrack: () -> Unit = { com.videoguard.app.service.PlaybackController.nextTrack() },
    onPrevTrack: () -> Unit = { com.videoguard.app.service.PlaybackController.prevTrack() },
    isBackgroundPlay: Boolean = true,
    onToggleBackgroundPlay: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val subscribedChannels by com.videoguard.app.data.UserDataRepository.subscribedChannels.collectAsState()
    val isSubscribed = subscribedChannels.contains(video.channelName)

    val userPlaylists by com.videoguard.app.data.UserDataRepository.playlists.collectAsState()
    val isLiked = remember(userPlaylists, video.id) {
        com.videoguard.app.data.UserDataRepository.isLiked(video.id)
    }
    val isSaved = remember(userPlaylists, video.id) {
        com.videoguard.app.data.UserDataRepository.isWatchLater(video.id)
    }

    var isDisliked by remember { mutableStateOf(false) }
    var showFullDescription by remember { mutableStateOf(false) }

    val liveQueue by com.videoguard.app.service.PlaybackController.playbackQueue.collectAsState()
    var dynamicRelated by remember(video.id) { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoadingRelated by remember(video.id) { mutableStateOf(false) }

    LaunchedEffect(video.id) {
        if (liveQueue.size <= 1) {
            isLoadingRelated = true
            val results = com.videoguard.app.data.YouTubeSearchEngine.getRelatedVideos(video, isMusicOnly = false)
            if (results.isNotEmpty()) {
                dynamicRelated = results
                com.videoguard.app.service.PlaybackController.setQueue(listOf(video) + results)
            }
            isLoadingRelated = false
        }
    }

    val displayVideos = remember(liveQueue, dynamicRelated, video.id) {
        if (liveQueue.size > 1) {
            val currentIdx = liveQueue.indexOfFirst { it.id == video.id }
            if (currentIdx != -1) {
                val upcoming = liveQueue.drop(currentIdx + 1)
                val previous = liveQueue.take(currentIdx)
                upcoming + previous
            } else {
                liveQueue.filter { it.id != video.id }
            }
        } else if (dynamicRelated.isNotEmpty()) {
            dynamicRelated
        } else {
            VideoRepository.sampleVideos.filter { it.id != video.id }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {}
            .background(YouTubeDarkBg)
    ) {
        // Top Player Section Spacer (Pinned player sits above this in MainScreen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .aspectRatio(16f / 9f)
        )

        // Scrollable Video Details & Related Videos
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            // Video Title
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                // View count & date
                Text(
                    text = "${video.views} • ${video.uploadTime}",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Channel Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = video.channelAvatarUrl,
                        contentDescription = video.channelName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF333333))
                            .clickable { onChannelClick(video) }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onChannelClick(video) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = video.channelName,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = video.subscriberCount,
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = { com.videoguard.app.data.UserDataRepository.toggleSubscribe(video.channelName) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubscribed) Color(0xFF272727) else Color.White,
                            contentColor = if (isSubscribed) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        if (isSubscribed) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isSubscribed) "Subscribed" else "Subscribe",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Action Pills Row (Scrollable - Modern YouTube Order)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Like / Dislike Combined Split Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF242424))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    com.videoguard.app.data.UserDataRepository.toggleLiked(video)
                                    if (!isLiked) isDisliked = false
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = if (isLiked) Color.White else Color(0xFFDDDDDD),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = video.likes,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color(0xFF404040))
                        )
                        IconButton(
                            onClick = {
                                isDisliked = !isDisliked
                                if (isDisliked && isLiked) {
                                    com.videoguard.app.data.UserDataRepository.toggleLiked(video)
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "Dislike",
                                tint = if (isDisliked) Color.White else Color(0xFFAAAAAA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Share
                    ActionPill(
                        icon = Icons.Outlined.Share,
                        label = "Share",
                        onClick = {}
                    )

                    // Remix
                    ActionPill(
                        icon = Icons.Outlined.AutoAwesome,
                        label = "Remix",
                        onClick = {}
                    )

                    // Download
                    ActionPill(
                        icon = Icons.Outlined.Download,
                        label = "Download",
                        onClick = {}
                    )

                    // Save / Watch later
                    ActionPill(
                        icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        label = if (isSaved) "Saved" else "Save",
                        onClick = { com.videoguard.app.data.UserDataRepository.toggleWatchLater(video) }
                    )

                    // Switch to YouTube Music Mode Action Pill
                    Button(
                        onClick = { onSwitchToMusicMode(video) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2D141C)
                        ),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Switch to Music",
                            tint = YTMusicRedAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play in YT Music",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Comments Preview Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF202020))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Comments",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "4.2K",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00897B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "This track never gets old! Incredible sound quality.",
                                color = Color(0xFFE0E0E0),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Up Next / Queue Header
            item {
                val currentIdx = liveQueue.indexOfFirst { it.id == video.id }
                val queueText = if (liveQueue.size > 1 && currentIdx != -1) {
                    "Queue (${currentIdx + 1}/${liveQueue.size})"
                } else {
                    "Up next"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = queueText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (liveQueue.size > 1) {
                        Text(
                            text = "Auto-play queue",
                            color = Color(0xFFAAAAAA),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (isLoadingRelated && displayVideos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Up Next Videos / Queue items
            items(displayVideos, key = { it.id }) { relatedVideo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val activeQueue = if (liveQueue.size > 1) liveQueue else listOf(video) + displayVideos
                            com.videoguard.app.service.PlaybackController.playTrack(relatedVideo, activeQueue)
                        }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 68.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF222222))
                    ) {
                        AsyncImage(
                            model = relatedVideo.thumbnailUrl,
                            contentDescription = relatedVideo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.85f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = relatedVideo.duration,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = relatedVideo.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${relatedVideo.channelName} • ${relatedVideo.views}",
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { /* Item options */ },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF242424))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFFF1F1F1),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = Color(0xFFF1F1F1),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
