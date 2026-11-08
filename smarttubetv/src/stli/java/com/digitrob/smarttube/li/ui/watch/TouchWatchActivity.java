package com.digitrob.smarttube.li.ui.watch;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.digitrob.smarttube.li.LiMetrics;
import com.digitrob.smarttube.li.LiNowPlaying;
import com.digitrob.smarttube.li.ui.playback.SimplePlayerListener;
import com.digitrob.smarttube.li.LiPrefs;
import com.digitrob.smarttube.li.ui.playback.WatchIntent;
import com.digitrob.smarttube.li.ui.TouchBrowseActivity;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemMetadata;
import com.liskovsoft.mediaserviceinterfaces.data.SponsorSegment;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.settings.SponsorBlockSettingsPresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.SponsorBlockData;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.ExoPlayerController;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.PlayerView;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.ExoPlayerInitializer;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.renderer.CustomOverridesRenderersFactory;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 2-pane watch page: video + metadata on the left, Up next on the right.
 *
 * <p>Uses {@link ExoPlayerController} directly, so it does not depend on Leanback. Fullscreen
 * simply hides the side panel and the metadata block.</p>
 */
public class TouchWatchActivity extends MotherActivity implements LiNowPlaying.Host {
    private static final long CONTROLS_TIMEOUT_MS = 3_000;
    private static final long SEEK_STEP_MS = 10_000;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final List<Disposable> mActions = new ArrayList<>();

    private com.google.android.exoplayer2.ui.PlayerView mExoPlayerView;
    private View mVideoContainer;
    private View mOverlay;
    private View mControls;
    private View mSidePane;
    private View mMetaPane;
    private ImageButton mBackButton;
    private ImageButton mPlayButton;
    private ImageButton mNextButton;
    private ImageButton mGearButton;
    private ProgressBar mBuffering;
    private ProgressBar mMiniProgress;
    private TextView mPlayerTitle;
    private SeekBar mSeekBar;
    private TextView mPosition;
    private TextView mDuration;
    private ImageButton mFullscreenButton;
    private TextView mTitle;
    private TextView mAuthor;
    private TextView mStats;
    private TextView mDescription;

    private UpNextAdapter mUpNextAdapter;

    private SimpleExoPlayer mPlayer;
    private ExoPlayerInitializer mInitializer;
    private ExoPlayerController mController;
    private MediaItemService mItemService;

