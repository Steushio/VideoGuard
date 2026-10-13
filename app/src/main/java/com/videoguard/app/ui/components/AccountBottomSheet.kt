package com.videoguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.videoguard.app.ui.theme.YouTubeRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBottomSheet(
    currentMode: AppMode,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAccount by AccountRepository.currentAccount.collectAsState()
    val isIncognito by AccountRepository.isIncognito.collectAsState()
    var showGoogleAuthDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val accountPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val accountName = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                val displayName = AccountRepository.formatAccountName(accountName)
                AccountRepository.setGoogleAccount(
                    name = displayName,
                    email = accountName,
                    avatarUrl = if (accountName.contains("genfox", ignoreCase = true)) currentAccount?.avatarUrl else null,
                    handle = "@" + accountName.substringBefore("@")
                )
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    com.videoguard.app.data.YouTubeSyncEngine.syncAllUserData()
                }
            }
        }
    }

    val isMusic = currentMode == AppMode.YOUTUBE_MUSIC
    val sheetBg = if (isMusic) Color(0xFF161616) else Color(0xFF212121)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = sheetBg,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFF666666))
        },
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            if (currentAccount == null && !isIncognito) {
                // NOT SIGNED IN: YouTube Official Google Sign-In Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF333333)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Guest",
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (isMusic) "Sign in to YouTube Music" else "Sign in to YouTube",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Access your subscriptions, playlists, upload videos, and listen ad-free with your Google account.",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showGoogleAuthDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3EA6FF)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Sign In",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign in with Google",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showGoogleAuthDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = "Device Account",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Google account from device", fontSize = 14.sp)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                // SIGNED IN: Real Google Account Profile Info
                item {
                    val account = currentAccount
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isIncognito) Color(0xFF424242)
                                    else Color(account?.avatarBgColorHex ?: 0xFF1976D2L)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isIncognito) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Incognito",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else if (!account?.avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = account!!.avatarUrl,
                                    contentDescription = account.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = account?.initial ?: "G",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isIncognito) "You're incognito" else (account?.name ?: "Google Account"),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isIncognito) "Browsing privately" else (account?.email ?: ""),
                                color = Color(0xFFAAAAAA),
                                fontSize = 13.sp
                            )
                            if (!isIncognito && account?.handle?.isNotEmpty() == true) {
                                Text(
                                    text = account.handle,
                                    color = Color(0xFF888888),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showGoogleAuthDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF3EA6FF)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwitchAccount,
                                contentDescription = "Switch",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Switch Account", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    com.videoguard.app.data.YouTubeSyncEngine.syncAllUserData()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272727), contentColor = Color.White),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Sync YT Data", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { AccountRepository.signOut() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(text = "Sign out", fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // Quick Actions: Incognito, Channels, Settings
            item {
                AccountMenuItem(
                    icon = if (isIncognito) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    title = if (isIncognito) "Turn off Incognito" else "Turn on Incognito",
                    onClick = {
                        AccountRepository.toggleIncognito()
                    }
                )

                AccountMenuItem(
                    icon = Icons.Outlined.AccountBox,
                    title = if (isMusic) "Your music profile" else "Your channel",
                    onClick = onDismiss
                )

                AccountMenuItem(
                    icon = Icons.Outlined.WorkspacePremium,
                    title = if (isMusic) "Get Music Premium" else "Get YouTube Premium",
                    badge = "Ad-free",
                    onClick = onDismiss
                )

                AccountMenuItem(
                    icon = Icons.Outlined.History,
                    title = "History & playlists",
                    onClick = onDismiss
                )

                AccountMenuItem(
                    icon = Icons.Outlined.Settings,
                    title = "Settings",
                    onClick = onDismiss
                )

                AccountMenuItem(
                    icon = Icons.Outlined.HelpOutline,
                    title = "Help & feedback",
                    onClick = onDismiss
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (showGoogleAuthDialog) {
        GoogleAuthDialog(
            onDismiss = { showGoogleAuthDialog = false },
            onLoginSuccess = {
                showGoogleAuthDialog = false
            }
        )
    }
}

@Composable
private fun AccountMenuItem(
    icon: ImageVector,
    title: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(YouTubeRed)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
