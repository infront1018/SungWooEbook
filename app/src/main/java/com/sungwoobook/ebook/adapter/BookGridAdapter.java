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

import java.util.List;

public class BookGridAdapter extends RecyclerView.Adapter<BookGridAdapter.BookViewHolder> {

    private final List<Book> books;
    private final OnBookClickListener listener;

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    // URL 캐시 (상세 화면에서도 중복 Storage 호출 방지)
    private static final java.util.Map<String, android.net.Uri> urlCache = new java.util.concurrent.ConcurrentHashMap<>();

    public BookGridAdapter(List<Book> books, OnBookClickListener listener) {
        this.books = books;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book_grid, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        
        holder.txtTitle.setText(book.getTitle());
        
        String thumbnailUrl = book.getThumbnailUrl();
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            if (thumbnailUrl.startsWith("http")) {
                Glide.with(holder.imgCover.getContext())
                        .load(thumbnailUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.imgCover);
            } else {
                if (urlCache.containsKey(thumbnailUrl)) {
                    Glide.with(holder.imgCover.getContext())
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
                            Glide.with(holder.imgCover.getContext())
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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onBookClick(book);
        });
    }

    @Override
    public int getItemCount() {
        return books == null ? 0 : books.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView txtTitle;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imageBookCover);
            txtTitle = itemView.findViewById(R.id.textBookTitle);
        }
    }
}
