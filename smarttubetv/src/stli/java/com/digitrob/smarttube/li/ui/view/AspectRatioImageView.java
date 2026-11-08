package com.digitrob.smarttube.li.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * ImageView that keeps a fixed width/height ratio. Used for 16:9 thumbnails and 9:16 Shorts
 * cards so the grid can be laid out from the column width alone.
 */
public class AspectRatioImageView extends ImageView {
    public static final float RATIO_16_9 = 16f / 9f;
    public static final float RATIO_9_16 = 9f / 16f;

    private float mRatio = RATIO_16_9;

    public AspectRatioImageView(Context context) {
        super(context);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAspectRatio(float ratio) {
        if (ratio <= 0) {
            return;
        }
        mRatio = ratio;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        if (width > 0) {
            setMeasuredDimension(width, Math.round(width / mRatio));
        }
    }
}
