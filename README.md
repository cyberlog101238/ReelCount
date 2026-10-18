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

## 🛡️ Note on Google Play Protect (Statement & Appeal)

> [!IMPORTANT]
> **Developer Statement Regarding Play Protect Warning:**
> When sideloading ReelCount (especially if downloaded or shared via chat apps like WhatsApp), Google Play Protect may display a prompt stating:  
> *"App blocked to protect your device — This app can request access to sensitive data. This can increase the risk of identity theft or financial fraud."*
>
> **Why this happens:**
> * To automatically count short-form videos on Instagram, Facebook, and YouTube, ReelCount must declare an Android `AccessibilityService`.
> * Google Play Protect has an automated **"Enhanced Fraud Protection"** rule for internet-sideloaded APKs. Because malicious banking trojans frequently abuse accessibility permissions, Google automatically flags any sideloaded app requesting accessibility services as a potential risk.
>
> **Privacy Guarantee:**
> * **100% Offline (Zero Network Permissions):** ReelCount has **NO `INTERNET` permission**. It cannot send, receive, or upload any data outside your device.
> * **Zero Personal Data Collection:** The app never inspects passwords, private chats, or banking apps. The accessibility service is strictly scoped to Instagram, Facebook, and YouTube.
>
> **Official Appeal Submitted:**
> An official appeal has been submitted to Google via the [Google Play Protect Developer Appeal Form](https://support.google.com/googleplay/android-developer/contact/protectappeals) requesting formal whitelisting of the package `com.jabir.brot`.
>
> **How to install while the appeal is pending:**
> 1. **Option A (Recommended):** Open your phone's built-in **Files / File Manager** app (e.g. *Files by Google*, *My Files*, or *Xiaomi File Manager*), navigate to **Downloads**, and tap `ReelCount.apk` directly (installing from local storage instead of inside WhatsApp allows standard installation).
> 2. **Option B:** Open **Google Play Store** $\rightarrow$ tap Profile avatar $\rightarrow$ **Play Protect** $\rightarrow$ tap ⚙️ (Settings) $\rightarrow$ temporarily turn off **"Scan apps with Play Protect"** $\rightarrow$ install the app $\rightarrow$ turn Play Protect back on.

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
