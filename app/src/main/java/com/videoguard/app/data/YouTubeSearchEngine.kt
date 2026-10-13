package com.videoguard.app.data

import com.videoguard.app.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object YouTubeSearchEngine {

    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=$encoded")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }
            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                val suggestionsArray = jsonArray.optJSONArray(1) ?: return@withContext emptyList()
                val list = mutableListOf<String>()
                for (i in 0 until suggestionsArray.length()) {
                    list.add(suggestionsArray.getString(i))
                }
                return@withContext list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    suspend fun searchVideos(query: String, isMusicOnly: Boolean = false): List<VideoItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext if (isMusicOnly) VideoRepository.getMusicTracks() else VideoRepository.sampleVideos
        }

        val searchQuery = if (isMusicOnly && !query.lowercase().contains("song") && !query.lowercase().contains("music")) {
            "$query song"
        } else {
            query
        }

        try {
            val encoded = URLEncoder.encode(searchQuery.trim(), "UTF-8")
            val url = URL("https://www.youtube.com/results?search_query=$encoded")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            }

            if (conn.responseCode == 200) {
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = parseSearchResults(html, isMusicOnly)
                if (parsed.isNotEmpty()) {
                    return@withContext parsed
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to local filtering if online fails or returns empty
        if (isMusicOnly) VideoRepository.searchMusic(query) else VideoRepository.searchVideos(query)
    }

    private fun parseSearchResults(html: String, isMusic: Boolean): List<VideoItem> {
        val results = mutableListOf<VideoItem>()
        try {
            val marker = "var ytInitialData = "
            val startIdx = html.indexOf(marker)
            if (startIdx != -1) {
                val jsonStart = startIdx + marker.length
                val endIdx = html.indexOf(";</script>", jsonStart)
                if (endIdx != -1) {
                    val jsonStr = html.substring(jsonStart, endIdx)
                    val root = JSONObject(jsonStr)
                    val contents = root.optJSONObject("contents")
                        ?.optJSONObject("twoColumnSearchResultsRenderer")
                        ?.optJSONObject("primaryContents")
                        ?.optJSONObject("sectionListRenderer")
                        ?.optJSONArray("contents")

                    if (contents != null) {
                        for (i in 0 until contents.length()) {
                            val section = contents.optJSONObject(i)
                            val itemSection = section?.optJSONObject("itemSectionRenderer")?.optJSONArray("contents")
                            if (itemSection != null) {
                                for (j in 0 until itemSection.length()) {
                                    val item = itemSection.optJSONObject(j)
                                    val vr = item?.optJSONObject("videoRenderer")
                                    if (vr != null) {
                                        val vid = vr.optString("videoId")
                                        val title = vr.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                                        val channel = vr.optJSONObject("ownerText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                                        val views = vr.optJSONObject("viewCountText")?.optString("simpleText") ?: ""
                                        val duration = vr.optJSONObject("lengthText")?.optString("simpleText") ?: ""
                                        val uploadTime = vr.optJSONObject("publishedTimeText")?.optString("simpleText") ?: ""

                                        if (vid.isNotEmpty() && title.isNotEmpty()) {
                                            results.add(
                                                VideoItem(
                                                    id = vid,
                                                    title = title,
                                                    channelName = channel.ifEmpty { "YouTube" },
                                                    channelAvatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=150",
                                                    subscriberCount = "Subscribers",
                                                    thumbnailUrl = "https://img.youtube.com/vi/$vid/hqdefault.jpg",
                                                    views = views.ifEmpty { "1M views" },
                                                    uploadTime = uploadTime.ifEmpty { "Recently" },
                                                    duration = duration.ifEmpty { "4:00" },
                                                    description = title,
                                                    likes = "100K",
                                                    isMusicTrack = isMusic || title.contains("Music", ignoreCase = true) || title.contains("Song", ignoreCase = true),
                                                    artist = channel,
                                                    album = "YouTube Single",
                                                    lyrics = "[Lyrics available on YouTube Music]",
                                                    category = if (isMusic) "Music" else "YouTube"
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Secondary regex fallback if JSON format differed
        if (results.isEmpty()) {
            val vidRegex = Regex("\"videoId\":\"([a-zA-Z0-9_-]{11})\"")
            val matches = vidRegex.findAll(html).map { it.groupValues[1] }.distinct().take(15).toList()
            for (vid in matches) {
                results.add(
                    VideoItem(
                        id = vid,
                        title = "YouTube Video ($vid)",
                        channelName = "YouTube Channel",
                        channelAvatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=150",
                        subscriberCount = "1M subscribers",
                        thumbnailUrl = "https://img.youtube.com/vi/$vid/hqdefault.jpg",
                        views = "500K views",
                        uploadTime = "Recently",
                        duration = "3:45",
                        description = "YouTube stream playback",
                        likes = "50K",
                        isMusicTrack = isMusic,
                        artist = "Artist",
                        album = "Single",
                        lyrics = "",
                        category = if (isMusic) "Music" else "YouTube"
                    )
                )
            }
        }

        return results
    }

    suspend fun getRelatedVideos(video: VideoItem, isMusicOnly: Boolean = false): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            // First search with clean title or artist + title
            val cleanTitle = video.title.replace(Regex("[\\[\\(].*?[\\]\\)]"), "").trim()
            val query = if (video.artist.isNotBlank() && !video.title.contains(video.artist, ignoreCase = true)) {
                "${video.artist} $cleanTitle"
            } else if (video.channelName.isNotBlank() && !video.channelName.startsWith("@") && !video.title.contains(video.channelName, ignoreCase = true)) {
                "${video.channelName} $cleanTitle"
            } else {
                cleanTitle
            }

            val searchRes = searchVideos(query, isMusicOnly).filter { it.id != video.id }
            if (searchRes.isNotEmpty()) {
                return@withContext searchRes
            }

            // Fallback: search by channel or artist alone
            val secondaryQuery = video.artist.ifBlank { video.channelName }
            if (secondaryQuery.isNotBlank()) {
                val secRes = searchVideos(secondaryQuery, isMusicOnly).filter { it.id != video.id }
                if (secRes.isNotEmpty()) {
                    return@withContext secRes
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Final fallback: sample videos
        if (isMusicOnly) VideoRepository.getMusicTracks().filter { it.id != video.id }
        else VideoRepository.sampleVideos.filter { it.id != video.id }
    }
}
