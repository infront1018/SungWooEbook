package com.sungwoobook.ebook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.model.Book;
import com.sungwoobook.ebook.util.ButtonEffectUtil;

import java.util.List;

public class SeriesAdapter extends RecyclerView.Adapter<SeriesAdapter.SeriesViewHolder> {

    private final List<SeriesItem> items;
    private final OnSeriesClickListener listener;

    public interface OnSeriesClickListener {
        void onSeriesClick(SeriesItem item);
    }
    
    // URL 캐시 (동일한 썸네일 경로에 대해 반복적인 Storage 호출 방지)
    private static final java.util.Map<String, android.net.Uri> urlCache = new java.util.concurrent.ConcurrentHashMap<>();

    public static class SeriesItem {
        public final String categoryId;
        public final String title;
        public final int bookCount;
        public final String thumbnailUrl;

        public SeriesItem(String categoryId, String title, int bookCount, String thumbnailUrl) {
            this.categoryId = categoryId;
            this.title = title;
            this.bookCount = bookCount;
            this.thumbnailUrl = thumbnailUrl;
        }
    }

    public SeriesAdapter(List<SeriesItem> items, OnSeriesClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SeriesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_series_card, parent, false);
        return new SeriesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeriesViewHolder holder, int position) {
        SeriesItem item = items.get(position);
        holder.txtTitle.setText(item.title);
        holder.txtCount.setText("총 " + item.bookCount + "권");

        String thumbnailUrl = item.thumbnailUrl;
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            if (thumbnailUrl.startsWith("http")) {
                Glide.with(holder.itemView.getContext())
                        .load(thumbnailUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.imgCover);
            } else {
                // 캐시 확인
                if (urlCache.containsKey(thumbnailUrl)) {
                    Glide.with(holder.itemView)
                            .load(urlCache.get(thumbnailUrl))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.imgCover);
                } else {
                    holder.imgCover.setTag(thumbnailUrl);
                    com.google.firebase.storage.StorageReference ref;
                    if (thumbnailUrl.startsWith("gs://")) {
                        ref = com.google.firebase.storage.FirebaseStorage.getInstance().getReferenceFromUrl(thumbnailUrl);
                    } else {
                        ref = com.google.firebase.storage.FirebaseStorage.getInstance().getReference().child(thumbnailUrl);
                    }

                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        urlCache.put(thumbnailUrl, uri);
                        if (thumbnailUrl.equals(holder.imgCover.getTag())) {
                            Glide.with(holder.itemView)
                                    .load(uri)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(holder.imgCover);
                        }
                    }).addOnFailureListener(e -> {
                        holder.imgCover.setImageDrawable(null);
                    });
                }
            }
        } else {
            holder.imgCover.setImageDrawable(null);
        }

        // 🚀 카드 Push 효과
        ButtonEffectUtil.applyWithClick(holder.itemView, 0.96f);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSeriesClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class SeriesViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView txtTitle;
        TextView txtCount;

        SeriesViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgSeriesCover);
            txtTitle = itemView.findViewById(R.id.txtSeriesTitle);
            txtCount = itemView.findViewById(R.id.txtBookCount);
        }
    }
}
