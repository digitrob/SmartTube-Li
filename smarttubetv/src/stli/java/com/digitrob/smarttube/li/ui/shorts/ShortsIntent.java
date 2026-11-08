package com.digitrob.smarttube.li.ui.shorts;

import android.app.Activity;
import android.content.Intent;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

import java.util.ArrayList;
import java.util.List;

/** Hand-off for the Shorts pager (Video is not Parcelable; same process only). */
public final class ShortsIntent {
    private static List<Video> sShorts;
    private static int sIndex;

    private ShortsIntent() {
    }

    public static void start(Activity activity, List<Video> shorts, int index) {
        if (shorts == null || shorts.isEmpty()) {
            return;
        }

        sShorts = new ArrayList<>(shorts);
        sIndex = Math.max(0, Math.min(index, shorts.size() - 1));

        activity.startActivity(new Intent(activity, TouchShortsActivity.class));
    }

    static List<Video> takeShorts() {
        List<Video> shorts = sShorts;
        sShorts = null;
        return shorts;
    }

    static int takeIndex() {
        return sIndex;
    }
}
