/**
 * 📌 파일 경로: com.sungwoobook.ebook.adapter.AllContentAdapter.java
 * 📌 설명: 전체 콘텐츠를 가로 리사이클러뷰로 표시하는 어댑터
 */
package com.sungwoobook.ebook.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sungwoobook.ebook.Model.ContentModel;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.Viewer.PdfViewerActivity;
import com.sungwoobook.ebook.Viewer.VideoViewerActivity;

import java.util.List;

public class AllContentAdapter extends RecyclerView.Adapter<AllContentAdapter.ViewHolder> {

    private List<ContentModel> itemList;

    public AllContentAdapter(List<ContentModel> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public AllContentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_horizontal, parent, false);
        return new AllContentAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AllContentAdapter.ViewHolder holder, int position) {
        ContentModel item = itemList.get(position);
        holder.title.setText(item.getTitle());

        Glide.with(holder.itemView.getContext())
                .load(item.getThumbnailUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.thumbnail);

        holder.itemView.setOnClickListener(v -> showDialog(v.getContext(), item));
    }

    private void showDialog(Context context, ContentModel item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_content_choice, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        Button btnBook = dialogView.findViewById(R.id.btnBook);
        Button btnVideo = dialogView.findViewById(R.id.btnVideo);

        btnBook.setOnClickListener(v -> {
            if (item.getBookUrl() != null && !item.getBookUrl().isEmpty()) {
                Intent intent = new Intent(context, PdfViewerActivity.class);
                intent.putExtra("pdfUrl", item.getBookUrl());
                context.startActivity(intent);
                dialog.dismiss();
            } else {
                Toast.makeText(context, "책 URL이 없습니다", Toast.LENGTH_SHORT).show();
            }
        });

        btnVideo.setOnClickListener(v -> {
            if (item.getVideoUrl() != null && !item.getVideoUrl().isEmpty()) {
                Intent intent = new Intent(context, VideoViewerActivity.class);
                intent.putExtra("videoUrl", item.getVideoUrl());
                context.startActivity(intent);
                dialog.dismiss();
            } else {
                Toast.makeText(context, "영상 URL이 없습니다", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.imgThumbnail);
            title = itemView.findViewById(R.id.txtTitle);
        }
    }
}