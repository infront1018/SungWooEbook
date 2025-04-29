package com.sungwoobook.ebook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sungwoobook.ebook.Model.ContentModel;
import com.sungwoobook.ebook.R;

import java.util.List;

/**
 * 📌 파일 경로: com.sungwoobook.ebook.adapter.VerticalAdapter.java
 * 📌 설명: 세로 리사이클러뷰 어댑터 (섹션별 타이틀 + 가로 리스트)
 */
public class VerticalAdapter extends RecyclerView.Adapter<VerticalAdapter.VerticalViewHolder> {

    private List<ContentModel> verticalList;

    public VerticalAdapter(List<ContentModel> verticalList) {
        this.verticalList = verticalList;
    }

    @NonNull
    @Override
    public VerticalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vertical, parent, false);
        return new VerticalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VerticalViewHolder holder, int position) {
        ContentModel content = verticalList.get(position);

        holder.textSectionTitle.setText(content.getTitle()); // 책 제목 = 섹션 제목

        // 가로 리사이클러뷰 연결
        holder.recyclerHorizontal.setLayoutManager(
                new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        holder.recyclerHorizontal.setAdapter(new HorizontalAdapter(content));
    }

    @Override
    public int getItemCount() {
        return verticalList.size();
    }

    static class VerticalViewHolder extends RecyclerView.ViewHolder {
        TextView textSectionTitle;
        RecyclerView recyclerHorizontal;

        public VerticalViewHolder(@NonNull View itemView) {
            super(itemView);
            textSectionTitle = itemView.findViewById(R.id.textSectionTitle);
            recyclerHorizontal = itemView.findViewById(R.id.recyclerHorizontal);
        }
    }
}
