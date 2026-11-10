package com.digitrob.smarttube.li.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.digitrob.smarttube.li.LiMetrics;
import com.digitrob.smarttube.li.LiNowPlaying;
import com.bumptech.glide.Glide;
import com.digitrob.smarttube.li.ui.browse.FeedAdapter;
import com.digitrob.smarttube.li.ui.browse.SettingsAdapter;
import com.digitrob.smarttube.li.ui.playback.WatchIntent;
import com.digitrob.smarttube.li.ui.shorts.ShortsIntent;
import com.digitrob.smarttube.li.ui.shell.LiSections;
import com.digitrob.smarttube.li.ui.shell.NavRailController;
import com.digitrob.smarttube.li.ui.watch.TouchWatchActivity;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Touch home: nav rail + 3-column feed. Reuses {@link BrowsePresenter} for all data and actions.
 */
public class TouchBrowseActivity extends MotherActivity implements BrowseView {
    private static final int MIN_CARD_WIDTH_DP = 300;

    private NavRailController mRail;
    private TextView mHint;
    private RecyclerView mRecycler;
    private SwipeRefreshLayout mRefresh;
    private FeedAdapter mAdapter;
    private SettingsAdapter mSettingsAdapter;
    private boolean mSettingsShown;
    private View mMiniPlayer;
    private ImageView mMiniThumb;
    private TextView mMiniTitle;
    private ImageButton mMiniPlay;
    private final LiNowPlaying.Listener mMiniListener = this::updateMiniPlayer;
    private GridLayoutManager mLayoutManager;
    private BrowsePresenter mPresenter;
    private float mDensity = 2.25f;
    private int mCurrentSectionId = -1;
    private String mLastScrollEndKey;
    private boolean mProgressShowing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LiMetrics.applyCarDensity(this);
        mDensity = getResources().getDisplayMetrics().density;
        setTheme(R.style.LiTheme);
        setContentView(R.layout.li_touch_browse);
        LiWindow.applyImmersive(this);
        // MotherActivity.onCreate already armed a dim timer; cancel it before blocking
        // further scheduling, otherwise the black overlay still appears after 10 s.
        getScreensaverManager().disable();
        getScreensaverManager().setBlocked(true);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mHint = findViewById(R.id.li_content_hint);
        mRecycler = findViewById(R.id.li_content);
        mRefresh = findViewById(R.id.li_refresh);

        setupFeed();

        findViewById(R.id.li_search).setOnClickListener(v ->
                com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter
                        .instance(this).startSearch(null));

        mRail = new NavRailController(this, sectionId -> {
            if (mPresenter != null) {
                mPresenter.selectSection(sectionId);
            }
        });

        mPresenter = BrowsePresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();

        int pendingSection = getIntent().getIntExtra(LiSections.EXTRA_SECTION_ID, -1);
        if (pendingSection != -1) {
            mPresenter.selectSection(pendingSection);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int sectionId = intent.getIntExtra(LiSections.EXTRA_SECTION_ID, -1);
        if (sectionId != -1 && mPresenter != null) {
            mPresenter.selectSection(sectionId);
        }
    }

