package com.simpletools.swiperefresh;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
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

public class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(22, 70, 186);
    private static final int DARK = Color.rgb(21, 35, 62);
    private static final int MUTED = Color.rgb(91, 107, 128);
    private TextView statusText, lastSwipeText, intervalText;
    private Button pauseButton;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(246, 248, 252));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(246, 248, 252));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(22), dp(30), dp(22), dp(30));
        scroll.addView(column);
        column.addView(text("Swipe Refresh", 30, DARK, true));
        TextView subtitle = text("Automatic, on-device swipe timer", 15, MUTED, false);
        subtitle.setPadding(0, dp(6), 0, dp(22));
        column.addView(subtitle);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(20), dp(20), dp(20));
        panel.setBackground(round(Color.WHITE, 20));
        column.addView(panel, new LinearLayout.LayoutParams(-1, -2));
        TextView motion = text("20%  ↓  50%", 28, BLUE, true);
        motion.setGravity(Gravity.CENTER);
        panel.addView(motion);
        TextView desc = text("One downward swipe at the centre of the screen", 14, MUTED, false);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, dp(8), 0, dp(18));
        panel.addView(desc);
        intervalText = text("", 17, DARK, true);
        panel.addView(intervalText);
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, dp(58));
        op.topMargin = dp(12);
        panel.addView(options, op);
        Button five = button("5 minutes", false);
        Button seven = button("7 minutes", false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        lp.rightMargin = dp(8);
        options.addView(five, lp);
        options.addView(seven, new LinearLayout.LayoutParams(0, -1, 1));
        five.setOnClickListener(v -> setMinutes(5));
        seven.setOnClickListener(v -> setMinutes(7));
        statusText = text("", 16, DARK, true);
        statusText.setPadding(0, dp(16), 0, dp(8));
        panel.addView(statusText);
        lastSwipeText = text("", 13, MUTED, false);
        lastSwipeText.setPadding(0, 0, 0, dp(12));
        panel.addView(lastSwipeText);
        pauseButton = button("Pause automatic swipes", true);
        panel.addView(pauseButton, buttonParams());
        pauseButton.setOnClickListener(v -> togglePause());
        Button access = button("Enable Accessibility (one-time)", false);
        LinearLayout.LayoutParams ap = buttonParams();
        ap.topMargin = dp(18);
        column.addView(access, ap);
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        Button open = button("Open Fiverr", false);
        column.addView(open, buttonParams());
        open.setOnClickListener(v -> openFiverr());
        Button test = button("Test one swipe in 5 seconds", false);
        column.addView(test, buttonParams());
        test.setOnClickListener(v -> {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "Enable Accessibility first", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } else if (!SwipeAccessibilityService.isBound()) {
                Toast.makeText(this, "Service is not ready yet", Toast.LENGTH_LONG).show();
            } else {
                SwipeAccessibilityService.testAfterFiveSeconds();
                openFiverr();
            }
        });
        TextView note = text(
            "After one-time Android permission the timer starts automatically. It works only when Fiverr is visible, the phone is unlocked, and its screen is on. Android can still turn off services. No guarantee of Fiverr detection avoidance or account safety.",
            13, MUTED, false);
        note.setPadding(0, dp(20), 0, 0);
        column.addView(note);
        setContentView(scroll);
        refreshStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (statusText != null) refreshStatus();
    }
    private void setMinutes(int minutes) {
        SwipePreferences.get(this).edit().putInt(SwipePreferences.KEY_INTERVAL, minutes).apply();
        SwipeAccessibilityService.notifySettingsChanged();
        refreshStatus();
        Toast.makeText(this, "Updated to every " + minutes + " minutes", Toast.LENGTH_SHORT).show();
    }
    private void togglePause() {
        boolean next = !SwipePreferences.running(this);
        SwipePreferences.get(this).edit().putBoolean(SwipePreferences.KEY_ENABLED, next).apply();
        SwipeAccessibilityService.notifySettingsChanged();
        refreshStatus();
    }
    private boolean isAccessibilityEnabled() {
        ComponentName desired = new ComponentName(this, SwipeAccessibilityService.class);
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        for (String item : splitter) {
            ComponentName candidate = ComponentName.unflattenFromString(item);
            if (candidate != null && candidate.equals(desired)) return true;
        }
        return false;
    }
    private void refreshStatus() {
        boolean access = isAccessibilityEnabled();
        boolean running = SwipePreferences.running(this);
        intervalText.setText("Repeat every " + SwipePreferences.interval(this) + " minutes");
        statusText.setText(!access ? "Accessibility off — one-time setup needed" :
            running ? "Automatic timer on — open Fiverr" : "Automatic timer paused");
        pauseButton.setText(running ? "Pause automatic swipes" : "Resume automatic swipes");
        long last = SwipePreferences.get(this).getLong(SwipePreferences.KEY_LAST_SUCCESS, 0L);
        lastSwipeText.setText(last <= 0 ? "Last swipe: none yet" :
            "Last swipe: " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(last)));
    }
    private void openFiverr() {
        Intent i = getPackageManager().getLaunchIntentForPackage(SwipeAccessibilityService.FIVERR_PACKAGE);
        if (i != null) startActivity(i);
        else startActivity(new Intent(Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=com.fiverr.fiverr")));
    }
    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private Button button(String caption, boolean primary) {
        Button b = new Button(this);
        b.setText(caption);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.WHITE : BLUE);
        b.setBackground(round(primary ? BLUE : Color.WHITE, 14));
        b.setElevation(dp(2));
        return b;
    }
    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        p.bottomMargin = dp(8);
        return p;
    }
    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }
    private int dp(int v) { return (int)(getResources().getDisplayMetrics().density * v + 0.5f); }
}
