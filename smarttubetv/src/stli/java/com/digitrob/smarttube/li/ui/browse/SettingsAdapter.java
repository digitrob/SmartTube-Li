package com.digitrob.smarttube.li.ui.browse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/** Touch rows for the Settings section (upstream {@code SettingsGroup}). */
public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.Holder> {
    public interface Listener {
        void onSettingClicked(SettingsItem item);
    }

    private final List<SettingsItem> mItems = new ArrayList<>();
    private final Listener mListener;

    public SettingsAdapter(Listener listener) {
        mListener = listener;
    }

    public void setItems(List<SettingsItem> items) {
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
                .inflate(R.layout.li_item_setting, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SettingsItem item = mItems.get(position);
        holder.title.setText(item.title);

        if (item.imageResId > 0) {
            holder.icon.setImageResource(item.imageResId);
            holder.icon.setVisibility(View.VISIBLE);
        } else {
            holder.icon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> mListener.onSettingClicked(item));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;

        Holder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.li_setting_icon);
            title = itemView.findViewById(R.id.li_setting_title);
        }
    }
}
