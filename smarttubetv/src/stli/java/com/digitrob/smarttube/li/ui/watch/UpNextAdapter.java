package com.digitrob.smarttube.li.ui.watch;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.digitrob.smarttube.li.ui.view.AspectRatioImageView;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/** Compact "Up next" list shown on the right of the watch page. */
public class UpNextAdapter extends RecyclerView.Adapter<UpNextAdapter.Holder> {
    public interface Listener {
        void onVideoClicked(Video video);
    }

    private final Listener mListener;
    private final List<Video> mItems = new ArrayList<>();
    private final int mCornerRadiusPx;
    private final ColorDrawable mPlaceholder;

    public UpNextAdapter(Context context, Listener listener) {
        mListener = listener;
        mCornerRadiusPx = Math.round(8 * context.getResources().getDisplayMetrics().density);
        mPlaceholder = new ColorDrawable(ContextCompat.getColor(context, R.color.li_surface_high));
    }

    public void setItems(List<Video> videos) {
        mItems.clear();
        if (videos != null) {
            mItems.addAll(videos);
        }
        notifyDataSetChanged();
    }

    public List<Video> getItems() {
        return mItems;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.li_item_upnext, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Video video = mItems.get(position);

        holder.title.setText(video.getTitle());
        holder.meta.setText(video.getSecondTitle());

        RequestOptions options = new RequestOptions()
                .transform(new MultiTransformation<>(new CenterCrop(), new RoundedCorners(mCornerRadiusPx)))
                .placeholder(mPlaceholder)
                .error(mPlaceholder);

        if (video.cardImageUrl != null && !video.cardImageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(video.cardImageUrl)
                    .apply(options)
                    .into(holder.thumb);
        } else {
            holder.thumb.setImageDrawable(mPlaceholder);
        }

        holder.itemView.setOnClickListener(v -> mListener.onVideoClicked(video));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView title;
        final TextView meta;

        Holder(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.li_up_next_thumb);
            title = itemView.findViewById(R.id.li_up_next_title);
            meta = itemView.findViewById(R.id.li_up_next_meta);
            ((AspectRatioImageView) thumb).setAspectRatio(AspectRatioImageView.RATIO_16_9);
        }
    }
}
