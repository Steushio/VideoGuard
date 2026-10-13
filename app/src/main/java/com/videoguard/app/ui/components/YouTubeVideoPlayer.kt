package com.videoguard.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import android.view.MotionEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.videoguard.app.service.PlaybackController

class BackgroundPlayWebView(context: Context) : WebView(context) {
    override fun onWindowVisibilityChanged(visibility: Int) {
        // Report VISIBLE so Chromium does not kill audio decoder in background
        super.onWindowVisibilityChanged(View.VISIBLE)
    }

    override fun dispatchWindowVisibilityChanged(visibility: Int) {
        super.dispatchWindowVisibilityChanged(View.VISIBLE)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, View.VISIBLE)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(true)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Delegate all touch events to Compose layer so native YouTube controls handle them
        return false
    }
}

class YouTubeBridge(
    private val readyCallback: () -> Unit,
    private val stateChangeCallback: (Int) -> Unit,
    private val timeUpdateCallback: (Float, Float) -> Unit
) {
    @JavascriptInterface
    fun onReady() {
        readyCallback()
    }

    @JavascriptInterface
    fun onStateChange(state: Int) {
        stateChangeCallback(state)
    }

    @JavascriptInterface
    fun onTimeUpdate(currentTime: Float, duration: Float) {
        timeUpdateCallback(currentTime, duration)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeVideoPlayer(
    videoId: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onStateChange: (Int) -> Unit = {}
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentLoadedId by remember { mutableStateOf<String?>(null) }

    // Register with PlaybackController so transport buttons execute JS commands directly
    DisposableEffect(webViewRef) {
        PlaybackController.setCommandListener { command ->
            webViewRef?.post {
                webViewRef?.evaluateJavascript(command, null)
            }
        }
        onDispose {
            PlaybackController.setCommandListener(null)
        }
    }

    // Video ID updates
    LaunchedEffect(videoId) {
        if (currentLoadedId != null && currentLoadedId != videoId) {
            webViewRef?.evaluateJavascript("loadVideo('$videoId');", null)
            currentLoadedId = videoId
        }
    }

    // Play/Pause sync from Compose state
    LaunchedEffect(isPlaying) {
        webViewRef?.evaluateJavascript("setPlaybackState($isPlaying);", null)
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                BackgroundPlayWebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(AndroidColor.BLACK)

                    clearCache(true)
                    settings.apply {
                        javaScriptEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        allowContentAccess = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            android.util.Log.d("YouTubePlayerJS", "${consoleMessage?.message()}")
                            return true
                        }
                    }

                    val chromeUserAgent = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    settings.userAgentString = chromeUserAgent

                    webViewClient = WebViewClient()

                    val bridge = YouTubeBridge(
                        readyCallback = {
                            currentLoadedId = videoId
                            evaluateJavascript("setPlaybackState($isPlaying);", null)
                        },
                        stateChangeCallback = { state ->
                            // Synchronize player state with PlaybackController
                            when (state) {
                                1 -> PlaybackController.setPlayingState(true)   // PLAYING
                                2 -> PlaybackController.setPlayingState(false)  // PAUSED
                                0 -> {
                                    if (PlaybackController.isLoopEnabled.value) {
                                        PlaybackController.seekTo(0f)
                                        PlaybackController.play()
                                    } else {
                                        PlaybackController.nextTrack()
                                    }
                                }
                            }
                            onStateChange(state)
                        },
                        timeUpdateCallback = { current, duration ->
                            PlaybackController.updateProgress(current, duration)
                        }
                    )
                    addJavascriptInterface(bridge, "AndroidBridge")

                    val htmlData = getYouTubePlayerHtml(videoId)
                    loadDataWithBaseURL("https://www.youtube-nocookie.com", htmlData, "text/html", "UTF-8", null)

                    webViewRef = this
                }
            },
            update = { webView ->
                webViewRef = webView
            }
        )
    }
}

