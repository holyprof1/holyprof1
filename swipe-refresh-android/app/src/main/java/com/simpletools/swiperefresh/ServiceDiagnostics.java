package com.simpletools.swiperefresh;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** Local diagnostics for this application only. No network or screen content. */
final class ServiceDiagnostics {
    private static final String FILE = "service_diagnostics";
    private ServiceDiagnostics() {}
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
    static void event(Context c, String event) {
        prefs(c).edit().putString("event", event)
            .putLong("event_time", System.currentTimeMillis()).apply();
    }
    static void failure(Context c, String operation, RuntimeException error) {
        prefs(c).edit().putString("error", operation + ": " + error.getClass().getSimpleName())
            .putLong("error_time", System.currentTimeMillis()).apply();
    }
    private static String date(long t) {
        return t <= 0 ? "Not recorded" : DateFormat.getDateTimeInstance().format(new Date(t));
    }
    static String report(Context c, boolean permission, boolean connected) {
        SharedPreferences p = prefs(c);
        StringBuilder b = new StringBuilder();
        b.append("Swipe Refresh 1.6\nAndroid API: ").append(Build.VERSION.SDK_INT)
            .append("\nAccessibility permission: ").append(permission ? "Enabled" : "Off")
            .append("\nService connected: ").append(connected ? "Yes" : "No")
            .append("\nLast service event: ").append(p.getString("event", "None"))
            .append("\nEvent time: ").append(date(p.getLong("event_time", 0)))
            .append("\nLast handled error: ").append(p.getString("error", "None"))
            .append("\nError time: ").append(date(p.getLong("error_time", 0)));
        PowerManager pm = (PowerManager)c.getSystemService(Context.POWER_SERVICE);
        if (pm != null) b.append("\nBattery optimization exempt: ")
            .append(pm.isIgnoringBatteryOptimizations(c.getPackageName()) ? "Yes" : "No");
        if (Build.VERSION.SDK_INT >= 30) appendExitInfo(c, b);
        else b.append("\nProcess exit history requires Android 11 or newer.");
        return b.toString();
    }
    @android.annotation.TargetApi(30)
    private static void appendExitInfo(Context c, StringBuilder b) {
        try {
            ActivityManager am = (ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return;
            List<ApplicationExitInfo> exits = am.getHistoricalProcessExitReasons(c.getPackageName(), 0, 3);
            if (exits.isEmpty()) b.append("\nAndroid process exit history: None recorded");
            for (ApplicationExitInfo exit : exits) {
                String reason;
                switch (exit.getReason()) {
                    case ApplicationExitInfo.REASON_CRASH: reason = "Java crash"; break;
                    case ApplicationExitInfo.REASON_CRASH_NATIVE: reason = "Native crash"; break;
                    case ApplicationExitInfo.REASON_ANR: reason = "Not responding (ANR)"; break;
                    case ApplicationExitInfo.REASON_LOW_MEMORY: reason = "Low memory"; break;
                    case ApplicationExitInfo.REASON_USER_REQUESTED: reason = "User-requested stop"; break;
                    case ApplicationExitInfo.REASON_SIGNALED: reason = "Process killed by signal"; break;
                    case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE: reason = "Excessive resource use"; break;
                    case ApplicationExitInfo.REASON_PERMISSION_CHANGE: reason = "Permission changed"; break;
                    default: reason = "Android reason " + exit.getReason();
                }
                b.append("\nProcess exit: ").append(reason).append(" — ").append(date(exit.getTimestamp()));
            }
        } catch (RuntimeException ex) {
            b.append("\nProcess exit history unavailable: ").append(ex.getClass().getSimpleName());
        }
    }
}
