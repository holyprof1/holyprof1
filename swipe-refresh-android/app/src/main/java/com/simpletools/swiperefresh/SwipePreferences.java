package com.simpletools.swiperefresh;
import android.content.Context;
import android.content.SharedPreferences;

final class SwipePreferences {
    static final String KEY_ENABLED = "running";
    static final String KEY_LAST_SUCCESS = "last_swipe_ms";
    static final String KEY_NEXT_DUE_MS = "next_due_ms";
    static final String KEY_LAST_RESULT = "last_result";
    static final String KEY_OVERLAY_HIDDEN = "overlay_hidden";
    private SwipePreferences() {}
    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("swipe_options", Context.MODE_PRIVATE);
    }
    static boolean running(Context c) { return get(c).getBoolean(KEY_ENABLED, false); }
}
