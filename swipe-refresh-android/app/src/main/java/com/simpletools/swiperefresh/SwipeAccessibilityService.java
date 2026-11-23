package com.simpletools.swiperefresh;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.lang.ref.WeakReference;

/**
 * Performs a user-authorized swipe only while Fiverr is the foreground app.
 * Does not access the network or capture screen text. Accessibility consent is managed by Android.
 */
public class SwipeAccessibilityService extends AccessibilityService {
    static final String FIVERR_PACKAGE = "com.fiverr.fiverr";
    private static WeakReference<SwipeAccessibilityService> live = new WeakReference<>(null);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable periodicSwipe = new Runnable() {
        @Override public void run() {
            if (!SwipePreferences.running(SwipeAccessibilityService.this)) return;
            if (shouldSwipe()) {
                dispatchDownwardSwipe();
            } else {
                SwipePreferences.get(SwipeAccessibilityService.this).edit()
                    .putString(SwipePreferences.KEY_LAST_RESULT,
                        "Skipped: Fiverr not visible or screen locked").apply();
            }
            schedule(SwipePreferences.interval(SwipeAccessibilityService.this) * 60000L);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        live = new WeakReference<>(this);
        if (SwipePreferences.running(this)) {
            long remaining = SwipePreferences.get(this)
                .getLong(SwipePreferences.KEY_NEXT_DUE_MS, 0L) - System.currentTimeMillis();
            long max = SwipePreferences.interval(this) * 60000L;
            if (remaining < 1500L || remaining > max) remaining = 5000L;
            schedule(remaining);
        }
    }

    static boolean isReady() { return live.get() != null; }

    /** Begin with a visible test swipe after five seconds; then repeat at the selected interval. */
    static boolean startByUser() {
        SwipeAccessibilityService s = live.get();
        if (s == null) return false;
        s.handler.post(() -> s.schedule(5000L));
        return true;
    }

    static void stopByUser() {
        SwipeAccessibilityService s = live.get();
        if (s != null) s.handler.post(s::cancel);
    }

    static void intervalChanged() {
        SwipeAccessibilityService s = live.get();
        if (s != null && SwipePreferences.running(s))
            s.handler.post(() -> s.schedule(SwipePreferences.interval(s) * 60000L));
    }

    private void cancel() {
        handler.removeCallbacks(periodicSwipe);
        SwipePreferences.get(this).edit().remove(SwipePreferences.KEY_NEXT_DUE_MS).apply();
    }

    private void schedule(long delayMs) {
        handler.removeCallbacks(periodicSwipe);
        if (!SwipePreferences.running(this)) { cancel(); return; }
        long delay = Math.max(1500L, delayMs);
        SwipePreferences.get(this).edit()
            .putLong(SwipePreferences.KEY_NEXT_DUE_MS, System.currentTimeMillis() + delay)
            .apply();
        handler.postDelayed(periodicSwipe, delay);
    }

    private boolean shouldSwipe() {
        PowerManager power = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (power == null || !power.isInteractive()) return false;
        KeyguardManager keys = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        if (keys != null && keys.isKeyguardLocked()) return false;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        return root != null && root.getPackageName() != null &&
            FIVERR_PACKAGE.contentEquals(root.getPackageName());
    }

    private void dispatchDownwardSwipe() {
        DisplayMetrics m = new DisplayMetrics();
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return;
        wm.getDefaultDisplay().getRealMetrics(m);
        if (m.widthPixels <= 0 || m.heightPixels <= 0) return;
        float x = m.widthPixels / 2f;
        Path path = new Path();
        path.moveTo(x, m.heightPixels * .20f);
        path.lineTo(x, m.heightPixels * .50f);
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0L, 550L)).build();
        boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription description) {
                SwipePreferences.get(SwipeAccessibilityService.this).edit()
                    .putLong(SwipePreferences.KEY_LAST_SUCCESS, System.currentTimeMillis())
                    .putString(SwipePreferences.KEY_LAST_RESULT, "Swipe completed")
                    .apply();
            }
            @Override public void onCancelled(GestureDescription description) {
                SwipePreferences.get(SwipeAccessibilityService.this).edit()
                    .putString(SwipePreferences.KEY_LAST_RESULT, "Gesture cancelled").apply();
            }
        }, handler);
        if (!accepted) {
            SwipePreferences.get(this).edit()
                .putString(SwipePreferences.KEY_LAST_RESULT, "Gesture rejected by Android").apply();
        }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    @Override public boolean onUnbind(Intent intent) {
        cancel();
        live = new WeakReference<>(null);
        return super.onUnbind(intent);
    }
    @Override public void onDestroy() {
        cancel();
        live = new WeakReference<>(null);
        super.onDestroy();
    }
}
