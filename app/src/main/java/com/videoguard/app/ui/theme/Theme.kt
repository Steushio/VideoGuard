package com.videoguard.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.videoguard.app.model.AppMode

private val YouTubeColorScheme = darkColorScheme(
    primary = YouTubeRed,
    onPrimary = YouTubeTextPrimary,
    background = YouTubeDarkBg,
    onBackground = YouTubeTextPrimary,
    surface = YouTubeCardBg,
    onSurface = YouTubeTextPrimary,
    surfaceVariant = YouTubeChipBg,
    onSurfaceVariant = YouTubeTextSecondary
)

private val YTMusicColorScheme = darkColorScheme(
    primary = YTMusicRedAccent,
    onPrimary = YTMusicTextPrimary,
    background = YTMusicBlack,
    onBackground = YTMusicTextPrimary,
    surface = YTMusicSurface,
    onSurface = YTMusicTextPrimary,
    surfaceVariant = YTMusicChipBg,
    onSurfaceVariant = YTMusicTextSecondary
)

@Composable
fun VideoGuardTheme(
    appMode: AppMode = AppMode.YOUTUBE,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appMode) {
        AppMode.YOUTUBE -> YouTubeColorScheme
        AppMode.YOUTUBE_MUSIC -> YTMusicColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
