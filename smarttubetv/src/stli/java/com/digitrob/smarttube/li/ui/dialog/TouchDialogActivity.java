package com.digitrob.smarttube.li.ui.dialog;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.digitrob.smarttube.li.LiMetrics;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Touch renderer for upstream's {@link AppDialogView}.
 *
 * <p>Upstream UI (settings, long-press menus, player menus) is described as a list of
 * {@link OptionCategory}; this activity renders every category type as a touch row, so all of
 * those menus work without the Leanback dialog.</p>
 */
public class TouchDialogActivity extends MotherActivity implements AppDialogView {
    private static final class Page {
        final List<OptionCategory> categories;
        final CharSequence title;
        final boolean isExpandable;
        final boolean isTransparent;
        final boolean isOverlay;
        final int id;

        Page(List<OptionCategory> categories, CharSequence title, boolean isExpandable,
             boolean isTransparent, boolean isOverlay, int id) {
            this.categories = categories;
            this.title = title;
            this.isExpandable = isExpandable;
            this.isTransparent = isTransparent;
            this.isOverlay = isOverlay;
            this.id = id;
        }
    }

    private static final class BoundRow {
        final OptionItem item;
        final TextView check;
        final int type;

        BoundRow(OptionItem item, TextView check, int type) {
            this.item = item;
            this.check = check;
            this.type = type;
        }
    }

    private final Deque<Page> mBackStack = new ArrayDeque<>();
    private final List<BoundRow> mBoundRows = new ArrayList<>();

    private AppDialogPresenter mPresenter;
    private LinearLayout mContent;
    private TextView mTitle;
    private TextView mClose;
    private List<OptionCategory> mCategories;
    private CharSequence mCurrentTitle;
    private int mId;
    private boolean mExpandable;
    private boolean mTransparent;
    private boolean mOverlay;
    private boolean mPaused;
    private boolean mFinishNotified;
    private float mDensity = 2.25f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LiMetrics.applyCarDensity(this);
        mDensity = getResources().getDisplayMetrics().density;
        setTheme(R.style.LiDialogTheme);
        setContentView(R.layout.li_dialog);

        mContent = findViewById(R.id.li_dialog_content);
        mTitle = findViewById(R.id.li_dialog_title);
        mClose = findViewById(R.id.li_dialog_close);
        mClose.setOnClickListener(v -> finish());

