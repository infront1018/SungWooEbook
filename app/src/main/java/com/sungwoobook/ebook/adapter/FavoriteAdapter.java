package com.sungwoobook.ebook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sungwoobook.ebook.Model.ContentModel;
import com.sungwoobook.ebook.R;

import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ContentModel content);
    }

    private final List<ContentModel> items;
    private final OnItemClickListener listener;

    public FavoriteAdapter(List<ContentModel> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_content, parent, false);  // 기존 콘텐츠용 XML 재활용
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContentModel content = items.get(position);
        holder.textTitle.setText(content.getTitle());

        Glide.with(holder.itemView.getContext())
                .load(content.getThumbnailUrl())
                .placeholder(R.drawable.default_thumbnail)
                .into(holder.imageThumbnail);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(content));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        ImageView imageThumbnail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_title);
            imageThumbnail = itemView.findViewById(R.id.imageThumbnail);
        }
    }
}
