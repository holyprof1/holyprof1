package com.simpletools.swiperefresh;
import android.content.Context;
import android.content.SharedPreferences;

final class SwipePreferences {
    static final String KEY_ENABLED = "running";
    static final String KEY_INTERVAL = "interval_minutes";
    static final String KEY_LAST_SUCCESS = "last_swipe_ms";
    private SwipePreferences() {}
    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("swipe_options", Context.MODE_PRIVATE);
    }
    static int interval(Context c) { return get(c).getInt(KEY_INTERVAL, 7) == 5 ? 5 : 7; }
    static boolean running(Context c) { return get(c).getBoolean(KEY_ENABLED, true); }
}
