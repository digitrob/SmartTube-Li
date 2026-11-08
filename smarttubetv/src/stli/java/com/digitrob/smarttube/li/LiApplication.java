package com.digitrob.smarttube.li;

import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData;
import com.liskovsoft.smartyoutubetv2.tv.ui.main.MainApplication;

/**
 * Li Auto (car headunit) application entry point.
 *
 * <p>Upstream {@link MainApplication} registers the TV activities. This subclass runs afterwards
 * and re-registers the touch-first activities when the touch UI is enabled. Nothing in the
 * upstream class needs to change, which keeps upstream merges cheap.</p>
 */
public class LiApplication extends MainApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        LiPrefs prefs = LiPrefs.instance(this);

        if (prefs.isTouchUiEnabled()) {
            applyCarDisplayPolicy();
        }

        LiUiBinder.bind(this);
    }

    /**
     * A headunit must never dim itself or fall asleep while the touch UI is in the foreground.
     *
     * <p>Upstream's screensaver is a TV feature (shows a dim overlay after a timeout). The
     * activities also block it at runtime, but persisting "disabled" here covers the window
     * between process start and the first activity resume.</p>
     */
    private void applyCarDisplayPolicy() {
        GeneralData data = GeneralData.instance(this);

        if (!data.isScreensaverDisabled()) {
            data.setScreensaverDisabled(true);
        }

        if (data.getScreensaverTimeoutMs() != GeneralData.SCREENSAVER_TIMEOUT_NEVER) {
            data.setScreensaverTimeoutMs(GeneralData.SCREENSAVER_TIMEOUT_NEVER);
        }

        // H.264 is the safest hardware-decoded codec on the Snapdragon 8155; upstream
        // otherwise prefers VP9 when the preset matches several 1080p tracks.
        PlayerTweaksData tweaks = PlayerTweaksData.instance(this);

        if (!tweaks.isAvcOverVp9Preferred()) {
            tweaks.setAvcOverVp9Preferred(true);
        }
    }
}
