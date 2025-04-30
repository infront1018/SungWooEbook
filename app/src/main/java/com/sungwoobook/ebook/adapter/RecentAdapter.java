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

/**
 * 📌 파일 경로: com.sungwoobook.ebook.adapter.RecentAdapter.java
 * 📌 설명: 최근 이용한 콘텐츠용 가로 리사이클러뷰 어댑터
 */
public class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.RecentViewHolder> {

    private List<ContentModel> recentList;

    public RecentAdapter(List<ContentModel> recentList) {
        this.recentList = recentList;
    }

    @NonNull
    @Override
    public RecentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent, parent, false);
        return new RecentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentViewHolder holder, int position) {
        ContentModel content = recentList.get(position);

        holder.textTitle.setText(content.getTitle());

        Glide.with(holder.itemView.getContext())
                .load(content.getUrl())
                .placeholder(R.drawable.default_thumbnail)  // 기본 썸네일
                .error(R.drawable.default_thumbnail)
                .into(holder.imageThumbnail);
    }

    @Override
    public int getItemCount() {
        return recentList.size();
    }

    static class RecentViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        ImageView imageThumbnail;

        public RecentViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitleRecent);
            imageThumbnail = itemView.findViewById(R.id.imageThumbnailRecent);
        }
    }
}