# Swipe Refresh v1.3 (Android)

A simple gesture timer **not connected to any specific application**. The app never opens, names, or identifies other applications and requests no internet permission.

### How it works
- Select 5 or 7 minutes, tap **START**.
- It leaves the app and returns to Android Home. Open whichever app you want yourself.
- First gesture attempts after 5 seconds; next gestures every chosen interval.
- A small floating pill near the top-right shows the countdown. Tap the countdown to open Swipe Refresh, or tap × to hide the floating pill without stopping the swipes. Open Swipe Refresh again to reveal the floating timer.
- **STOP** in Swipe Refresh stops the gestures.
- Gesture uses the center of the screen from 20% to 50% height over 550 ms.
- Works only while unlocked with display on. If a phone restarts or disables Accessibility, automated swipes can pause or stop. The app cannot turn Accessibility on by itself.

### Permissions and privacy
Android still requires one-time Accessibility authorization to dispatch gestures and draw an accessibility overlay. The Accessibility configuration explicitly sets canRetrieveWindowContent=false. The code never reads contents, messages, account details, apps currently in use, or any other app's package name. It has no Internet permission or analytics. A system Accessibility warning can still appear; review permissions carefully.

### Safety
Because it does not inspect foreground apps, **it can swipe in any app**, including sensitive screens. It may interfere with typing, calls, games, banking, or other activity; use STOP when you don't want swipes. The Android gesture callback confirms only that a gesture was completed, not that a page refreshed. Other apps may have restrictions on automation, and no detection or account-safety guarantees are offered. It does not attempt to hide automation.

### Build and install
Build uses SDK 35, Gradle 8.9 and Java 17, with a GitHub Actions workflow on the swipe-refresh-android branch. Sideloading may prompt security warnings. Do not disable Play Protect to get around a block. Debug APK signing keys can change; if an update fails to install, a reinstall may be required. Physical device behaviour has not been verified.
