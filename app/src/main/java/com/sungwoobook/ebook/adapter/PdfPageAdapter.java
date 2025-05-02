package com.sungwoobook.ebook.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.view.ZoomImageView;
import com.sungwoobook.ebook.view.ZoomStateListener;

import java.util.List;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageViewHolder> {

    private final List<Bitmap> pages;
    private final ZoomStateListener zoomStateListener;

    public PdfPageAdapter(List<Bitmap> pages, ZoomStateListener listener) {
        this.pages = pages;
        this.zoomStateListener = listener;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.imageView.setImageBitmap(pages.get(position));
        holder.imageView.setZoomListener(zoomStateListener);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    public void resetZoomAt(int position) {
        notifyItemChanged(position);
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        ZoomImageView imageView;

        public PageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.pdfPageImage);
        }
    }
}
