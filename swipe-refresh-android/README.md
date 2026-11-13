# Swipe Refresh — Android app

One-time opt-in Android Accessibility gesture timer. Automatically schedules a 550ms downward swipe at the center of the screen from 20% to 50% every 7 minutes by default, or every 5 minutes when selected. No Start button needed.

**Safety limitations:** Works only while the genuine Fiverr Android app is visible, screen is on, and phone is unlocked. The app does not access network or store credentials. It does not claim Fiverr cannot detect automation or that use is safe from account penalties. Fiverr policies warn against manipulated activity/artificial traffic.

**Android security:** Users must manually grant Accessibility in Android Settings after installation. No ordinary app can secretly grant or re-enable that access. If Android keeps the permission after restarting, this system-bound service automatically reschedules. Android device makers may revoke or suspend Accessibility for power-management reasons. There is no universal always-on guarantee.

**Build:** Android SDK 35, Java 17, Gradle 8.9. In Android Studio build APK, or use the included GitHub Actions workflow when this project is put at the root of a new repository with main branch. This code has not been compiled or tested on a phone in this environment.

**Installation:** Open downloaded APK on Android; open app; go to Accessibility Settings and enable Swipe Refresh gesture service; return to choose 5/7 minutes if desired; open Fiverr and keep it in the foreground. A manual pause/resume button is provided. If Android 13+ blocks sideloaded Accessibility services, you may need to use App info → Allow restricted settings; enable only after checking and trusting the code.

**Important:** A swipe doesn't necessarily refresh the page or change Fiverr online status.
