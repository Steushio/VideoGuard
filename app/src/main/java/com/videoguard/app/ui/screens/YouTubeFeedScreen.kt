package com.videoguard.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
import com.videoguard.app.model.AppMode
import com.videoguard.app.model.VideoItem
import com.videoguard.app.ui.theme.YouTubeDarkBg
import com.videoguard.app.ui.theme.YTMusicRedAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeFeedScreen(
    onVideoSelected: (VideoItem, List<VideoItem>) -> Unit,
    onSwitchToMusic: () -> Unit,
    onChannelClick: (VideoItem) -> Unit = {},
    scrollToTopTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = VideoRepository.youtubeCategories
    var feedVideos by remember { mutableStateOf(VideoRepository.getVideosByCategory(selectedCategory)) }
    var isLoadingFeed by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val currentAccount by com.videoguard.app.data.AccountRepository.currentAccount.collectAsState()
    val synchronizedFeed by com.videoguard.app.data.YouTubeSyncEngine.homeFeed.collectAsState()

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    // Drag / animate scroll to top when Home button is clicked
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                // Already at the very top: trigger refresh like YouTube
                isRefreshing = true
                try {
                    val fresh = com.videoguard.app.data.YouTubeSyncEngine.fetchHomeFeed(selectedCategory)
                    if (fresh.isNotEmpty()) {
                        feedVideos = fresh
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isRefreshing = false
                }
            } else {
                // Smoothly drag scroll back to top
                if (listState.firstVisibleItemIndex > 8) {
                    listState.scrollToItem(8)
                }
                listState.animateScrollToItem(0)
            }
        }
    }

    // Initial load / category change
    LaunchedEffect(selectedCategory, currentAccount) {
        isLoadingFeed = true
        val online = com.videoguard.app.data.YouTubeSyncEngine.fetchHomeFeed(selectedCategory)
        if (online.isNotEmpty()) {
            feedVideos = online
        } else {
            feedVideos = VideoRepository.getVideosByCategory(selectedCategory)
        }
        isLoadingFeed = false
    }

    // Keep UI in sync when synchronizedFeed is updated (e.g. after fetchMoreHomeFeed appends)
    LaunchedEffect(synchronizedFeed) {
        if (synchronizedFeed.isNotEmpty() && selectedCategory == "All") {
            feedVideos = synchronizedFeed
        }
    }

    // Infinite scroll: detect when user is near the last item
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            totalItems > 0 && lastVisible >= totalItems - 4
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !isLoadingMore && !isLoadingFeed && !isRefreshing) {
            isLoadingMore = true
            com.videoguard.app.data.YouTubeSyncEngine.fetchMoreHomeFeed()
            // feedVideos will update via the synchronizedFeed LaunchedEffect above
            isLoadingMore = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                try {
                    val fresh = com.videoguard.app.data.YouTubeSyncEngine.fetchHomeFeed(selectedCategory)
                    if (fresh.isNotEmpty()) {
                        feedVideos = fresh
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isRefreshing = false
                }
            }
        },
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .background(YouTubeDarkBg),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Color(0xFF282828),
                color = Color.White
            )
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
        // Category Chips Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF0F0F0F) else Color(0xFFF1F1F1)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF272727),
                            selectedContainerColor = Color(0xFFF1F1F1)
                        ),
                        border = null,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        if (isLoadingFeed && feedVideos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        // Videos Feed
        items(feedVideos, key = { it.id }) { video ->
            YouTubeVideoCard(
                video = video,
                onClick = { onVideoSelected(video, feedVideos) },
                onChannelClick = { onChannelClick(video) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Loading More spinner at bottom
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
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

        // Bottom space so mini-player won't hide last item
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
}

@Composable
fun YouTubeVideoCard(
    video: VideoItem,
    onClick: () -> Unit,
    onChannelClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Thumbnail with Duration overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Duration Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = video.duration,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Channel Avatar and Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = video.channelAvatarUrl,
                contentDescription = video.channelName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onChannelClick)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    color = Color(0xFFF1F1F1),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${video.channelName} • ${video.views} • ${video.uploadTime}",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onChannelClick)
                )
            }

            IconButton(
                onClick = { /* Menu */ },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
