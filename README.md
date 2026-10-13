# VideoGuard

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen?style=for-the-badge)
![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-orange?style=for-the-badge)

**All-in-one, ad-free YouTube & YouTube Music client for Android with continuous background playback.**

</div>

---

## 📖 Overview

**VideoGuard** brings the complete YouTube video catalog and YouTube Music together in a single native Android app. Built entirely with **Jetpack Compose** and **Material 3**, VideoGuard lets you switch between watching full-fidelity videos and streaming music without interrupting playback, all while keeping audio playing in the background with the screen turned off.

---

## 🌟 Key Features

### 🎧 Background Audio Playback
- **Screen-Off Listening**: Audio keeps streaming smoothly when your phone screen is locked.
- **App Minimization**: Continue listening while switching apps or browsing other tasks.
- **Persistent Engine**: Background audio service ensures zero audio cutoffs or decoder drops.

### 🔄 Dual-Mode Experience (Video & Music)
- **Instant Mode Switch**: Seamlessly transition between YouTube Video and YouTube Music with a single tap.
- **Playback Continuity**: Switching modes preserves the exact track, timestamp, and play state without reloading.
- **Top Header Toggle**: Quick-switch pill accessible directly from the top bar.

### 🎬 Modernized Video Player
- **Zero Cropping / Distortion**: Full native 16:9 aspect ratio video presentation with YouTube default clarity.
- **Modern Compose HUD Overlay**:
  - Center solid Play / Pause button.
  - Rewind 10s and Fast-Forward 10s quick buttons.
  - Next / Previous track buttons.
  - Working **Fullscreen** toggle for immersive landscape viewing.
  - Interactive scrubbable progress bar with elapsed and total duration.
- **Queue & Related Shelf**: Scrollable "Up Next" queue and related video recommendations right below the player.

### 🎵 Dedicated YouTube Music Mode
- **Song / Video Switcher**: Placed conveniently at the top center of music playback to switch between pure album art and music video.
- **Protected Album View**: Touch-optimized album artwork that prevents accidental pause/play triggers during browsing.
- **Curated Feeds**: Quick picks, Listen again, Trending Charts, and mood filters (*Energize, Relax, Focus, Workout*).
- **Music Library**: Liked music, synced playlists, and history.

### 🎛️ Floating Mini-Player
- Docks smoothly above bottom navigation tabs when browsing feeds or channels.
- Displays thumbnail, video title, channel name, play/pause controls, and live progress bar.
- Expand back to the full player with a single tap.

### 👤 Account & Playlist Management
- **Universal Playlist Discovery**: Automatically fetches all user playlists from YouTube library, playlist aggregation, channel playlists, and sidebar guide.
- **Deep Video Pagination**: Supports full continuation pagination up to 2,000+ videos per playlist (Liked Videos, Watch Later, and large custom series/podcasts).
- **Google Account Sync**: Syncs personalized recommendations, watch history, subscriptions, and profile info.
- **Account Switcher**: Easy sheet for switching connected Google accounts or logging out.

### 📱 Native Navigation & Feed Polish
- **Pull-to-Refresh Feed**: Drag down on the home feed to refresh with native YouTube-styled indicator.
- **Home Tap to Scroll-to-Top**: Tapping the Home navigation button while on the main feed smoothly scrolls back to the very top (just like the official YouTube app).
- **Infinite Search & Feed Scrolling**: Seamless automatic pagination as you scroll down for more videos.
- **Minimalist Top Bar**: Clean typography with instant toggle between Video and Music modes.

---

## 🏗️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/) (v2.0.21)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
- **Image Loading**: [Coil Compose](https://coil-kt.github.io/coil/) (v2.7.0)
- **Player Bridge**: Hardware-accelerated Chromium WebView bridge with bidirectional JavaScript interface
- **Media Controls**: AndroidX Media & Foreground Service integration
- **Build System**: Gradle 8.13 with Android Gradle Plugin 8.7.3

---

## 🚀 Getting Started

### Prerequisites
- **JDK 17** or **JDK 21**
- **Android SDK** (API Level 35 build-tools)
- Android device or emulator running **Android 8.0 (API 26)** or higher

### Building the APK

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Steushio/VideoGuard.git
   cd VideoGuard
   ```

2. **Build Debug APK**:
   - On Windows (PowerShell / Command Prompt):
     ```cmd
     gradlew.bat assembleDebug
     ```
   - On macOS / Linux:
     ```bash
     ./gradlew assembleDebug
     ```

3. **Locate the APK**:
   The compiled APK will be created at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

### Installing via ADB

With USB Debugging enabled on your Android phone:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📁 Project Structure

```
VideoGuard/
├── app/
│   ├── src/main/
│   │   ├── java/com/videoguard/app/
│   │   │   ├── data/            # YouTube API sync, search engines & repositories
│   │   │   ├── model/           # Data models (VideoItem, UserAccount, Playlist)
│   │   │   ├── service/         # MediaPlaybackService & PlaybackController
│   │   │   ├── ui/
│   │   │   │   ├── components/  # Player overlay, top bar, nav bar, mini-player
│   │   │   │   ├── screens/     # Feed, VideoDetail, MusicPlayer, You, Search
│   │   │   │   └── theme/       # Color palette, Typography, MaterialTheme
│   │   │   └── MainActivity.kt  # Root Compose Activity
│   │   ├── AndroidManifest.xml
│   │   └── res/                 # App launcher icons & resources
│   └── build.gradle.kts
├── gradle/                      # Version catalog & Gradle wrapper
├── build.gradle.kts
└── settings.gradle.kts
```

---

## ⚖️ Disclaimer

VideoGuard is an open-source project developed for educational and personal use. All video content, trademarks, and logos belong to their respective owners.
