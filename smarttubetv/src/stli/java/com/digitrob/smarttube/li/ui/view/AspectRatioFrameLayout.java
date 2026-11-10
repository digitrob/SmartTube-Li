package com.digitrob.smarttube.li.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * FrameLayout that derives its height from its width (16:9 by default). Used so the player
 * keeps the right shape while the watch page scrolls.
 */
public class AspectRatioFrameLayout extends FrameLayout {
    private float mRatio = 16f / 9f;
    private boolean mMatchHeight;

    public AspectRatioFrameLayout(Context context) {
        super(context);
    }

    public AspectRatioFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAspectRatio(float ratio) {
        if (ratio > 0) {
            mRatio = ratio;
            requestLayout();
        }
    }

    /** In fullscreen the container should fill the screen; the player letterboxes the video. */
    public void setMatchHeight(boolean matchHeight) {
        if (mMatchHeight != matchHeight) {
            mMatchHeight = matchHeight;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mMatchHeight) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round(width / mRatio);
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
}
