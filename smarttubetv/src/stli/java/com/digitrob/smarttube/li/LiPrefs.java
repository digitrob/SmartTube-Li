package com.digitrob.smarttube.li;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

/**
 * Small preference store owned by the Li edition. Kept separate from upstream prefs so that
 * upstream refactors never break it (and upstream never sees our keys).
 */
public final class LiPrefs {
    /**
     * Dev/automation override, readable without permissions:
     * <pre>
     *   adb shell settings put global smarttube_touch_ui 1   # force touch UI
     *   adb shell settings put global smarttube_touch_ui 0   # force TV UI
     *   adb shell settings delete global smarttube_touch_ui  # use stored preference
     * </pre>
     */
    public static final String GLOBAL_TOUCH_UI = "smarttube_touch_ui";

    private static final String FILE = "li_prefs";
    private static final String KEY_TOUCH_UI = "touch_ui";
    private static final String KEY_RAIL_COLLAPSED = "rail_collapsed";
    private static final String KEY_RAIL_COLLAPSED_SET = "rail_collapsed_set";
    private static final String KEY_PLAYER_FORMAT_INITIALIZED = "player_format_initialized";

    private static LiPrefs sInstance;

    private final Context mContext;
    private final SharedPreferences mPrefs;

    private LiPrefs(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = mContext.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static synchronized LiPrefs instance(Context context) {
        if (sInstance == null) {
            sInstance = new LiPrefs(context);
        }
        return sInstance;
    }

    public boolean isTouchUiEnabled() {
        try {
            String override = Settings.Global.getString(mContext.getContentResolver(), GLOBAL_TOUCH_UI);
            if ("1".equals(override)) {
                return true;
            }
            if ("0".equals(override)) {
                return false;
            }
        } catch (Throwable ignore) {
            // Some ROMs restrict Settings.Global reads; fall back to the stored value.
        }

        return mPrefs.getBoolean(KEY_TOUCH_UI, false);
    }

    public void setTouchUiEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_TOUCH_UI, enabled).apply();
    }

    public boolean isRailCollapsed() {
        return mPrefs.getBoolean(KEY_RAIL_COLLAPSED, false);
    }

    public void setRailCollapsed(boolean collapsed) {
        mPrefs.edit()
                .putBoolean(KEY_RAIL_COLLAPSED, collapsed)
                .putBoolean(KEY_RAIL_COLLAPSED_SET, true)
                .apply();
    }

    /** True once the user has explicitly toggled the rail. */
    public boolean hasRailCollapsedPreference() {
        return mPrefs.getBoolean(KEY_RAIL_COLLAPSED_SET, false);
    }

    /**
     * Upstream's {@code PlayerData.getFormat()} never returns null (it falls back to the device
     * default, which is 4K VP9 on capable devices). This flag marks that we have applied the
     * car default (1080p AVC) once, so later user choices are never overwritten.
     */
    public boolean isPlayerFormatInitialized() {
        return mPrefs.getBoolean(KEY_PLAYER_FORMAT_INITIALIZED, false);
    }

    public void setPlayerFormatInitialized(boolean initialized) {
        mPrefs.edit().putBoolean(KEY_PLAYER_FORMAT_INITIALIZED, initialized).apply();
    }
}
