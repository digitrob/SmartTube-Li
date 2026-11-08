package com.digitrob.smarttube.li;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Brute-force in-app "now playing" bridge.
 *
 * <p>The watch page owns the player. When the user goes back to the feed, the watch activity is
 * kept alive behind it and this singleton exposes a tiny control surface (play/pause/close) plus
 * the current video so the feed can render a mini player. A foreground service is not needed for
 * this flow; if the process is killed in the background, playback stops (PiP covers that case).</p>
 */
public final class LiNowPlaying {
    public interface Host {
        void onMiniPlayPause();

        void onMiniClose();
    }

    public interface Listener {
        void onNowPlayingChanged();
    }

    private static LiNowPlaying sInstance;

    private final List<Listener> mListeners = new CopyOnWriteArrayList<>();

    private Host mHost;
    private Video mVideo;
    private boolean mPlaying;
    private boolean mActive;

    private LiNowPlaying() {
    }

    public static synchronized LiNowPlaying instance() {
        if (sInstance == null) {
            sInstance = new LiNowPlaying();
        }
        return sInstance;
    }

    public void start(Host host, Video video, boolean playing) {
        mHost = host;
        mVideo = video;
        mPlaying = playing;
        mActive = video != null;
        notifyListeners();
    }

    /** Internal: stop showing the mini player without invoking the host (e.g. watch page resumed). */
    public void dismiss() {
        mHost = null;
        mVideo = null;
        mActive = false;
        mPlaying = false;
        notifyListeners();
    }

    public void update(boolean playing) {
        if (!mActive) {
            return;
        }
        mPlaying = playing;
        notifyListeners();
    }

    public void playPause() {
        if (mHost != null) {
            mHost.onMiniPlayPause();
        }
    }

    public void close() {
        Host host = mHost;
        dismiss();
        if (host != null) {
            host.onMiniClose();
        }
    }

    public boolean isActive() {
        return mActive;
    }

    public Video getVideo() {
        return mVideo;
    }

    public boolean isPlaying() {
        return mPlaying;
    }

    public void addListener(Listener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Listener listener : mListeners) {
            listener.onNowPlayingChanged();
        }
    }
}
