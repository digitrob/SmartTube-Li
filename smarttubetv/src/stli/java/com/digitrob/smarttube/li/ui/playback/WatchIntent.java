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
    private static Video sLastVideo;

    private WatchIntent() {
    }

    public static void start(Activity activity, Video video) {
        sVideo = video;
        sLastVideo = video;
        Intent intent = new Intent(activity, TouchWatchActivity.class);
        // Reuse the already-running watch activity (e.g. the in-app mini player) so there is
        // only ever one player instance.
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
    }

    public static Video take() {
        Video video = sVideo;
        sVideo = null;
        return video;
    }

    /** Last opened video. Used to rebuild the watch page after an orientation change. */
    public static Video last() {
        return sLastVideo;
    }
}
