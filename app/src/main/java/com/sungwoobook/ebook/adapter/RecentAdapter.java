/**
 * 📌 파일 경로: com.sungwoobook.ebook.adapter.RecentAdapter.java
 * 📌 설명: 최근 이용한 콘텐츠용 가로 리사이클러뷰 어댑터
 */

package com.sungwoobook.ebook.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sungwoobook.ebook.Model.ContentModel;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.Viewer.PdfViewerActivity;
import com.sungwoobook.ebook.Viewer.VideoViewerActivity;
import com.sungwoobook.ebook.dialog.ContentChoiceDialog;

import java.util.List;

public class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.RecentViewHolder> {

    private List<ContentModel> recentList;
    private OnItemClickListener listener; // ✅ 클릭 리스너 인터페이스 추가

    // ✅ 클릭 이벤트 전달용 인터페이스
    public interface OnItemClickListener {
        void onRecentItemClicked(ContentModel item);
    }

    public RecentAdapter(List<ContentModel> recentList, OnItemClickListener listener) {
        this.recentList = recentList;
        this.listener = listener;

        // ❌ 정렬은 HomeFragment에서 관리하도록 변경
        // Collections.reverse(this.recentList);
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
                .load(content.getThumbnailUrl())
                .placeholder(R.drawable.default_thumbnail) // 기본 썸네일
                .error(R.drawable.default_thumbnail)
                .into(holder.imageThumbnail);

        // ✅ 아이템 클릭 시 책/영상 다이얼로그 표시 + 클릭 콜백 전달
        holder.itemView.setOnClickListener(v -> {
            ContentChoiceDialog.show(v.getContext(), content);

            // ✅ 클릭한 콘텐츠를 HomeFragment에 전달
            if (listener != null) {
                listener.onRecentItemClicked(content);
            }
        });
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
