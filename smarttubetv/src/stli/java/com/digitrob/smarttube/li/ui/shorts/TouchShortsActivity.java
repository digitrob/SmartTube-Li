package com.digitrob.smarttube.li.ui.shorts;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.digitrob.smarttube.li.LiMetrics;
import com.digitrob.smarttube.li.ui.playback.SimplePlayerListener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.ExoPlayerController;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.PlayerView;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.ExoPlayerInitializer;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.renderer.CustomOverridesRenderersFactory;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Full-screen Shorts pager: vertical swipe, one shared player.
 *
 * <p>A single ExoPlayer sits behind the pager. Swiping snaps to the next page and reloads the
 * player with that Short; taps toggle playback.</p>
 */
public class TouchShortsActivity extends MotherActivity {
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private RecyclerView mPager;
    private LinearLayoutManager mLayoutManager;
    private PagerSnapHelper mSnapHelper;
    private ShortsAdapter mAdapter;
    private ProgressBar mProgress;
    private TextView mPauseIndicator;

    private SimpleExoPlayer mPlayer;
    private ExoPlayerInitializer mInitializer;
    private ExoPlayerController mController;
    private MediaItemService mItemService;

    private List<Video> mShorts;
    private Video mCurrentVideo;
    private String mLoadedVideoId;
    private int mIndex;
    private Disposable mFormatAction;

    private final PlayerView mControllerView = new PlayerView() {
        @Override
        public void setQualityInfo(String info) {
        }

        @Override
        public void setVideo(Video video) {
        }
    };

    private final SimplePlayerListener mPlayerListener = new SimplePlayerListener() {
        @Override
        public void onPlay() {
            updatePauseIndicator();
        }

        @Override
        public void onPause() {
            updatePauseIndicator();
        }

        @Override
        public void onPlayEnd() {
            playNext();
        }

        @Override
        public void onEngineError(int type, int rendererIndex, Throwable error) {
            MessageHelpers.showMessage(TouchShortsActivity.this,
                    error != null ? String.valueOf(error.getMessage()) : "Playback error");
        }

        @Override
        public void onVideoLoaded(Video item) {
            if (mPlayer != null) {
                mPlayer.setPlayWhenReady(true);
            }
            updatePauseIndicator();
        }
    };

