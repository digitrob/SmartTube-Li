package com.digitrob.smarttube.li.ui.shell;

import android.app.Activity;
import android.content.Intent;

import com.digitrob.smarttube.li.ui.TouchBrowseActivity;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/** Section ids shared with upstream {@link MediaGroup}, plus the default rail entries. */
public final class LiSections {
    public static final String EXTRA_SECTION_ID = "li_section_id";

    public static final int HOME = MediaGroup.TYPE_HOME;
    public static final int SHORTS = MediaGroup.TYPE_SHORTS;
    public static final int KIDS = MediaGroup.TYPE_KIDS_HOME;
    public static final int SPORTS = MediaGroup.TYPE_SPORTS;
    public static final int LIVE = MediaGroup.TYPE_LIVE;
    public static final int GAMING = MediaGroup.TYPE_GAMING;
    public static final int NEWS = MediaGroup.TYPE_NEWS;
    public static final int MUSIC = MediaGroup.TYPE_MUSIC;
    public static final int CHANNELS = MediaGroup.TYPE_CHANNEL_UPLOADS;
    public static final int SUBSCRIPTIONS = MediaGroup.TYPE_SUBSCRIPTIONS;
    public static final int HISTORY = MediaGroup.TYPE_HISTORY;
    public static final int PLAYLISTS = MediaGroup.TYPE_USER_PLAYLISTS;
    public static final int MY_VIDEOS = MediaGroup.TYPE_MY_VIDEOS;
    public static final int SETTINGS = MediaGroup.TYPE_SETTINGS;

    public static final class Entry {
        public final int id;
        public final String title;

        Entry(int id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    private LiSections() {
    }

    /** Same order as the default SmartTube sidebar. */
    public static List<Entry> defaults() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(HOME, "Home"));
        entries.add(new Entry(SHORTS, "Shorts"));
        entries.add(new Entry(KIDS, "Kids"));
        entries.add(new Entry(SPORTS, "Sports"));
        entries.add(new Entry(LIVE, "LIVE"));
        entries.add(new Entry(GAMING, "Gaming"));
        entries.add(new Entry(NEWS, "News"));
        entries.add(new Entry(MUSIC, "Music"));
        entries.add(new Entry(CHANNELS, "Channels"));
        entries.add(new Entry(SUBSCRIPTIONS, "Subscriptions"));
        entries.add(new Entry(HISTORY, "History"));
        entries.add(new Entry(PLAYLISTS, "Playlists"));
        entries.add(new Entry(MY_VIDEOS, "My videos"));
        entries.add(new Entry(SETTINGS, "Settings"));
        return entries;
    }

    /** Open the browse shell on a section (used by the search page rail). */
    public static void open(Activity activity, int sectionId) {
        Intent intent = new Intent(activity, TouchBrowseActivity.class);
        intent.putExtra(EXTRA_SECTION_ID, sectionId);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
        activity.finish();
    }

    public static int iconFor(int sectionId) {
        switch (sectionId) {
            case HOME:
                return R.drawable.ic_li_home;
            case SHORTS:
            case KIDS:
            case SPORTS:
            case LIVE:
            case GAMING:
            case NEWS:
            case MUSIC:
                return R.drawable.ic_li_shorts;
            case CHANNELS:
                return R.drawable.ic_li_channels;
            case SUBSCRIPTIONS:
                return R.drawable.ic_li_subscriptions;
            case HISTORY:
                return R.drawable.ic_li_history;
            case PLAYLISTS:
                return R.drawable.ic_li_playlists;
            case MY_VIDEOS:
                return R.drawable.ic_li_my_videos;
            case SETTINGS:
                return R.drawable.ic_li_settings;
            default:
                return R.drawable.ic_li_shorts;
        }
    }
}