private fun getYouTubePlayerHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body, html { width: 100%; height: 100%; background-color: #000; overflow: hidden; pointer-events: none; user-select: none; }
            #player-wrapper { width: 100%; height: 100%; position: relative; overflow: hidden; background-color: #000; }
            #player { width: 100%; height: 100%; position: absolute; top: 0; left: 0; border: none; }
          </style>
          <script>
            window.userWantsPlay = true;

            try {
              Object.defineProperty(document, 'hidden', { get: function() { return false; } });
              Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; } });
              Object.defineProperty(document, 'webkitVisibilityState', { get: function() { return 'visible'; } });
              document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
              window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
              window.addEventListener('pagehide', function(e) { e.stopImmediatePropagation(); }, true);
              window.addEventListener('blur', function(e) { e.stopImmediatePropagation(); }, true);
            } catch(e) {}

            function setPlaybackState(play) {
              window.userWantsPlay = play;
              var iframe = document.getElementById('player');
              var cmd = play ? 'playVideo' : 'pauseVideo';
              if (iframe && iframe.contentWindow) {
                iframe.contentWindow.postMessage(JSON.stringify({
                  "event": "command",
                  "func": cmd,
                  "args": ""
                }), '*');
              }
              if (window.ytPlayer) {
                if (play && window.ytPlayer.playVideo) window.ytPlayer.playVideo();
                if (!play && window.ytPlayer.pauseVideo) window.ytPlayer.pauseVideo();
              }
            }

            function loadVideo(id) {
              var iframe = document.getElementById('player');
              if (iframe && iframe.contentWindow) {
                iframe.contentWindow.postMessage(JSON.stringify({
                  "event": "command",
                  "func": "loadVideoById",
                  "args": [id]
                }), '*');
              }
              if (window.ytPlayer && window.ytPlayer.loadVideoById) {
                window.ytPlayer.loadVideoById(id);
              }
            }

            function seekTo(seconds) {
              if (window.ytPlayer && window.ytPlayer.seekTo) {
                window.ytPlayer.seekTo(seconds, true);
              }
            }

            function seekBy(seconds) {
              if (window.ytPlayer && window.ytPlayer.getCurrentTime && window.ytPlayer.seekTo) {
                var cur = window.ytPlayer.getCurrentTime();
                window.ytPlayer.seekTo(cur + seconds, true);
              }
            }

            function setPlaybackRate(rate) {
              if (window.ytPlayer && window.ytPlayer.setPlaybackRate) {
                window.ytPlayer.setPlaybackRate(rate);
              }
            }

            function setPlaybackQuality(quality) {
              if (window.ytPlayer && window.ytPlayer.setPlaybackQuality) {
                window.ytPlayer.setPlaybackQuality(quality);
              }
            }

            setInterval(function() {
              if (window.ytPlayer && typeof window.ytPlayer.getCurrentTime === 'function' && typeof window.ytPlayer.getDuration === 'function') {
                try {
                  var cur = window.ytPlayer.getCurrentTime();
                  var dur = window.ytPlayer.getDuration();
                  if (typeof cur === 'number' && typeof dur === 'number' && !isNaN(cur) && !isNaN(dur) && cur >= 0) {
                    if (window.AndroidBridge && window.AndroidBridge.onTimeUpdate) {
                      window.AndroidBridge.onTimeUpdate(cur, dur);
                    }
                  }
                } catch(e) {}
              }
            }, 500);
          </script>
        </head>
        <body>
          <div id="player-wrapper">
            <div id="player"></div>
          </div>
          <script>
            var tag = document.createElement('script');
            tag.src = "https://www.youtube.com/iframe_api";
            var firstScriptTag = document.getElementsByTagName('script')[0];
            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

            var ytPlayer;
            function onYouTubeIframeAPIReady() {
              ytPlayer = new YT.Player('player', {
                videoId: '$videoId',
                host: 'https://www.youtube-nocookie.com',
                playerVars: {
                  'autoplay': 1,
                  'playsinline': 1,
                  'controls': 0,
                  'rel': 0,
                  'modestbranding': 1,
                  'fs': 0,
                  'disablekb': 1,
                  'enablejsapi': 1,
                  'iv_load_policy': 3,
                  'showinfo': 0,
                  'autohide': 1
                },
                events: {
                  'onReady': onPlayerReady,
                  'onStateChange': onPlayerStateChange
                }
              });
              window.ytPlayer = ytPlayer;
            }

            function onPlayerReady(event) {
              window.ytPlayer = event.target;
              if (window.userWantsPlay) {
                event.target.playVideo();
              }
              if (window.AndroidBridge) {
                window.AndroidBridge.onReady();
              }
            }

            function onPlayerStateChange(event) {
              // 1: PLAYING, 2: PAUSED, 0: ENDED
              if (event.data === 2) {
                window.userWantsPlay = false;
              } else if (event.data === 1) {
                window.userWantsPlay = true;
              }
              if (window.AndroidBridge) {
                window.AndroidBridge.onStateChange(event.data);
              }
            }
          </script>
        </body>
        </html>
    """.trimIndent()
}
