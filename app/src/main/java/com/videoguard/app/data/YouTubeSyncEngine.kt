package com.videoguard.app.data

import android.webkit.CookieManager
import com.videoguard.app.model.UserPlaylist
import com.videoguard.app.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Syncs real personalized YouTube feed, watch history, playlists,
 * and user account profile using the authenticated session cookies and InnerTube API.
 */
object YouTubeSyncEngine {

    private const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    /**
     * Gathers authenticated cookies strictly from YouTube domains to prevent Google domain cookie conflicts.
     */
    fun getAuthCookies(): String {
        return try {
            val cm = CookieManager.getInstance()
            cm.flush()
            val ytCookies = cm.getCookie("https://www.youtube.com") ?: ""
            if (ytCookies.contains("LOGIN_INFO") || ytCookies.contains("SAPISID")) {
                android.util.Log.d("YouTubeSyncEngine", "getAuthCookies youtube.com: length=${ytCookies.length}")
                return ytCookies
            }
            val mCookies = cm.getCookie("https://m.youtube.com") ?: ""
            if (mCookies.contains("LOGIN_INFO") || mCookies.contains("SAPISID")) {
                android.util.Log.d("YouTubeSyncEngine", "getAuthCookies m.youtube.com: length=${mCookies.length}")
                return mCookies
            }
            ytCookies.ifBlank { mCookies }
        } catch (e: Exception) {
            android.util.Log.e("YouTubeSyncEngine", "getAuthCookies error", e)
            ""
        }
    }

    fun hasAuthCookies(): Boolean {
        val cookies = getAuthCookies()
        return cookies.contains("SAPISID") || cookies.contains("__Secure-3PAPISID") ||
                cookies.contains("LOGIN_INFO") || cookies.contains("SID")
    }

    /**
     * Generates SAPISIDHASH Authorization header for authenticated YouTube InnerTube requests.
     */
    private fun getSapisidHash(cookies: String, origin: String = "https://www.youtube.com"): String? {
        val sapisidMatch = Regex("(?:SAPISID|__Secure-3PAPISID)=([^;\\s]+)").find(cookies)
        val sapisid = sapisidMatch?.groupValues?.get(1)?.trim() ?: return null
        return try {
            val timestamp = System.currentTimeMillis() / 1000
            val input = "$timestamp $sapisid $origin"
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString("") { "%02x".format(it) }
            val hash = "SAPISIDHASH ${timestamp}_$hex"
            android.util.Log.d("YouTubeSyncEngine", "Generated SAPISIDHASH: $hash")
            hash
        } catch (e: Exception) {
            android.util.Log.e("YouTubeSyncEngine", "getSapisidHash error", e)
            null
        }
    }

    const val INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    const val CLIENT_VERSION = "2.20261010.01.00"

    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    private val _homeFeed = kotlinx.coroutines.flow.MutableStateFlow<List<VideoItem>>(emptyList())
    val homeFeed: kotlinx.coroutines.flow.StateFlow<List<VideoItem>> = _homeFeed.asStateFlow()

    // Stores the continuation token for the home feed so we can page in more results
    private var homeFeedContinuationToken: String? = null
    private var homeFeedCategory: String = "All"

    fun cacheHomeFeed(videos: List<VideoItem>) {
        if (videos.isNotEmpty()) {
            _homeFeed.value = videos
        }
    }

