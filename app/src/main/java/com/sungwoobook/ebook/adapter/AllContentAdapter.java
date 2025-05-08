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
import com.sungwoobook.ebook.dialog.ContentChoiceDialog;

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
            ContentChoiceDialog.show(v.getContext(), item);

            // ✅ 클릭된 아이템을 리스너로 전달
            if (listener != null) listener.onContentClicked(item);
        });
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
