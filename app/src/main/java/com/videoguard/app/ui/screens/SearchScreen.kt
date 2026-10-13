package com.videoguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.videoguard.app.data.UserDataRepository
import com.videoguard.app.data.YouTubeSearchEngine
import com.videoguard.app.model.AppMode
import com.videoguard.app.model.VideoItem
import com.videoguard.app.ui.theme.YouTubeDarkBg
import com.videoguard.app.ui.theme.YouTubeRed
import com.videoguard.app.ui.theme.YTMusicBlack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    currentMode: AppMode,
    onBack: () -> Unit,
    onVideoSelected: (VideoItem, List<VideoItem>) -> Unit,
    onChannelClick: (VideoItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val isMusic = currentMode == AppMode.YOUTUBE_MUSIC
    val bgColor = if (isMusic) YTMusicBlack else YouTubeDarkBg
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var suggestionsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    val searchHistory by UserDataRepository.searchHistory.collectAsState()

    // Infinite scroll state
    val searchListState = rememberLazyListState()
    var isLoadingMoreSearch by remember { mutableStateOf(false) }
    var seenSearchIds by remember { mutableStateOf(setOf<String>()) }

    // Detect when user is near the bottom of search results
    val shouldLoadMoreSearch = remember {
        derivedStateOf {
            val layoutInfo = searchListState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = layoutInfo.totalItemsCount
            hasSearched && total > 0 && lastVisible >= total - 3
        }
    }

    // Load more when near bottom
    LaunchedEffect(shouldLoadMoreSearch.value) {
        if (shouldLoadMoreSearch.value && !isLoadingMoreSearch && searchQuery.isNotBlank()) {
            isLoadingMoreSearch = true
            try {
                val more = YouTubeSearchEngine.searchVideos(searchQuery, isMusicOnly = isMusic)
                val currentIds = seenSearchIds
                val newItems = more.filter { it.id !in currentIds }
                if (newItems.isNotEmpty()) {
                    seenSearchIds = currentIds + newItems.map { it.id }
                    searchResults = searchResults + newItems
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMoreSearch = false
            }
        }
    }

    val presetChips = listOf(
        "Malayalam", "Malayalam Songs", "The Weeknd", "Lofi Beats",
        "Imagine Dragons", "Dua Lipa", "Taylor Swift", "Trending News"
    )

    fun performSearch(query: String) {
        if (query.isBlank()) return
        val trimmed = query.trim()
        searchQuery = trimmed
        suggestionsList = emptyList()
        isSearching = true
        hasSearched = true
        seenSearchIds = emptySet()
        keyboardController?.hide()
        UserDataRepository.addSearchQuery(trimmed)

        coroutineScope.launch {
            try {
                val results = YouTubeSearchEngine.searchVideos(trimmed, isMusicOnly = isMusic)
                searchResults = results
                seenSearchIds = results.map { it.id }.toSet()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSearching = false
            }
        }
    }

    // Dynamic suggestions fetcher on typing
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && !hasSearched) {
            debounceJob?.cancel()
            debounceJob = coroutineScope.launch {
                delay(250)
                suggestionsList = YouTubeSearchEngine.getSuggestions(searchQuery)
            }
        } else if (searchQuery.isBlank()) {
            suggestionsList = emptyList()
            searchResults = emptyList()
            hasSearched = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        // Top Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF272727))
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFFAAAAAA),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            hasSearched = false
                        },
                        placeholder = {
                            Text(
                                if (isMusic) "Search songs, artists, albums..." else "Search YouTube...",
                                color = Color(0xFFAAAAAA),
                                fontSize = 15.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            performSearch(searchQuery)
                        }),
                        modifier = Modifier.weight(1f)
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                searchResults = emptyList()
                                suggestionsList = emptyList()
                                hasSearched = false
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            IconButton(onClick = { /* Voice Search */ }) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    tint = Color.White
                )
            }
        }

        // Suggestions Horizontal Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetChips.forEach { chipText ->
                val isSelected = searchQuery.equals(chipText, ignoreCase = true)
                AssistChip(
                    onClick = {
                        performSearch(chipText)
                    },
                    label = {
                        Text(
                            text = chipText,
                            color = if (isSelected) Color.Black else Color(0xFFF1F1F1),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) Color.White else Color(0xFF272727)
                    ),
                    border = null,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        HorizontalDivider(color = Color(0xFF2E2E2E), thickness = 1.dp)

        // Loading Indicator
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = if (isMusic) Color.White else YouTubeRed,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Searching YouTube...",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (hasSearched) {
            if (searchResults.isEmpty()) {
                // No Results State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "No Results",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No results found for \"$searchQuery\"",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Try different keywords or check out suggestions above",
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            } else {
                // Results Section
                LazyColumn(
                    state = searchListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    if (isMusic) {
                        // YouTube Music search results
                        items(searchResults, key = { "music_${it.id}" }) { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onVideoSelected(track, searchResults) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = track.thumbnailUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
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
                                        text = "${track.channelName} • ${track.duration}",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        // YouTube Video search results
                        items(searchResults, key = { "yt_${it.id}" }) { video ->
                            YouTubeVideoCard(
                                video = video,
                                onClick = { onVideoSelected(video, searchResults) },
                                onChannelClick = { onChannelClick(video) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Loading more spinner at the bottom
                    if (isLoadingMoreSearch) {
                        item(key = "load_more_spinner") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = if (isMusic) Color.White else YouTubeRed,
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }
                    }
                }
            }
        } else if (suggestionsList.isNotEmpty()) {
            // Live Autocomplete Suggestions List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp)
            ) {
                // Matching search history items at the top
                val matchingHistory = searchHistory.filter {
                    it.contains(searchQuery.trim(), ignoreCase = true)
                }.take(3)

                if (matchingHistory.isNotEmpty()) {
                    items(matchingHistory) { historyItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { performSearch(historyItem) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "Search history",
                                tint = Color(0xFFBB86FC),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = historyItem,
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    searchQuery = historyItem
                                    hasSearched = false
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Insert",
                                    tint = Color(0xFF888888),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .rotate(45f)
                                )
                            }
                            IconButton(
                                onClick = { UserDataRepository.removeSearchQuery(historyItem) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color(0xFF888888),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Autocomplete suggestions
                val suggestionsFiltered = suggestionsList.filter { sug ->
                    matchingHistory.none { it.equals(sug, ignoreCase = true) }
                }
                items(suggestionsFiltered) { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { performSearch(suggestion) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search suggestion",
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = suggestion,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                searchQuery = suggestion
                                hasSearched = false
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Insert into search bar",
                                tint = Color(0xFF888888),
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(45f)
                            )
                        }
                    }
                }
            }
        } else {
            // RECENT SEARCH HISTORY SECTION
            if (searchHistory.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent searches",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Clear all",
                                color = Color(0xFF3EA6FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clickable { UserDataRepository.clearSearchHistory() }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }
                    }

                    items(searchHistory) { historyItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { performSearch(historyItem) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "Search history",
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = historyItem,
                                color = Color.White,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            // Insert arrow button (North-West)
                            IconButton(
                                onClick = {
                                    searchQuery = historyItem
                                    hasSearched = false
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Insert into search bar",
                                    tint = Color(0xFF888888),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .rotate(45f)
                                )
                            }
                            // Delete button (X)
                            IconButton(
                                onClick = { UserDataRepository.removeSearchQuery(historyItem) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove from history",
                                    tint = Color(0xFF888888),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "No search history",
                        tint = Color(0xFF555555),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No recent searches",
                        color = Color.LightGray,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Searches you make will appear here",
                        color = Color(0xFF777777),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