    private final Runnable mProgressTick = new Runnable() {
        @Override
        public void run() {
            if (mController == null) {
                return;
            }
            long duration = mController.getDurationMs();
            long position = mController.getPositionMs();
            if (duration > 0) {
                mProgress.setProgress((int) (position * 1000 / duration));
            }
            updatePauseIndicator();
            mHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LiMetrics.applyCarDensity(this);
        setTheme(R.style.LiTheme);
        setContentView(R.layout.li_shorts);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getScreensaverManager().setBlocked(true);

        mShorts = ShortsIntent.takeShorts();
        mIndex = ShortsIntent.takeIndex();

        if (mShorts == null || mShorts.isEmpty()) {
            finish();
            return;
        }

        mPager = findViewById(R.id.li_shorts_pager);
        mProgress = findViewById(R.id.li_shorts_progress);
        mPauseIndicator = findViewById(R.id.li_shorts_pause_indicator);
        findViewById(R.id.li_shorts_back).setOnClickListener(v -> finish());

        mItemService = YouTubeServiceManager.instance().getMediaItemService();

        setupPlayer();
        setupPager();
    }

    private void setupPlayer() {
        mInitializer = new ExoPlayerInitializer(this);
        DefaultTrackSelector trackSelector = new RestoreTrackSelector(new AdaptiveTrackSelection.Factory());

        mController = new ExoPlayerController(this, mPlayerListener);
        mController.setTrackSelector(trackSelector);

        mPlayer = mInitializer.createPlayer(this, new CustomOverridesRenderersFactory(this), trackSelector);
        mController.setPlayer(mPlayer);
        mController.setPlayerView(mControllerView);

        com.google.android.exoplayer2.ui.PlayerView playerView = findViewById(R.id.li_shorts_player);
        playerView.setPlayer(mPlayer);
        playerView.setUseController(false);
    }

    private void setupPager() {
        mAdapter = new ShortsAdapter(new ShortsAdapter.Listener() {
            @Override
            public void onTap() {
                togglePlay();
            }

            @Override
            public void onLike(Video video) {
                rate(video, true);
            }

            @Override
            public void onDislike(Video video) {
                rate(video, false);
            }

            @Override
            public void onShare(Video video) {
                share(video);
            }
        });
        mAdapter.setItems(mShorts);

        mLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        mPager.setLayoutManager(mLayoutManager);
        mPager.setAdapter(mAdapter);
        mPager.setItemAnimator(null);

        mSnapHelper = new PagerSnapHelper();
        mSnapHelper.attachToRecyclerView(mPager);

        mPager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }

                View snap = mSnapHelper.findSnapView(mLayoutManager);
                int position = snap != null ? mLayoutManager.getPosition(snap) : -1;
                if (position >= 0 && position != mIndex) {
                    mIndex = position;
                    playIndex(position);
                }
            }
        });

        mLayoutManager.scrollToPositionWithOffset(mIndex, 0);
        playIndex(mIndex);
    }

    private void playIndex(int index) {
        if (index < 0 || index >= mShorts.size()) {
            return;
        }

        Video video = mShorts.get(index);
        if (video == null || video.videoId == null) {
            return;
        }
        if (video.videoId.equals(mLoadedVideoId)) {
            return;
        }

        mLoadedVideoId = video.videoId;
        mCurrentVideo = video;
        mProgress.setProgress(0);

        if (mFormatAction != null && !mFormatAction.isDisposed()) {
            mFormatAction.dispose();
        }

        mController.setVideo(video);

        mFormatAction = mItemService.getFormatInfoObserve(video.videoId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::openFormat, error ->
                        MessageHelpers.showMessage(this, "Could not load this Short"));
    }

    private void openFormat(MediaItemFormatInfo info) {
        if (info == null || info.isUnplayable()) {
            MessageHelpers.showMessage(this,
                    info != null && info.getPlayabilityReason() != null
                            ? info.getPlayabilityReason() : "Short is unavailable");
            return;
        }

        if (info.isLive() && info.containsSabrFormats()) {
            mController.openSabr(info);
        } else if (info.containsDashFormats()) {
            mController.openDash(info);
        } else if (info.containsSabrFormats()) {
            mController.openSabr(info);
        } else if (info.isLive() && info.containsDashUrl()) {
            mController.openDashUrl(info.getDashManifestUrl());
        } else if (info.isLive() && info.containsHlsUrl()) {
            mController.openHlsUrl(info.getHlsManifestUrl());
        } else if (info.containsUrlFormats()) {
            mController.openUrlList(info.createUrlList());
        } else {
            MessageHelpers.showMessage(this, "Short is unavailable");
            return;
        }

        mPlayer.setPlayWhenReady(true);
    }

    private void togglePlay() {
        if (mController == null) {
            return;
        }
        mController.setPlayWhenReady(!mController.getPlayWhenReady());
        updatePauseIndicator();
    }

    private void updatePauseIndicator() {
        boolean playing = mController != null && mController.getPlayWhenReady();
        mPauseIndicator.setVisibility(playing ? View.GONE : View.VISIBLE);
    }

    private void playNext() {
        if (mIndex + 1 < mShorts.size()) {
            mPager.smoothScrollToPosition(mIndex + 1);
        }
    }

    private void playPrevious() {
        if (mIndex - 1 >= 0) {
            mPager.smoothScrollToPosition(mIndex - 1);
        }
    }

    private void rate(Video video, boolean like) {
        if (video == null || video.mediaItem == null) {
            MessageHelpers.showMessage(this, "Sign in to rate videos");
            return;
        }

        Observable.fromCallable(() -> {
                    if (like) {
                        mItemService.setLike(video.mediaItem);
                    } else {
                        mItemService.setDislike(video.mediaItem);
                    }
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ok -> MessageHelpers.showMessage(this, like ? "Liked" : "Disliked"),
                        error -> MessageHelpers.showMessage(this, "Could not update rating"));
    }

    private void share(Video video) {
        if (video == null) {
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "https://youtu.be/" + video.videoId);
        startActivity(Intent.createChooser(send, "Share"));
    }

    @Override
    protected void initTheme() {
        setTheme(R.style.LiTheme);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LiMetrics.applyCarDensity(this);
        mHandler.post(mProgressTick);
        updatePauseIndicator();
    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            mPlayer.setPlayWhenReady(false);
        }
        mHandler.removeCallbacks(mProgressTick);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        if (mFormatAction != null && !mFormatAction.isDisposed()) {
            mFormatAction.dispose();
        }
        if (mController != null) {
            mController.release();
            mController = null;
        }
        if (mInitializer != null) {
            mInitializer.release();
            mInitializer = null;
        }
        mPlayer = null;
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    togglePlay();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    playNext();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                case KeyEvent.KEYCODE_DPAD_UP:
                    playPrevious();
                    return true;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
