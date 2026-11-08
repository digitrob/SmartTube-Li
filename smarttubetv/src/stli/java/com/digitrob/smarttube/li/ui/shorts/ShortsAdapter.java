package com.digitrob.smarttube.li.ui.shorts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/** One full-screen page per Short. The video surface lives behind the pager. */
public class ShortsAdapter extends RecyclerView.Adapter<ShortsAdapter.Holder> {
    public interface Listener {
        void onTap();

        void onLike(Video video);

        void onDislike(Video video);

        void onShare(Video video);
    }

    private final List<Video> mItems = new ArrayList<>();
    private final Listener mListener;

    public ShortsAdapter(Listener listener) {
        mListener = listener;
    }

    public void setItems(List<Video> items) {
        mItems.clear();
        if (items != null) {
            mItems.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.li_shorts_page, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Video video = mItems.get(position);

        holder.title.setText(video.getTitle());
        holder.author.setText(video.getAuthor());

        holder.itemView.setOnClickListener(v -> mListener.onTap());
        holder.like.setOnClickListener(v -> mListener.onLike(video));
        holder.dislike.setOnClickListener(v -> mListener.onDislike(video));
        holder.share.setOnClickListener(v -> mListener.onShare(video));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView author;
        final TextView like;
        final TextView dislike;
        final TextView share;

        Holder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.li_short_page_title);
            author = itemView.findViewById(R.id.li_short_page_author);
            like = itemView.findViewById(R.id.li_short_page_like);
            dislike = itemView.findViewById(R.id.li_short_page_dislike);
            share = itemView.findViewById(R.id.li_short_page_share);
        }
    }
}
