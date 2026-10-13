package com.videoguard.app.model

data class UserPlaylist(
    val id: String,
    val title: String,
    val description: String = "",
    val videos: List<VideoItem> = emptyList(),
    val isSystem: Boolean = false,
    val iconType: String = "playlist", // "like", "watch_later", "playlist"
    val customCoverUrl: String? = null,
    val totalVideosCount: Int? = null
) {
    val videoCount: Int
        get() = totalVideosCount ?: videos.size

    val coverThumbnailUrl: String?
        get() = customCoverUrl ?: videos.firstOrNull()?.thumbnailUrl
}
