package com.digitrob.smarttube.li.ui.search;

import com.digitrob.smarttube.li.ui.LiWindow;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.digitrob.smarttube.li.LiMetrics;
import com.digitrob.smarttube.li.ui.browse.FeedAdapter;
import com.digitrob.smarttube.li.ui.playback.WatchIntent;
import com.digitrob.smarttube.li.ui.shorts.ShortsIntent;
import com.digitrob.smarttube.li.ui.shell.LiSections;
import com.digitrob.smarttube.li.ui.shell.NavRailController;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.MediaServiceSearchTagProvider;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SearchView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Touch search: real IME field + 3-column results grid. Results come from {@link SearchPresenter}.
 */
public class TouchSearchActivity extends MotherActivity implements SearchView {

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private EditText mInput;
    private TextView mHint;
    private RecyclerView mRecycler;
    private SwipeRefreshLayout mRefresh;
    private FeedAdapter mAdapter;
    private SearchPresenter mPresenter;
    private float mDensity = 2.25f;
    private NavRailController mRail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LiMetrics.applyCarDensity(this);
        mDensity = getResources().getDisplayMetrics().density;
        setTheme(R.style.LiTheme);
        setContentView(R.layout.li_search);
        LiWindow.applyImmersive(this);
        // MotherActivity.onCreate already armed a dim timer; cancel it before blocking
        // further scheduling, otherwise the black overlay still appears after 10 s.
        getScreensaverManager().disable();
        getScreensaverManager().setBlocked(true);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mInput = findViewById(R.id.li_search_input);
        mHint = findViewById(R.id.li_search_hint);
        mRecycler = findViewById(R.id.li_search_results);
        mRefresh = findViewById(R.id.li_search_refresh);

        mRail = new NavRailController(this, sectionId -> LiSections.open(this, sectionId));
        mRail.setItems(LiSections.defaults());

        setupResults();
        setupInput();

        mPresenter = SearchPresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();

        mInput.requestFocus();
        mHandler.postDelayed(this::showKeyboard, 250);
    }

    private void setupResults() {
        mAdapter = new FeedAdapter(this, new FeedAdapter.Listener() {
            @Override
            public void onVideoClicked(Video video) {
                WatchIntent.start(TouchSearchActivity.this, video);
            }

            @Override
            public void onVideoLongClicked(Video video) {
                mPresenter.onVideoItemLongClicked(video);
            }

            @Override
            public void onShortsClicked(java.util.List<Video> shorts, int index) {
                ShortsIntent.start(TouchSearchActivity.this, shorts, index);
            }
        });

        mAdapter.setListMode(true);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(mAdapter);
        mRecycler.setItemAnimator(null);

        mRefresh.setOnRefreshListener(() -> {
            String query = mInput.getText().toString().trim();
            if (!query.isEmpty()) {
                mPresenter.onSearch(query);
            }
        });
    }

    private void setupInput() {
        findViewById(R.id.li_search_back).setOnClickListener(v -> finish());
        findViewById(R.id.li_search_clear).setOnClickListener(v -> {
            mInput.setText("");
            mAdapter.clear();
            mHint.setVisibility(View.VISIBLE);
            mInput.requestFocus();
        });

        mInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_NULL;
            boolean isEnter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;

            if (isSearchAction || isEnter) {
                runSearch();
                return true;
            }
            return false;
        });

        mInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                runSearch();
                return true;
            }
            return false;
        });
    }

    private void runSearch() {
        String query = mInput.getText().toString().trim();
        if (query.isEmpty()) {
            return;
        }
        hideKeyboard();
        mAdapter.clear();
        mHint.setVisibility(View.VISIBLE);
        mPresenter.onSearch(query);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(mInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
        }
    }

    @Override
    protected void initTheme() {
        setTheme(R.style.LiTheme);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LiMetrics.applyCarDensity(this);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        if (mPresenter != null) {
            mPresenter.onViewDestroyed();
        }
        super.onDestroy();
    }

    // ----- SearchView -------------------------------------------------------

    @Override
    public void updateSearch(VideoGroup group) {
        if (group == null) {
            return;
        }
        runOnUiThread(() -> {
            mAdapter.applySearchGroup(group);
            if (mAdapter.getItemCount() > 0) {
                mHint.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void clearSearch() {
        runOnUiThread(() -> mAdapter.clear());
    }

    @Override
    public void clearSearchTags() {
    }

    @Override
    public void removeSearchTag(Tag tag) {
    }

    @Override
    public void setTagsProvider(MediaServiceSearchTagProvider provider) {
        // Search history/tag chips are a later refinement.
    }

    @Override
    public void showProgressBar(boolean show) {
        runOnUiThread(() -> mRefresh.setRefreshing(show));
    }

    @Override
    public void startSearch(String searchText) {
        if (searchText != null) {
            mInput.setText(searchText);
            mInput.setSelection(searchText.length());
            mPresenter.onSearch(searchText);
        }
    }

    @Override
    public String getSearchText() {
        return mInput.getText().toString();
    }

    @Override
    public void startVoiceRecognition() {
        MessageHelpers.showMessage(this, "Voice search is not available in the car UI");
    }

    @Override
    public void finishReally() {
        finish();
    }
}
