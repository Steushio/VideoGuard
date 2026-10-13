package com.videoguard.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.videoguard.app.model.AppMode
import com.videoguard.app.model.MusicPlayerViewMode
import com.videoguard.app.model.UserPlaylist
import com.videoguard.app.model.VideoItem
import com.videoguard.app.model.YouTubeMusicTab
import com.videoguard.app.model.YouTubeTab
import com.videoguard.app.service.PlaybackController
import com.videoguard.app.ui.components.AccountBottomSheet
import com.videoguard.app.ui.components.BottomNavBar
import com.videoguard.app.ui.components.GoogleAuthDialog
import com.videoguard.app.ui.components.MiniPlayerBar
import com.videoguard.app.ui.components.PlayerControlsOverlay
import com.videoguard.app.ui.components.TopBar
import com.videoguard.app.ui.components.YouTubeVideoPlayer
import com.videoguard.app.ui.theme.YouTubeDarkBg
import com.videoguard.app.ui.theme.YTMusicBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var appMode by remember { mutableStateOf(AppMode.YOUTUBE) }
    var musicViewMode by remember { mutableStateOf(MusicPlayerViewMode.SONG) }
    var selectedYTTab by remember { mutableStateOf(YouTubeTab.HOME) }
    var selectedMusicTab by remember { mutableStateOf(YouTubeMusicTab.HOME) }
    var homeScrollToTopTrigger by remember { mutableStateOf(0) }

    val currentVideo by PlaybackController.currentTrack.collectAsState()
    val isPlaying by PlaybackController.isPlaying.collectAsState()
    val isBackgroundPlay by PlaybackController.isBackgroundPlayEnabled.collectAsState()
    val currentTime by PlaybackController.currentTimeSeconds.collectAsState()
    val duration by PlaybackController.durationSeconds.collectAsState()

    val context = LocalContext.current
    val activity = context as? Activity
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showGoogleAuthDialog by remember { mutableStateOf(false) }

    // Synchronize window system bars & screen orientation with isFullscreen
    DisposableEffect(isFullscreen) {
        val window = activity?.window
        if (window != null && activity != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (isFullscreen && activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                val window = activity.window
                if (window != null) {
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    // Automatically exit fullscreen if player is collapsed or mode is changed
    LaunchedEffect(isPlayerExpanded, appMode) {
        if (!isPlayerExpanded || appMode != AppMode.YOUTUBE) {
            if (isFullscreen) isFullscreen = false
        }
    }

    // Channel page & Playlist page navigation states
    var activeChannelVideo by remember { mutableStateOf<VideoItem?>(null) }
    var activePlaylist by remember { mutableStateOf<UserPlaylist?>(null) }

    // Mode-aware back button handling
    BackHandler(
        enabled = isFullscreen || isPlayerExpanded || isSearchActive || showAccountSheet ||
                showGoogleAuthDialog || activeChannelVideo != null || activePlaylist != null ||
                (appMode == AppMode.YOUTUBE && selectedYTTab != YouTubeTab.HOME) ||
                (appMode == AppMode.YOUTUBE_MUSIC && selectedMusicTab != YouTubeMusicTab.HOME)
    ) {
        if (showGoogleAuthDialog) {
            showGoogleAuthDialog = false
        } else if (showAccountSheet) {
            showAccountSheet = false
        } else if (isFullscreen) {
            isFullscreen = false
        } else if (isPlayerExpanded) {
            isPlayerExpanded = false
        } else if (activePlaylist != null) {
            activePlaylist = null
        } else if (activeChannelVideo != null) {
            activeChannelVideo = null
        } else if (isSearchActive) {
            isSearchActive = false
        } else if (appMode == AppMode.YOUTUBE && selectedYTTab != YouTubeTab.HOME) {
            selectedYTTab = YouTubeTab.HOME
        } else if (appMode == AppMode.YOUTUBE_MUSIC && selectedMusicTab != YouTubeMusicTab.HOME) {
            selectedMusicTab = YouTubeMusicTab.HOME
        }
    }

    val isMusic = appMode == AppMode.YOUTUBE_MUSIC
    val bgColor = if (isMusic) YTMusicBlack else YouTubeDarkBg

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val showVideoInYT = isPlayerExpanded && appMode == AppMode.YOUTUBE
        val showVideoInMusic = isPlayerExpanded && appMode == AppMode.YOUTUBE_MUSIC && musicViewMode == MusicPlayerViewMode.VIDEO

        // Persistent Audio & Video Player Engine (Stays alive across all navigations & backgrounding!)
        if (currentVideo != null) {
            val playerModifier = when {
                isFullscreen -> Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .zIndex(50f)
                showVideoInYT -> Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .aspectRatio(16f / 9f)
                    .zIndex(15f)
                showVideoInMusic -> Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .align(Alignment.Center)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .zIndex(15f)
                else -> Modifier
                    .size(1.dp)
                    .alpha(0.01f)
                    .zIndex(-10f)
            }

            Box(modifier = playerModifier) {
                YouTubeVideoPlayer(
                    videoId = currentVideo!!.id,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxSize()
                )
                if (showVideoInYT) {
                    PlayerControlsOverlay(
                        isPlaying = isPlaying,
                        currentTime = currentTime,
                        duration = duration,
                        onPlayPauseToggle = { PlaybackController.togglePlayPause() },
                        onNext = { PlaybackController.nextTrack() },
                        onPrev = { PlaybackController.prevTrack() },
                        onSeekBy = { PlaybackController.seekBy(it) },
                        onSeekTo = { PlaybackController.seekTo(it) },
                        onCollapse = {
                            if (isFullscreen) {
                                isFullscreen = false
                            } else {
                                isPlayerExpanded = false
                            }
                        },
                        isFullscreen = isFullscreen,
                        onToggleFullscreen = { isFullscreen = !isFullscreen }
                    )
                } else if (showVideoInMusic) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { /* Consume touches on video in music mode so it does not toggle play/pause */ }
                    )
                }
            }
        }

        // Persistent Shell Structure
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Mode-Aware Top Bar (shown on Feed / Home tabs when no child overlay is active)
            if (!isSearchActive && activeChannelVideo == null && activePlaylist == null) {
                if ((appMode == AppMode.YOUTUBE && selectedYTTab != YouTubeTab.YOU) ||
                    (appMode == AppMode.YOUTUBE_MUSIC && selectedMusicTab != YouTubeMusicTab.LIBRARY)
                ) {
                    TopBar(
                        currentMode = appMode,
                        onModeChanged = { newMode ->
                            appMode = newMode
                        },
                        onTitleClick = {
                            val wasAlreadyOnHome = if (appMode == AppMode.YOUTUBE) {
                                selectedYTTab == YouTubeTab.HOME
                            } else {
                                selectedMusicTab == YouTubeMusicTab.HOME
                            }
                            if (wasAlreadyOnHome) {
                                homeScrollToTopTrigger++
                            } else {
                                if (appMode == AppMode.YOUTUBE) {
                                    selectedYTTab = YouTubeTab.HOME
                                } else {
                                    selectedMusicTab = YouTubeMusicTab.HOME
                                }
                            }
                        },
                        onSearchClick = {
                            isSearchActive = true
                        },
                        isBackgroundPlay = isBackgroundPlay,
                        onToggleBackgroundPlay = {
                            PlaybackController.toggleBackgroundPlay()
                        },
                        onAccountClick = {
                            if (appMode == AppMode.YOUTUBE) {
                                selectedYTTab = YouTubeTab.YOU
                            } else {
                                showAccountSheet = true
                            }
                        }
                    )
                }
            }

            // Main Content Area (Weight 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isSearchActive -> {
                        SearchScreen(
                            currentMode = appMode,
                            onBack = { isSearchActive = false },
                            onVideoSelected = { video, queue ->
                                PlaybackController.playTrack(video, queue)
                                isPlayerExpanded = true
                                isSearchActive = false
                            },
                            onChannelClick = { video ->
                                activeChannelVideo = video
                                isSearchActive = false
                            }
                        )
                    }
                    activePlaylist != null -> {
                        PlaylistDetailScreen(
                            playlistId = activePlaylist!!.id,
                            initialPlaylist = activePlaylist,
                            onBack = { activePlaylist = null },
                            onVideoSelected = { video, queue ->
                                PlaybackController.playTrack(video, queue)
                                isPlayerExpanded = true
                            }
                        )
                    }
                    activeChannelVideo != null -> {
                        ChannelScreen(
                            channelName = activeChannelVideo!!.channelName,
                            channelAvatarUrl = activeChannelVideo!!.channelAvatarUrl,
                            subscriberCount = activeChannelVideo!!.subscriberCount,
                            onBack = { activeChannelVideo = null },
                            onVideoSelected = { video, queue ->
                                PlaybackController.playTrack(video, queue)
                                isPlayerExpanded = true
                            },
                            onPlaylistClick = { playlist ->
                                activePlaylist = playlist
                            }
                        )
                    }
                    else -> {
                        when (appMode) {
                            AppMode.YOUTUBE -> {
                                when (selectedYTTab) {
                                    YouTubeTab.YOU -> {
                                        YouScreen(
                                            onVideoSelected = { video, queue ->
                                                PlaybackController.playTrack(video, queue)
                                                isPlayerExpanded = true
                                            },
                                            onPlaylistClick = { playlist ->
                                                activePlaylist = playlist
                                            },
                                            onOpenSearch = { isSearchActive = true },
                                            onOpenAccountSheet = { showAccountSheet = true },
                                            onSignInClick = { showGoogleAuthDialog = true }
                                        )
                                    }
                                    else -> {
                                        YouTubeFeedScreen(
                                            onVideoSelected = { video, queue ->
                                                PlaybackController.playTrack(video, queue)
                                                isPlayerExpanded = true
                                            },
                                            onSwitchToMusic = {
                                                appMode = AppMode.YOUTUBE_MUSIC
                                            },
                                            onChannelClick = { video ->
                                                activeChannelVideo = video
                                            },
                                            scrollToTopTrigger = homeScrollToTopTrigger
                                        )
                                    }
                                }
                            }
                            AppMode.YOUTUBE_MUSIC -> {
                                when (selectedMusicTab) {
                                    YouTubeMusicTab.LIBRARY -> {
                                        MusicLibraryScreen(
                                            onTrackSelected = { track, queue ->
                                                PlaybackController.playTrack(track, queue)
                                                isPlayerExpanded = true
                                            },
                                            onPlaylistClick = { playlist ->
                                                activePlaylist = playlist
                                            },
                                            onOpenSearch = { isSearchActive = true },
                                            onOpenAccountSheet = { showAccountSheet = true }
                                        )
                                    }
                                    else -> {
                                        YouTubeMusicScreen(
                                            onTrackSelected = { track, queue ->
                                                PlaybackController.playTrack(track, queue)
                                                isPlayerExpanded = true
                                            },
                                            scrollToTopTrigger = homeScrollToTopTrigger
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Mini Player Bar (docked above Bottom Nav when video is loaded & minimized)
            if (currentVideo != null && !isPlayerExpanded) {
                MiniPlayerBar(
                    video = currentVideo,
                    isPlaying = isPlaying,
                    currentMode = appMode,
                    onExpand = { isPlayerExpanded = true },
                    onPlayPauseToggle = { PlaybackController.togglePlayPause() },
                    onClose = { PlaybackController.stop() }
                )
            }

            // Bottom Navigation Bar
            if (!isPlayerExpanded) {
                BottomNavBar(
                    currentMode = appMode,
                    selectedYTTab = selectedYTTab,
                    selectedMusicTab = selectedMusicTab,
                    isSearchActive = isSearchActive,
                    onHomeClick = {
                        val wasAlreadyOnHome = if (appMode == AppMode.YOUTUBE) {
                            !isSearchActive && activeChannelVideo == null && activePlaylist == null && selectedYTTab == YouTubeTab.HOME
                        } else {
                            !isSearchActive && activeChannelVideo == null && activePlaylist == null && selectedMusicTab == YouTubeMusicTab.HOME
                        }
                        activeChannelVideo = null
                        activePlaylist = null
                        isSearchActive = false
                        if (wasAlreadyOnHome) {
                            homeScrollToTopTrigger++
                        }
                        if (appMode == AppMode.YOUTUBE) {
                            selectedYTTab = YouTubeTab.HOME
                        } else {
                            selectedMusicTab = YouTubeMusicTab.HOME
                        }
                    },
                    onSearchClick = {
                        activeChannelVideo = null
                        activePlaylist = null
                        isSearchActive = true
                    },
                    onAccountClick = {
                        activeChannelVideo = null
                        activePlaylist = null
                        isSearchActive = false
                        if (appMode == AppMode.YOUTUBE) {
                            if (selectedYTTab == YouTubeTab.YOU) {
                                showAccountSheet = true
                            } else {
                                selectedYTTab = YouTubeTab.YOU
                            }
                        } else {
                            if (selectedMusicTab == YouTubeMusicTab.LIBRARY) {
                                showAccountSheet = true
                            } else {
                                selectedMusicTab = YouTubeMusicTab.LIBRARY
                            }
                        }
                    },
                    onYTTabSelected = { tab ->
                        activeChannelVideo = null
                        activePlaylist = null
                        isSearchActive = false
                        selectedYTTab = tab
                    },
                    onMusicTabSelected = { tab ->
                        activeChannelVideo = null
                        activePlaylist = null
                        isSearchActive = false
                        selectedMusicTab = tab
                    }
                )
            }
        }

        // Full Screen Player Overlay (Animated Slide/Fade)
        AnimatedVisibility(
            visible = isPlayerExpanded && currentVideo != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
                .pointerInput(Unit) {
                    detectTapGestures { /* Absorb background touches */ }
                }
        ) {
            currentVideo?.let { video ->
                when (appMode) {
                    AppMode.YOUTUBE -> {
                        VideoDetailScreen(
                            video = video,
                            isPlaying = isPlaying,
                            onPlayPauseToggle = { PlaybackController.togglePlayPause() },
                            onCollapse = { isPlayerExpanded = false },
                            onSwitchToMusicMode = {
                                appMode = AppMode.YOUTUBE_MUSIC
                            },
                            onVideoSelected = { newVideo ->
                                val activeQueue = PlaybackController.playbackQueue.value.ifEmpty { listOf(video) }
                                PlaybackController.playTrack(newVideo, activeQueue)
                            },
                            onChannelClick = { chVideo ->
                                activeChannelVideo = chVideo
                                isPlayerExpanded = false
                            },
                            onNextTrack = { PlaybackController.nextTrack() },
                            onPrevTrack = { PlaybackController.prevTrack() },
                            isBackgroundPlay = isBackgroundPlay,
                            onToggleBackgroundPlay = {
                                PlaybackController.toggleBackgroundPlay()
                            }
                        )
                    }
                    AppMode.YOUTUBE_MUSIC -> {
                        MusicPlayerScreen(
                            track = video,
                            isPlaying = isPlaying,
                            onPlayPauseToggle = { PlaybackController.togglePlayPause() },
                            onCollapse = { isPlayerExpanded = false },
                            onNextTrack = { PlaybackController.nextTrack() },
                            onPrevTrack = { PlaybackController.prevTrack() },
                            onTrackSelected = { newTrack ->
                                val activeQueue = PlaybackController.playbackQueue.value.ifEmpty { listOf(video) }
                                PlaybackController.playTrack(newTrack, activeQueue)
                            },
                            isBackgroundPlay = isBackgroundPlay,
                            onToggleBackgroundPlay = {
                                PlaybackController.toggleBackgroundPlay()
                            },
                            viewMode = musicViewMode,
                            onViewModeChanged = { musicViewMode = it }
                        )
                    }
                }
            }
        }

        // Account Switcher Bottom Sheet
        if (showAccountSheet) {
            AccountBottomSheet(
                currentMode = appMode,
                onDismiss = { showAccountSheet = false }
            )
        }

        // Direct Google Sign-In Dialog
        if (showGoogleAuthDialog) {
            GoogleAuthDialog(
                onDismiss = { showGoogleAuthDialog = false },
                onLoginSuccess = { showGoogleAuthDialog = false }
            )
        }
    }
}
