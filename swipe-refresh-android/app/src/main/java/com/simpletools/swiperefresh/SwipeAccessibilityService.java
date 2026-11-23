package com.simpletools.swiperefresh;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.Locale;

/** User-authorized gesture timer; intentionally does not inspect or retrieve window contents. */
public class SwipeAccessibilityService extends AccessibilityService {
    private static WeakReference<SwipeAccessibilityService> live = new WeakReference<>(null);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View overlay;
    private TextView timerLabel;

    private final Runnable countdownTick = new Runnable() {
        @Override public void run() {
            updateOverlayText();
            if (overlay != null) handler.postDelayed(this, 1000L);
        }
    };

    private final Runnable periodicSwipe = new Runnable() {
        @Override public void run() {
            if (!SwipePreferences.running(SwipeAccessibilityService.this)) return;
            if (screenIsUsable()) dispatchDownwardSwipe();
            else SwipePreferences.get(SwipeAccessibilityService.this).edit()
                .putString(SwipePreferences.KEY_LAST_RESULT, "Skipped — screen is locked or off").apply();
            schedule(SwipePreferences.interval(SwipeAccessibilityService.this) * 60000L);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        live = new WeakReference<>(this);
        if (SwipePreferences.running(this)) {
            long remain = SwipePreferences.get(this).getLong(SwipePreferences.KEY_NEXT_DUE_MS, 0L)
                - System.currentTimeMillis();
            long maximum = SwipePreferences.interval(this) * 60000L;
            schedule(remain > 1500L && remain <= maximum ? remain : 5000L);
        } else {
            updateOverlayVisibility();
        }
    }

    static boolean isReady() { return live.get() != null; }

    static boolean startByUser() {
        SwipeAccessibilityService service = live.get();
        if (service == null) return false;
        service.handler.post(() -> service.schedule(5000L));
        return true;
    }

    static void stopByUser() {
        SwipeAccessibilityService service = live.get();
        if (service != null) service.handler.post(service::cancel);
    }

    static void intervalChanged() {
        SwipeAccessibilityService service = live.get();
        if (service != null && SwipePreferences.running(service))
            service.handler.post(() -> service.schedule(SwipePreferences.interval(service) * 60000L));
    }

    static void syncOverlay() {
        SwipeAccessibilityService service = live.get();
        if (service != null) service.handler.post(service::updateOverlayVisibility);
    }

    private void cancel() {
        handler.removeCallbacks(periodicSwipe);
        SwipePreferences.get(this).edit().remove(SwipePreferences.KEY_NEXT_DUE_MS).apply();
        updateOverlayVisibility();
    }

    private void schedule(long delayMs) {
        handler.removeCallbacks(periodicSwipe);
        if (!SwipePreferences.running(this)) { cancel(); return; }
        long delay = Math.max(1500L, delayMs);
        SwipePreferences.get(this).edit()
            .putLong(SwipePreferences.KEY_NEXT_DUE_MS, System.currentTimeMillis() + delay)
            .apply();
        handler.postDelayed(periodicSwipe, delay);
        updateOverlayVisibility();
    }

    private boolean screenIsUsable() {
        PowerManager p = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (p == null || !p.isInteractive()) return false;
        KeyguardManager k = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        return k == null || !k.isKeyguardLocked();
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
            @Override public void onCompleted(GestureDescription d) {
                SwipePreferences.get(SwipeAccessibilityService.this).edit()
                    .putLong(SwipePreferences.KEY_LAST_SUCCESS, System.currentTimeMillis())
                    .putString(SwipePreferences.KEY_LAST_RESULT, "Swipe completed by Android").apply();
            }
            @Override public void onCancelled(GestureDescription d) {
                SwipePreferences.get(SwipeAccessibilityService.this).edit()
                    .putString(SwipePreferences.KEY_LAST_RESULT, "Gesture cancelled").apply();
            }
        }, handler);
        if (!accepted) SwipePreferences.get(this).edit()
            .putString(SwipePreferences.KEY_LAST_RESULT, "Android rejected gesture").apply();
    }

    private void updateOverlayVisibility() {
        if (SwipePreferences.running(this) &&
            !SwipePreferences.get(this).getBoolean(SwipePreferences.KEY_OVERLAY_HIDDEN, false)) {
            if (overlay == null) createOverlay();
            updateOverlayText();
        } else removeOverlay();
    }

    private int dp(int n) { return (int)(getResources().getDisplayMetrics().density*n+.5f); }

    private void createOverlay() {
        windowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) return;
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(12), dp(6), dp(7), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(28,49,100));
        bg.setCornerRadius(dp(20));
        pill.setBackground(bg);
        pill.setElevation(dp(5));

        timerLabel = new TextView(this);
        timerLabel.setTextColor(Color.WHITE);
        timerLabel.setTextSize(13);
        timerLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        timerLabel.setText("↓ 00:05");
        pill.addView(timerLabel);
        timerLabel.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
        TextView hide = new TextView(this);
        hide.setText("  ×");
        hide.setTextSize(21);
        hide.setTextColor(Color.WHITE);
        hide.setPadding(dp(8), dp(2), dp(3), dp(2));
        pill.addView(hide);
        hide.setOnClickListener(v -> {
            SwipePreferences.get(this).edit().putBoolean(SwipePreferences.KEY_OVERLAY_HIDDEN, true).apply();
            removeOverlay();
        });

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        );
        lp.gravity = Gravity.TOP | Gravity.RIGHT;
        lp.x = dp(10);
        lp.y = dp(58);
        try {
            windowManager.addView(pill, lp);
            overlay = pill;
            handler.removeCallbacks(countdownTick);
            handler.post(countdownTick);
        } catch (RuntimeException e) {
            overlay = null;
            timerLabel = null;
            SwipePreferences.get(this).edit()
                .putString(SwipePreferences.KEY_LAST_RESULT, "Floating timer unavailable").apply();
        }
    }

    private void updateOverlayText() {
        if (overlay == null || timerLabel == null) return;
        long when = SwipePreferences.get(this).getLong(SwipePreferences.KEY_NEXT_DUE_MS, 0L);
        long seconds = Math.max(0, (when - System.currentTimeMillis() + 999L) / 1000L);
        timerLabel.setText(String.format(Locale.getDefault(), "↓ %02d:%02d", seconds/60L, seconds%60L));
    }

    private void removeOverlay() {
        handler.removeCallbacks(countdownTick);
        if (overlay != null && windowManager != null) {
            try { windowManager.removeView(overlay); } catch (RuntimeException ignored) { }
        }
        overlay = null;
        timerLabel = null;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }

    @Override public boolean onUnbind(Intent intent) {
        handler.removeCallbacks(periodicSwipe);
        removeOverlay();
        live = new WeakReference<>(null);
        return super.onUnbind(intent);
    }
    @Override public void onDestroy() {
        handler.removeCallbacks(periodicSwipe);
        removeOverlay();
        live = new WeakReference<>(null);
        super.onDestroy();
    }
}
