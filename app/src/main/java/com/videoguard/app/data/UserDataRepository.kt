package com.videoguard.app.data

import android.content.Context
import android.content.SharedPreferences
import com.videoguard.app.model.UserPlaylist
import com.videoguard.app.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object UserDataRepository {

    private const val PREFS_NAME = "videoguard_user_data"
    private const val KEY_HISTORY = "history_json"
    private const val KEY_PLAYLISTS = "playlists_json"
    private const val KEY_SUBSCRIPTIONS = "subscriptions_json"
    private const val KEY_SEARCH_HISTORY = "search_history_json"

    private var prefs: SharedPreferences? = null

    private val _history = MutableStateFlow<List<VideoItem>>(emptyList())
    val history: StateFlow<List<VideoItem>> = _history.asStateFlow()

    private val _playlists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val playlists: StateFlow<List<UserPlaylist>> = _playlists.asStateFlow()

    private val _subscribedChannels = MutableStateFlow<Set<String>>(emptySet())
    val subscribedChannels: StateFlow<Set<String>> = _subscribedChannels.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        // Load History
        val historyStr = sp.getString(KEY_HISTORY, null)
        if (!historyStr.isNullOrBlank()) {
            try {
                val arr = JSONArray(historyStr)
                val list = mutableListOf<VideoItem>()
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { list.add(jsonToVideo(it)) }
                }
                _history.value = list.filter {
                    it.id != "jfKfPfyJRdk" && it.id != "5qap5aO4i9A" && it.id != "dtp6bOj69QD" &&
                            it.id != "dvgZkm1xWPE" && it.id != "OPf0YbXqDm0"
                }
                saveHistory()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load Playlists
        val playlistsStr = sp.getString(KEY_PLAYLISTS, null)
        if (!playlistsStr.isNullOrBlank()) {
            try {
                val arr = JSONArray(playlistsStr)
                val list = mutableListOf<UserPlaylist>()
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { list.add(jsonToPlaylist(it)) }
                }
                _playlists.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (_playlists.value.isEmpty()) {
            // Seed default playlists
            _playlists.value = listOf(
                UserPlaylist(
                    id = "liked_videos",
                    title = "Liked videos",
                    description = "Videos you've liked",
                    videos = emptyList(),
                    isSystem = true,
                    iconType = "like"
                ),
                UserPlaylist(
                    id = "watch_later",
                    title = "Watch later",
                    description = "Saved for later viewing",
                    videos = emptyList(),
                    isSystem = true,
                    iconType = "watch_later"
                ),
                UserPlaylist(
                    id = "favorites",
                    title = "Favorites",
                    description = "Personal favorite tracks & clips",
                    videos = emptyList(),
                    isSystem = false,
                    iconType = "playlist"
                )
            )
            savePlaylists()
        }

        // Load Subscriptions
        val subsStr = sp.getString(KEY_SUBSCRIPTIONS, null)
        if (!subsStr.isNullOrBlank()) {
            try {
                val arr = JSONArray(subsStr)
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getString(i))
                }
                _subscribedChannels.value = set
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            _subscribedChannels.value = setOf("The Weeknd", "Dua Lipa")
            saveSubscriptions()
        }
        // Load Search History
        val searchHistoryStr = sp.getString(KEY_SEARCH_HISTORY, null)
        if (!searchHistoryStr.isNullOrBlank()) {
            try {
                val arr = JSONArray(searchHistoryStr)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i)
                    if (item.isNotBlank()) list.add(item)
                }
                _searchHistory.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Search History Management ---
    fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _searchHistory.value.toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val trimmedList = current.take(50)
        _searchHistory.value = trimmedList
        saveSearchHistory(trimmedList)
    }

    fun removeSearchQuery(query: String) {
        val current = _searchHistory.value.toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        _searchHistory.value = current
        saveSearchHistory(current)
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        saveSearchHistory(emptyList())
    }

    private fun saveSearchHistory(list: List<String>) {
        try {
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            prefs?.edit()?.putString(KEY_SEARCH_HISTORY, arr.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- History Management ---
    fun addToHistory(video: VideoItem) {
        val current = _history.value.toMutableList()
        current.removeAll { it.id == video.id }
        current.add(0, video)
        if (current.size > 100) {
            _history.value = current.take(100)
        } else {
            _history.value = current
        }
        saveHistory()
    }

    fun removeFromHistory(videoId: String) {
        _history.value = _history.value.filter { it.id != videoId }
        saveHistory()
    }

    fun clearHistory() {
        _history.value = emptyList()
        saveHistory()
    }

    fun setOnlineHistory(onlineVideos: List<VideoItem>) {
        if (onlineVideos.isNotEmpty()) {
            _history.value = onlineVideos.take(100)
            saveHistory()
        }
    }

    fun setOnlinePlaylists(onlinePlaylists: List<UserPlaylist>) {
        if (onlinePlaylists.isNotEmpty()) {
            val systemPlaylists = _playlists.value.filter { it.isSystem }
            val localCustom = _playlists.value.filter { it.id.startsWith("playlist_") && !it.isSystem }
            val existingVideosMap = _playlists.value.associate { it.id to it.videos }

            val mergedOnline = onlinePlaylists.map { pl ->
                val cached = existingVideosMap[pl.id]
                if (pl.videos.isNotEmpty()) {
                    pl
                } else if (!cached.isNullOrEmpty()) {
                    pl.copy(videos = cached, customCoverUrl = pl.customCoverUrl ?: cached.firstOrNull()?.thumbnailUrl)
                } else {
                    pl
                }
            }

            _playlists.value = systemPlaylists + mergedOnline + localCustom
            savePlaylists()
        }
    }

    fun updatePlaylistVideos(playlistId: String, videos: List<VideoItem>) {
        if (videos.isEmpty()) return
        if (playlistId == "liked_videos") {
            updateLikedVideos(videos)
            return
        }
        if (playlistId == "watch_later") {
            updateWatchLaterVideos(videos)
            return
        }
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val pl = current[index]
            val cover = pl.customCoverUrl ?: videos.firstOrNull()?.thumbnailUrl
            current[index] = pl.copy(videos = videos, customCoverUrl = cover)
            _playlists.value = current
            savePlaylists()
        }
    }

    fun updateWatchLaterVideos(watchLaterVideos: List<VideoItem>) {
        if (watchLaterVideos.isEmpty()) return
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == "watch_later" }
        if (index != -1) {
            current[index] = current[index].copy(videos = watchLaterVideos)
        } else {
            current.add(UserPlaylist(
                id = "watch_later",
                title = "Watch later",
                description = "Saved for later viewing",
                videos = watchLaterVideos,
                isSystem = true,
                iconType = "watch_later"
            ))
        }
        _playlists.value = current
        savePlaylists()
    }

    fun updateLikedVideos(likedVideos: List<VideoItem>) {
        if (likedVideos.isEmpty()) return
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == "liked_videos" }
        if (index != -1) {
            current[index] = current[index].copy(videos = likedVideos)
        } else {
            current.add(0, UserPlaylist(
                id = "liked_videos",
                title = "Liked videos",
                description = "Videos you've liked",
                videos = likedVideos,
                isSystem = true,
                iconType = "like"
            ))
        }
        _playlists.value = current
        savePlaylists()
    }

    private fun saveHistory() {
        val sp = prefs ?: return
        try {
            val arr = JSONArray()
            _history.value.forEach { arr.put(videoToJson(it)) }
            sp.edit().putString(KEY_HISTORY, arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Playlist Management ---
    fun isLiked(videoId: String): Boolean {
        val likedPlaylist = _playlists.value.find { it.id == "liked_videos" } ?: return false
        return likedPlaylist.videos.any { it.id == videoId }
    }

    fun toggleLiked(video: VideoItem) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == "liked_videos" }
        if (index != -1) {
            val playlist = current[index]
            val videos = playlist.videos.toMutableList()
            if (videos.any { it.id == video.id }) {
                videos.removeAll { it.id == video.id }
            } else {
                videos.add(0, video)
            }
            current[index] = playlist.copy(videos = videos)
            _playlists.value = current
            savePlaylists()
        }
    }

    fun isWatchLater(videoId: String): Boolean {
        val watchLater = _playlists.value.find { it.id == "watch_later" } ?: return false
        return watchLater.videos.any { it.id == videoId }
    }

    fun toggleWatchLater(video: VideoItem) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == "watch_later" }
        if (index != -1) {
            val playlist = current[index]
            val videos = playlist.videos.toMutableList()
            if (videos.any { it.id == video.id }) {
                videos.removeAll { it.id == video.id }
            } else {
                videos.add(0, video)
            }
            current[index] = playlist.copy(videos = videos)
            _playlists.value = current
            savePlaylists()
        }
    }

    fun createPlaylist(title: String): UserPlaylist {
        val newPlaylist = UserPlaylist(
            id = "playlist_" + UUID.randomUUID().toString().take(8),
            title = title.ifBlank { "New Playlist" },
            videos = emptyList(),
            isSystem = false,
            iconType = "playlist"
        )
        val current = _playlists.value.toMutableList()
        current.add(newPlaylist)
        _playlists.value = current
        savePlaylists()
        return newPlaylist
    }

    fun addVideoToPlaylist(playlistId: String, video: VideoItem) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = current[index]
            val videos = playlist.videos.toMutableList()
            if (videos.none { it.id == video.id }) {
                videos.add(video)
                current[index] = playlist.copy(videos = videos)
                _playlists.value = current
                savePlaylists()
            }
        }
    }

    fun removeVideoFromPlaylist(playlistId: String, videoId: String) {
        val current = _playlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = current[index]
            val videos = playlist.videos.filter { it.id != videoId }
            current[index] = playlist.copy(videos = videos)
            _playlists.value = current
            savePlaylists()
        }
    }

    fun deletePlaylist(playlistId: String) {
        _playlists.value = _playlists.value.filter { it.id != playlistId || it.isSystem }
        savePlaylists()
    }

    private fun savePlaylists() {
        val sp = prefs ?: return
        try {
            val arr = JSONArray()
            _playlists.value.forEach { arr.put(playlistToJson(it)) }
            sp.edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Subscriptions Management ---
    fun isSubscribed(channelName: String): Boolean {
        return _subscribedChannels.value.contains(channelName)
    }

    fun toggleSubscribe(channelName: String) {
        val current = _subscribedChannels.value.toMutableSet()
        if (current.contains(channelName)) {
            current.remove(channelName)
        } else {
            current.add(channelName)
        }
        _subscribedChannels.value = current
        saveSubscriptions()
    }

    private fun saveSubscriptions() {
        val sp = prefs ?: return
        try {
            val arr = JSONArray()
            _subscribedChannels.value.forEach { arr.put(it) }
            sp.edit().putString(KEY_SUBSCRIPTIONS, arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- JSON Helpers ---
    private fun videoToJson(v: VideoItem): JSONObject = JSONObject().apply {
        put("id", v.id)
        put("title", v.title)
        put("channelName", v.channelName)
        put("channelAvatarUrl", v.channelAvatarUrl)
        put("subscriberCount", v.subscriberCount)
        put("thumbnailUrl", v.thumbnailUrl)
        put("views", v.views)
        put("uploadTime", v.uploadTime)
        put("duration", v.duration)
        put("description", v.description)
        put("likes", v.likes)
        put("isMusicTrack", v.isMusicTrack)
        put("artist", v.artist)
        put("album", v.album)
        put("lyrics", v.lyrics)
        put("category", v.category)
        put("mood", v.mood)
    }

    private fun jsonToVideo(obj: JSONObject): VideoItem = VideoItem(
        id = obj.optString("id", ""),
        title = obj.optString("title", ""),
        channelName = obj.optString("channelName", ""),
        channelAvatarUrl = obj.optString("channelAvatarUrl", ""),
        subscriberCount = obj.optString("subscriberCount", "5.2M subscribers"),
        thumbnailUrl = obj.optString("thumbnailUrl", ""),
        views = obj.optString("views", ""),
        uploadTime = obj.optString("uploadTime", ""),
        duration = obj.optString("duration", ""),
        description = obj.optString("description", ""),
        likes = obj.optString("likes", "45K"),
        isMusicTrack = obj.optBoolean("isMusicTrack", false),
        artist = obj.optString("artist", ""),
        album = obj.optString("album", ""),
        lyrics = obj.optString("lyrics", ""),
        category = obj.optString("category", "All"),
        mood = obj.optString("mood", "Energize")
    )

    private fun playlistToJson(p: UserPlaylist): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("title", p.title)
        put("description", p.description)
        put("isSystem", p.isSystem)
        put("iconType", p.iconType)
        val arr = JSONArray()
        p.videos.forEach { arr.put(videoToJson(it)) }
        put("videos", arr)
    }

    private fun jsonToPlaylist(obj: JSONObject): UserPlaylist {
        val videosList = mutableListOf<VideoItem>()
        val arr = obj.optJSONArray("videos")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val vObj = arr.optJSONObject(i)
                if (vObj != null) {
                    videosList.add(jsonToVideo(vObj))
                }
            }
        }
        return UserPlaylist(
            id = obj.optString("id", UUID.randomUUID().toString()),
            title = obj.optString("title", "Untitled"),
            description = obj.optString("description", ""),
            videos = videosList,
            isSystem = obj.optBoolean("isSystem", false),
            iconType = obj.optString("iconType", "playlist")
        )
    }
}
