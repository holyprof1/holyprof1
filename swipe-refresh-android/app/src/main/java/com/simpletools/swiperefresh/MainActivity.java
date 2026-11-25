package com.simpletools.swiperefresh;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(35, 83, 212);
    private static final int NAVY = Color.rgb(22, 37, 69);
    private static final int MUTED = Color.rgb(99, 112, 137);
    private static final int BACKGROUND = Color.rgb(245, 248, 255);
    private static final int LIGHTBLUE = Color.rgb(231, 238, 255);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status, countdown, lastSwipe, permissionHint;
    private Button action, showTimerButton;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ScrollView screen = new ScrollView(this);
        screen.setFillViewport(true);
        screen.setBackgroundColor(BACKGROUND);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(34), dp(22), dp(24));
        screen.addView(page);

        LinearLayout logo = new LinearLayout(this);
        logo.setOrientation(LinearLayout.HORIZONTAL);
        logo.setGravity(Gravity.CENTER_VERTICAL);
        TextView glyph = text("↓", 30, Color.WHITE, true);
        glyph.setGravity(Gravity.CENTER);
        glyph.setBackground(rounded(BLUE, 16, 0));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(dp(58), dp(58));
        gp.rightMargin = dp(13);
        logo.addView(glyph, gp);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.addView(text("Swipe Refresh", 27, NAVY, true));
        TextView sub = text("Simple swipe timer", 14, MUTED, false);
        sub.setPadding(0, dp(4), 0, 0);
        header.addView(sub);
        logo.addView(header);
        page.addView(logo);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(24), dp(20), dp(23));
        card.setBackground(rounded(Color.WHITE, 24, 0));
        card.setElevation(dp(3));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(32);
        page.addView(card, cardParams);

        TextView choose = text("VARIABLE INTERVAL", 12, MUTED, true);
        choose.setLetterSpacing(.08f);
        card.addView(choose);
        TextView timing = text("6–8 minutes", 28, BLUE, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, dp(66));
        tp.topMargin = dp(8);
        card.addView(timing, tp);
        TextView timingHint = text("Each swipe gets a new interval.", 13, MUTED, false);
        card.addView(timingHint);

        TextView swipePath = text("SWIPE MOVEMENT", 12, MUTED, true);
        swipePath.setLetterSpacing(.08f);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(26);
        card.addView(swipePath, sp);
        TextView route = text("20%   ↓   50%", 27, BLUE, true);
        route.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(77));
        rp.topMargin = dp(10);
        card.addView(route, rp);

        action = new Button(this);
        action.setAllCaps(false);
        action.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        action.setTextColor(Color.WHITE);
        action.setMinHeight(0);
        action.setMinimumHeight(0);
        action.setPadding(dp(6), 0, dp(6), 0);
        action.setStateListAnimator(null);
        action.setBackground(rounded(BLUE, 16, 0));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(70));
        ap.topMargin = dp(12);
        card.addView(action, ap);
        action.setOnClickListener(v -> onAction());

        TextView under = text("Starts in 10 seconds. Open any app yourself.", 13, MUTED, false);
        under.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(-1, -2);
        up.topMargin = dp(16);
        card.addView(under, up);
        showTimerButton = choiceButton("Show floating timer");
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, dp(48));
        hp.topMargin = dp(12);
        card.addView(showTimerButton, hp);
        showTimerButton.setOnClickListener(v -> {
            SwipePreferences.get(this).edit().putBoolean(SwipePreferences.KEY_OVERLAY_HIDDEN, false).apply();
            SwipeAccessibilityService.syncOverlay();
            refreshStatus();
        });

        LinearLayout report = new LinearLayout(this);
        report.setOrientation(LinearLayout.VERTICAL);
        report.setPadding(dp(18), dp(18), dp(18), dp(18));
        report.setBackground(rounded(LIGHTBLUE, 18, 0));
        LinearLayout.LayoutParams rep = new LinearLayout.LayoutParams(-1, -2);
        rep.topMargin = dp(19);
        page.addView(report, rep);
        status = text("● Ready", 17, NAVY, true);
        report.addView(status);
        countdown = text("Not started", 14, NAVY, false);
        countdown.setPadding(0, dp(7), 0, 0);
        report.addView(countdown);
        lastSwipe = text("Last swipe: —", 13, MUTED, false);
        lastSwipe.setPadding(0, dp(7), 0, 0);
        report.addView(lastSwipe);
        permissionHint = text("Accessibility is off. Tap START to open Android Settings.", 13, Color.rgb(148, 71, 36), true);
        permissionHint.setPadding(0, dp(15), 0, 0);
        page.addView(permissionHint);

        Button batterySettings = choiceButton("Battery settings");
        batterySettings.setTextColor(BLUE);
        batterySettings.setBackground(rounded(Color.WHITE, 13, Color.rgb(224, 230, 242)));
        LinearLayout.LayoutParams batteryParams = new LinearLayout.LayoutParams(-1, dp(49));
        batteryParams.topMargin = dp(13);
        page.addView(batterySettings, batteryParams);
        batterySettings.setOnClickListener(v -> {
            Toast.makeText(this, "If available, set Swipe Refresh battery use to Unrestricted.", Toast.LENGTH_LONG).show();
            Intent appInfo = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
            startActivity(appInfo);
        });

        Button diagnostics = choiceButton("Service diagnostics");
        diagnostics.setTextColor(BLUE);
        diagnostics.setBackground(rounded(Color.WHITE, 13, Color.rgb(224, 230, 242)));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, dp(49));
        dp.topMargin = dp(8);
        page.addView(diagnostics, dp);
        diagnostics.setOnClickListener(v -> {
            String reportText = ServiceDiagnostics.report(this, isAccessibilityEnabled(),
                SwipeAccessibilityService.isReady());
            new AlertDialog.Builder(this).setTitle("Service diagnostics")
                .setMessage(reportText)
                .setPositiveButton("Copy", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Swipe Refresh diagnostics", reportText));
                }).setNegativeButton("Close", null).show();
        });

        TextView foot = text("Swipes anywhere on the unlocked screen — including other apps. Keep battery use Unrestricted if possible. Android may still turn Accessibility off; it cannot be restored silently. No swipes when screen is locked or off. Only use where permitted.", 12, MUTED, false);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = dp(22);
        page.addView(foot, fp);
        setContentView(screen);
        refreshStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(tick);
        if (status != null) handler.post(tick);
    }

    @Override protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    private void onAction() {
        if (SwipePreferences.running(this)) {
            SwipePreferences.get(this).edit().putBoolean(SwipePreferences.KEY_ENABLED, false).apply();
            SwipeAccessibilityService.stopByUser();
            refreshStatus();
            return;
        }
        if (!isAccessibilityEnabled() || !SwipeAccessibilityService.isReady()) {
            Toast.makeText(this, "Enable Swipe Refresh Accessibility once", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        SwipePreferences.get(this).edit()
            .putBoolean(SwipePreferences.KEY_ENABLED, true)
            .putBoolean(SwipePreferences.KEY_OVERLAY_HIDDEN, false)
            .putString(SwipePreferences.KEY_LAST_RESULT, "First swipe in 10 seconds")
            .apply();
        if (!SwipeAccessibilityService.startByUser()) {
            SwipePreferences.get(this).edit().putBoolean(SwipePreferences.KEY_ENABLED, false).apply();
            Toast.makeText(this, "Accessibility not ready", Toast.LENGTH_LONG).show();
            refreshStatus();
            return;
        }
        refreshStatus();
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
    }

    private void refreshStatus() {
        if (status == null) return;
        boolean enabled = isAccessibilityEnabled();
        boolean running = SwipePreferences.running(this);
        action.setText(running ? "■  STOP" : "▶  START");
        action.setBackground(rounded(running ? NAVY : BLUE, 16, 0));
        status.setText(!enabled ? "● Accessibility disabled" :
            !SwipeAccessibilityService.isReady() ? "● Accessibility reconnecting" :
            running ? "● Running" : "● Ready to start");
        permissionHint.setVisibility(enabled && SwipeAccessibilityService.isReady() ? View.GONE : View.VISIBLE);
        permissionHint.setText(!enabled ? "Accessibility is off. Enable it in Android Settings." :
            "Android has not connected the service. Open Service diagnostics for details.");
        if (!enabled || !SwipeAccessibilityService.isReady()) {
            permissionHint.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        }
        boolean hidden = SwipePreferences.get(this).getBoolean(SwipePreferences.KEY_OVERLAY_HIDDEN, false);
        showTimerButton.setVisibility(running && hidden ? View.VISIBLE : View.GONE);
        long nextAt = SwipePreferences.get(this).getLong(SwipePreferences.KEY_NEXT_DUE_MS, 0L);
        if (running && nextAt > 0L) {
            long remaining = Math.max(0L, (nextAt - System.currentTimeMillis() + 999L) / 1000L);
            countdown.setText(!SwipeAccessibilityService.isReady() ?
                "Waiting for Android to reconnect the service" :
                String.format(Locale.getDefault(),
                    "Next swipe in %02d:%02d  ·  Variable 6–8 min",
                    remaining / 60L, remaining % 60L));
        } else {
            countdown.setText(running ? "Preparing first swipe…" : "Tap START to begin variable 6–8 min swipes");
        }
        long last = SwipePreferences.get(this).getLong(SwipePreferences.KEY_LAST_SUCCESS, 0L);
        String result = SwipePreferences.get(this).getString(SwipePreferences.KEY_LAST_RESULT, "");
        lastSwipe.setText(last > 0L ?
            "Last completed: " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(last)) +
            (result.isEmpty() || result.equals("Swipe completed") ? "" : "  ·  " + result) :
            (result.isEmpty() ? "Last swipe: none yet" : result));
    }

    private boolean isAccessibilityEnabled() {
        ComponentName wanted = new ComponentName(this, SwipeAccessibilityService.class);
        String services = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(services)) return false;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(services);
        for (String service : splitter) {
            ComponentName candidate = ComponentName.unflattenFromString(service);
            if (wanted.equals(candidate)) return true;
        }
        return false;
    }

    private Button choiceButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setStateListAnimator(null);
        return b;
    }
    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private GradientDrawable rounded(int fill, int radius, int borderColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (borderColor != 0) d.setStroke(dp(1), borderColor);
        return d;
    }
    private int dp(int value) {
        return (int)(getResources().getDisplayMetrics().density * value + .5f);
    }
}
