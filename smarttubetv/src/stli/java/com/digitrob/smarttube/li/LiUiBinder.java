package com.digitrob.smarttube.li;

import android.content.Context;

import com.digitrob.smarttube.li.ui.TouchBrowseActivity;
import com.liskovsoft.smartyoutubetv2.common.app.views.AddDeviceView;
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelUploadsView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelView;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.app.views.SearchView;
import com.liskovsoft.smartyoutubetv2.common.app.views.SignInView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.app.views.WebBrowserView;
import com.liskovsoft.smartyoutubetv2.tv.ui.adddevice.AddDeviceActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.channel.ChannelActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.channeluploads.ChannelUploadsActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.AppDialogActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.PlaybackActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.search.tags.SearchTagsActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.signin.SignInActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.webbrowser.WebBrowserActivity;

/**
 * Re-registers view interfaces with touch-first activities.
 *
 * <p>Called after upstream {@code MainApplication} has registered the TV activities, so this
 * simply overwrites the mappings we care about. Views we do not override keep the TV
 * implementation, which is the intended fallback while the touch UI is being built out.</p>
 *
 * <p>The parent activity is re-pointed to {@link TouchBrowseActivity} as well; otherwise
 * upstream's parent mapping sends the user back to the TV browse screen after playback.</p>
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

        // TV activities stay for now (M2/M3), but they must return to the touch home.
        viewManager.register(PlaybackView.class, PlaybackActivity.class, TouchBrowseActivity.class);
        viewManager.register(AppDialogView.class, AppDialogActivity.class, TouchBrowseActivity.class);
        viewManager.register(SearchView.class, SearchTagsActivity.class, TouchBrowseActivity.class);
        viewManager.register(SignInView.class, SignInActivity.class, TouchBrowseActivity.class);
        viewManager.register(AddDeviceView.class, AddDeviceActivity.class, TouchBrowseActivity.class);
        viewManager.register(ChannelView.class, ChannelActivity.class, TouchBrowseActivity.class);
        viewManager.register(ChannelUploadsView.class, ChannelUploadsActivity.class, TouchBrowseActivity.class);
        viewManager.register(WebBrowserView.class, WebBrowserActivity.class, TouchBrowseActivity.class);
    }
}
