package com.sungwoobook.ebook.adapter;

import static com.google.android.exoplayer2.util.Log.w;

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

import java.util.List;

public class AllContentAdapter extends RecyclerView.Adapter<AllContentAdapter.ViewHolder> {

    private List<ContentModel> itemList;
    private OnContentClickListener listener;

    // ✅ 인터페이스 추가
    public interface OnContentClickListener {
        void onContentClicked(ContentModel item);
    }

    public AllContentAdapter(List<ContentModel> itemList, OnContentClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
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

        holder.itemView.setOnClickListener(v -> {
            showDialog(v.getContext(), item);

            // ✅ 클릭된 아이템을 리스너로 전달
            if (listener != null) listener.onContentClicked(item);
        });
    }

    // ✅ 책/영상 선택 다이얼로그 메서드
    private void showDialog(Context context, ContentModel item) {
        // ✅ context가 유효한 Activity인지 확인 + 종료 상태 체크
        if (!(context instanceof AppCompatActivity)) return;
        AppCompatActivity activity = (AppCompatActivity) context;
        if (activity.isFinishing() || activity.isDestroyed()) {
            Log.e("Dialog", "Activity가 종료되어 Dialog를 띄울 수 없습니다.");
            return;
        }

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
                try {
                    context.startActivity(intent);
                } catch (Exception e) {
                    Log.e("Dialog", "startActivity 실패", e);
                }
                dialog.dismiss();
            } else {
                Toast.makeText(context, "책 URL이 없습니다", Toast.LENGTH_SHORT).show();
            }
        });

        btnVideo.setOnClickListener(v -> {
            if (item.getVideoUrl() != null && !item.getVideoUrl().isEmpty()) {
                Intent intent = new Intent(context, VideoViewerActivity.class);
                intent.putExtra("videoUrl", item.getVideoUrl());
                try {
                    context.startActivity(intent);
                } catch (Exception e) {
                    w("Dialog", "startActivity 실패", e);
                }
                dialog.dismiss();
            } else {
                Toast.makeText(context, "영상 URL이 없습니다", Toast.LENGTH_SHORT).show();
            }
        });

        if (!activity.isFinishing() && !activity.isDestroyed()) { // ✅ show 전에 다시 체크
            if (activity.getWindow() != null && activity.getWindow().getDecorView().getWindowToken() != null) {
                dialog.show(); // ✅ 정말 안전한 조건일 때만 show
            } else {
                w("Dialog", "⚠️ WindowToken 없음 → show 생략");
            }
        }
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