    /**
     * Appends more videos to the home feed using the stored continuation token.
     * Returns true if more items were loaded, false if nothing new was found.
     */
    suspend fun fetchMoreHomeFeed(): Boolean = withContext(Dispatchers.IO) {
        val token = homeFeedContinuationToken
        val category = homeFeedCategory

        // If we have a continuation token, try to use it via InnerTube
        if (!token.isNullOrBlank()) {
            try {
                val contPayload = JSONObject().apply {
                    put("context", buildContext())
                    put("continuation", token)
                }
                val resp = callInnerTube("browse", contPayload)
                if (resp != null) {
                    val batch = mutableListOf<VideoItem>()
                    extractVideosRecursively(resp, batch)
                    val newToken = extractContinuationToken(resp)
                    homeFeedContinuationToken = newToken

                    if (batch.isNotEmpty()) {
                        val existingIds = _homeFeed.value.map { it.id }.toSet()
                        val fresh = batch.filter { it.id !in existingIds }
                        if (fresh.isNotEmpty()) {
                            _homeFeed.value = _homeFeed.value + fresh
                            return@withContext true
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback: load another page of search results for the category
        try {
            val q = if (category == "All") "trending" else category
            val extra = YouTubeSearchEngine.searchVideos(q)
            if (extra.isNotEmpty()) {
                val existingIds = _homeFeed.value.map { it.id }.toSet()
                val fresh = extra.filter { it.id !in existingIds }
                if (fresh.isNotEmpty()) {
                    _homeFeed.value = _homeFeed.value + fresh
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        false
    }

    fun parseVideosFromJsonString(jsonStr: String): List<VideoItem> {
        val results = mutableListOf<VideoItem>()
        try {
            val root = JSONObject(jsonStr)
            extractVideosRecursively(root, results)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results.distinctBy { it.id }
    }

    private fun buildContext(): JSONObject {
        return JSONObject().apply {
            put("client", JSONObject().apply {
                put("clientName", "WEB")
                put("clientVersion", CLIENT_VERSION)
                put("hl", "en")
                put("gl", "US")
            })
        }
    }

    /**
     * Executes an authenticated InnerTube API call.
     */
    private fun callInnerTube(endpoint: String, payload: JSONObject): JSONObject? {
        return try {
            val cookies = getAuthCookies()
            val url = URL("https://www.youtube.com/youtubei/v1/$endpoint?key=$INNERTUBE_API_KEY&prettyPrint=false")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", DESKTOP_UA)
                setRequestProperty("X-YouTube-Client-Name", "1")
                setRequestProperty("X-YouTube-Client-Version", CLIENT_VERSION)
                setRequestProperty("Origin", "https://www.youtube.com")
                setRequestProperty("Referer", "https://www.youtube.com/")
                setRequestProperty("X-Origin", "https://www.youtube.com")
                setRequestProperty("X-Goog-AuthUser", "0")
                if (cookies.isNotBlank()) {
                    setRequestProperty("Cookie", cookies)
                    getSapisidHash(cookies, "https://www.youtube.com")?.let {
                        setRequestProperty("Authorization", it)
                    }
                }
            }
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            android.util.Log.d("YouTubeSyncEngine", "callInnerTube: endpoint=$endpoint, code=${conn.responseCode}")
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                android.util.Log.d("YouTubeSyncEngine", "callInnerTube response length=${text.length}")
                JSONObject(text)
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                android.util.Log.e("YouTubeSyncEngine", "callInnerTube error response: $err")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("YouTubeSyncEngine", "callInnerTube exception", e)
            null
        }
    }

    /**
     * Fetches personalized or category-based YouTube home feed videos.
     */
    suspend fun fetchHomeFeed(category: String = "All"): List<VideoItem> = withContext(Dispatchers.IO) {
        homeFeedCategory = category
        val cookies = getAuthCookies()

        // 1. If category chip selected (Music, Gaming, Podcasts, News, etc.)
        if (category != "All" && category.isNotBlank()) {
            val searchResults = YouTubeSearchEngine.searchVideos(category)
            if (searchResults.isNotEmpty()) return@withContext searchResults
        }

        // 2. For "All": Fetch YouTube Home feed via InnerTube FEwhat_to_watch
        try {
            val payload = JSONObject().apply {
                put("context", buildContext())
                put("browseId", "FEwhat_to_watch")
            }
            val resp = callInnerTube("browse", payload)
            if (resp != null) {
                val videos = mutableListOf<VideoItem>()
                extractVideosRecursively(resp, videos)
                if (videos.isNotEmpty()) {
                    val distinct = videos.distinctBy { it.id }
                    cacheHomeFeed(distinct)
                    // Store continuation token for "load more"
                    homeFeedContinuationToken = extractContinuationToken(resp)
                    return@withContext distinct
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback with cookies: Fetch live HTML from YouTube homepage with auth cookies
        if (cookies.isNotBlank()) {
            try {
                val url = URL("https://www.youtube.com/")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", DESKTOP_UA)
                    setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                    setRequestProperty("Cookie", cookies)
                }

                if (conn.responseCode == 200) {
                    val html = conn.inputStream.bufferedReader().use { it.readText() }
                    val videos = parseVideosFromHtml(html)
                    if (videos.isNotEmpty()) {
                        cacheHomeFeed(videos)
                        return@withContext videos
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Fallback: Search based on real user watch history
        val recentHistory = UserDataRepository.history.value.filter {
            it.channelName != "YouTube Creator" && it.title != "YouTube Video"
        }
        val historyTopic = recentHistory.firstOrNull()?.channelName
        if (!historyTopic.isNullOrBlank()) {
            val dynamicVideos = YouTubeSearchEngine.searchVideos(historyTopic)
            if (dynamicVideos.isNotEmpty()) {
                cacheHomeFeed(dynamicVideos)
                return@withContext dynamicVideos
            }
        }

        // 4. Fallback: Trending popular videos
        val popular = YouTubeSearchEngine.searchVideos("trending")
        if (popular.isNotEmpty()) {
            cacheHomeFeed(popular)
            return@withContext popular
        }

        // Offline local catalog fallback
        VideoRepository.getVideosByCategory(category)
    }

    /**
     * Fetches real user watch history from YouTube when signed in.
     */
    suspend fun fetchHistory(): List<VideoItem> = withContext(Dispatchers.IO) {
        val cookies = getAuthCookies()
        if (cookies.isBlank()) {
            return@withContext emptyList()
        }

        // 1. Try InnerTube browse FEhistory (direct authenticated history)
        try {
            val payload = JSONObject().apply {
                put("context", buildContext())
                put("browseId", "FEhistory")
            }
            val resp = callInnerTube("browse", payload)
            if (resp != null) {
                val videos = mutableListOf<VideoItem>()
                extractVideosRecursively(resp, videos)
                android.util.Log.d("YouTubeSyncEngine", "fetchHistory FEhistory extracted ${videos.size} videos")
                if (videos.isNotEmpty()) {
                    return@withContext videos.distinctBy { it.id }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback: GET https://www.youtube.com/feed/history HTML with cookies
        try {
            val url = URL("https://www.youtube.com/feed/history")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", DESKTOP_UA)
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Cookie", cookies)
            }

            if (conn.responseCode == 200) {
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                val videos = parseVideosFromHtml(html)
                if (videos.isNotEmpty()) {
                    return@withContext videos
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        emptyList()
    }

    /**
     * Fetches real user playlists from YouTube library, playlist aggregation, channel playlists, and guide.
     */
    suspend fun fetchPlaylists(): List<UserPlaylist> = withContext(Dispatchers.IO) {
        val cookies = getAuthCookies()
        val playlists = mutableListOf<UserPlaylist>()

        // 1. Try InnerTube browse FEplaylist_aggregation (with deep pagination up to 25 pages)
        if (cookies.isNotBlank()) {
            try {
                val payload = JSONObject().apply {
                    put("context", buildContext())
                    put("browseId", "FEplaylist_aggregation")
                }
                var resp = callInnerTube("browse", payload)
                var iterations = 0
                while (resp != null && iterations < 25) {
                    extractPlaylistsRecursively(resp, playlists)
                    val token = extractContinuationToken(resp)
                    if (token.isNullOrBlank()) break
                    iterations++
                    val contPayload = JSONObject().apply {
                        put("context", buildContext())
                        put("continuation", token)
                    }
                    resp = callInnerTube("browse", contPayload)
                }
                android.util.Log.d("YouTubeSyncEngine", "FEplaylist_aggregation extracted ${playlists.size} playlists (iterations=$iterations)")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Try InnerTube browse FElibrary (Library Playlists)
            try {
                val payload = JSONObject().apply {
                    put("context", buildContext())
                    put("browseId", "FElibrary")
                }
                val resp = callInnerTube("browse", payload)
                if (resp != null) {
                    val before = playlists.size
                    extractPlaylistsRecursively(resp, playlists)
                    android.util.Log.d("YouTubeSyncEngine", "FElibrary added ${playlists.size - before} playlists")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Try InnerTube browse FEchannels
            try {
                val payload = JSONObject().apply {
                    put("context", buildContext())
                    put("browseId", "FEchannels")
                }
                val resp = callInnerTube("browse", payload)
                if (resp != null) {
                    val before = playlists.size
                    extractPlaylistsRecursively(resp, playlists)
                    android.util.Log.d("YouTubeSyncEngine", "FEchannels added ${playlists.size - before} playlists")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 4. Try InnerTube guide endpoint (YouTube sidebar contains all saved and user playlists)
            try {
                val payload = JSONObject().apply {
                    put("context", buildContext())
                }
                val resp = callInnerTube("guide", payload)
                if (resp != null) {
                    val before = playlists.size
                    extractPlaylistsRecursively(resp, playlists)
                    android.util.Log.d("YouTubeSyncEngine", "guide added ${playlists.size - before} playlists")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 5. Fetch user channel playlists & feed directly with redirect following
        val channelUrls = mutableListOf<String>()
        val handle = AccountRepository.currentAccount.value?.handle?.trim()?.removePrefix("@")
        if (!handle.isNullOrBlank() && handle != "youtube_user") {
            channelUrls.add("https://www.youtube.com/@$handle/playlists")
        }
        channelUrls.add("https://www.youtube.com/feed/playlists")
        channelUrls.add("https://www.youtube.com/channel/mine/playlists")

        for (urlStr in channelUrls) {
            try {
                val html = fetchHtmlWithRedirects(urlStr, cookies)
                if (!html.isNullOrBlank()) {
                    val before = playlists.size
                    playlists.addAll(parsePlaylistsFromHtml(html))
                    android.util.Log.d("YouTubeSyncEngine", "URL '$urlStr' added ${playlists.size - before} playlists")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val result = playlists
            .filter { it.id.isNotBlank() && it.title.isNotBlank() && it.id != "WL" && it.id != "LL" && it.id != "history" && !it.id.startsWith("UC") && !it.id.startsWith("FE") }
            .distinctBy { it.id }

        result.forEach {
            android.util.Log.d("YouTubeSyncEngine", "Final Playlist: id='${it.id}', title='${it.title}'")
        }

        android.util.Log.d("YouTubeSyncEngine", "fetchPlaylists total unique: ${result.size}")
        result
    }

    /**
     * Fetches videos inside any YouTube playlist (e.g. user playlist, channel playlist, or mix)
     * using InnerTube continuation pagination up to maxVideos (e.g. 2,000+).
     */
    suspend fun fetchPlaylistVideos(playlistId: String, maxVideos: Int = 2000): List<VideoItem> = withContext(Dispatchers.IO) {
        if (playlistId.startsWith("playlist_") || playlistId == "favorites") {
            val local = UserDataRepository.playlists.value.find { it.id == playlistId }
            return@withContext local?.videos ?: emptyList()
        }

        val cookies = getAuthCookies()
        val rawId = when (playlistId) {
            "liked_videos", "LL", "VLLL" -> "LL"
            "watch_later", "WL", "VLWL" -> "WL"
            else -> playlistId.removePrefix("VL")
        }
        val cleanId = when (rawId) {
            "LL" -> "VLLL"
            "WL" -> "VLWL"
            else -> "VL$rawId"
        }

        // InnerTube browse runner with continuation loop
        suspend fun queryInnerTubeBrowse(browseId: String): List<VideoItem> {
            val collected = mutableListOf<VideoItem>()
            try {
                val payload = JSONObject().apply {
                    put("context", buildContext())
                    put("browseId", browseId)
                }
                var resp = callInnerTube("browse", payload)
                var iterations = 0
                while (resp != null && iterations < 50 && collected.size < maxVideos) {
                    val batch = mutableListOf<VideoItem>()
                    extractVideosRecursively(resp, batch)
                    if (batch.isNotEmpty()) {
                        collected.addAll(batch)
                    }

                    val token = extractContinuationToken(resp)
                    if (token.isNullOrBlank()) break

                    iterations++
                    val contPayload = JSONObject().apply {
                        put("context", buildContext())
                        put("continuation", token)
                    }
                    resp = callInnerTube("browse", contPayload)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return collected.distinctBy { it.id }
        }

        // 1. Try with cleanId (e.g. "VLPL...", "VLLL", "VLWL")
        var results = queryInnerTubeBrowse(cleanId)
        if (results.isNotEmpty()) {
            android.util.Log.d("YouTubeSyncEngine", "fetchPlaylistVideos $cleanId total fetched ${results.size} videos")
            return@withContext results
        }

        // 2. If cleanId yielded nothing, try with rawId (e.g. "PL...")
        if (rawId != cleanId && rawId != "WL" && rawId != "LL") {
            results = queryInnerTubeBrowse(rawId)
            if (results.isNotEmpty()) {
                android.util.Log.d("YouTubeSyncEngine", "fetchPlaylistVideos $rawId total fetched ${results.size} videos")
                return@withContext results
            }
        }

        // 3. Fallback: GET https://www.youtube.com/playlist?list=rawId
        try {
            val html = fetchHtmlWithRedirects("https://www.youtube.com/playlist?list=$rawId", cookies)
            if (!html.isNullOrBlank()) {
                val videos = parseVideosFromHtml(html)
                if (videos.isNotEmpty()) {
                    android.util.Log.d("YouTubeSyncEngine", "fetchPlaylistVideos HTML extracted ${videos.size} videos")
                    return@withContext videos.distinctBy { it.id }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        emptyList()
    }

    /**
     * Syncs real user channel name, handle, and avatar from YouTube account.
     */
    suspend fun syncProfileDetails() = withContext(Dispatchers.IO) {
        val cookies = getAuthCookies()
        if (cookies.isBlank()) return@withContext

        var extractedName = ""
        var extractedEmail = ""
        var extractedHandle = ""
        var extractedAvatar: String? = null

        // 1. Try InnerTube account/account_menu (most accurate for official YouTube channel name)
        try {
            val payload = JSONObject().apply {
                put("context", buildContext())
            }
            val resp = callInnerTube("account/account_menu", payload)
            if (resp != null) {
                val respStr = resp.toString()
                android.util.Log.d("YouTubeSyncEngine", "account_menu snippet: " + respStr.take(1000))
                val actions = resp.optJSONArray("actions")
                val header = actions?.optJSONObject(0)
                    ?.optJSONObject("openPopupAction")
                    ?.optJSONObject("popup")
                    ?.optJSONObject("multiPageMenuRenderer")
                    ?.optJSONObject("header")
                    ?.optJSONObject("activeAccountHeaderRenderer")

                if (header != null) {
                    val nameObj = header.optJSONObject("accountName")
                    val name = nameObj?.optString("simpleText")?.ifBlank {
                        nameObj.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    } ?: ""

                    val emailObj = header.optJSONObject("email")
                    val email = emailObj?.optString("simpleText")?.ifBlank {
                        emailObj.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    } ?: ""

                    val handleObj = header.optJSONObject("channelHandle")
                    val handle = handleObj?.optString("simpleText")?.ifBlank {
                        handleObj.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    } ?: ""

                    val thumbs = header.optJSONObject("accountPhoto")?.optJSONArray("thumbnails")
                    val avatar = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url")

                    if (name.isNotBlank()) extractedName = name.trim()
                    if (email.isNotBlank()) extractedEmail = email.trim()
                    if (handle.isNotBlank()) extractedHandle = handle.trim()
                    if (!avatar.isNullOrBlank()) extractedAvatar = avatar
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback: GET https://www.youtube.com/channel/mine to detect real channel redirect
        if (extractedName.isBlank() || extractedHandle.isBlank()) {
            try {
                val mineUrl = URL("https://www.youtube.com/channel/mine")
                val mineConn = (mineUrl.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", DESKTOP_UA)
                    setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                    setRequestProperty("Cookie", cookies)
                }

                if (mineConn.responseCode == 200) {
                    val html = mineConn.inputStream.bufferedReader().use { it.readText() }
                    Regex("<title>(.*?) - YouTube</title>").find(html)?.let {
                        val t = it.groupValues[1].trim()
                        if (t.isNotBlank() && t != "YouTube" && t != "Account") {
                            if (extractedName.isBlank()) extractedName = t
                        }
                    }
                    Regex("\"channelTitle\":\"(.*?)\"").find(html)?.let {
                        if (extractedName.isBlank()) extractedName = it.groupValues[1]
                    }
                    Regex("\"channelHandle\":\"(.*?)\"").find(html)?.let {
                        if (extractedHandle.isBlank()) extractedHandle = it.groupValues[1]
                    }
                    Regex("\"avatar\":\\{\"thumbnails\":\\[\\{\"url\":\"(.*?)\"").find(html)?.let {
                        if (extractedAvatar == null) extractedAvatar = it.groupValues[1]
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        android.util.Log.d("YouTubeSyncEngine", "syncProfileDetails extracted: name='$extractedName', email='$extractedEmail', handle='$extractedHandle'")
        if (extractedName.isNotBlank() || extractedEmail.isNotBlank()) {
            val current = AccountRepository.currentAccount.value
            val finalName = extractedName.ifBlank { current?.name ?: "YouTube User" }
            val finalEmail = extractedEmail.ifBlank { current?.email ?: "" }
            val finalHandle = if (extractedHandle.isNotBlank()) {
                if (extractedHandle.startsWith("@")) extractedHandle else "@$extractedHandle"
            } else {
                current?.handle ?: ("@" + finalName.lowercase().replace("[^a-z0-9_]".toRegex(), "_"))
            }
            val finalAvatar = extractedAvatar ?: current?.avatarUrl

            AccountRepository.setGoogleAccount(
                name = finalName,
                email = finalEmail,
                avatarUrl = finalAvatar,
                handle = finalHandle
            )
        }
    }

    private val isSyncRunning = java.util.concurrent.atomic.AtomicBoolean(false)

    /**
     * Syncs all user data (profile, history, playlists, feed) in background.
     */
    suspend fun syncAllUserData() = withContext(Dispatchers.IO) {
        if (!isSyncRunning.compareAndSet(false, true)) {
            android.util.Log.d("YouTubeSyncEngine", "syncAllUserData already running, skipping")
            return@withContext
        }
        try {
            android.util.Log.d("YouTubeSyncEngine", "syncAllUserData started...")
            syncProfileDetails()

            // Sync real History
            val onlineHistory = fetchHistory()
            android.util.Log.d("YouTubeSyncEngine", "syncAllUserData: onlineHistory count=${onlineHistory.size}")
            if (onlineHistory.isNotEmpty()) {
                UserDataRepository.setOnlineHistory(onlineHistory)
            }

            // Sync real Playlists
            val onlinePlaylists = fetchPlaylists()
            android.util.Log.d("YouTubeSyncEngine", "syncAllUserData: onlinePlaylists count=${onlinePlaylists.size}")
            if (onlinePlaylists.isNotEmpty()) {
                UserDataRepository.setOnlinePlaylists(onlinePlaylists)
            }

            // Sync real Liked Videos with full pagination (1,000+ videos)
            try {
                val liked = fetchPlaylistVideos("VLLL", maxVideos = 2000)
                if (liked.isNotEmpty()) {
                    android.util.Log.d("YouTubeSyncEngine", "syncAllUserData: Liked videos count=${liked.size}")
                    UserDataRepository.updateLikedVideos(liked)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Sync real Watch Later with pagination (up to 2,000 videos)
            try {
                val watchLater = fetchPlaylistVideos("VLWL", maxVideos = 2000)
                if (watchLater.isNotEmpty()) {
                    android.util.Log.d("YouTubeSyncEngine", "syncAllUserData: Watch later count=${watchLater.size}")
                    UserDataRepository.updateWatchLaterVideos(watchLater)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Sync user custom playlists (e.g. Music, podcasts, series, etc.)
            for (pl in onlinePlaylists) {
                if (pl.id != "liked_videos" && pl.id != "watch_later" && !pl.id.startsWith("playlist_") && pl.id != "favorites") {
                    try {
                        val vids = fetchPlaylistVideos(pl.id, maxVideos = 2000)
                        if (vids.isNotEmpty()) {
                            android.util.Log.d("YouTubeSyncEngine", "syncAllUserData: Custom playlist '${pl.title}' (${pl.id}) synced ${vids.size} videos")
                            UserDataRepository.updatePlaylistVideos(pl.id, vids)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Pre-fetch personalized home feed so it's instantly available
            fetchHomeFeed("All")
        } finally {
            isSyncRunning.set(false)
        }
    }


    /**
     * Generic extractor for video items from YouTube's ytInitialData
     */
    private fun parseVideosFromHtml(html: String): List<VideoItem> {
        val results = mutableListOf<VideoItem>()
        try {
            val markerIdx = html.indexOf("ytInitialData")
            if (markerIdx != -1) {
                val eqIdx = html.indexOf("=", markerIdx)
                if (eqIdx != -1) {
                    val braceIdx = html.indexOf("{", eqIdx)
                    if (braceIdx != -1 && braceIdx - eqIdx < 15) {
                        val endIdx = html.indexOf("</script>", braceIdx)
                        if (endIdx != -1) {
                            var jsonStr = html.substring(braceIdx, endIdx).trim()
                            if (jsonStr.endsWith(";")) {
                                jsonStr = jsonStr.substring(0, jsonStr.length - 1).trim()
                            }
                            val root = JSONObject(jsonStr)
                            extractVideosRecursively(root, results)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results.distinctBy { it.id }
    }

    private fun extractVideosRecursively(obj: Any, outList: MutableList<VideoItem>) {
        when (obj) {
            is JSONObject -> {
                if (obj.has("lockupViewModel")) {
                    parseLockupViewModel(obj.optJSONObject("lockupViewModel"))?.let { outList.add(it) }
                }
                if (obj.has("videoRenderer")) {
                    parseVideoRenderer(obj.optJSONObject("videoRenderer"))?.let { outList.add(it) }
                }
                if (obj.has("playlistVideoRenderer")) {
                    parseVideoRenderer(obj.optJSONObject("playlistVideoRenderer"))?.let { outList.add(it) }
                }
                if (obj.has("playlistPanelVideoRenderer")) {
                    parseVideoRenderer(obj.optJSONObject("playlistPanelVideoRenderer"))?.let { outList.add(it) }
                }
                if (obj.has("compactVideoRenderer")) {
                    parseVideoRenderer(obj.optJSONObject("compactVideoRenderer"))?.let { outList.add(it) }
                }
                if (obj.has("gridVideoRenderer")) {
                    parseVideoRenderer(obj.optJSONObject("gridVideoRenderer"))?.let { outList.add(it) }
                }
                if (obj.has("reelItemRenderer")) {
                    parseVideoRenderer(obj.optJSONObject("reelItemRenderer"))?.let { outList.add(it) }
                }
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    extractVideosRecursively(obj.get(key), outList)
                }
            }
            is JSONArray -> {
                for (i in 0 until obj.length()) {
                    val elem = obj.opt(i)
                    if (elem != null) {
                        extractVideosRecursively(elem, outList)
                    }
                }
            }
        }
    }

    private fun parseLockupViewModel(lvm: JSONObject?): VideoItem? {
        if (lvm == null) return null
        val contentType = lvm.optString("contentType")
        if (contentType == "LOCKUP_CONTENT_TYPE_PLAYLIST" || contentType == "LOCKUP_CONTENT_TYPE_PODCAST") {
            return null
        }
        val vid = lvm.optString("contentId")
        if (vid.isBlank() || vid.startsWith("PL") || vid.startsWith("VL")) return null

        val meta = lvm.optJSONObject("metadata")?.optJSONObject("lockupMetadataViewModel")
        val title = meta?.optJSONObject("title")?.optString("content") ?: ""
        if (title.isBlank()) return null

        var channel = "YouTube Creator"
        var views = "1M views"
        var uploadTime = "Recently"

        val rows = meta?.optJSONObject("metadata")?.optJSONObject("contentMetadataViewModel")?.optJSONArray("metadataRows")
        if (rows != null) {
            if (rows.length() > 0) {
                val row0 = rows.optJSONObject(0)?.optJSONArray("metadataParts")
                if (row0 != null && row0.length() > 0) {
                    val c = row0.optJSONObject(0)?.optJSONObject("text")?.optString("content")
                    if (!c.isNullOrBlank()) channel = c
                }
            }
            if (rows.length() > 1) {
                val row1 = rows.optJSONObject(1)?.optJSONArray("metadataParts")
                if (row1 != null) {
                    if (row1.length() > 0) {
                        val v = row1.optJSONObject(0)?.optJSONObject("text")?.optString("content")
                        if (!v.isNullOrBlank()) views = v
                    }
                    if (row1.length() > 1) {
                        val t = row1.optJSONObject(1)?.optJSONObject("text")?.optString("content")
                        if (!t.isNullOrBlank()) uploadTime = t
                    }
                }
            }
        }

        var avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
        val avatarSources = meta?.optJSONObject("image")?.optJSONObject("decoratedAvatarViewModel")
            ?.optJSONObject("avatar")?.optJSONObject("avatarViewModel")?.optJSONObject("image")?.optJSONArray("sources")
        if (avatarSources != null && avatarSources.length() > 0) {
            avatar = avatarSources.optJSONObject(0)?.optString("url") ?: avatar
        }

        var thumb = "https://img.youtube.com/vi/$vid/hqdefault.jpg"
        val thumbSources = lvm.optJSONObject("contentImage")?.optJSONObject("thumbnailViewModel")
            ?.optJSONObject("image")?.optJSONArray("sources")
        if (thumbSources != null && thumbSources.length() > 0) {
            thumb = thumbSources.optJSONObject(thumbSources.length() - 1)?.optString("url") ?: thumb
        }

        return VideoItem(
            id = vid,
            title = title,
            channelName = channel,
            channelAvatarUrl = avatar,
            subscriberCount = "Subscribers",
            thumbnailUrl = thumb,
            views = views,
            uploadTime = uploadTime,
            duration = "3:45",
            description = title,
            likes = "100K",
            isMusicTrack = title.contains("Music", ignoreCase = true) || title.contains("Song", ignoreCase = true) || title.contains("ft.", ignoreCase = true),
            artist = channel,
            album = "Single",
            lyrics = "",
            category = "YouTube"
        )
    }

    private fun parseVideoRenderer(vr: JSONObject?): VideoItem? {
        if (vr == null) return null
        var vid = vr.optString("videoId")
        if (vid.isBlank()) {
            vid = vr.optJSONObject("watchEndpoint")?.optString("videoId") ?: ""
        }
        if (vid.isBlank()) {
            vid = vr.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId") ?: ""
        }
        if (vid.isBlank()) {
            vid = vr.optJSONObject("navigationEndpoint")?.optJSONObject("reelWatchEndpoint")?.optString("videoId") ?: ""
        }
        if (vid.isBlank()) {
            vid = vr.optJSONObject("onTap")?.optJSONObject("innertubeCommand")?.optJSONObject("reelWatchEndpoint")?.optString("videoId") ?: ""
        }
        if (vid.isBlank()) return null

        val title = vr.optJSONObject("title")?.let { t ->
            t.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                ?: t.optString("simpleText")
        } ?: vr.optJSONObject("headline")?.let { h ->
            h.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                ?: h.optString("simpleText")
        } ?: vr.optJSONObject("overlayMetadata")?.optJSONObject("primaryText")?.optString("content")
        ?: vr.optString("title")

        if (title.isBlank()) return null

        val channel = vr.optJSONObject("ownerText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: vr.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: vr.optJSONObject("longBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: vr.optJSONObject("bylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            ?: "YouTube Creator"

        val views = vr.optJSONObject("viewCountText")?.let { vc ->
            vc.optString("simpleText").ifBlank {
                vc.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            }
        } ?: vr.optJSONObject("shortViewCountText")?.let { vc ->
            vc.optString("simpleText").ifBlank {
                vc.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            }
        } ?: "1M views"

        val duration = vr.optJSONObject("lengthText")?.let { lt ->
            lt.optString("simpleText").ifBlank {
                lt.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            }
        } ?: "3:45"

        val uploadTime = vr.optJSONObject("publishedTimeText")?.optString("simpleText") ?: "Recently"

        val avatar = vr.optJSONObject("channelThumbnailSupportedRenderers")
            ?.optJSONObject("channelThumbnailWithLinkRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.optJSONObject(0)
            ?.optString("url")
            ?: vr.optJSONObject("channelThumbnail")
                ?.optJSONArray("thumbnails")
                ?.optJSONObject(0)
                ?.optString("url")
            ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"

        return VideoItem(
            id = vid,
            title = title,
            channelName = channel,
            channelAvatarUrl = avatar,
            subscriberCount = "Subscribers",
            thumbnailUrl = "https://img.youtube.com/vi/$vid/hqdefault.jpg",
            views = views,
            uploadTime = uploadTime,
            duration = duration,
            description = title,
            likes = "100K",
            isMusicTrack = title.contains("Music", ignoreCase = true) || title.contains("Song", ignoreCase = true) || title.contains("ft.", ignoreCase = true),
            artist = channel,
            album = "Single",
            lyrics = "",
            category = "YouTube"
        )
    }

    private fun parsePlaylistsFromHtml(html: String): List<UserPlaylist> {
        val playlists = mutableListOf<UserPlaylist>()
        try {
            val markerIdx = html.indexOf("ytInitialData")
            if (markerIdx != -1) {
                val eqIdx = html.indexOf("=", markerIdx)
                if (eqIdx != -1) {
                    val braceIdx = html.indexOf("{", eqIdx)
                    if (braceIdx != -1 && braceIdx - eqIdx < 15) {
                        val endIdx = html.indexOf("</script>", braceIdx)
                        if (endIdx != -1) {
                            var jsonStr = html.substring(braceIdx, endIdx).trim()
                            if (jsonStr.endsWith(";")) {
                                jsonStr = jsonStr.substring(0, jsonStr.length - 1).trim()
                            }
                            val root = JSONObject(jsonStr)
                            extractPlaylistsRecursively(root, playlists)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return playlists.distinctBy { it.id }
    }

    private fun fetchHtmlWithRedirects(urlStr: String, cookies: String): String? {
        var currentUrl = urlStr
        var redirects = 0
        while (redirects < 5) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", DESKTOP_UA)
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                if (cookies.isNotBlank()) {
                    setRequestProperty("Cookie", cookies)
                }
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val loc = conn.getHeaderField("Location") ?: break
                currentUrl = if (loc.startsWith("http")) loc else URL(URL(currentUrl), loc).toString()
                redirects++
                continue
            }
            if (code == 200) {
                return conn.inputStream.bufferedReader().use { it.readText() }
            }
            break
        }
        return null
    }

    private fun extractFirstImageUrl(obj: Any?): String? {
        if (obj == null) return null
        when (obj) {
            is JSONObject -> {
                val sources = obj.optJSONArray("sources") ?: obj.optJSONArray("thumbnails")
                if (sources != null && sources.length() > 0) {
                    val last = sources.optJSONObject(sources.length() - 1)?.optString("url")
                    if (!last.isNullOrBlank()) return last
                }
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val res = extractFirstImageUrl(obj.get(keys.next()))
                    if (res != null) return res
                }
            }
            is JSONArray -> {
                for (i in 0 until obj.length()) {
                    val elem = obj.opt(i)
                    if (elem != null) {
                        val res = extractFirstImageUrl(elem)
                        if (res != null) return res
                    }
                }
            }
        }
        return null
    }

    private fun extractContinuationToken(obj: Any?): String? {
        if (obj == null) return null
        when (obj) {
            is JSONObject -> {
                // 1. nextContinuationData (standard InnerTube browse pagination)
                val ncd = obj.optJSONObject("nextContinuationData")
                if (ncd != null) {
                    val token = ncd.optString("continuation")
                    if (token.isNotBlank()) return token
                }

                // 2. reloadContinuationData
                val rcd = obj.optJSONObject("reloadContinuationData")
                if (rcd != null) {
                    val token = rcd.optString("continuation")
                    if (token.isNotBlank()) return token
                }

                // 3. continuationCommand
                val cc = obj.optJSONObject("continuationCommand")
                if (cc != null) {
                    val token = cc.optString("token")
                    if (token.isNotBlank()) return token
                }

                // 4. continuationItemRenderer
                val cir = obj.optJSONObject("continuationItemRenderer")
                if (cir != null) {
                    val ep = cir.optJSONObject("continuationEndpoint")
                    val token = ep?.optJSONObject("continuationCommand")?.optString("token")
                        ?: ep?.optString("continuation")
                        ?: ep?.optJSONObject("continuationCommand")?.optString("continuation")
                    if (!token.isNullOrBlank()) return token
                }

                // 5. Direct continuation key with a long token
                val directCont = obj.optString("continuation")
                if (directCont.length > 20) {
                    return directCont
                }

                val keys = obj.keys()
                while (keys.hasNext()) {
                    val res = extractContinuationToken(obj.get(keys.next()))
                    if (res != null) return res
                }
            }
            is JSONArray -> {
                for (i in 0 until obj.length()) {
                    val res = extractContinuationToken(obj.opt(i))
                    if (res != null) return res
                }
            }
        }
        return null
    }

    private fun extractPlaylistIdFromObject(obj: JSONObject): String? {
        // 1. Direct playlistId
        val plId = obj.optString("playlistId")
        if (plId.isNotBlank() && plId != "WL" && plId != "LL" && plId != "history") {
            return plId.removePrefix("VL")
        }

        // 2. contentId
        val cid = obj.optString("contentId")
        if (cid.startsWith("PL") || cid.startsWith("RD") || cid.startsWith("FL") ||
            cid.startsWith("UU") || cid.startsWith("OLAK") || cid.startsWith("TLGG") || cid.startsWith("CL")) {
            return cid
        }
        if (cid.startsWith("VLPL") || cid.startsWith("VL")) {
            return cid.removePrefix("VL")
        }

        // 3. Check endpoints recursively or by known paths
        fun findInEndpoint(ep: JSONObject?): String? {
            if (ep == null) return null
            val pid = ep.optJSONObject("watchEndpoint")?.optString("playlistId")
            if (!pid.isNullOrBlank() && pid != "WL" && pid != "LL") return pid.removePrefix("VL")

            val bid = ep.optJSONObject("browseEndpoint")?.optString("browseId")
            if (!bid.isNullOrBlank()) {
                if (bid.startsWith("VLPL") || bid.startsWith("VL")) return bid.removePrefix("VL")
                if (bid.startsWith("PL") || bid.startsWith("RD") || bid.startsWith("FL") ||
                    bid.startsWith("UU") || bid.startsWith("OLAK")) return bid
            }

            val url = ep.optJSONObject("commandMetadata")?.optJSONObject("webCommandMetadata")?.optString("url")
                ?: ep.optString("url")
            if (url.contains("list=")) {
                Regex("[?&]list=([a-zA-Z0-9_-]+)").find(url)?.let {
                    val found = it.groupValues[1]
                    if (found != "WL" && found != "LL") return found
                }
            }
            return null
        }

        findInEndpoint(obj.optJSONObject("navigationEndpoint"))?.let { return it }
        findInEndpoint(obj.optJSONObject("onTap")?.optJSONObject("innertubeCommand"))?.let { return it }
        findInEndpoint(obj.optJSONObject("rendererContext")?.optJSONObject("commandContext")?.optJSONObject("onTap")?.optJSONObject("innertubeCommand"))?.let { return it }
        findInEndpoint(obj.optJSONObject("serviceEndpoint"))?.let { return it }

        return null
    }

    private fun extractTitleFromObject(obj: JSONObject): String? {
        val directTitle = obj.optString("title")
        if (directTitle.isNotBlank() && !directTitle.startsWith("{")) return directTitle

        obj.optJSONObject("title")?.let { t ->
            t.optJSONArray("runs")?.optJSONObject(0)?.optString("text")?.ifBlank { null }?.let { return it }
            t.optString("simpleText").ifBlank { null }?.let { return it }
            t.optString("content").ifBlank { null }?.let { return it }
        }

        obj.optJSONObject("formattedTitle")?.let { t ->
            t.optJSONArray("runs")?.optJSONObject(0)?.optString("text")?.ifBlank { null }?.let { return it }
            t.optString("simpleText").ifBlank { null }?.let { return it }
        }

        obj.optJSONObject("metadata")?.optJSONObject("lockupMetadataViewModel")?.let { lmm ->
            lmm.optJSONObject("title")?.let { t ->
                t.optString("content").ifBlank { null }?.let { return it }
                t.optJSONArray("runs")?.optJSONObject(0)?.optString("text")?.ifBlank { null }?.let { return it }
                t.optString("simpleText").ifBlank { null }?.let { return it }
            }
        }

        obj.optJSONObject("headline")?.let { h ->
            h.optJSONArray("runs")?.optJSONObject(0)?.optString("text")?.ifBlank { null }?.let { return it }
            h.optString("simpleText").ifBlank { null }?.let { return it }
        }

        return null
    }

    private fun extractPlaylistDescription(obj: JSONObject): String {
        val vc = obj.optJSONObject("videoCountText")?.let {
            it.optString("simpleText").ifBlank {
                it.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            }
        } ?: obj.optJSONObject("videoCountShortText")?.optString("simpleText") ?: ""

        val parts = mutableListOf<String>()
        if (vc.isNotBlank()) parts.add(vc)

        val rows = obj.optJSONObject("metadata")?.optJSONObject("lockupMetadataViewModel")
            ?.optJSONObject("metadata")?.optJSONObject("contentMetadataViewModel")
            ?.optJSONArray("metadataRows")
        if (rows != null) {
            for (i in 0 until rows.length()) {
                val rowParts = rows.optJSONObject(i)?.optJSONArray("metadataParts")
                if (rowParts != null) {
                    for (j in 0 until rowParts.length()) {
                        val text = rowParts.optJSONObject(j)?.optJSONObject("text")?.optString("content")
                        if (!text.isNullOrBlank() && text != "•") {
                            parts.add(text)
                        }
                    }
                }
            }
        }

        return if (parts.isNotEmpty()) parts.distinct().joinToString(" • ") else "Synced from YouTube"
    }

    private fun parseAnyPlaylistRenderer(obj: JSONObject): UserPlaylist? {
        val contentType = obj.optString("contentType")
        if (contentType == "LOCKUP_CONTENT_TYPE_VIDEO" || contentType == "LOCKUP_CONTENT_TYPE_SHORTS") {
            return null
        }

        val plId = extractPlaylistIdFromObject(obj) ?: return null
        if (plId == "WL" || plId == "LL" || plId == "history" || plId.startsWith("UC") || plId.startsWith("FE")) {
            return null
        }

        val title = extractTitleFromObject(obj) ?: return null
        val cover = extractFirstImageUrl(obj.optJSONObject("contentImage")) ?: extractFirstImageUrl(obj)
        val desc = extractPlaylistDescription(obj)
        val count = extractPlaylistVideoCount(obj)

        return UserPlaylist(
            id = plId,
            title = title,
            description = desc,
            customCoverUrl = cover,
            isSystem = false,
            iconType = "playlist",
            totalVideosCount = count
        )
    }

    private fun extractPlaylistVideoCount(obj: JSONObject): Int? {
        fun parseCount(text: String?): Int? {
            if (text.isNullOrBlank()) return null
            val match = Regex("""([\d,]+)\s*(?:videos|video)?""", RegexOption.IGNORE_CASE).find(text)
            return match?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
        }

        val vc = obj.optString("videoCount")
        parseCount(vc)?.let { return it }

        val vct = obj.optJSONObject("videoCountText")?.let {
            it.optString("simpleText").ifBlank {
                it.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            }
        }
        parseCount(vct)?.let { return it }

        val vcst = obj.optJSONObject("videoCountShortText")?.let {
            it.optString("simpleText").ifBlank {
                it.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
            }
        }
        parseCount(vcst)?.let { return it }

        fun searchBadge(o: Any?): Int? {
            if (o == null) return null
            when (o) {
                is JSONObject -> {
                    val tb = o.optJSONObject("thumbnailBadgeViewModel")
                    if (tb != null) {
                        parseCount(tb.optString("text"))?.let { return it }
                    }
                    val text = o.optString("text")
                    if (text.contains("video", ignoreCase = true)) {
                        parseCount(text)?.let { return it }
                    }
                    val keys = o.keys()
                    while (keys.hasNext()) {
                        val res = searchBadge(o.get(keys.next()))
                        if (res != null) return res
                    }
                }
                is JSONArray -> {
                    for (i in 0 until o.length()) {
                        val res = searchBadge(o.opt(i))
                        if (res != null) return res
                    }
                }
            }
            return null
        }

        return searchBadge(obj.opt("contentImage") ?: obj)
    }

    private fun extractPlaylistsRecursively(obj: Any, outList: MutableList<UserPlaylist>) {
        when (obj) {
            is JSONObject -> {
                // Check all known playlist renderer wrappers
                val wrappers = listOf(
                    "lockupViewModel",
                    "playlistRenderer",
                    "gridPlaylistRenderer",
                    "compactPlaylistRenderer",
                    "radioRenderer",
                    "guideEntryRenderer",
                    "guideCollapsibleSectionEntryRenderer",
                    "guideItemRenderer",
                    "playlistCardViewModel",
                    "musicTwoRowItemRenderer",
                    "musicResponsiveListItemRenderer"
                )
                for (wrapper in wrappers) {
                    if (obj.has(wrapper)) {
                        obj.optJSONObject(wrapper)?.let { pObj ->
                            parseAnyPlaylistRenderer(pObj)?.let { outList.add(it) }
                        }
                    }
                }

                // Also check if obj itself is a renderer with playlist data
                parseAnyPlaylistRenderer(obj)?.let { outList.add(it) }

                val keys = obj.keys()
                while (keys.hasNext()) {
                    extractPlaylistsRecursively(obj.get(keys.next()), outList)
                }
            }
            is JSONArray -> {
                for (i in 0 until obj.length()) {
                    val elem = obj.opt(i)
                    if (elem != null) extractPlaylistsRecursively(elem, outList)
                }
            }
        }
    }

    private val channelIdCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    /**
     * Resolves a channel name to its YouTube Channel ID (UC...) using InnerTube search.
     */
    suspend fun resolveChannelId(channelNameOrId: String): String? = withContext(Dispatchers.IO) {
        val trimmed = channelNameOrId.trim()
        if (trimmed.isBlank()) return@withContext null
        if (trimmed.startsWith("UC") && trimmed.length >= 20) return@withContext trimmed

        channelIdCache[trimmed]?.let { return@withContext it }

        try {
            val payload = JSONObject().apply {
                put("context", buildContext())
                put("query", trimmed)
                put("params", "EgIQAg%3D%3D") // Filter: Channel
            }
            val resp = callInnerTube("search", payload)
            if (resp != null) {
                val cid = findChannelIdRecursively(resp)
                if (!cid.isNullOrBlank()) {
                    channelIdCache[trimmed] = cid
                    android.util.Log.d("YouTubeSyncEngine", "Resolved channel '$trimmed' -> $cid")
                    return@withContext cid
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("YouTubeSyncEngine", "resolveChannelId error for $trimmed", e)
        }
        null
    }

    private fun findChannelIdRecursively(obj: Any?): String? {
        if (obj == null) return null
        when (obj) {
            is JSONObject -> {
                val cr = obj.optJSONObject("channelRenderer")
                if (cr != null) {
                    val id = cr.optString("channelId")
                    if (id.startsWith("UC")) return id
                }
                val cid = obj.optString("channelId")
                if (cid.startsWith("UC")) return cid

                val be = obj.optJSONObject("browseEndpoint")
                val bid = be?.optString("browseId")
                if (!bid.isNullOrBlank() && bid.startsWith("UC")) return bid

                val keys = obj.keys()
                while (keys.hasNext()) {
                    val r = findChannelIdRecursively(obj.get(keys.next()))
                    if (r != null) return r
                }
            }
            is JSONArray -> {
                for (i in 0 until obj.length()) {
                    val r = findChannelIdRecursively(obj.opt(i))
                    if (r != null) return r
                }
            }
        }
        return null
    }

    /**
     * Fetches real channel videos with continuation token pagination support.
     * Returns Pair of (videos, nextContinuationToken).
     */
    suspend fun fetchChannelVideos(
        channelNameOrId: String,
        continuationToken: String? = null
    ): Pair<List<VideoItem>, String?> = withContext(Dispatchers.IO) {
        val outVideos = mutableListOf<VideoItem>()
        var nextToken: String? = null

        if (!continuationToken.isNullOrBlank()) {
            try {
                val payload = JSONObject().apply {
                    put("context", buildContext())
                    put("continuation", continuationToken)
                }
                val resp = callInnerTube("browse", payload)
                if (resp != null) {
                    extractVideosRecursively(resp, outVideos)
                    nextToken = extractContinuationToken(resp)
                    return@withContext Pair(outVideos.distinctBy { it.id }, nextToken)
                }
            } catch (e: Exception) {
                android.util.Log.e("YouTubeSyncEngine", "fetchChannelVideos continuation error", e)
            }
        }

        // Initial page via channelId
        val cid = resolveChannelId(channelNameOrId)
        if (!cid.isNullOrBlank()) {
            try {
                val payload = JSONObject().apply {
                    put("context", buildContext())
                    put("browseId", cid)
                    put("params", "EgZ2aWRlb3PyBgQKAjoA")
                }
                val resp = callInnerTube("browse", payload)
                if (resp != null) {
                    extractVideosRecursively(resp, outVideos)
                    nextToken = extractContinuationToken(resp)
                }
            } catch (e: Exception) {
                android.util.Log.e("YouTubeSyncEngine", "fetchChannelVideos initial error", e)
            }
        }

        // Fallback if empty: YouTubeSearchEngine search
        if (outVideos.isEmpty()) {
            val searchFallback = YouTubeSearchEngine.searchVideos("$channelNameOrId official")
            if (searchFallback.isNotEmpty()) {
                return@withContext Pair(searchFallback, null)
            }
        }

        Pair(outVideos.distinctBy { it.id }, nextToken)
    }

    /**
     * Fetches real channel playlists with continuation token pagination support.
     * Returns Pair of (playlists, nextContinuationToken).
     */
    suspend fun fetchChannelPlaylists(
        channelNameOrId: String,
        continuationToken: String? = null
    ): Pair<List<UserPlaylist>, String?> = withContext(Dispatchers.IO) {
        val outPlaylists = mutableListOf<UserPlaylist>()
        var nextToken: String? = null

        if (!continuationToken.isNullOrBlank()) {
            try {
                val payload = JSONObject().apply {
                    put("context", buildContext())
                    put("continuation", continuationToken)
                }
                val resp = callInnerTube("browse", payload)
                if (resp != null) {
                    extractPlaylistsRecursively(resp, outPlaylists)
                    nextToken = extractContinuationToken(resp)
                    return@withContext Pair(outPlaylists.distinctBy { it.id }, nextToken)
                }
            } catch (e: Exception) {
                android.util.Log.e("YouTubeSyncEngine", "fetchChannelPlaylists continuation error", e)
            }
        }

        // Initial page via channelId
        val cid = resolveChannelId(channelNameOrId)
        if (!cid.isNullOrBlank()) {
            try {
                val payload1 = JSONObject().apply {
                    put("context", buildContext())
                    put("browseId", cid)
                    put("params", "EglwbGF5bGlzdHPyBgoKCEIGCgIQaCIA")
                }
                val resp1 = callInnerTube("browse", payload1)
                if (resp1 != null) {
                    extractPlaylistsRecursively(resp1, outPlaylists)
                    nextToken = extractContinuationToken(resp1)
                }

                if (outPlaylists.isEmpty()) {
                    val payload2 = JSONObject().apply {
                        put("context", buildContext())
                        put("browseId", cid)
                        put("params", "EghwbGF5bGlzdHPyBgQKAjoA")
                    }
                    val resp2 = callInnerTube("browse", payload2)
                    if (resp2 != null) {
                        extractPlaylistsRecursively(resp2, outPlaylists)
                        nextToken = extractContinuationToken(resp2)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("YouTubeSyncEngine", "fetchChannelPlaylists initial error", e)
            }
        }

        Pair(outPlaylists.distinctBy { it.id }, nextToken)
    }

    /**
     * Fetches real channel shorts with continuation token pagination support.
     * Returns Pair of (shorts, nextContinuationToken).
     */
    suspend fun fetchChannelShorts(
        channelNameOrId: String,
        continuationToken: String? = null
    ): Pair<List<VideoItem>, String?> = withContext(Dispatchers.IO) {
        val outShorts = mutableListOf<VideoItem>()
        var nextToken: String? = null

        if (!continuationToken.isNullOrBlank()) {
            try {
                val payload = JSONObject().apply {
                    put("context", buildContext())
                    put("continuation", continuationToken)
                }
                val resp = callInnerTube("browse", payload)
                if (resp != null) {
                    extractVideosRecursively(resp, outShorts)
                    nextToken = extractContinuationToken(resp)
                    return@withContext Pair(outShorts.distinctBy { it.id }, nextToken)
                }
            } catch (e: Exception) {
                android.util.Log.e("YouTubeSyncEngine", "fetchChannelShorts continuation error", e)
            }
        }

        val cid = resolveChannelId(channelNameOrId)
        if (!cid.isNullOrBlank()) {
            try {
                val payload = JSONObject().apply {
                    put("context", buildContext())
                    put("browseId", cid)
                    put("params", "EgZzaG9ydHPyBgUKA5oBAA%3D%3D")
                }
                val resp = callInnerTube("browse", payload)
                if (resp != null) {
                    extractVideosRecursively(resp, outShorts)
                    nextToken = extractContinuationToken(resp)
                }
            } catch (e: Exception) {
                android.util.Log.e("YouTubeSyncEngine", "fetchChannelShorts initial error", e)
            }
        }

        Pair(outShorts.distinctBy { it.id }, nextToken)
    }
}
