package com.digitrob.smarttube.li.ui.browse;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.digitrob.smarttube.li.ui.view.AspectRatioImageView;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Touch feed: one vertical RecyclerView with full-span headers/shelves and 1-span video cards.
 * The span count is chosen from the available width (min 3 columns, ~300 dp per card), so the
 * same adapter scales from the 16:9 headunit to wider screens.
 */
public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_VIDEO = 1;
    private static final int TYPE_SHELF = 2;
    private static final int TYPE_VIDEO_ROW = 3;

    private static final int MIN_CARD_WIDTH_DP = 300;
    private static final int MAX_SHORT_COLUMNS = 6;

    public interface Listener {
        void onVideoClicked(Video video);

        void onVideoLongClicked(Video video);

        void onShortsClicked(java.util.List<Video> shorts, int index);
    }

    private static final class Group {
        int sectionId;
        String title;
        List<Video> videos = new ArrayList<>();
        boolean shorts;
    }

    private static final class HeaderRow {
        String title;

        HeaderRow(String title) {
            this.title = title;
        }
    }

    private static final class ShelfRow {
        List<Video> videos;

        ShelfRow(List<Video> videos) {
            this.videos = videos;
        }
    }

    private final Context mContext;
    private final Listener mListener;
    private final List<Group> mGroups = new ArrayList<>();
    private final List<Object> mRows = new ArrayList<>();
    private final int mCornerRadiusPx;
    private final ColorDrawable mPlaceholder;

    private int mSpanCount = 3;
    private int mContentWidthPx;
    private boolean mListMode;

    public FeedAdapter(Context context, Listener listener) {
        mContext = context;
        mListener = listener;
        mCornerRadiusPx = Math.round(12 * context.getResources().getDisplayMetrics().density);
        mPlaceholder = new ColorDrawable(ContextCompat.getColor(context, R.color.li_surface_high));
    }

    public void setSpanCount(int spanCount) {
        if (spanCount > 0) {
            mSpanCount = spanCount;
        }
    }

    public void setContentWidth(int widthPx) {
        mContentWidthPx = widthPx;
    }

    /** Search results use a full-width row instead of the grid card. */
    public void setListMode(boolean listMode) {
        if (mListMode != listMode) {
            mListMode = listMode;
            notifyDataSetChanged();
        }
    }

    public void clear() {
        mGroups.clear();
        mRows.clear();
        notifyDataSetChanged();
    }

    public void applyGroup(VideoGroup group) {
        if (group == null || group.getSection() == null) {
            return;
        }
        applyGroupInternal(group, group.getSection().getId(),
                group.getTitle() != null ? group.getTitle().toString() : null);
    }

    /** Search results are a flat list: no section, no group headers. */
    public void applySearchGroup(VideoGroup group) {
        if (group == null) {
            return;
        }
        applyGroupInternal(group, -1, null);
    }

    private void applyGroupInternal(VideoGroup group, int sectionId, String title) {
        int action = group.getAction();

        if (action == VideoGroup.ACTION_REPLACE) {
            removeGroups(sectionId, null);
        }

        if (action == VideoGroup.ACTION_REMOVE || action == VideoGroup.ACTION_REMOVE_AUTHOR) {
            removeGroups(sectionId, title);
            rebuildRows();
            return;
        }

        Group target = findGroup(sectionId, title);
        if (target == null) {
            target = new Group();
            target.sectionId = sectionId;
            target.title = title;
            mGroups.add(target);
        }

        List<Video> incoming = filter(group.getVideos());

        if (action == VideoGroup.ACTION_APPEND && !target.videos.isEmpty()) {
            target.videos.addAll(incoming);
        } else {
            target.videos = new ArrayList<>(incoming);
        }

        target.shorts = isShortsGroup(target.videos);

        if (target.videos.isEmpty()) {
            mGroups.remove(target);
        }

        rebuildRows();
    }

    /** Last non-Shorts video, used as the pagination anchor for BrowsePresenter.onScrollEnd(). */
    public Video getLastPlainVideo() {
        for (int i = mGroups.size() - 1; i >= 0; i--) {
            Group group = mGroups.get(i);
            if (!group.shorts && !group.videos.isEmpty()) {
                return group.videos.get(group.videos.size() - 1);
            }
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= mRows.size()) {
            return TYPE_VIDEO;
        }
        Object row = mRows.get(position);
        if (row instanceof HeaderRow) {
            return TYPE_HEADER;
        }
        if (row instanceof ShelfRow) {
            return TYPE_SHELF;
        }
        return mListMode ? TYPE_VIDEO_ROW : TYPE_VIDEO;
    }

    public int getSpanSize(int position) {
        return getItemViewType(position) == TYPE_VIDEO ? 1 : mSpanCount;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_HEADER:
                return new HeaderHolder(inflater.inflate(R.layout.li_item_header, parent, false));
            case TYPE_SHELF:
                return new ShelfHolder(inflater.inflate(R.layout.li_item_shorts_shelf, parent, false));
            case TYPE_VIDEO_ROW:
                return new VideoHolder(inflater.inflate(R.layout.li_item_video_row, parent, false));
            default:
                return new VideoHolder(inflater.inflate(R.layout.li_item_video_card, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object row = mRows.get(position);

        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).title.setText(((HeaderRow) row).title);
        } else if (holder instanceof VideoHolder) {
            bindVideo((VideoHolder) holder, (Video) row);
        } else if (holder instanceof ShelfHolder) {
            bindShelf((ShelfHolder) holder, (ShelfRow) row);
        }
    }

    @Override
    public int getItemCount() {
        return mRows.size();
    }

    private void bindVideo(VideoHolder holder, Video video) {
        holder.title.setText(video.getTitle());
        holder.meta.setText(video.getSecondTitle());

        String author = video.getAuthor();
        holder.avatar.setText(author != null && !author.isEmpty()
                ? author.substring(0, 1).toUpperCase() : "•");

        String badge = null;
        if (video.hasNewContent) {
            badge = mContext.getString(R.string.badge_new_content);
        } else if (video.isLive) {
            badge = mContext.getString(R.string.badge_live);
        } else if (video.badge != null && !video.badge.isEmpty()) {
            badge = video.badge;
        }

        if (badge != null) {
            holder.badge.setText(badge);
            holder.badge.setVisibility(View.VISIBLE);
        } else {
            holder.badge.setVisibility(View.GONE);
        }

        int progress = video.percentWatched > 0 && video.percentWatched < 1
                ? 1 : Math.round(video.percentWatched);
        if (progress > 0) {
            holder.progress.setProgress(Math.min(progress, 100));
            holder.progress.setVisibility(View.VISIBLE);
        } else {
            holder.progress.setVisibility(View.GONE);
        }

        loadThumbnail(holder.thumb, video.cardImageUrl, AspectRatioImageView.RATIO_16_9);

        holder.itemView.setOnClickListener(v -> mListener.onVideoClicked(video));
        holder.itemView.setOnLongClickListener(v -> {
            mListener.onVideoLongClicked(video);
            return true;
        });
        holder.more.setOnClickListener(v -> mListener.onVideoLongClicked(video));
    }

    private void bindShelf(ShelfHolder holder, ShelfRow row) {
        int width = mContentWidthPx > 0 ? mContentWidthPx : mContext.getResources().getDisplayMetrics().widthPixels;
        int gap = dp(8);
        int cardWidth = Math.max(dp(120), (width - gap * (MAX_SHORT_COLUMNS - 1)) / MAX_SHORT_COLUMNS);

        ShortAdapter adapter = new ShortAdapter(row.videos, cardWidth);
        holder.recycler.setAdapter(adapter);

        int height = Math.round(cardWidth * 16f / 9f) + dp(52);
        ViewGroup.LayoutParams lp = holder.recycler.getLayoutParams();
        lp.height = height;
        holder.recycler.setLayoutParams(lp);
    }

    private void loadThumbnail(ImageView imageView, String url, float ratio) {
        if (imageView instanceof AspectRatioImageView) {
            ((AspectRatioImageView) imageView).setAspectRatio(ratio);
        }

        if (url == null || url.isEmpty()) {
            imageView.setImageDrawable(mPlaceholder);
            return;
        }

        RequestOptions options = new RequestOptions()
                .transform(new MultiTransformation<>(new CenterCrop(), new RoundedCorners(mCornerRadiusPx)))
                .placeholder(mPlaceholder)
                .error(mPlaceholder);

        Glide.with(mContext).load(url).apply(options).into(imageView);
    }

    private void rebuildRows() {
        mRows.clear();

        for (Group group : mGroups) {
            if (group.videos.isEmpty()) {
                continue;
            }

            if (group.title != null && !group.title.isEmpty() && mGroups.size() > 1) {
                mRows.add(new HeaderRow(group.title));
            }

            if (group.shorts) {
                mRows.add(new ShelfRow(group.videos));
            } else {
                mRows.addAll(group.videos);
            }
        }

        notifyDataSetChanged();
    }

    private Group findGroup(int sectionId, String title) {
        for (Group group : mGroups) {
            if (group.sectionId == sectionId && (title == null || title.equals(group.title))) {
                return group;
            }
        }
        return null;
    }

    private void removeGroups(int sectionId, String title) {
        for (int i = mGroups.size() - 1; i >= 0; i--) {
            Group group = mGroups.get(i);
            if (group.sectionId == sectionId && (title == null || title.equals(group.title))) {
                mGroups.remove(i);
            }
        }
    }

    private static List<Video> filter(List<Video> videos) {
        if (videos == null) {
            return Collections.emptyList();
        }

        List<Video> result = new ArrayList<>(videos.size());
        for (Video video : videos) {
            if (video != null && !video.isContinuation()) {
                result.add(video);
            }
        }
        return result;
    }

    private static boolean isShortsGroup(List<Video> videos) {
        if (videos.isEmpty()) {
            return false;
        }
        int shorts = 0;
        for (Video video : videos) {
            if (video.isShorts) {
                shorts++;
            }
        }
        return shorts * 2 > videos.size();
    }

    private int dp(int value) {
        return Math.round(value * mContext.getResources().getDisplayMetrics().density);
    }

    // ----- holders ----------------------------------------------------------

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.li_header_title);
        }
    }

    static final class VideoHolder extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView title;
        final TextView meta;
        final TextView badge;
        final TextView avatar;
        final ImageButton more;
        final ProgressBar progress;

        VideoHolder(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.li_card_thumb);
            title = itemView.findViewById(R.id.li_card_title);
            meta = itemView.findViewById(R.id.li_card_meta);
            badge = itemView.findViewById(R.id.li_card_badge);
            avatar = itemView.findViewById(R.id.li_card_avatar);
            more = itemView.findViewById(R.id.li_card_more);
            progress = itemView.findViewById(R.id.li_card_progress);
        }
    }

    static final class ShelfHolder extends RecyclerView.ViewHolder {
        final RecyclerView recycler;

        ShelfHolder(View itemView) {
            super(itemView);
            recycler = (RecyclerView) itemView;
            recycler.setNestedScrollingEnabled(false);
            recycler.setLayoutManager(new LinearLayoutManager(itemView.getContext(), RecyclerView.HORIZONTAL, false));
        }
    }

    private final class ShortAdapter extends RecyclerView.Adapter<ShortHolder> {
        private final List<Video> mVideos;
        private final int mCardWidth;

        ShortAdapter(List<Video> videos, int cardWidth) {
            mVideos = videos;
            mCardWidth = cardWidth;
        }

        @NonNull
        @Override
        public ShortHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.li_item_short_card, parent, false);
            view.getLayoutParams().width = mCardWidth;
            return new ShortHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ShortHolder holder, int position) {
            Video video = mVideos.get(position);
            holder.title.setText(video.getTitle());
            loadThumbnail(holder.thumb, video.cardImageUrl, AspectRatioImageView.RATIO_9_16);

            holder.itemView.setOnClickListener(v ->
                    mListener.onShortsClicked(mVideos, holder.getAdapterPosition()));
            holder.itemView.setOnLongClickListener(v -> {
                mListener.onVideoLongClicked(video);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return mVideos.size();
        }
    }

    static final class ShortHolder extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView title;

        ShortHolder(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.li_short_thumb);
            title = itemView.findViewById(R.id.li_short_title);
        }
    }
}
