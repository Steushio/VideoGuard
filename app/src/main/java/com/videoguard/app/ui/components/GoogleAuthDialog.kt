package com.videoguard.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.videoguard.app.data.AccountRepository
import com.videoguard.app.data.UserDataRepository
import com.videoguard.app.data.YouTubeSyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleAuthDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("https://accounts.google.com/AccountChooser?service=youtube&continue=https%3A%2F%2Fm.youtube.com%2F") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF282828))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sign in to YouTube",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (currentUrl.contains("youtube.com")) "m.youtube.com" else "accounts.google.com",
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp
                        )
                    }

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFF3EA6FF),
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { webViewInstance?.reload() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Complete / Done Button
                    IconButton(
                        onClick = {
                            CookieManager.getInstance().flush()
                            webViewInstance?.evaluateJavascript(EXTRACTION_SCRIPT, null)
                            scope.launch(Dispatchers.IO) {
                                YouTubeSyncEngine.syncAllUserData()
                            }
                            scope.launch(Dispatchers.Main) {
                                onLoginSuccess()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Complete Login",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Interactive WebView for Google & YouTube Authentication
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewInstance = this
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                            }

                            // JS Interface Bridge
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onChannelProfileExtracted(name: String, email: String, avatarUrl: String?, handle: String) {
                                    val trimmedName = name.trim()
                                    if (trimmedName.isBlank() || trimmedName.equals("Google Account", ignoreCase = true) ||
                                        trimmedName.equals("YouTube", ignoreCase = true) || trimmedName.equals("Sign in", ignoreCase = true)
                                    ) return

                                    val finalName = trimmedName
                                    val finalEmail = email.trim()
                                    val finalHandle = if (handle.isNotBlank()) {
                                        if (handle.startsWith("@")) handle else "@$handle"
                                    } else {
                                        "@" + finalName.lowercase().replace("[^a-z0-9_]".toRegex(), "_")
                                    }

                                    AccountRepository.setGoogleAccount(
                                        name = finalName,
                                        email = finalEmail,
                                        avatarUrl = avatarUrl?.takeIf { it.isNotBlank() },
                                        handle = finalHandle
                                    )
                                }

                                @JavascriptInterface
                                fun onHistoryExtracted(jsonStr: String) {
                                    if (jsonStr.isNotBlank()) {
                                        try {
                                            val videos = YouTubeSyncEngine.parseVideosFromJsonString(jsonStr)
                                            if (videos.isNotEmpty()) {
                                                UserDataRepository.setOnlineHistory(videos)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun onFeedExtracted(jsonStr: String) {
                                    if (jsonStr.isNotBlank()) {
                                        try {
                                            val videos = YouTubeSyncEngine.parseVideosFromJsonString(jsonStr)
                                            if (videos.isNotEmpty()) {
                                                YouTubeSyncEngine.cacheHomeFeed(videos)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }

                                @JavascriptInterface
                                fun onSyncCompleted() {
                                    CookieManager.getInstance().flush()
                                    scope.launch(Dispatchers.IO) {
                                        YouTubeSyncEngine.syncAllUserData()
                                    }
                                    scope.launch(Dispatchers.Main) {
                                        onLoginSuccess()
                                    }
                                }
                            }, "VideoGuardAuth")

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let { currentUrl = it }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    url?.let { currentUrl = it }

                                    // When user successfully lands back on YouTube
                                    if (url != null && (url.contains("m.youtube.com") || url.contains("www.youtube.com")) &&
                                        !url.contains("accounts.google.com") && !url.contains("ServiceLogin") && !url.contains("signin")
                                    ) {
                                        CookieManager.getInstance().flush()
                                        view?.evaluateJavascript(EXTRACTION_SCRIPT, null)
                                    }
                                }
                            }

                            loadUrl("https://accounts.google.com/AccountChooser?service=youtube&continue=https%3A%2F%2Fm.youtube.com%2F")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private const val EXTRACTION_SCRIPT = """
(function() {
    if (!window.location.hostname.includes('youtube.com')) return;

    var apiKey = (window.ytcfg && window.ytcfg.get) ? (window.ytcfg.get('INNERTUBE_API_KEY') || 'AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8') : 'AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8';
    var clientVer = (window.ytcfg && window.ytcfg.get) ? (window.ytcfg.get('INNERTUBE_CLIENT_VERSION') || '2.20261010.01.00') : '2.20261010.01.00';

    function getContext() {
        if (window.ytcfg && window.ytcfg.get) {
            return window.ytcfg.get('INNERTUBE_CONTEXT') || {
                client: { clientName: 'MWEB', clientVersion: clientVer, hl: 'en', gl: 'US' }
            };
        }
        return { client: { clientName: 'MWEB', clientVersion: clientVer, hl: 'en', gl: 'US' } };
    }

    // 1. Extract Profile (Channel Name, Handle, Email, Avatar)
    fetch('/youtubei/v1/account/account_menu?key=' + apiKey, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ context: getContext() })
    }).then(function(r) { return r.json(); }).then(function(data) {
        try {
            var header = data.actions[0].openPopupAction.popup.multiPageMenuRenderer.header.activeAccountHeaderRenderer;
            if (header) {
                var name = header.accountName ? (header.accountName.simpleText || (header.accountName.runs && header.accountName.runs[0] && header.accountName.runs[0].text) || '') : '';
                var email = header.email ? (header.email.simpleText || (header.email.runs && header.email.runs[0] && header.email.runs[0].text) || '') : '';
                var handle = header.channelHandle ? (header.channelHandle.simpleText || (header.channelHandle.runs && header.channelHandle.runs[0] && header.channelHandle.runs[0].text) || '') : '';
                var avatarUrl = '';
                if (header.accountPhoto && header.accountPhoto.thumbnails && header.accountPhoto.thumbnails.length > 0) {
                    avatarUrl = header.accountPhoto.thumbnails[header.accountPhoto.thumbnails.length - 1].url || '';
                }
                if (name && name !== 'Google Account' && name !== 'YouTube') {
                    window.VideoGuardAuth.onChannelProfileExtracted(name, email, avatarUrl, handle);
                }
            }
        } catch(e) {}
    }).catch(function(e) {});

    // 2. Extract Watch History
    fetch('/youtubei/v1/browse?key=' + apiKey, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ context: getContext(), browseId: 'FEhistory' })
    }).then(function(r) { return r.json(); }).then(function(data) {
        try {
            window.VideoGuardAuth.onHistoryExtracted(JSON.stringify(data));
        } catch(e) {}
    }).catch(function(e) {});

    // 3. Extract Home Feed Recommendations
    fetch('/youtubei/v1/browse?key=' + apiKey, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ context: getContext(), browseId: 'FEwhat_to_watch' })
    }).then(function(r) { return r.json(); }).then(function(data) {
        try {
            window.VideoGuardAuth.onFeedExtracted(JSON.stringify(data));
        } catch(e) {}
    }).catch(function(e) {});

    // 4. Automatically complete and dismiss dialog
    setTimeout(function() {
        window.VideoGuardAuth.onSyncCompleted();
    }, 1200);
})();
"""
