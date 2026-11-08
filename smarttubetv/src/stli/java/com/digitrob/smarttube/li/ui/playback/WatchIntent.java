package com.digitrob.smarttube.li.ui.playback;

import android.app.Activity;
import android.content.Intent;

import com.digitrob.smarttube.li.ui.watch.TouchWatchActivity;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

/**
 * Passes the selected {@link Video} to {@link TouchWatchActivity}. Video is not Parcelable,
 * and the watch page always runs in the same process, so a static hand-off is enough.
 */
public final class WatchIntent {
    private static Video sVideo;

    private WatchIntent() {
    }

    public static void start(Activity activity, Video video) {
        sVideo = video;
        activity.startActivity(new Intent(activity, TouchWatchActivity.class));
    }

    public static Video take() {
        Video video = sVideo;
        sVideo = null;
        return video;
    }
}
