# Contributing to ReelCount

Thank you for your interest in contributing to **ReelCount**! We welcome bug reports, feature suggestions, and code contributions to help users take back control of their digital wellbeing.

---

## 🛠️ Codebase Structure

The project follows Clean Architecture principles:
- **`app/src/main/java/com/jabir/brot/domain/detector/`**: Contains short-form video heuristics (`InstagramReelsDetector`, `FacebookReelsDetector`, `YouTubeShortsDetector`).
- **`app/src/main/java/com/jabir/brot/domain/guard/`**: Deduplication logic (`DuplicateEventGuard`) ensuring videos aren't double-counted during scroll micro-flings or loops.
- **`app/src/main/java/com/jabir/brot/service/`**: System integrations (`ReelCountAccessibilityService`, `FloatingOverlayController`).
- **`app/src/main/java/com/jabir/brot/ui/`**: Jetpack Compose UI with Material 3 Obsidian & Warm Amber styling.

---

## 🚀 Adding a New Platform Detector (e.g. TikTok, Snapchat Spotlight)

1. Create a new detector implementing `PlatformDetector` in `domain/detector/`:
   ```kotlin
   class TikTokDetector : PlatformDetector {
       override val platform: Platform = Platform.TIKTOK
       override fun evaluate(snapshot: ScreenSnapshot): DetectionEvent? { ... }
   }
   ```
2. Add the platform enum to `Platform.kt` with its package identifier.
3. Register the detector in `VideoTransitionDetector.kt`.
4. Add the package name to `ReelCountAccessibilityService.MONITORED_PACKAGES` and `accessibility_service_config.xml`.

---

## 🧪 Testing Your Changes

Before submitting a Pull Request, run the automated tests:
```bash
./gradlew testDebugUnitTest
```
Ensure all state machine transitions and deduplication conditions pass cleanly.

---

## 📋 Pull Request Guidelines

1. Fork the repo and create a feature branch (`git checkout -b feat/my-feature`).
2. Follow standard Kotlin style guidelines and ensure code compiles with zero lint errors.
3. Commit with a concise Conventional Commit message (`feat: add TikTok detection heuristic`).
4. Push and submit a Pull Request describing your changes and test results.
