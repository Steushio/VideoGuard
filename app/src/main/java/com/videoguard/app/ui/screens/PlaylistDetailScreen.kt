package com.videoguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.videoguard.app.data.UserDataRepository
import com.videoguard.app.data.YouTubeSyncEngine
import com.videoguard.app.model.UserPlaylist
import com.videoguard.app.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    onVideoSelected: (VideoItem, List<VideoItem>) -> Unit,
    initialPlaylist: UserPlaylist? = null,
    modifier: Modifier = Modifier
) {
    val playlists by UserDataRepository.playlists.collectAsState()
    val repoPlaylist = playlists.find { it.id == playlistId }
    val playlist = repoPlaylist ?: initialPlaylist ?: UserPlaylist(
        id = playlistId,
        title = "Playlist",
        videos = emptyList()
    )

    var currentVideos by remember(playlistId) { mutableStateOf(playlist.videos) }
    var isLoading by remember(playlistId) { mutableStateOf(playlist.videos.isEmpty()) }
    var isRefreshing by remember(playlistId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Keep currentVideos in sync with repository updates
    LaunchedEffect(repoPlaylist?.videos) {
        val updated = repoPlaylist?.videos
        if (!updated.isNullOrEmpty()) {
            currentVideos = updated
            isLoading = false
        }
    }

    suspend fun refreshVideos() {
        isRefreshing = true
        val online = withContext(Dispatchers.IO) {
            YouTubeSyncEngine.fetchPlaylistVideos(playlistId)
        }
        if (online.isNotEmpty()) {
            currentVideos = online
            when (playlistId) {
                "liked_videos" -> UserDataRepository.updateLikedVideos(online)
                "watch_later" -> UserDataRepository.updateWatchLaterVideos(online)
                else -> UserDataRepository.updatePlaylistVideos(playlistId, online)
            }
        }
        isRefreshing = false
        isLoading = false
    }

    // Always fetch fresh real-time videos on open
    LaunchedEffect(playlistId) {
        if (currentVideos.isEmpty()) {
            isLoading = true
        }
        refreshVideos()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(52.dp)
                    .background(Color(0xFF0F0F0F))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = playlist.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { /* Search */ }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White
                    )
                }
            }
        },
        containerColor = Color(0xFF0F0F0F),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Playlist Header Hero Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2C3440),
                                    Color(0xFF0F0F0F)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playlist Cover Artwork
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222222)),
                            contentAlignment = Alignment.Center
                        ) {
                            val coverUrl = playlist.customCoverUrl ?: currentVideos.firstOrNull()?.thumbnailUrl
                            if (!coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = playlist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = when (playlist.iconType) {
                                        "like" -> Icons.Default.ThumbUp
                                        "watch_later" -> Icons.Default.WatchLater
                                        else -> Icons.Default.PlaylistPlay
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.title,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isRefreshing) "Syncing latest changes..." else "${currentVideos.size} videos • Synced with YouTube",
                                color = if (isRefreshing) Color(0xFF3EA6FF) else Color(0xFFAAAAAA),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (playlist.description.isNotBlank()) playlist.description else "Auto playlist",
                                color = Color(0xFF888888),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons: Play All & Shuffle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (currentVideos.isNotEmpty()) {
                                    onVideoSelected(currentVideos.first(), currentVideos)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            enabled = currentVideos.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play all",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play all", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                if (currentVideos.isNotEmpty()) {
                                    val shuffled = currentVideos.shuffled()
                                    onVideoSelected(shuffled.first(), shuffled)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF272727),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            enabled = currentVideos.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Empty or loading state
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
                    }
                }
            } else if (currentVideos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No videos in this playlist yet.",
                            color = Color(0xFFAAAAAA),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                itemsIndexed(currentVideos, key = { idx, item -> "${item.id}_$idx" }) { index, video ->
                    PlaylistItemRow(
                        index = index + 1,
                        video = video,
                        onClick = { onVideoSelected(video, currentVideos) },
                        onRemove = {
                            currentVideos = currentVideos.filter { it.id != video.id }
                            UserDataRepository.removeVideoFromPlaylist(playlist.id, video.id)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Bottom space for mini player
            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }
}

@Composable
private fun PlaylistItemRow(
    index: Int,
    video: VideoItem,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index",
            color = Color(0xFFAAAAAA),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )

        // Thumbnail
        Box(
            modifier = Modifier
                .width(110.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF222222))
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = video.duration,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = video.channelName,
                color = Color(0xFFAAAAAA),
                fontSize = 11.sp
            )
        }

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
