package com.videoguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun ModeTogglePill(
    currentMode: AppMode,
    onModeChanged: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMusic = currentMode == AppMode.YOUTUBE_MUSIC

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isMusic) Color(0xFF242424) else Color(0xFF272727))
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // YouTube Mode Option
            ModeOptionItem(
                title = "Video",
                icon = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        modifier = Modifier.size(16.dp),
                        tint = if (!isMusic) Color.White else Color(0xFFAAAAAA)
                    )
                },
                isSelected = !isMusic,
                selectedBgColor = YouTubeRed,
                onClick = { onModeChanged(AppMode.YOUTUBE) }
            )

            // YouTube Music Mode Option
            ModeOptionItem(
                title = "Music",
                icon = {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        modifier = Modifier.size(16.dp),
                        tint = if (isMusic) Color.White else Color(0xFFAAAAAA)
                    )
                },
                isSelected = isMusic,
                selectedBgColor = YTMusicRedAccent,
                onClick = { onModeChanged(AppMode.YOUTUBE_MUSIC) }
            )
        }
    }
}

@Composable
private fun ModeOptionItem(
    title: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    selectedBgColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) selectedBgColor else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bgAnim"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFFAAAAAA),
        label = "textAnim"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