    private Video mVideo;
    private long mPendingSeekMs;
    private boolean mFullscreen;
    private boolean mControlsVisible = true;
    private boolean mUserSeeking;
    private boolean mKeepPlaying;
    private boolean mInPip;
    private boolean mMinimized;
    private List<SponsorSegment> mSponsorSegments;
    private Disposable mSponsorAction;
    private long mLastSkipEndMs = -1;
    private long mMutedSegmentEndMs = -1;
    private static final long SKIP_WINDOW_MS = 2_000;
    private GestureDetector mGestureDetector;

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
            updatePlayButton();
            LiNowPlaying.instance().update(true);
        }

        @Override
        public void onPause() {
            updatePlayButton();
            LiNowPlaying.instance().update(false);
            showControls();
            mHandler.removeCallbacks(mHideControls);
        }

        @Override
        public void onBuffering() {
            if (mBuffering != null) {
                mBuffering.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPlayEnd() {
            playNext();
        }

        @Override
        public void onEngineError(int type, int rendererIndex, Throwable error) {
            showError(error != null ? String.valueOf(error.getMessage()) : "Playback error");
        }

        @Override
        public void onVideoLoaded(Video item) {
            if (mBuffering != null) {
                mBuffering.setVisibility(View.GONE);
            }
            if (mController != null) {
                FormatItem savedFormat = PlayerData.instance(TouchWatchActivity.this)
                        .getFormat(FormatItem.TYPE_VIDEO);
                if (savedFormat != null) {
                    mController.selectFormat(savedFormat);
                }
                mController.setSpeed(PlayerData.instance(TouchWatchActivity.this).getSpeed());
            }
            if (mPendingSeekMs > 0 && mController != null) {
                mController.setPositionMs(mPendingSeekMs);
                mPendingSeekMs = 0;
            }
            updatePlayButton();
            updateDuration();
        }
    };

    private final Runnable mProgressTick = new Runnable() {
        @Override
        public void run() {
            if (mController == null) {
                return;
            }

            long position = mController.getPositionMs();
            long duration = mController.getDurationMs();

            if (duration > 0) {
                mMiniProgress.setProgress((int) (position * 1000 / duration));
            }
            if (!mUserSeeking) {
                if (duration > 0) {
                    mSeekBar.setProgress((int) (position * 1000 / duration));
                }
                mPosition.setText(formatTime(position));
            }
            mDuration.setText(formatTime(duration));
            mBuffering.setVisibility(mController.isBuffering() ? View.VISIBLE : View.GONE);
            checkSponsorSegments(position);

            mHandler.postDelayed(this, 500);
        }
    };

    private final Runnable mHideControls = () -> setControlsVisible(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LiMetrics.applyCarDensity(this);
        setTheme(R.style.LiTheme);
        setContentView(R.layout.li_watch);
        // MotherActivity.onCreate already armed a dim timer; cancel it before blocking
        // further scheduling, otherwise the black overlay still appears after 10 s.
        getScreensaverManager().disable();
        getScreensaverManager().setBlocked(true);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        bindViews();

        mVideo = WatchIntent.take();
        if (mVideo == null) {
            finish();
            return;
        }

        mItemService = YouTubeServiceManager.instance().getMediaItemService();

        if (!LiPrefs.instance(this).isPlayerFormatInitialized()) {
            // Safe default for the headunit: 1080p H.264. Users can change it via the gear
            // sheet; the flag makes sure we never overwrite that choice on later launches.
            PlayerData.instance(this).setFormat(FormatItem.VIDEO_FHD_AVC_30);
            LiPrefs.instance(this).setPlayerFormatInitialized(true);
        }

        setupUpNext();
        setupControls();
        createPlayer();
        loadVideo(mVideo);
    }

    private void bindViews() {
        mExoPlayerView = findViewById(R.id.li_watch_player);
        mVideoContainer = findViewById(R.id.li_watch_video_container);
        mOverlay = findViewById(R.id.li_watch_overlay);
        mControls = findViewById(R.id.li_watch_controls);
        mSidePane = findViewById(R.id.li_watch_side);
        mMetaPane = findViewById(R.id.li_watch_meta);
        mBackButton = findViewById(R.id.li_watch_back);
        mPlayButton = findViewById(R.id.li_watch_play);
        mNextButton = findViewById(R.id.li_watch_next);
        mGearButton = findViewById(R.id.li_watch_gear);
        mBuffering = findViewById(R.id.li_watch_buffering);
        mMiniProgress = findViewById(R.id.li_watch_mini_progress);
        mPlayerTitle = findViewById(R.id.li_watch_player_title);
        mSeekBar = findViewById(R.id.li_watch_seek);
        mPosition = findViewById(R.id.li_watch_position);
        mDuration = findViewById(R.id.li_watch_duration);
        mFullscreenButton = findViewById(R.id.li_watch_fullscreen);
        mTitle = findViewById(R.id.li_watch_title);
        mAuthor = findViewById(R.id.li_watch_author);
        mStats = findViewById(R.id.li_watch_stats);
        mDescription = findViewById(R.id.li_watch_description);
    }

    private void setupUpNext() {
        RecyclerView recycler = findViewById(R.id.li_watch_up_next);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        mUpNextAdapter = new UpNextAdapter(this, this::loadVideo);
        recycler.setAdapter(mUpNextAdapter);
    }

    private void setupControls() {
        mBackButton.setOnClickListener(v -> finish());
        mPlayButton.setOnClickListener(v -> togglePlay());
        mFullscreenButton.setOnClickListener(v -> toggleFullscreen());

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mController != null) {
                    long duration = mController.getDurationMs();
                    mPosition.setText(formatTime(duration * progress / 1000));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mUserSeeking = true;
                showControls();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mUserSeeking = false;
                if (mController != null) {
                    long duration = mController.getDurationMs();
                    if (duration > 0) {
                        mController.setPositionMs(duration * seekBar.getProgress() / 1000);
                    }
                }
                showControls();
            }
        });

        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                setControlsVisible(!mControlsVisible);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                boolean forward = e.getX() > mVideoContainer.getWidth() / 2f;
                seekBy(forward ? SEEK_STEP_MS : -SEEK_STEP_MS);
                return true;
            }
        });

        // Consume the stream here so a tap is handled exactly once. Returning false lets the
        // View fire its click listener as well, which used to toggle the controls twice.
        mOverlay.setOnTouchListener((v, event) -> {
            mGestureDetector.onTouchEvent(event);
            return true;
        });

        findViewById(R.id.li_watch_like).setOnClickListener(v -> rate(true));
        findViewById(R.id.li_watch_dislike).setOnClickListener(v -> rate(false));
        findViewById(R.id.li_watch_share).setOnClickListener(v -> share());
        mNextButton.setOnClickListener(v -> playNext());
        mGearButton.setOnClickListener(v -> showPlayerSettings());

        showControls();
    }

    private void createPlayer() {
        mInitializer = new ExoPlayerInitializer(this);
        DefaultTrackSelector trackSelector = new RestoreTrackSelector(new AdaptiveTrackSelection.Factory());

        mController = new ExoPlayerController(this, mPlayerListener);
        mController.setTrackSelector(trackSelector);

        mPlayer = mInitializer.createPlayer(this, new CustomOverridesRenderersFactory(this), trackSelector);

        mController.setPlayer(mPlayer);
        mController.setPlayerView(mControllerView);

        mExoPlayerView.setPlayer(mPlayer);
        mExoPlayerView.setUseController(false);
    }

    private void loadVideo(Video video) {
        if (video == null) {
            return;
        }

        mVideo = video;
        disposeActions();

        if (mController != null) {
            mController.setVideo(video);
        }
        mPendingSeekMs = resumePosition(video);
        bindMetadata(video);
        mUpNextAdapter.setItems(null);
        mBuffering.setVisibility(View.VISIBLE);
        mSidePane.setVisibility(mFullscreen ? View.GONE : View.VISIBLE);

        if (mItemService == null) {
            mItemService = YouTubeServiceManager.instance().getMediaItemService();
        }

        mActions.add(mItemService.getFormatInfoObserve(video.videoId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::openFormat, this::onLoadError));

        mActions.add(mItemService.getMetadataObserve(
                        video.videoId, video.getPlaylistId(), video.playlistIndex, video.playlistParams)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onMetadata, error -> { }));

        loadSponsorSegments(video);
    }

    private void openFormat(MediaItemFormatInfo info) {
        if (info == null) {
            showError("No video formats");
            return;
        }

        if (info.isUnplayable()) {
            showError(info.getPlayabilityReason());
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
            showError(info.getPlayabilityReason());
            return;
        }

        mPlayer.setPlayWhenReady(true);
        updatePlayButton();
    }

    private void onMetadata(MediaItemMetadata metadata) {
        if (metadata == null) {
            return;
        }

        if (metadata.getAuthor() != null) {
            mAuthor.setText(metadata.getAuthor());
        }
        if (metadata.getSecondTitle() != null) {
            mStats.setText(metadata.getSecondTitle());
        }
        if (metadata.getDescription() != null && !metadata.getDescription().isEmpty()) {
            mDescription.setText(metadata.getDescription());
            mDescription.setVisibility(View.VISIBLE);
        }

        if (mPendingSeekMs <= 0 && metadata.getPercentWatched() > 0 && metadata.getDurationMs() > 0) {
            mPendingSeekMs = metadata.getDurationMs() * metadata.getPercentWatched() / 100;
        }

        List<Video> videos = new ArrayList<>();
        List<MediaGroup> suggestions = metadata.getSuggestions();
        if (suggestions != null) {
            for (MediaGroup group : suggestions) {
                VideoGroup videoGroup = VideoGroup.from(group);
                if (videoGroup == null) {
                    continue;
                }
                for (Video video : videoGroup.getVideos()) {
                    if (video != null && video.videoId != null) {
                        videos.add(video);
                    }
                }
            }
        }
        mUpNextAdapter.setItems(videos);
    }

    private void onLoadError(Throwable error) {
        showError(error != null ? String.valueOf(error.getMessage()) : "Loading error");
    }

    private void bindMetadata(Video video) {
        mTitle.setText(video.getTitle());
        if (mPlayerTitle != null) {
            mPlayerTitle.setText(video.getTitle());
        }
        mAuthor.setText(video.getAuthor());
        mStats.setText(video.getSecondTitle());

        if (video.description != null && !video.description.isEmpty()) {
            mDescription.setText(video.description);
            mDescription.setVisibility(View.VISIBLE);
        } else {
            mDescription.setVisibility(View.GONE);
        }

        updatePlayButton();
    }

    private long resumePosition(Video video) {
        if (video.pendingPosMs > 0) {
            return video.pendingPosMs;
        }
        if (video.startTimeSeconds > 0) {
            return video.startTimeSeconds * 1_000L;
        }
        return 0;
    }

    private void togglePlay() {
        if (mController == null) {
            return;
        }
        mController.setPlayWhenReady(!mController.getPlayWhenReady());
        updatePlayButton();
        showControls();
    }

    private void updatePlayButton() {
        boolean play = mController == null || !mController.getPlayWhenReady();
        mPlayButton.setImageResource(play ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause);
    }

    private void updateDuration() {
        if (mController != null) {
            mDuration.setText(formatTime(mController.getDurationMs()));
        }
    }

    private void seekBy(long deltaMs) {
        if (mController == null) {
            return;
        }
        long position = Math.max(0, mController.getPositionMs() + deltaMs);
        mController.setPositionMs(position);
        showControls();
    }

    private void playNext() {
        List<Video> videos = mUpNextAdapter.getItems();
        if (!videos.isEmpty()) {
            loadVideo(videos.get(0));
        }
    }

    @Override
    public void onMiniPlayPause() {
        togglePlay();
        LiNowPlaying.instance().update(mController != null && mController.getPlayWhenReady());
    }

    @Override
    public void onMiniClose() {
        mMinimized = false;
        finish();
    }

    private void minimize() {
        mMinimized = true;
        LiNowPlaying.instance().start(this, mVideo,
                mController != null && mController.getPlayWhenReady());

        Intent intent = new Intent(this, TouchBrowseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void loadSponsorSegments(Video video) {
        disposeSponsor();

        mSponsorSegments = null;
        mLastSkipEndMs = -1;
        mMutedSegmentEndMs = -1;

        if (video == null || video.videoId == null || video.isLive) {
            return;
        }

        SponsorBlockData data = SponsorBlockData.instance(this);
        if (!data.isSponsorBlockEnabled() || data.getEnabledCategories().isEmpty()) {
            return;
        }

        mSponsorAction = mItemService.getSponsorSegmentsObserve(video.videoId, data.getEnabledCategories())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(segments -> mSponsorSegments = segments, error -> { });
    }

    private void toggleSponsorBlock() {
        SponsorBlockData data = SponsorBlockData.instance(this);
        boolean enabled = !data.isSponsorBlockEnabled();
        data.setSponsorBlockEnabled(enabled);
        MessageHelpers.showMessage(this, enabled ? "SponsorBlock on" : "SponsorBlock off");
        loadSponsorSegments(mVideo);
    }

    private void checkSponsorSegments(long positionMs) {
        if (mSponsorSegments == null || mSponsorSegments.isEmpty() || mController == null) {
            if (mMutedSegmentEndMs > 0) {
                mController.setVolume(1f);
                mMutedSegmentEndMs = -1;
            }
            return;
        }

        SponsorBlockData data = SponsorBlockData.instance(this);
        if (!data.isSponsorBlockEnabled()) {
            return;
        }

        if (mMutedSegmentEndMs > 0 && positionMs > mMutedSegmentEndMs) {
            mController.setVolume(1f);
            mMutedSegmentEndMs = -1;
        }

        for (SponsorSegment segment : mSponsorSegments) {
            boolean inside = positionMs >= segment.getStartMs()
                    && positionMs <= Math.min(segment.getStartMs() + SKIP_WINDOW_MS, segment.getEndMs());

            if (!inside) {
                continue;
            }

            int action = data.getAction(segment.getCategory());
            if (action == SponsorBlockData.ACTION_DO_NOTHING || action == SponsorBlockData.ACTION_UNDEFINED) {
                continue;
            }

            if (SponsorSegment.ACTION_MUTE.equals(segment.getAction())) {
                if (mMutedSegmentEndMs != segment.getEndMs()) {
                    mMutedSegmentEndMs = segment.getEndMs();
                    mController.setVolume(0f);
                }
                return;
            }

            if (mLastSkipEndMs == segment.getEndMs()) {
                continue;
            }
            mLastSkipEndMs = segment.getEndMs();

            if (action != SponsorBlockData.ACTION_SKIP_ONLY) {
                Integer res = data.getLocalizedRes(segment.getCategory());
                String label = res != null ? getString(res) : segment.getCategory();
                MessageHelpers.showMessage(this, "Skipped: " + label);
            }

            mController.setPositionMs(segment.getEndMs());
            return;
        }
    }

    private void disposeSponsor() {
        if (mSponsorAction != null && !mSponsorAction.isDisposed()) {
            mSponsorAction.dispose();
        }
        mSponsorAction = null;
    }

    private void showPlayerSettings() {
        if (mController == null) {
            return;
        }

        AppDialogPresenter dialog = AppDialogPresenter.instance(this);

        List<OptionItem> quality = new ArrayList<>();
        for (FormatItem format : mController.getVideoFormats()) {
            quality.add(UiOptionItem.from(format, item -> {
                mController.selectFormat(format);
                PlayerData.instance(this).setFormat(format);
            }));
        }
        dialog.appendRadioCategory("Quality", quality);

        float[] speeds = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
        float currentSpeed = PlayerData.instance(this).getSpeed();
        List<OptionItem> speedItems = new ArrayList<>();
        for (float speed : speeds) {
            String title = speed == 1.0f ? "Normal" : String.format("%sx", speed);
            speedItems.add(UiOptionItem.from(title, item -> {
                mController.setSpeed(speed);
                PlayerData.instance(this).setSpeed(speed);
            }, Math.abs(currentSpeed - speed) < 0.01f));
        }
        dialog.appendRadioCategory("Speed", speedItems);

        List<OptionItem> subtitles = new ArrayList<>();
        subtitles.add(UiOptionItem.from("Off", item ->
                mController.selectFormat(FormatItem.SUBTITLE_NONE),
                mController.getSubtitleFormat() == null));
        for (FormatItem format : mController.getSubtitleFormats()) {
            subtitles.add(UiOptionItem.from(format, item -> {
                mController.selectFormat(format);
                PlayerData.instance(this).setFormat(format);
            }));
        }
        dialog.appendRadioCategory("Subtitles", subtitles);

        dialog.appendSingleSwitch(UiOptionItem.from("SponsorBlock",
                item -> {
                    SponsorBlockData.instance(this).setSponsorBlockEnabled(item.isSelected());
                    loadSponsorSegments(mVideo);
                },
                SponsorBlockData.instance(this).isSponsorBlockEnabled()));

        mKeepPlaying = true;
        dialog.showDialog("Player");
    }

    private void showQuality() {
        if (mController == null) {
            return;
        }

        List<OptionItem> items = new ArrayList<>();
        for (FormatItem format : mController.getVideoFormats()) {
            items.add(UiOptionItem.from(format, item -> {
                mController.selectFormat(format);
                PlayerData.instance(this).setFormat(format);
            }));
        }

        showSheet("Quality", items);
    }

    private void showSpeed() {
        if (mController == null) {
            return;
        }

        float[] speeds = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
        float current = PlayerData.instance(this).getSpeed();
        List<OptionItem> items = new ArrayList<>();

        for (float speed : speeds) {
            String title = speed == 1.0f ? "Normal" : String.format("%sx", speed);
            items.add(UiOptionItem.from(title,
                    item -> {
                        mController.setSpeed(speed);
                        PlayerData.instance(this).setSpeed(speed);
                    },
                    Math.abs(current - speed) < 0.01f));
        }

        showSheet("Speed", items);
    }

    private void showCaptions() {
        if (mController == null) {
            return;
        }

        List<OptionItem> items = new ArrayList<>();
        items.add(UiOptionItem.from("Off", item -> {
            mController.selectFormat(FormatItem.SUBTITLE_NONE);
        }, mController.getSubtitleFormat() == null));

        for (FormatItem format : mController.getSubtitleFormats()) {
            items.add(UiOptionItem.from(format, item -> {
                mController.selectFormat(format);
                PlayerData.instance(this).setFormat(format);
            }));
        }

        showSheet("Subtitles", items);
    }

    private void showSheet(String title, List<OptionItem> items) {
        AppDialogPresenter dialog = AppDialogPresenter.instance(this);
        dialog.appendRadioCategory(title, items);
        // The sheet runs in its own translucent activity; keep the video playing behind it.
        mKeepPlaying = true;
        dialog.showDialog(title);
    }

    private void toggleFullscreen() {
        mFullscreen = !mFullscreen;
        mSidePane.setVisibility(mFullscreen ? View.GONE : View.VISIBLE);
        mMetaPane.setVisibility(mFullscreen ? View.GONE : View.VISIBLE);
        mFullscreenButton.setImageResource(mFullscreen
                ? R.drawable.ic_li_fullscreen_exit
                : R.drawable.ic_li_fullscreen);
        mFullscreenButton.setContentDescription(mFullscreen ? "Exit fullscreen" : "Fullscreen");
        showControls();
    }

    private void setControlsVisible(boolean visible) {
        mControlsVisible = visible;
        mControls.animate().alpha(visible ? 1f : 0f).setDuration(200).start();
        mBackButton.animate().alpha(visible ? 1f : 0f).setDuration(200).start();
        if (mPlayerTitle != null) {
            mPlayerTitle.animate().alpha(visible ? 1f : 0f).setDuration(200).start();
        }
        mControls.setClickable(visible);

        mHandler.removeCallbacks(mHideControls);
        if (visible && CONTROLS_TIMEOUT_MS > 0) {
            mHandler.postDelayed(mHideControls, CONTROLS_TIMEOUT_MS);
        }
    }

    private void showControls() {
        setControlsVisible(true);
    }

    private void rate(boolean like) {
        if (mVideo == null || mVideo.mediaItem == null) {
            MessageHelpers.showMessage(this, "Sign in to rate videos");
            return;
        }

        mActions.add(Observable.fromCallable(() -> {
                    if (like) {
                        mItemService.setLike(mVideo.mediaItem);
                    } else {
                        mItemService.setDislike(mVideo.mediaItem);
                    }
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ok -> MessageHelpers.showMessage(this, like ? "Liked" : "Disliked"),
                        error -> MessageHelpers.showMessage(this, "Could not update rating")));
    }

    private void share() {
        if (mVideo == null) {
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "https://youtu.be/" + mVideo.videoId);
        startActivity(Intent.createChooser(send, "Share"));
    }

    private void showError(String message) {
        mBuffering.setVisibility(View.GONE);
        MessageHelpers.showMessage(this, message != null ? message : "Playback error");
    }

    private void saveHistoryPosition() {
        if (mVideo == null || mVideo.mediaItem == null || mController == null) {
            return;
        }

        final float seconds = mController.getPositionMs() / 1000f;
        final com.liskovsoft.mediaserviceinterfaces.data.MediaItem item = mVideo.mediaItem;

        Observable.fromCallable(() -> {
                    mItemService.updateHistoryPosition(item, seconds);
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(ok -> { }, error -> { });
    }

    private void disposeActions() {
        disposeSponsor();

        for (Disposable action : mActions) {
            if (action != null && !action.isDisposed()) {
                action.dispose();
            }
        }
        mActions.clear();
    }

    private static String formatTime(long ms) {
        long totalSeconds = Math.max(0, ms / 1_000);
        long hours = totalSeconds / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    protected void initTheme() {
        setTheme(R.style.LiTheme);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LiMetrics.applyCarDensity(this);
        mKeepPlaying = false;
        mMinimized = false;
        LiNowPlaying.instance().dismiss();
        mHandler.post(mProgressTick);
        if (!mInPip) {
            showControls();
        }
    }

    @Override
    protected void onPause() {
        if (mPlayer != null && !mKeepPlaying && !mInPip && !mMinimized) {
            mPlayer.setPlayWhenReady(false);
        }
        saveHistoryPosition();
        mHandler.removeCallbacks(mProgressTick);
        mHandler.removeCallbacks(mHideControls);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        LiNowPlaying.instance().dismiss();
        disposeActions();
        mHandler.removeCallbacksAndMessages(null);

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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        Video video = WatchIntent.take();
        if (video != null) {
            loadVideo(video);
        }
    }

    @Override
    public void onBackPressed() {
        if (mController != null && mController.containsMedia() && mVideo != null) {
            minimize();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        enterPipIfPossible();
    }

    private void enterPipIfPossible() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || mController == null || !mController.isPlaying()) {
            return;
        }

        try {
            mInPip = true;
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9))
                    .build();
            enterPictureInPictureMode(params);
        } catch (Throwable t) {
            mInPip = false;
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        mInPip = isInPictureInPictureMode;

        if (isInPictureInPictureMode) {
            mSidePane.setVisibility(View.GONE);
            mMetaPane.setVisibility(View.GONE);
            mControls.setVisibility(View.GONE);
            mBackButton.setVisibility(View.GONE);
        } else {
            mSidePane.setVisibility(mFullscreen ? View.GONE : View.VISIBLE);
            mMetaPane.setVisibility(mFullscreen ? View.GONE : View.VISIBLE);
            mControls.setVisibility(View.VISIBLE);
            mBackButton.setVisibility(View.VISIBLE);
            showControls();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LiMetrics.applyCarDensity(this);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    togglePlay();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    mController.setPlayWhenReady(true);
                    updatePlayButton();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    mController.setPlayWhenReady(false);
                    updatePlayButton();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    playNext();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    seekBy(-SEEK_STEP_MS);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    seekBy(SEEK_STEP_MS);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    seekBy(-SEEK_STEP_MS);
                    return true;
                default:
                    break;
            }
        }

        return super.dispatchKeyEvent(event);
    }
}
