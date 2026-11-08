package com.digitrob.smarttube.li;

import android.content.Context;

import com.digitrob.smarttube.li.ui.TouchBrowseActivity;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;

/**
 * Re-registers view interfaces with touch-first activities.
 *
 * <p>Called after upstream {@code MainApplication} has registered the TV activities, so this
 * simply overwrites the mappings we care about. Views we do not override keep the TV
 * implementation, which is the intended fallback while the touch UI is being built out.</p>
 */
public final class LiUiBinder {
    private LiUiBinder() {
    }

    public static void bind(Context context) {
        if (context == null || !LiPrefs.instance(context).isTouchUiEnabled()) {
            return;
        }

        ViewManager viewManager = ViewManager.instance(context);

        viewManager.register(BrowseView.class, TouchBrowseActivity.class);
        viewManager.setRoot(TouchBrowseActivity.class);
    }
}
