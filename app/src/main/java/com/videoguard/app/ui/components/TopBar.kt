package com.videoguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videoguard.app.model.AppMode
import com.videoguard.app.ui.theme.YouTubeRed
import com.videoguard.app.ui.theme.YTMusicRedAccent

@Composable
fun TopBar(
    currentMode: AppMode,
    onModeChanged: (AppMode) -> Unit,
    onTitleClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    isBackgroundPlay: Boolean = true,
    onToggleBackgroundPlay: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isMusic = currentMode == AppMode.YOUTUBE_MUSIC
    val bgColor = if (isMusic) Color(0xFF030303) else Color(0xFF0F0F0F)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Name on Top Left (Crisp modern branding)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onTitleClick() }
        ) {
            Text(
                text = "VideoGuard",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = (-0.5).sp
            )
        }

        // Right Action: Mode switcher pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF222222))
                .clickable {
                    onModeChanged(if (isMusic) AppMode.YOUTUBE else AppMode.YOUTUBE_MUSIC)
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isMusic) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                    contentDescription = "Switch Mode",
                    tint = if (isMusic) YouTubeRed else YTMusicRedAccent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isMusic) "Video" else "Music",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
