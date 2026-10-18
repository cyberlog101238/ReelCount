# ReelCount — Local-First Short Video Tracker & Digital Wellbeing

<p align="center">
  <img src="assets/logo.png" width="140" height="140" alt="ReelCount Logo" />
  <br />
  <b>Break the doomscroll. Protect your brain from digital rot.</b>
</p>

<p align="center">
  <a href="https://github.com/cyberlog101238"><img src="https://img.shields.io/badge/Developer-SM_Ali_Ahsan_Al_Jabir-F59E0B.svg?style=flat" alt="Developer" /></a>
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84.svg?style=flat&logo=android" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin_1.9-7F52FF.svg?style=flat&logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-4285F4.svg?style=flat" alt="Compose" />
  <img src="https://img.shields.io/badge/Privacy-100%25_Offline-brightgreen.svg" alt="Offline" />
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License" /></a>
</p>

---

## 👨‍💻 Developer & Contact
* **Lead Developer:** SM Ali Ahsan Al Jabir
* **GitHub Profile:** [https://github.com/cyberlog101238](https://github.com/cyberlog101238)
* **Facebook:** [https://www.facebook.com/sm.ali.ahsan.al.jabir](https://www.facebook.com/sm.ali.ahsan.al.jabir)
* **Application ID:** `com.jabir.brot`

---

## 🌟 Key Features

* 🚀 **Universal Feed Detection:** Seamlessly monitors short-form videos across **Instagram Reels**, **Facebook Reels**, and **YouTube Shorts**.
* 🎯 **Smart Deduplication Guard:** Distinguishes genuine video transitions from playback scrub loops, likes, and comments. Enforces a 1.2-second human digestion cooldown and a 6-second re-entry window.
* 🫧 **Movable Floating Live Badge:**
  * Shows **only** the clean, round reel count badge.
  * Smooth, bounded drag-and-drop repositioning with persistent coordinate memory.
  * Hysteresis-backed debounced exit (1200ms) to prevent annoying flashing between videos.
  * **ZERO Persistent Status-Bar Notifications:** Uses direct `WindowManager` integration bound to the accessibility lifecycle.
* 📊 **Cognitive Digital Wellbeing & Analytics:**
  * **Real-time Velocity:** Measures Reels per hour and average dwell time per video.
  * **Interactive Progress Tab:** Visualizes hourly segments across the day and daily segments across the week.
  * **Dynamic Brain State:** From *Fresh* 🧠✨ to *Brain Rot* 💀💥 as daily consumption limits are reached.
* 🎨 **Obsidian & Warm Amber Theme:** Pure dark AMOLED theme with warm amber accents (`#F59E0B`). Fully reactive to dark/light switching.
* 📱 **Home Screen Widget:** Glances today's reel count, active dwell time, and current brain state right on your home screen.
* 🛠️ **Dedicated Developer Tab:** Toggleable diagnostic panel showing real-time event logs, node signatures, and state machine transitions for open-source contributors.
* 🔒 **100% Privacy & Zero Permissions Abuse:**
  * **NO INTERNET Permission:** The app is completely offline.
  * **NO Account Required:** All statistics are stored locally in Room SQLite.
  * **NO Text/Password Sniffing:** Only checks structural container IDs and video layout bounds.

---

## 🏗️ Architecture

ReelCount follows **Clean Architecture + MVVM** principles:

```
app/src/main/java/com/jabir/brot/
├── data/                    # Room Database, DAO entities, DataStore Preferences
│   ├── dao/                 # ViewingEventDao, DailyAggregateDao, TrackingSessionDao
│   ├── entity/              # Event logs and daily aggregates
│   └── repository/          # Offline repositories
├── domain/                  # Pure Kotlin domain logic & state machines
│   ├── detector/            # Platform-specific heuristics
│   │   ├── InstagramReelsDetector.kt
│   │   ├── FacebookReelsDetector.kt
│   │   ├── YouTubeShortsDetector.kt
│   │   └── PlatformDetector.kt
│   ├── guard/               # DuplicateEventGuard (debounce, session deduplication)
│   ├── model/               # DetectionEvent, ScreenSnapshot, VideoFingerprint
│   └── TrackingCoordinator.kt
├── service/                 # System service integrations
│   ├── ReelCountAccessibilityService.kt   # System-bound UI inspector
│   ├── FloatingOverlayController.kt       # Direct WindowManager floating badge
│   ├── DailyNotificationScheduler.kt      # Scheduled summary receiver
│   └── BootReceiver.kt                    # Restores schedules on restart
└── ui/                      # Jetpack Compose UI
    ├── components/          # Reusable cards, mascot views, indicators
    ├── navigation/          # NavGraph and tab definitions
    ├── screens/             # Dashboard, Statistics, Goals, Settings, Developer
    ├── theme/               # Color palette and typography
    ├── viewmodel/           # Screen ViewModels
    └── widget/              # Home screen Glance/AppWidgetProvider
```

---

## 📋 Target Platform Detection Heuristics

| Platform | Package | Primary Detection Signals | Deduplication Strategy |
| :--- | :--- | :--- | :--- |
| **Instagram Reels** | `com.instagram.android` | `clips_viewer`, `clips_video_container`, `clips_media_item`, "Reel by...", full-screen bounds ($\ge 75\%$). | SHA-256 fingerprint from `@author` and caption; settled swipe state machine. |
| **Facebook Reels** | `com.facebook.katana` | `FbShortsViewerActivity`, `FbShorts`, Litho virtual nodes ("Reel by...", "Like reel", "Original audio"), right action stack. | Regex metric stripping (`^[\d.,\s]+[KkMmBbTt]?$`), composite text signature fallback. |
| **YouTube Shorts** | `com.google.android.youtube` | `reel_watch_fragment_root`, `reel_recycler`, `reel_player`, Shorts pivot tab, channel handle. | Distinguishes full-screen Shorts fragment from 16:9 standard player and feed carousels. |

---

## 🛠️ Build & Installation Instructions

### Prerequisites
* **JDK 17+**
* **Android SDK 34 (Android 14)**

### Building the Debug APK
```bash
# On Windows (PowerShell)
.\gradlew.bat assembleDebug

# On macOS / Linux
chmod +x gradlew
./gradlew assembleDebug
```

The APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📲 Setup on Android Device

1. **Install APK:**
   * Download the latest `ReelCount.apk` from the [Releases](https://github.com/cyberlog101238/ReelCount/releases) page.
   * Open via your device's native **Files / Downloads** app and tap install.
2. **Enable Accessibility:**
   * Go to **Settings** $\rightarrow$ **Accessibility** $\rightarrow$ **Downloaded Apps** $\rightarrow$ **ReelCount** $\rightarrow$ Turn **ON**.
   * *If Android 13/14 blocks with "Restricted Setting":* Go to **Settings** $\rightarrow$ **Apps** $\rightarrow$ **ReelCount** $\rightarrow$ tap top-right 3 dots $\rightarrow$ **Allow restricted settings**.
3. **Enable Floating Counter:**
   * In ReelCount, go to **Goals** $\rightarrow$ enable **Floating Live Counter** $\rightarrow$ grant **Display over other apps**.

---

## 🤝 Contributing
Contributions are warmly welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines on adding new platform detectors or filing issues.

---

## 📄 License
This project is open-source under the [MIT License](LICENSE).
Copyright (c) 2026 SM Ali Ahsan Al Jabir.
