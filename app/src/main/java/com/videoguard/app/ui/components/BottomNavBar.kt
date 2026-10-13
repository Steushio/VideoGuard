package com.videoguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.videoguard.app.data.AccountRepository
import com.videoguard.app.model.AppMode
import com.videoguard.app.model.YouTubeMusicTab
import com.videoguard.app.model.YouTubeTab

@Composable
fun BottomNavBar(
    currentMode: AppMode,
    selectedYTTab: YouTubeTab = YouTubeTab.HOME,
    selectedMusicTab: YouTubeMusicTab = YouTubeMusicTab.HOME,
    isSearchActive: Boolean = false,
    onHomeClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onYTTabSelected: (YouTubeTab) -> Unit = {},
    onMusicTabSelected: (YouTubeMusicTab) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isMusic = currentMode == AppMode.YOUTUBE_MUSIC
    val bgColor = if (isMusic) Color(0xFF030303) else Color(0xFF0F0F0F)

    val isHomeSelected = !isSearchActive && if (!isMusic) {
        selectedYTTab == YouTubeTab.HOME
    } else {
        selectedMusicTab == YouTubeMusicTab.HOME
    }

    val isSearchSelected = isSearchActive

    val isAccountSelected = !isSearchActive && if (!isMusic) {
        selectedYTTab == YouTubeTab.YOU
    } else {
        selectedMusicTab == YouTubeMusicTab.LIBRARY
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.6.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home
            BottomNavItem(
            title = "Home",
            icon = if (isHomeSelected) Icons.Filled.Home else Icons.Outlined.Home,
            isSelected = isHomeSelected,
            onClick = {
                onHomeClick()
                onYTTabSelected(YouTubeTab.HOME)
                onMusicTabSelected(YouTubeMusicTab.HOME)
            }
        )

        // 2. Search
        BottomNavItem(
            title = "Search",
            icon = if (isSearchSelected) Icons.Filled.Search else Icons.Outlined.Search,
            isSelected = isSearchSelected,
            onClick = onSearchClick
        )

        // 3. Account
        BottomAccountNavItem(
            title = "Account",
            isSelected = isAccountSelected,
            onClick = {
                onAccountClick()
                onYTTabSelected(YouTubeTab.YOU)
                onMusicTabSelected(YouTubeMusicTab.LIBRARY)
            }
        )
    }
}
}

@Composable
private fun RowScope.BottomNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) Color.White else Color(0xFFAAAAAA),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            color = if (isSelected) Color.White else Color(0xFFAAAAAA),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun RowScope.BottomAccountNavItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val currentAccount by AccountRepository.currentAccount.collectAsState()
    val isIncognito by AccountRepository.isIncognito.collectAsState()
    val account = currentAccount

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isIncognito) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = title,
                    tint = if (isSelected) Color.White else Color(0xFFAAAAAA),
                    modifier = Modifier.size(22.dp)
                )
            } else if (account != null && !account.avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = account.avatarUrl,
                    contentDescription = account.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .then(
                            if (isSelected) Modifier.border(1.5.dp, Color.White, CircleShape)
                            else Modifier
                        )
                )
            } else if (account != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(account.avatarBgColorHex))
                        .then(
                            if (isSelected) Modifier.border(1.5.dp, Color.White, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = account.initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            } else {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                    contentDescription = title,
                    tint = if (isSelected) Color.White else Color(0xFFAAAAAA),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            color = if (isSelected) Color.White else Color(0xFFAAAAAA),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
