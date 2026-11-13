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
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.lang.ref.WeakReference;

/** Android owns this service's lifecycle. Enabling Accessibility is a user action. */
public class SwipeAccessibilityService extends AccessibilityService {
    static final String FIVERR_PACKAGE = "com.fiverr.fiverr";
    private static WeakReference<SwipeAccessibilityService> live = new WeakReference<>(null);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable periodicSwipe = new Runnable() {
        @Override public void run() {
            if (!SwipePreferences.running(SwipeAccessibilityService.this)) return;
            if (shouldSwipe()) dispatchDownwardSwipe();
            handler.postDelayed(this, SwipePreferences.interval(SwipeAccessibilityService.this) * 60000L);
        }
    };
    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        live = new WeakReference<>(this);
        refreshSchedule();
    }
    static void notifySettingsChanged() {
        SwipeAccessibilityService s = live.get();
        if (s != null) s.handler.post(s::refreshSchedule);
    }
    static boolean isBound() { return live.get() != null; }
    static void testAfterFiveSeconds() {
        SwipeAccessibilityService s = live.get();
        if (s != null) s.handler.postDelayed(() -> {
            if (s.shouldSwipe()) s.dispatchDownwardSwipe();
        }, 5000L);
    }
    private void refreshSchedule() {
        handler.removeCallbacks(periodicSwipe);
        if (SwipePreferences.running(this))
            handler.postDelayed(periodicSwipe, SwipePreferences.interval(this) * 60000L);
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
        DisplayMetrics m = getResources().getDisplayMetrics();
        if (m.widthPixels <= 0 || m.heightPixels <= 0) return;
        float x = m.widthPixels / 2f;
        Path p = new Path();
        p.moveTo(x, m.heightPixels * 0.20f);
        p.lineTo(x, m.heightPixels * 0.50f);
        GestureDescription g = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p, 0L, 550L)).build();
        dispatchGesture(g, new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription d) {
                SwipePreferences.get(SwipeAccessibilityService.this).edit()
                   .putLong(SwipePreferences.KEY_LAST_SUCCESS, System.currentTimeMillis()).apply();
            }
        }, handler);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}
    @Override public boolean onUnbind(Intent intent) {
        handler.removeCallbacks(periodicSwipe);
        live = new WeakReference<>(null);
        return super.onUnbind(intent);
    }
    @Override public void onDestroy() {
        handler.removeCallbacks(periodicSwipe);
        live = new WeakReference<>(null);
        super.onDestroy();
    }
}
