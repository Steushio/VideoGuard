package com.videoguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.videoguard.app.data.VideoRepository
import com.videoguard.app.model.VideoItem
import com.videoguard.app.ui.theme.YTMusicBlack
import com.videoguard.app.ui.theme.YTMusicRedAccent

@Composable
fun YouTubeMusicScreen(
    onTrackSelected: (VideoItem, List<VideoItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMood by remember { mutableStateOf("Energize") }
    val moods = VideoRepository.musicCategories
    val musicTracks = remember(selectedMood) {
        VideoRepository.getMusicByMood(selectedMood)
    }
    val allMusicTracks = remember { VideoRepository.getMusicTracks() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(YTMusicBlack)
    ) {
        // Mood / Activity Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moods.forEach { mood ->
                    val isSelected = mood == selectedMood
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMood = mood },
                        label = {
                            Text(
                                text = mood,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF212121),
                            selectedContainerColor = Color.White
                        ),
                        border = null,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Section: "Quick picks" (The signature YT Music feature)
        item {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "START RADIO FROM A SONG",
                            color = Color(0xFF909090),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Quick picks",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            if (allMusicTracks.isNotEmpty()) onTrackSelected(allMusicTracks.first(), allMusicTracks)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Play all", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // Quick Picks Track Items
        items(musicTracks.take(4), key = { "quick_${it.id}" }) { track ->
            QuickPickTrackRow(
                track = track,
                onClick = { onTrackSelected(track, musicTracks) }
            )
        }

        // Section: "Listen again" Carousel
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                Text(
                    text = "Listen again",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(allMusicTracks, key = { "again_${it.id}" }) { track ->
                    AlbumCard(
                        track = track,
                        onClick = { onTrackSelected(track, allMusicTracks) }
                    )
                }
            }
        }

        // Section: "Recommended Music Videos" Carousel
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                Text(
                    text = "Recommended music videos",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(allMusicTracks.reversed(), key = { "video_${it.id}" }) { track ->
                    VideoMusicCard(
                        track = track,
                        onClick = { onTrackSelected(track, allMusicTracks.reversed()) }
                    )
                }
            }
        }

        // Section: "Top 100 Charts"
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                Text(
                    text = "Trending Charts",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(allMusicTracks, key = { "chart_${it.id}" }) { track ->
            QuickPickTrackRow(
                track = track,
                onClick = { onTrackSelected(track, allMusicTracks) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
private fun QuickPickTrackRow(
    track: VideoItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${track.artist.ifEmpty { track.channelName }} • ${track.duration}",
                color = Color(0xFF909090),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = { /* Menu */ },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = Color(0xFFAAAAAA),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AlbumCard(
    track: VideoItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = track.album,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = track.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist.ifEmpty { track.channelName },
            color = Color(0xFF909090),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoMusicCard(
    track: VideoItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(width = 220.dp, height = 124.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(track.duration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = track.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${track.artist.ifEmpty { track.channelName }} • ${track.views}",
            color = Color(0xFF909090),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
