# VideoGuard - Dual-Mode YouTube & YouTube Music Android App

An Android application that replicates the **YouTube mobile app** experience while featuring an integrated **YouTube Music mode toggle** that dynamically transforms the video app into a dedicated music streaming app without interrupting playback.

---

## 🌟 Key Features

### 1. 🔄 Instant Dual-Mode Toggle
- **Top Bar Pill Switcher (`[ ▶ Video | 🎵 Music ]`)**: Switch seamlessly at any point between standard YouTube video browsing and YouTube Music mode.
- **Continuous Playback Preservation**: When toggling between modes, the currently active track/video continues playing seamlessly with identical playback timestamp and state.
- **In-Player Quick-Switch**: One-tap "Play in YT Music" pill directly inside the YouTube video watch screen.

### 2. 📺 YouTube Mobile Mode (Video App)
- **Authentic YouTube Mobile UI**:
  - Classic YouTube Dark Theme (`#0F0F0F`) and red brand accents (`#FF0000`).
  - Top header with YouTube logo, Cast, Notifications, Search, and Profile avatar.
  - Category filter chips: *All, Music, Podcasts, Gaming, News, Tech, Trending, Recently uploaded*.
  - Full-fidelity video cards with 16:9 thumbnails, duration badges (`4:20`), channel avatars, video titles, view counts, and upload dates.
  - Bottom navigation bar: *Home*, *Shorts*, *Subscriptions*, *You*.
- **Video Watch Experience**:
  - Integrated YouTube Player with controls, buffering handling, and fullscreen capabilities.
  - Video title, views, and timestamp.
  - Channel row with subscriber count and interactive **Subscribe** state.
  - Action pills: **Like** (with counters), **Dislike**, **Share**, **Remix**, **Download**, and **Save**.
  - Comments preview with comment counts and top comment highlights.
  - Related videos queue.

### 3. 🎵 YouTube Music Mode (Music App)
- **Dedicated YouTube Music UI**:
  - Obsidian dark aesthetic (`#030303`), circular YouTube Music logo with inner white play glyph.
  - Mood & activity filter chips: *Energize, Workout, Relax, Focus, Commute, Party*.
  - **Quick picks** curated song lists with instant single-tap playback.
  - **Listen again** & **Recommended music videos** horizontal album carousels.
  - **Trending Charts** song ranking.
  - Bottom navigation bar: *Home*, *Samples*, *Explore*, *Library*.
- **YouTube Music Player Screen**:
  - **Signature `[ Song | Video ]` Toggle**:
    - **Song Mode**: Focuses on large 1:1 square album artwork with rounded corners, drop shadows, and animated audio equalizer visualizer waveforms.
    - **Video Mode**: Smoothly switches to the embedded music video player right inside the music player sheet.
  - Full playback controls: Shuffle, Previous, big circular Play/Pause button, Next, Repeat.
  - Like & Dislike rating buttons.
  - Scrubbable progress slider with elapsed time and total duration.
  - Bottom sheet with 3 tabs:
    - **UP NEXT**: Interactive queue of upcoming songs.
    - **LYRICS**: Beautifully formatted, readable lyrics.
    - **RELATED**: Artist information, discography highlights, and similar tracks.

### 4. 🎛️ Floating Mini-Player
- A persistent mini-player bar that docks directly above the bottom navigation bar when navigating through feeds.
- Displays thumbnail/album cover, song title, channel/artist, play/pause toggle, and progress indicator.
- Tap anywhere on the bar to smoothly expand back into the full player.

---

## 🏗️ Architecture & Technology Stack

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (Material 3)
- **Build System**: Gradle 8.13 + Android Gradle Plugin 8.7.3
- **Target SDK**: Android 15 (API 35) | **Min SDK**: Android 8.0 (API 26)
- **Image Loading**: Coil 2.7.0
- **Video Engine**: Hardware-accelerated WebView bridge utilizing the YouTube IFrame API.

---

## 🚀 Building & Running

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (API 35 platform & build-tools)

### Build Debug APK
```powershell
.\gradlew.bat assembleDebug
```
The compiled APK will be output to:
`app\build\outputs\apk\debug\app-debug.apk`

### Install on Connected Device or Emulator
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
