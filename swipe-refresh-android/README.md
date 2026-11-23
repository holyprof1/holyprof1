# Swipe Refresh v1.4 — Android

A simple user-authorized gesture timer, not connected to any specific app. It does not read window contents, active app names, or account data. It requires no internet permission.

Tap **START**. The app returns to Android Home; open whichever app you want yourself. The first swipe is attempted after **10 seconds**. After that, each new interval is chosen independently between **6 minutes 0 seconds and 8 minutes 0 seconds**, inclusive. The floating pill shows a countdown, and × hides the pill without stopping the timer. Open Swipe Refresh to view status or tap **STOP**. To redisplay a hidden pill, use the "Show floating timer" control.

Each gesture moves downward along the center of the screen from 20% to 50% of screen height over 550ms. Swipes only happen while Android reports the screen is unlocked and on. They can interfere with actions in other apps because no app detection is performed. The timer uses an Android Accessibility Service with canRetrieveWindowContent=false, and no network or analytics libraries. Android can suspend or revoke accessibility service access; the user must re-enable it manually.

An Accessibility gesture completion indicates a simulated swipe was dispatched and completed, not that any app actually refreshed. Automated activity can violate a site's rules and may be detectable. Variable timing is a convenience feature, **not a guarantee of stealth or account safety**. The app does not open or close other apps, impersonate a user, read screen data, or try to evade platform enforcement.

Build from source with Android SDK 35, Java 17, Gradle 8.9 or the GitHub Actions workflow on branch `swipe-refresh-android`. Sideloading may trigger Play Protect; don't disable security protections. This app is not physically device-tested.
