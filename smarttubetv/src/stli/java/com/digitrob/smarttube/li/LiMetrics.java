package com.digitrob.smarttube.li;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;

/**
 * Density helper for the touch UI.
 *
 * <p>Upstream lays the TV UI out on a virtual 1920 px @ 2.0 canvas scaled by the user's UI scale.
 * For touch we want a tablet-like canvas: on the 2880x1620 headunit screen, uiScale 0.75 yields
 * 1280x720 dp (2.25 px/dp). The computation adapts to wider screens (e.g. the 2025 29" 6K car),
 * so the same APK keeps roughly the same number of columns.</p>
 */
public final class LiMetrics {
    /** 0.75 puts 1280x720 dp on a 2880x1620 screen. */
    public static final float UI_SCALE = 0.75f;

    private static final float UPSTREAM_CANVAS_WIDTH_PX = 1920f;
    private static final float UPSTREAM_CANVAS_DENSITY = 2.0f;

    private LiMetrics() {
    }

    public static void applyCarDensity(Activity activity) {
        applyCarDensity(activity, UI_SCALE);
    }

    public static void applyCarDensity(Activity activity, float uiScale) {
        if (activity == null) {
            return;
        }

        int widthPx;
        try {
            DisplayMetrics real = new DisplayMetrics();
            Display display = activity.getWindowManager().getDefaultDisplay();
            display.getRealMetrics(real);
            widthPx = Math.max(real.widthPixels, real.heightPixels);
        } catch (Throwable t) {
            widthPx = activity.getResources().getDisplayMetrics().widthPixels;
        }

        if (widthPx <= 0) {
            return;
        }

        float density = UPSTREAM_CANVAS_DENSITY * (widthPx / UPSTREAM_CANVAS_WIDTH_PX) * uiScale;
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        dm.density = density;
        dm.scaledDensity = density;
        dm.densityDpi = Math.round(density * 160f);
    }
}
