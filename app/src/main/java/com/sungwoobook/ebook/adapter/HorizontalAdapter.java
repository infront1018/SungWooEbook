/**
 * 📌 파일 경로: com.sungwoobook.ebook.adapter.HorizontalAdapter.java
 * 📌 설명: 가로 리사이클러뷰 어댑터 (썸네일 + 목차)
 */

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

public class HorizontalAdapter extends RecyclerView.Adapter<HorizontalAdapter.HorizontalViewHolder> {

    private final ContentModel contentModel;

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
        holder.txtTitle.setText("목차 " + (position + 1)); // 임시 목차 이름

        Glide.with(holder.itemView.getContext())
                .load(contentModel.getThumbnailUrl())
                .error(R.drawable.default_thumbnail)
                .into(holder.imgThumbnail);
    }

    @Override
    public int getItemCount() {
        return 5; // 목차 5개 (데모용)
    }

    public static class HorizontalViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle;
        ImageView imgThumbnail;

        public HorizontalViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);        // ✅ item_horizontal.xml에 존재해야 함
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail); // ✅ item_horizontal.xml에 존재해야 함
        }
    }
}