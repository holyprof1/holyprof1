package com.simpletools.swiperefresh;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    private Button five, seven, action, showTimerButton;
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

        TextView choose = text("CHOOSE INTERVAL", 12, MUTED, true);
        choose.setLetterSpacing(.08f);
        card.addView(choose);
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(78));
        cp.topMargin = dp(13);
        card.addView(choices, cp);
        five = choiceButton("5 minutes");
        seven = choiceButton("7 minutes");
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, -1, 1);
        wp.rightMargin = dp(10);
        choices.addView(five, wp);
        choices.addView(seven, new LinearLayout.LayoutParams(0, -1, 1));
        five.setOnClickListener(v -> setMinutes(5));
        seven.setOnClickListener(v -> setMinutes(7));

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

        TextView under = text("Starts in 5 seconds. Open any app yourself.", 13, MUTED, false);
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

        TextView foot = text("Swipes anywhere on the unlocked screen — including other apps. Floating timer can be hidden. Android may stop Accessibility; this app cannot re-enable it by itself. Only use where permitted.", 12, MUTED, false);
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

    private void setMinutes(int minutes) {
        SwipePreferences.get(this).edit()
            .putInt(SwipePreferences.KEY_INTERVAL, minutes).apply();
        SwipeAccessibilityService.intervalChanged();
        refreshStatus();
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
            .putString(SwipePreferences.KEY_LAST_RESULT, "First swipe in 5 seconds")
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
        int interval = SwipePreferences.interval(this);
        selected(five, interval == 5);
        selected(seven, interval == 7);
        action.setText(running ? "■  STOP" : "▶  START");
        action.setBackground(rounded(running ? NAVY : BLUE, 16, 0));
        status.setText(!enabled ? "● Accessibility disabled" : running ? "● Running" : "● Ready to start");
        permissionHint.setVisibility(enabled ? View.GONE : View.VISIBLE);
        boolean hidden = SwipePreferences.get(this).getBoolean(SwipePreferences.KEY_OVERLAY_HIDDEN, false);
        showTimerButton.setVisibility(running && hidden ? View.VISIBLE : View.GONE);
        long nextAt = SwipePreferences.get(this).getLong(SwipePreferences.KEY_NEXT_DUE_MS, 0L);
        if (running && nextAt > 0L) {
            long remaining = Math.max(0L, (nextAt - System.currentTimeMillis() + 999L) / 1000L);
            countdown.setText(String.format(Locale.getDefault(),
                "Next swipe in %02d:%02d  ·  Every %d min",
                remaining / 60L, remaining % 60L, interval));
        } else {
            countdown.setText(running ? "Preparing first swipe…" : "Choose 5 or 7 minutes, then tap START");
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

    private void selected(Button b, boolean chosen) {
        b.setTextColor(chosen ? Color.WHITE : NAVY);
        b.setBackground(rounded(chosen ? BLUE : BACKGROUND, 13, chosen ? 0 : Color.rgb(224, 230, 242)));
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
