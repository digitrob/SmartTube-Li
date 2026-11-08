package com.digitrob.smarttube.li.ui;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.digitrob.smarttube.li.LiMetrics;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.HashMap;
import java.util.Map;

/**
 * First step of the touch UI: app shell (nav rail + top bar) wired to the real
 * {@link BrowsePresenter}. Content rendering (video grid) lands in M2.
 */
public class TouchBrowseActivity extends MotherActivity implements BrowseView {
    private final Map<Integer, Integer> mSectionItemCounts = new HashMap<>();

    private LinearLayout mRailItems;
    private TextView mHint;
    private ProgressBar mProgress;
    private BrowsePresenter mPresenter;
    private float mDensity = 2.25f;
    private boolean mProgressShowing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Upstream normalized density for the TV canvas; switch to the touch canvas before inflating.
        LiMetrics.applyCarDensity(this);
        mDensity = getResources().getDisplayMetrics().density;
        setTheme(R.style.LiTheme);
        setContentView(R.layout.li_touch_browse);

        mRailItems = findViewById(R.id.li_rail_items);
        mHint = findViewById(R.id.li_content_hint);
        mProgress = findViewById(R.id.li_progress);

        findViewById(R.id.li_search).setOnClickListener(v ->
                MessageHelpers.showMessage(this, "Search arrives in M3"));

        mPresenter = BrowsePresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();
    }

    @Override
    protected void initTheme() {
        setTheme(R.style.LiTheme);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // MotherActivity re-applies the TV density on resume; restore the touch canvas.
        LiMetrics.applyCarDensity(this);
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
        if (mPresenter != null) {
            mPresenter.onViewDestroyed();
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LiMetrics.applyCarDensity(this);
    }

    // ----- BrowseView -------------------------------------------------------

    @Override
    public void addSection(int index, BrowseSection section) {
        if (mRailItems == null || section == null) {
            return;
        }

        TextView item = new TextView(this);
        item.setText(section.getTitle());
        item.setSingleLine(true);
        item.setEllipsize(android.text.TextUtils.TruncateAt.END);
        item.setTextSize(TypedValue.COMPLEX_UNIT_PX, 14 * mDensity);
        item.setTextColor(Color.WHITE);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(16), dp(12), dp(12), dp(12));
        item.setBackgroundResource(R.drawable.li_rail_item_bg);
        item.setTag(section.getId());
        item.setOnClickListener(v -> mPresenter.selectSection(section.getId()));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        mRailItems.addView(item, Math.min(index, mRailItems.getChildCount()), lp);

        updateHint(null);
    }

    @Override
    public void removeSection(BrowseSection section) {
        if (mRailItems == null || section == null) {
            return;
        }

        for (int i = 0; i < mRailItems.getChildCount(); i++) {
            View child = mRailItems.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof Integer && (int) tag == section.getId()) {
                mRailItems.removeViewAt(i);
                break;
            }
        }

        mSectionItemCounts.remove(section.getId());
        updateHint(null);
    }

    @Override
    public void removeAllSections() {
        if (mRailItems != null) {
            mRailItems.removeAllViews();
        }
        mSectionItemCounts.clear();
        updateHint(null);
    }

    @Override
    public void selectSection(int index, boolean focusOnContent) {
        if (mRailItems == null) {
            return;
        }
        for (int i = 0; i < mRailItems.getChildCount(); i++) {
            mRailItems.getChildAt(i).setSelected(i == index);
        }
    }

    @Override
    public void updateSection(VideoGroup group) {
        if (group == null || group.getSection() == null) {
            return;
        }
        mSectionItemCounts.put(group.getSection().getId(), group.getVideos().size());
        updateHint(group.getTitle());
    }

    @Override
    public void updateSection(SettingsGroup group) {
        if (group == null || group.getCategory() == null) {
            return;
        }
        mSectionItemCounts.put(group.getCategory().getId(), group.getItems().size());
        updateHint(group.getTitle());
    }

    @Override
    public void clearSection(BrowseSection section) {
        if (section != null) {
            mSectionItemCounts.remove(section.getId());
        }
        updateHint(null);
    }

    @Override
    public void selectSectionItem(int index) {
        // M2: scroll the grid to the item.
    }

    @Override
    public void selectSectionItem(Video item) {
        // M2: scroll the grid to the item.
    }

    @Override
    public void showError(ErrorFragmentData data) {
        if (data != null && mHint != null) {
            mHint.setText(String.format("Error: %s", data.getMessage()));
        }
    }

    @Override
    public void showProgressBar(boolean show) {
        mProgressShowing = show;
        if (mProgress != null) {
            mProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean isProgressBarShowing() {
        return mProgressShowing;
    }

    @Override
    public void focusOnContent() {
        // M2
    }

    @Override
    public boolean isEmpty() {
        return mRailItems == null || mRailItems.getChildCount() == 0;
    }

    @Override
    public void updateBadge() {
        // M3
    }

    // ------------------------------------------------------------------------

    private void updateHint(String title) {
        if (mHint == null) {
            return;
        }

        int sections = mRailItems != null ? mRailItems.getChildCount() : 0;
        int items = 0;
        for (Integer count : mSectionItemCounts.values()) {
            items += count;
        }

        String suffix = title != null ? " · " + title : "";
        mHint.setText(String.format("Touch UI shell · %d sections · %d items%s", sections, items, suffix));
    }

    private int dp(int value) {
        return Math.round(value * mDensity);
    }
}