    private void setupFeed() {
        mAdapter = new FeedAdapter(this, new FeedAdapter.Listener() {
            @Override
            public void onVideoClicked(Video video) {
                WatchIntent.start(TouchBrowseActivity.this, video);
            }

            @Override
            public void onVideoLongClicked(Video video) {
                mPresenter.onVideoItemLongClicked(video);
            }

            @Override
            public void onShortsClicked(java.util.List<Video> shorts, int index) {
                ShortsIntent.start(TouchBrowseActivity.this, shorts, index);
            }
        });

        mLayoutManager = new GridLayoutManager(this, 1);
        mLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mAdapter.getSpanSize(position);
            }
        });

        mSettingsAdapter = new SettingsAdapter(item -> {
            if (item.onClick != null) {
                item.onClick.run();
            }
        });

        mRecycler.setLayoutManager(mLayoutManager);
        mRecycler.setAdapter(mAdapter);
        mRecycler.setItemAnimator(null);
        mRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    maybeLoadMore();
                }
            }
        });

        mRefresh.setOnRefreshListener(() -> {
            if (mPresenter != null) {
                mPresenter.refresh();
            }
        });

        mRecycler.post(this::updateGridMetrics);

        mMiniPlayer = findViewById(R.id.li_mini_player);
        mMiniThumb = findViewById(R.id.li_mini_thumb);
        mMiniTitle = findViewById(R.id.li_mini_title);
        mMiniPlay = findViewById(R.id.li_mini_play);
        mMiniPlayer.setOnClickListener(v -> openMiniPlayer());
        mMiniPlay.setOnClickListener(v -> LiNowPlaying.instance().playPause());
        findViewById(R.id.li_mini_close).setOnClickListener(v -> LiNowPlaying.instance().close());
        LiNowPlaying.instance().addListener(mMiniListener);
        updateMiniPlayer();
    }

    private void openMiniPlayer() {
        Intent intent = new Intent(this, TouchWatchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void updateMiniPlayer() {
        if (mMiniPlayer == null) {
            return;
        }

        LiNowPlaying now = LiNowPlaying.instance();
        if (!now.isActive() || now.getVideo() == null) {
            mMiniPlayer.setVisibility(View.GONE);
            return;
        }

        mMiniPlayer.setVisibility(View.VISIBLE);
        mMiniTitle.setText(now.getVideo().getTitle());
        mMiniPlay.setImageResource(now.isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);

        if (now.getVideo().cardImageUrl != null) {
            Glide.with(this).load(now.getVideo().cardImageUrl).centerCrop().into(mMiniThumb);
        }
    }

    private void updateGridMetrics() {
        if (mRecycler == null) {
            return;
        }

        int width = mRecycler.getWidth() - mRecycler.getPaddingStart() - mRecycler.getPaddingEnd();
        if (width <= 0) {
            return;
        }

        mAdapter.setContentWidth(width);

        int columns = Math.max(1, (int) (width / mDensity / MIN_CARD_WIDTH_DP));
        mAdapter.setSpanCount(columns);

        if (mLayoutManager.getSpanCount() != columns) {
            mLayoutManager.setSpanCount(columns);
        }
    }

    private void maybeLoadMore() {
        if (mAdapter == null || mPresenter == null) {
            return;
        }

        int last = mLayoutManager.findLastVisibleItemPosition();
        if (last < mAdapter.getItemCount() - 3) {
            return;
        }

        Video video = mAdapter.getLastPlainVideo();
        if (video == null) {
            return;
        }

        String key = (video.videoId != null ? video.videoId : video.title) + "#" + video.groupPosition;
        if (key.equals(mLastScrollEndKey)) {
            return;
        }
        mLastScrollEndKey = key;
        mPresenter.onScrollEnd(video);
    }

    @Override
    protected void initTheme() {
        setTheme(R.style.LiTheme);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LiMetrics.applyCarDensity(this);
        updateMiniPlayer();
        if (mPresenter != null) {
            mPresenter.onViewResumed();
        }
    }

    @Override
    protected void onPause() {
        if (mPresenter != null) {
            mPresenter.onViewPaused();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        LiNowPlaying.instance().removeListener(mMiniListener);
        if (mPresenter != null) {
            mPresenter.onViewDestroyed();
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LiMetrics.applyCarDensity(this);
        if (mRecycler != null) {
            mRecycler.post(this::updateGridMetrics);
        }
    }

    // ----- BrowseView -------------------------------------------------------

    @Override
    public void addSection(int index, BrowseSection section) {
        if (mRail == null || section == null) {
            return;
        }
        mRail.addItem(index, section.getId(), section.getTitle());
    }

    @Override
    public void removeSection(BrowseSection section) {
        if (mRail == null || section == null) {
            return;
        }
        mRail.removeItem(section.getId());

        if (section.getId() == mCurrentSectionId) {
            mCurrentSectionId = -1;
            mAdapter.clear();
            showHint(getString(R.string.li_shell_hint));
        }
    }

    @Override
    public void removeAllSections() {
        if (mRail != null) {
            mRail.clear();
        }
        mCurrentSectionId = -1;
        if (mAdapter != null) {
            mAdapter.clear();
        }
        showHint(getString(R.string.li_shell_hint));
    }

    @Override
    public void selectSection(int index, boolean focusOnContent) {
        if (mRail == null) {
            return;
        }

        mRail.selectIndex(index);

        int sectionId = mRail.idAt(index);
        if (sectionId == -1) {
            return;
        }

        if (sectionId != mCurrentSectionId) {
            mCurrentSectionId = sectionId;
            mLastScrollEndKey = null;
            showFeed();
            mAdapter.clear();
            showHint("Loading…");
        }
        // The TV UI relies on the headers' selection listener for this; touch has to ask.
        if (mPresenter != null) {
            mPresenter.onSectionFocused(sectionId);
        }
    }

    @Override
    public void updateSection(VideoGroup group) {
        if (group == null || group.getSection() == null || mAdapter == null) {
            return;
        }

        int sectionId = group.getSection().getId();
        if (mCurrentSectionId == -1) {
            mCurrentSectionId = sectionId;
        }

        if (sectionId != mCurrentSectionId) {
            return;
        }

        showFeed();

        if (group.getAction() == VideoGroup.ACTION_REPLACE) {
            mLastScrollEndKey = null;
        }

        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            mAdapter.applyGroup(group);
            updateEmptyState();
        });
    }

    @Override
    public void updateSection(SettingsGroup group) {
        if (group == null || group.getCategory() == null || mSettingsAdapter == null) {
            return;
        }
        if (group.getCategory().getId() != mCurrentSectionId) {
            return;
        }

        runOnUiThread(() -> {
            mSettingsAdapter.setItems(group.getItems());
            if (!mSettingsShown) {
                mSettingsShown = true;
                mRecycler.setLayoutManager(new LinearLayoutManager(this));
                mRecycler.setAdapter(mSettingsAdapter);
            }
            if (mHint != null) {
                mHint.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void clearSection(BrowseSection section) {
        if (section != null && section.getId() == mCurrentSectionId) {
            mLastScrollEndKey = null;
            mAdapter.clear();
            showHint("Loading…");
        }
    }

    @Override
    public void selectSectionItem(int index) {
        // M2.1: restore scroll position
    }

    @Override
    public void selectSectionItem(Video item) {
        // M2.1: restore scroll position
    }

    @Override
    public void showError(ErrorFragmentData data) {
        if (data != null) {
            showHint(String.format("%s\n%s", data.getMessage(),
                    data.getActionText() != null ? data.getActionText() : ""));
        }
    }

    @Override
    public void showProgressBar(boolean show) {
        mProgressShowing = show;
        if (mRefresh != null) {
            runOnUiThread(() -> mRefresh.setRefreshing(show));
        }
    }

    @Override
    public boolean isProgressBarShowing() {
        return mProgressShowing;
    }

    @Override
    public void focusOnContent() {
        if (mRecycler != null) {
            mRecycler.scrollToPosition(0);
        }
    }

    @Override
    public boolean isEmpty() {
        return mAdapter == null || mAdapter.getItemCount() == 0;
    }

    @Override
    public void updateBadge() {
        // M3
    }

    // ------------------------------------------------------------------------

    private void showFeed() {
        if (mSettingsShown && mRecycler != null) {
            mSettingsShown = false;
            mRecycler.setLayoutManager(mLayoutManager);
            mRecycler.setAdapter(mAdapter);
        }
    }

    private void updateEmptyState() {
        if (mAdapter != null && mAdapter.getItemCount() > 0) {
            if (mHint != null) {
                mHint.setVisibility(View.GONE);
            }
        } else {
            showHint("Nothing here yet");
        }
    }

    private void showHint(String text) {
        if (mHint != null) {
            mHint.setText(text);
            mHint.setVisibility(View.VISIBLE);
        }
    }

    private int dp(int value) {
        return Math.round(value * mDensity);
    }
}
