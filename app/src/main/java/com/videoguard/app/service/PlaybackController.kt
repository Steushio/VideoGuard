package com.videoguard.app.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.videoguard.app.data.VideoRepository
import com.videoguard.app.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object PlaybackController {

    private val _currentTrack = MutableStateFlow<VideoItem?>(null)
    val currentTrack: StateFlow<VideoItem?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBackgroundPlayEnabled = MutableStateFlow(true)
    val isBackgroundPlayEnabled: StateFlow<Boolean> = _isBackgroundPlayEnabled.asStateFlow()

    private val _playbackQueue = MutableStateFlow<List<VideoItem>>(emptyList())
    val playbackQueue: StateFlow<List<VideoItem>> = _playbackQueue.asStateFlow()

    private val _currentTimeSeconds = MutableStateFlow(0f)
    val currentTimeSeconds: StateFlow<Float> = _currentTimeSeconds.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0f)
    val durationSeconds: StateFlow<Float> = _durationSeconds.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackQuality = MutableStateFlow("Auto (1080p)")
    val playbackQuality: StateFlow<String> = _playbackQuality.asStateFlow()

    private val _isLoopEnabled = MutableStateFlow(false)
    val isLoopEnabled: StateFlow<Boolean> = _isLoopEnabled.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    // Listener to dispatch JavaScript commands directly to the active YouTube WebView
    private var webViewCommandListener: ((String) -> Unit)? = null

    fun setCommandListener(listener: ((String) -> Unit)?) {
        webViewCommandListener = listener
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        sendCommand("setPlaybackRate($speed)")
    }

    fun setPlaybackQuality(quality: String) {
        _playbackQuality.value = quality
        val ytQuality = when {
            quality.contains("1080") -> "hd1080"
            quality.contains("720") -> "hd720"
            quality.contains("480") -> "large"
            quality.contains("360") -> "medium"
            else -> "default"
        }
        sendCommand("setPlaybackQuality('$ytQuality')")
    }

    fun toggleLoop() {
        _isLoopEnabled.value = !_isLoopEnabled.value
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes != null && minutes > 0) {
            sleepTimerJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                kotlinx.coroutines.delay(minutes * 60 * 1000L)
                pause()
                _sleepTimerMinutes.value = null
            }
        }
    }

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        com.videoguard.app.data.UserDataRepository.init(context)
        com.videoguard.app.data.AccountRepository.init(context)
    }

    fun playTrack(track: VideoItem, queue: List<VideoItem> = emptyList()) {
        _currentTrack.value = track
        _isPlaying.value = true

        // Automatically record to User History
        com.videoguard.app.data.UserDataRepository.addToHistory(track)

        if (queue.isNotEmpty()) {
            _playbackQueue.value = queue
        } else if (_playbackQueue.value.none { it.id == track.id }) {
            _playbackQueue.value = listOf(track)
        }

        startOrUpdateService()
        sendCommand("loadVideo('${track.id}')")
        sendCommand("setPlaybackState(true)")
    }

    fun setQueue(queue: List<VideoItem>) {
        if (queue.isNotEmpty()) {
            _playbackQueue.value = queue
        }
    }

    fun setPlayingState(playing: Boolean) {
        if (_isPlaying.value != playing) {
            _isPlaying.value = playing
            if (_currentTrack.value != null) {
                startOrUpdateService()
            }
        }
    }

    fun play() {
        _isPlaying.value = true
        sendCommand("setPlaybackState(true)")
        startOrUpdateService()
    }

    fun pause() {
        _isPlaying.value = false
        sendCommand("setPlaybackState(false)")
        startOrUpdateService()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun updateProgress(current: Float, duration: Float) {
        _currentTimeSeconds.value = current
        if (duration > 0f) {
            _durationSeconds.value = duration
        }
    }

    fun seekTo(seconds: Float) {
        _currentTimeSeconds.value = seconds
        sendCommand("seekTo($seconds)")
    }

    fun seekBy(seconds: Int) {
        val newTime = (_currentTimeSeconds.value + seconds).coerceAtLeast(0f)
        _currentTimeSeconds.value = newTime
        sendCommand("seekBy($seconds)")
    }

    fun nextTrack() {
        val current = _currentTrack.value ?: return
        val queue = _playbackQueue.value.ifEmpty { VideoRepository.sampleVideos }
        val idx = queue.indexOfFirst { it.id == current.id }
        val nextIdx = if (idx in 0 until queue.lastIndex) idx + 1 else 0
        playTrack(queue[nextIdx], queue)
    }

    fun prevTrack() {
        val current = _currentTrack.value ?: return
        val queue = _playbackQueue.value.ifEmpty { VideoRepository.sampleVideos }
        val idx = queue.indexOfFirst { it.id == current.id }
        val prevIdx = if (idx > 0) idx - 1 else queue.lastIndex
        playTrack(queue[prevIdx], queue)
    }

    fun stop() {
        _isPlaying.value = false
        _currentTrack.value = null
        sendCommand("setPlaybackState(false)")
        stopService()
    }

    fun toggleBackgroundPlay() {
        _isBackgroundPlayEnabled.value = !_isBackgroundPlayEnabled.value
        if (!_isBackgroundPlayEnabled.value && !_isPlaying.value) {
            stopService()
        }
    }

    private fun sendCommand(command: String) {
        try {
            webViewCommandListener?.invoke(command)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startOrUpdateService() {
        val ctx = appContext ?: return
        val track = _currentTrack.value ?: return

        val intent = Intent(ctx, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_UPDATE
            putExtra(MediaPlaybackService.EXTRA_TITLE, track.title)
            putExtra(MediaPlaybackService.EXTRA_ARTIST, track.artist.ifEmpty { track.channelName })
            putExtra(MediaPlaybackService.EXTRA_IS_PLAYING, _isPlaying.value)
            putExtra(MediaPlaybackService.EXTRA_THUMBNAIL_URL, track.thumbnailUrl)
            putExtra(MediaPlaybackService.EXTRA_DURATION_SEC, _durationSeconds.value)
            putExtra(MediaPlaybackService.EXTRA_POSITION_SEC, _currentTimeSeconds.value)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopService() {
        val ctx = appContext ?: return
        val intent = Intent(ctx, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_STOP
        }
        ctx.stopService(intent)
    }
}
