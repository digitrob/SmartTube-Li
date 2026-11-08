package com.digitrob.smarttube.li.ui.shell;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.digitrob.smarttube.li.LiPrefs;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * YouTube-style navigation rail: icon + label, 240 dp expanded, 72 dp collapsed.
 * Shared by browse and search so the sidebar stays put outside playback.
 *
 * <p>Rows are stored by index, not by section id: upstream may insert a pinned copy of a
 * built-in section, so ids are not guaranteed to be unique and the presenter addresses
 * sections by index.</p>
 */
public class NavRailController {
    public interface Listener {
        void onSectionClicked(int sectionId);
    }

    private static final int EXPANDED_DP = 240;
    private static final int COLLAPSED_DP = 72;

    private final Context mContext;
    private final View mRoot;
    private final LinearLayout mItems;
    private final TextView mLogo;
    private final ImageButton mToggle;
    private final Listener mListener;
    private final List<Row> mRows = new ArrayList<>();

    private boolean mCollapsed;

    private static final class Row {
        final int sectionId;
        final LinearLayout container;
        final TextView label;
        final ImageView icon;

        Row(int sectionId, LinearLayout container, TextView label, ImageView icon) {
            this.sectionId = sectionId;
            this.container = container;
            this.label = label;
            this.icon = icon;
        }
    }

    public NavRailController(Activity activity, Listener listener) {
        mContext = activity;
        mListener = listener;

        mRoot = activity.findViewById(R.id.li_rail_root);
        mItems = activity.findViewById(R.id.li_rail_items);
        mLogo = activity.findViewById(R.id.li_rail_logo);
        mToggle = activity.findViewById(R.id.li_rail_toggle);

        mCollapsed = LiPrefs.instance(activity).isRailCollapsed();

        if (mToggle != null) {
            mToggle.setOnClickListener(v -> setCollapsed(!mCollapsed));
        }

        applyCollapsed();
    }

    public void setItems(List<LiSections.Entry> entries) {
        clear();
        for (LiSections.Entry entry : entries) {
            addItem(mRows.size(), entry.id, entry.title);
        }
    }

    public void addItem(int index, int sectionId, CharSequence title) {
        if (mItems == null) {
            return;
        }

        LinearLayout row = new LinearLayout(mContext);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(48));
        row.setBackgroundResource(R.drawable.li_rail_item_bg);
        row.setClickable(true);

        ImageView icon = new ImageView(mContext);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        icon.setLayoutParams(iconParams);
        icon.setImageResource(LiSections.iconFor(sectionId));
        icon.setColorFilter(Color.WHITE);

        TextView label = new TextView(mContext);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.setMarginStart(dp(24));
        label.setLayoutParams(labelParams);
        label.setText(title);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);

        row.addView(icon);
        row.addView(label);

        row.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onSectionClicked(sectionId);
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int insertAt = Math.min(Math.max(index, 0), mItems.getChildCount());
        mItems.addView(row, insertAt, params);

        Row railRow = new Row(sectionId, row, label, icon);
        mRows.add(insertAt, railRow);
        applyRowStyle(railRow);
    }

    public void removeItem(int sectionId) {
        for (int i = 0; i < mRows.size(); i++) {
            Row row = mRows.get(i);
            if (row.sectionId == sectionId) {
                mRows.remove(i);
                if (mItems != null) {
                    mItems.removeView(row.container);
                }
                return;
            }
        }
    }

    public void clear() {
        mRows.clear();
        if (mItems != null) {
            mItems.removeAllViews();
        }
    }

    public int size() {
        return mRows.size();
    }

    public int idAt(int index) {
        return index >= 0 && index < mRows.size() ? mRows.get(index).sectionId : -1;
    }

    public int indexOf(int sectionId) {
        for (int i = 0; i < mRows.size(); i++) {
            if (mRows.get(i).sectionId == sectionId) {
                return i;
            }
        }
        return -1;
    }

    public void selectIndex(int index) {
        for (int i = 0; i < mRows.size(); i++) {
            mRows.get(i).container.setSelected(i == index);
        }
    }

    public void selectId(int sectionId) {
        selectIndex(indexOf(sectionId));
    }

    public boolean isCollapsed() {
        return mCollapsed;
    }

    public void setCollapsed(boolean collapsed) {
        mCollapsed = collapsed;
        LiPrefs.instance(mContext).setRailCollapsed(collapsed);
        applyCollapsed();
    }

    private void applyCollapsed() {
        if (mRoot == null) {
            return;
        }

        View.OnClickListener toggle = v -> setCollapsed(!mCollapsed);

        mRoot.getLayoutParams().width = dp(mCollapsed ? COLLAPSED_DP : EXPANDED_DP);
        mRoot.requestLayout();

        if (mLogo != null) {
            mLogo.setVisibility(mCollapsed ? View.GONE : View.VISIBLE);
        }
        if (mToggle != null) {
            mToggle.setOnClickListener(toggle);
        }

        for (Row row : mRows) {
            applyRowStyle(row);
        }
    }

    private void applyRowStyle(Row row) {
        if (row == null) {
            return;
        }

        row.label.setVisibility(mCollapsed ? View.GONE : View.VISIBLE);
        row.container.setGravity(mCollapsed ? Gravity.CENTER : Gravity.CENTER_VERTICAL);
        row.container.setPaddingRelative(
                mCollapsed ? 0 : dp(16),
                0,
                mCollapsed ? 0 : dp(12),
                0);
        LinearLayout.LayoutParams iconParams =
                (LinearLayout.LayoutParams) row.icon.getLayoutParams();
        iconParams.setMarginStart(0);
        row.icon.setLayoutParams(iconParams);
    }

    private int dp(int value) {
        return Math.round(value * mContext.getResources().getDisplayMetrics().density);
    }
}
