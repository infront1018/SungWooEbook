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

/**
 * 📌 파일 경로: com.sungwoobook.ebook.adapter.HorizontalAdapter.java
 * 📌 설명: 가로 리사이클러뷰 어댑터 (썸네일 + 목차)
 */
public class HorizontalAdapter extends RecyclerView.Adapter<HorizontalAdapter.HorizontalViewHolder> {

    private ContentModel contentModel;

    public HorizontalAdapter(ContentModel contentModel) {
        this.contentModel = contentModel;
    }

    @NonNull
    @Override
    public HorizontalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_horizontal, parent, false);
        return new HorizontalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HorizontalViewHolder holder, int position) {
        holder.textChapter.setText("목차 " + (position + 1)); // 목차 이름 (데모)

        Glide.with(holder.itemView.getContext())
                .load(contentModel.getUrl())
                .placeholder(R.drawable.default_thumbnail)
                .error(R.drawable.default_thumbnail)
                .into(holder.imageChapterThumbnail);
    }

    @Override
    public int getItemCount() {
        return 5; // 목차 5개 임시 설정 (추후 실제 목차 리스트 연결)
    }

    static class HorizontalViewHolder extends RecyclerView.ViewHolder {
        TextView textChapter;
        ImageView imageChapterThumbnail;

        public HorizontalViewHolder(@NonNull View itemView) {
            super(itemView);
            textChapter = itemView.findViewById(R.id.textChapter);
            imageChapterThumbnail = itemView.findViewById(R.id.imageChapterThumbnail);
        }
    }
}