        mPresenter = AppDialogPresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();
    }

    @Override
    protected void initTheme() {
        setTheme(R.style.LiDialogTheme);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LiMetrics.applyCarDensity(this);
        mPaused = false;
    }

    @Override
    protected void onPause() {
        mPaused = true;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mPresenter != null && mPresenter.getView() == this) {
            mPresenter.onViewDestroyed();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    @Override
    public void finish() {
        notifyFinish();
        super.finish();
    }

    @Override
    public void goBack() {
        if (!mBackStack.isEmpty()) {
            Page page = mBackStack.pop();
            applyPage(page);
        } else {
            finish();
        }
    }

    @Override
    public void clearBackstack() {
        mBackStack.clear();
    }

    @Override
    public boolean canGoBack() {
        return !mBackStack.isEmpty();
    }

    @Override
    public boolean isShown() {
        return !isFinishing();
    }

    @Override
    public boolean isTransparent() {
        return mTransparent;
    }

    @Override
    public boolean isOverlay() {
        return mOverlay;
    }

    @Override
    public boolean isPaused() {
        return mPaused;
    }

    @Override
    public int getViewId() {
        return mId;
    }

    @Override
    public void show(List<OptionCategory> categories, CharSequence title, boolean isExpandable,
                     boolean isTransparent, boolean isOverlay, int id) {
        if (mCategories != null && categories != mCategories) {
            mBackStack.push(new Page(mCategories, mCurrentTitle, mExpandable, mTransparent, mOverlay, mId));
        }

        mCategories = categories;
        mCurrentTitle = title;
        mExpandable = isExpandable;
        mTransparent = isTransparent;
        mOverlay = isOverlay;
        mId = id;

        render(categories, title);
    }

    private void applyPage(Page page) {
        mCategories = page.categories;
        mCurrentTitle = page.title;
        mExpandable = page.isExpandable;
        mTransparent = page.isTransparent;
        mOverlay = page.isOverlay;
        mId = page.id;
        render(page.categories, page.title);
    }

    private void render(List<OptionCategory> categories, CharSequence title) {
        if (mContent == null) {
            return;
        }

        mBoundRows.clear();
        mContent.removeAllViews();

        mTitle.setText(title != null ? title : "");
        mTitle.setVisibility(title != null && !TextUtils.isEmpty(title) ? View.VISIBLE : View.GONE);

        if (categories == null) {
            return;
        }

        for (OptionCategory category : categories) {
            if (category == null) {
                continue;
            }

            if (category.title != null && !TextUtils.isEmpty(category.title)) {
                mContent.addView(createHeader(category.title));
            }

            switch (category.type) {
                case OptionCategory.TYPE_LONG_TEXT:
                    addLongText(category);
                    break;
                case OptionCategory.TYPE_CHAT:
                case OptionCategory.TYPE_COMMENTS:
                    addPlaceholder(category.title);
                    break;
                case OptionCategory.TYPE_SINGLE_SWITCH:
                case OptionCategory.TYPE_SINGLE_BUTTON:
                case OptionCategory.TYPE_STRING_LIST:
                case OptionCategory.TYPE_RADIO_LIST:
                case OptionCategory.TYPE_CHECKBOX_LIST:
                default:
                    for (OptionItem item : category.options) {
                        if (item != null) {
                            addOptionRow(item, category.type, category.options);
                        }
                    }
                    break;
            }
        }

        updateChecks();
    }

    private TextView createHeader(CharSequence title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextColor(getColorCompat(R.color.li_text_dim));
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        header.setPadding(dp(8), dp(20), dp(8), dp(6));
        return header;
    }

    private void addOptionRow(OptionItem item, int type, List<OptionItem> siblings) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(dp(8), dp(8), dp(8), dp(8));
        row.setBackgroundResource(R.drawable.li_rail_item_bg);
        row.setClickable(true);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText(item.getTitle());
        title.setTextColor(getColorCompat(R.color.li_text));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        texts.addView(title);

        if (item.getDescription() != null && !TextUtils.isEmpty(item.getDescription())) {
            TextView description = new TextView(this);
            description.setText(item.getDescription());
            description.setTextColor(getColorCompat(R.color.li_text_dim));
            description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            description.setPadding(0, dp(2), 0, 0);
            texts.addView(description);
        }

        TextView check = new TextView(this);
        check.setTextColor(getColorCompat(R.color.li_accent));
        check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        check.setGravity(Gravity.CENTER);
        check.setMinWidth(dp(40));

        row.addView(texts);
        row.addView(check);

        row.setOnClickListener(v -> {
            if (type == OptionCategory.TYPE_RADIO_LIST) {
                // The Leanback fragment enforces radio exclusivity; the touch sheet must too.
                for (OptionItem sibling : siblings) {
                    if (sibling != null && sibling != item && sibling.isSelected()) {
                        sibling.onSelect(false);
                    }
                }
                item.onSelect(true);
            } else if (type == OptionCategory.TYPE_CHECKBOX_LIST || type == OptionCategory.TYPE_SINGLE_SWITCH) {
                item.onSelect(!item.isSelected());
            } else {
                item.onSelect(true);
            }
            updateChecks();
        });

        mBoundRows.add(new BoundRow(item, check, type));
        mContent.addView(row);
    }

    private void addLongText(OptionCategory category) {
        for (OptionItem item : category.options) {
            if (item == null) {
                continue;
            }
            TextView text = new TextView(this);
            text.setText(item.getTitle());
            text.setTextColor(getColorCompat(R.color.li_text_dim));
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            text.setPadding(dp(8), dp(12), dp(8), dp(12));
            mContent.addView(text);
        }
    }

    private void addPlaceholder(CharSequence title) {
        TextView text = new TextView(this);
        text.setText("Not available in the touch UI yet");
        text.setTextColor(getColorCompat(R.color.li_text_dim));
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        text.setPadding(dp(8), dp(12), dp(8), dp(12));
        mContent.addView(text);
    }

    private void updateChecks() {
        for (BoundRow row : mBoundRows) {
            if (row.type == OptionCategory.TYPE_SINGLE_SWITCH) {
                row.check.setText(row.item.isSelected() ? "On" : "Off");
            } else {
                row.check.setText(row.item.isSelected() ? "✓" : "");
            }
        }
    }

    private void notifyFinish() {
        if (mFinishNotified) {
            return;
        }
        mFinishNotified = true;
        if (mPresenter != null) {
            mPresenter.onFinish();
        }
    }

    private int getColorCompat(int resId) {
        return androidx.core.content.ContextCompat.getColor(this, resId);
    }

    private int dp(int value) {
        return Math.round(value * mDensity);
    }
}
