package com.videoguard.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.videoguard.app.service.PlaybackController
import com.videoguard.app.ui.screens.MainScreen
import com.videoguard.app.ui.theme.VideoGuardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission granted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlaybackController.init(this)

        CoroutineScope(Dispatchers.IO).launch {
            com.videoguard.app.data.YouTubeSyncEngine.syncAllUserData()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            VideoGuardTheme {
                MainScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (com.videoguard.app.data.YouTubeSyncEngine.hasAuthCookies()) {
            CoroutineScope(Dispatchers.IO).launch {
                com.videoguard.app.data.YouTubeSyncEngine.syncAllUserData()
            }
        }
    }
}
