package com.digitrob.smarttube.li;

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
        LiUiBinder.bind(this);
    }
}
