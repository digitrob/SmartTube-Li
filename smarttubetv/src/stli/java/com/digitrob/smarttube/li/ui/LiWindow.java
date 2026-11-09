package com.digitrob.smarttube.li.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Fullscreen helpers for the touch UI.
 *
 * <p>A headunit has no status or navigation bars, and the app should use every pixel of the
 * 2880x1620 panel. The same flags also stop emulator display cutouts from insetting the window
 * (the AVD profiles come from phones).</p>
 */
public final class LiWindow {
    private LiWindow() {
    }

    public static void applyImmersive(Activity activity) {
        if (activity == null) {
            return;
        }

        Window window = activity.getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= 30) {
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            window.setAttributes(attrs);
        } else if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attrs);
        }

        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
        }

        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
