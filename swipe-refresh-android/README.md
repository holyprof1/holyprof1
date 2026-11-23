# Swipe Refresh 1.2 — Android

A single-screen on-device swipe timer for a user who has granted Android Accessibility permission.

**Flow:** Choose **5 minutes** or **7 minutes**, then tap **START & OPEN FIVERR**. The app launches Fiverr, attempts one test swipe after 5 seconds, then repeats every selected interval. The same big button becomes **STOP**, stopping the timer. The screen shows an actual countdown, last completed gesture, and skipped/failed result text.

Swipe: vertical center line from 20% to 50% of the screen, duration 550 ms. Gestures only dispatch while Fiverr is the foreground app and the phone is unlocked and screen on.

**Permissions:** Android must be granted Accessibility manually. This app does not require an internet, SMS, or account permission. It uses Accessibility to check the foreground package and dispatch gestures. System or battery restrictions may still suspend the service. No guarantees that Android will allow continuous operation.

**Fiverr:** A generated swipe may not refresh online status. Automated activity may violate Fiverr policies or be detected and restricted. No attempt is made to disguise activity or circumvent controls.

**Build:** Java 17, Android SDK 35, Gradle 8.9. GitHub Actions on the swipe-refresh-android branch compiles a debug APK. If installing over a debug APK built with a different signing key, Android may require uninstalling the old version first (which removes app settings). Do not disable Play Protect to force an install.

**Device testing:** Build success is not proof it works reliably on a specific device. Verify the 5-second test, timer, STOP behavior, Accessibility continuity, and your phone's power management locally.
