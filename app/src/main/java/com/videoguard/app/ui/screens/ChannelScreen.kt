package com.videoguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
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
import com.videoguard.app.data.VideoRepository
import com.videoguard.app.data.YouTubeSearchEngine
import com.videoguard.app.model.UserPlaylist
import com.videoguard.app.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ChannelScreen(
    channelName: String,
    channelAvatarUrl: String,
    subscriberCount: String = "10M subscribers",
    onBack: () -> Unit,
    onVideoSelected: (VideoItem, List<VideoItem>) -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val subscribedChannels by UserDataRepository.subscribedChannels.collectAsState()
    val isSubscribed = subscribedChannels.contains(channelName)

    var selectedTab by remember { mutableStateOf("Videos") }
    var channelVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val channelPlaylists = remember(channelName, channelVideos) {
        val vids = channelVideos.ifEmpty { VideoRepository.sampleVideos }
        listOf(
            UserPlaylist(
                id = "pl_${channelName.replace("[^A-Za-z0-9]".toRegex(), "_")}_official",
                title = "$channelName - Official Music Videos",
                description = "Complete collection of music videos and single releases from $channelName.",
                videos = vids
            ),
            UserPlaylist(
                id = "pl_${channelName.replace("[^A-Za-z0-9]".toRegex(), "_")}_hits",
                title = "$channelName - Greatest Hits & Essentials",
                description = "The biggest hits and most-streamed tracks from $channelName.",
                videos = vids.take(6)
            ),
            UserPlaylist(
                id = "pl_${channelName.replace("[^A-Za-z0-9]".toRegex(), "_")}_discography",
                title = "$channelName - Complete Discography",
                description = "Studio albums, B-sides, and featured collaborations.",
                videos = vids.reversed()
            ),
            UserPlaylist(
                id = "pl_${channelName.replace("[^A-Za-z0-9]".toRegex(), "_")}_live",
                title = "$channelName - Live Performances & World Tour",
                description = "Live on stage, festival sets, and acoustic performances.",
                videos = vids.takeLast(4).ifEmpty { vids }
            )
        )
    }

    // Collect initial local videos and fetch real live channel videos via YouTubeSearchEngine
    LaunchedEffect(channelName) {
        isLoading = true
        val localMatches = VideoRepository.sampleVideos.filter {
            it.channelName.equals(channelName, ignoreCase = true) ||
                    (it.artist.isNotBlank() && it.artist.contains(channelName, ignoreCase = true))
        }

        channelVideos = localMatches

        // Fetch live search results for this channel
        val liveVideos = withContext(Dispatchers.IO) {
            YouTubeSearchEngine.searchVideos("$channelName official")
        }

        val combined = (localMatches + liveVideos).distinctBy { it.id }
        if (combined.isNotEmpty()) {
            channelVideos = combined
        } else if (localMatches.isEmpty()) {
            channelVideos = VideoRepository.sampleVideos.take(6)
        }
        isLoading = false
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
                    text = channelName,
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
                IconButton(onClick = { /* More */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
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
            // Channel Banner Art
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF8E24AA),
                                    Color(0xFFE91E63),
                                    Color(0xFFFF5722)
                                )
                            )
                        )
                )
            }

            // Channel Header: Avatar, Name, Handle, Subs, Subscribe Button
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = channelAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=150" },
                            contentDescription = channelName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF333333))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = channelName,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFFAAAAAA),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val cleanHandle = "@" + channelName.replace("[^A-Za-z0-9]".toRegex(), "").lowercase()
                            Text(
                                text = "$cleanHandle • $subscriberCount",
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${channelVideos.size} videos • Official YouTube Artist",
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Subscribe Button Full Width
                    Button(
                        onClick = { UserDataRepository.toggleSubscribe(channelName) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubscribed) Color(0xFF272727) else Color.White,
                            contentColor = if (isSubscribed) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Text(
                            text = if (isSubscribed) "Subscribed" else "Subscribe",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons: Play All & Shuffle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (channelVideos.isNotEmpty()) {
                                    onVideoSelected(channelVideos.first(), channelVideos)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF272727),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play all",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play all", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (channelVideos.isNotEmpty()) {
                                    val shuffled = channelVideos.shuffled()
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
                                .height(40.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Tab Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Videos", "Shorts", "Playlists", "About").forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White else Color(0xFF272727))
                                .clickable { selectedTab = tab }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Tab Content
            when (selectedTab) {
                "Playlists" -> {
                    item {
                        Text(
                            text = "Created playlists",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    items(channelPlaylists, key = { it.id }) { playlist ->
                        ChannelPlaylistItem(
                            playlist = playlist,
                            channelName = channelName,
                            onClick = { onPlaylistClick(playlist) },
                            onPlayAll = {
                                if (playlist.videos.isNotEmpty()) {
                                    onVideoSelected(playlist.videos.first(), playlist.videos)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                "Shorts" -> {
                    items(channelVideos.take(10), key = { "short_${it.id}" }) { video ->
                        ChannelShortsItem(
                            video = video,
                            onClick = { onVideoSelected(video, channelVideos) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                "About" -> {
                    item {
                        ChannelAboutSection(
                            channelName = channelName,
                            subscriberCount = subscriberCount,
                            videoCount = channelVideos.size
                        )
                    }
                }
                else -> { // "Videos"
                    if (isLoading && channelVideos.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    } else if (channelVideos.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No videos found for this channel.",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        items(channelVideos, key = { it.id }) { video ->
                            ChannelVideoItem(
                                video = video,
                                onClick = { onVideoSelected(video, channelVideos) }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
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
private fun ChannelPlaylistItem(
    playlist: UserPlaylist,
    channelName: String,
    onClick: () -> Unit,
    onPlayAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with authentic YouTube playlist badge overlay
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222222))
        ) {
            AsyncImage(
                model = playlist.coverThumbnailUrl ?: playlist.videos.firstOrNull()?.thumbnailUrl,
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Translucent dark overlay on the right with playlist icon & count
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(46.dp)
                    .align(Alignment.CenterEnd)
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = "Playlist",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${playlist.videoCount}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$channelName • Playlist",
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "View full playlist",
                color = Color(0xFF3EA6FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Quick Play All button
        IconButton(
            onClick = onPlayAll,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play all",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ChannelShortsItem(
    video: VideoItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(8.dp))
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
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${video.views} views",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Shorts • ${video.views}",
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ChannelAboutSection(
    channelName: String,
    subscriberCount: String,
    videoCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Description",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Welcome to the official YouTube channel for $channelName. Subscribe for all official music videos, exclusive behind-the-scenes footage, live tour performances, and new releases.",
            color = Color(0xFFCCCCCC),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Channel details",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "https://www.youtube.com/@${channelName.replace("[^A-Za-z0-9]".toRegex(), "").lowercase()}", color = Color(0xFF3EA6FF), fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = subscriberCount, color = Color.White, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "$videoCount videos", color = Color.White, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "Joined YouTube 2011 • Official Artist Channel", color = Color(0xFFAAAAAA), fontSize = 13.sp)
        }
    }
}

@Composable
private fun ChannelVideoItem(
    video: VideoItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Thumbnail with duration
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
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
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = video.duration,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and meta
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${video.views} • ${video.uploadTime}",
                color = Color(0xFFAAAAAA),
                fontSize = 11.sp
            )
        }

        IconButton(
            onClick = { /* Menu */ },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
