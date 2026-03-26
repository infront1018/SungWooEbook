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

/**
 * 개별 도서 카드 어댑터 (가로 RecyclerView용).
 * - Glide override(200, 280)로 메모리 최적화
 * - LinearSnapHelper는 외부(Fragment/Adapter)에서 연결
 */
public class BookHorizontalAdapter extends RecyclerView.Adapter<BookHorizontalAdapter.BookViewHolder> {

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    private final List<Book> books;
    private final OnBookClickListener listener;

    public BookHorizontalAdapter(List<Book> books, OnBookClickListener listener) {
        this.books = books;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book_card, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);

        // 제목 표시
        holder.txtBookNo.setText(book.getTitle() != null ? book.getTitle() : "");

        // Storage full URL을 직접 가져와서 Glide 로드
        String thumbnailUrl = book.getThumbnailUrl();
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            if (holder.itemView.getContext() != null) {
                // 1. 단순 HTTP URL인 경우 그대로 로드
                if (thumbnailUrl.startsWith("http")) {
                    android.util.Log.d("BookAdapter", "Loading HTTP URL: " + thumbnailUrl);
                    Glide.with(holder.itemView.getContext())
                            .load(thumbnailUrl)
                            .override(200, 280)
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.NONE) // 테스트를 위해 일시적으로 캐시 무효화
                            .skipMemoryCache(true)
                            .placeholder(R.drawable.default_thumbnail)
                            .error(R.drawable.default_thumbnail)
                            .into(holder.imgBookCover);
                }
                // 2. Storage 경로 (ebooks/Thumb/...) 또는 gs:// 인 경우 URL 획득 후 로드
                else {
                    android.util.Log.d("BookAdapter", "Loading Storage Path: " + thumbnailUrl);
                    com.google.firebase.storage.StorageReference ref;
                    if (thumbnailUrl.startsWith("gs://")) {
                        ref = com.google.firebase.storage.FirebaseStorage.getInstance().getReferenceFromUrl(thumbnailUrl);
                    } else {
                        ref = com.sungwoobook.ebook.model.EBookFirestoreModule.getInstance()
                                .getStorage().getReference(thumbnailUrl);
                    }

                    // 해당 아이템이 재활용되는지 확인하기 위해 태그 설정
                    holder.imgBookCover.setTag(thumbnailUrl);

                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        android.util.Log.d("BookAdapter", "Download URL for " + thumbnailUrl + " is: " + uri.toString());
                        // Success 콜백 시점에 여전히 이 뷰홀더가 같은 URL을 로드해야 하는지 확인
                        if (thumbnailUrl.equals(holder.imgBookCover.getTag())) {
                            Glide.with(holder.itemView.getContext())
                                    .load(uri)
                                    .override(200, 280)
                                    .centerCrop()
                                    .diskCacheStrategy(DiskCacheStrategy.ALL) // 실배포용 캐시 활성화
                                    .placeholder(R.drawable.default_thumbnail)
                                    .error(R.drawable.default_thumbnail)
                                    .into(holder.imgBookCover);
                        }
                    }).addOnFailureListener(e -> {
                        android.util.Log.e("BookAdapter", "Failed to get URL for: " + thumbnailUrl, e);
                        holder.imgBookCover.setImageResource(R.drawable.default_thumbnail);
                    });
                }
            }
        } else {
            holder.imgBookCover.setImageResource(R.drawable.default_thumbnail);
        }

        holder.itemView.setOnClickListener(v -> {
            int vol = com.sungwoobook.ebook.Fragment.HomeFragment.extractVolumeNo(book);
            if (vol >= 6) {
                android.widget.Toast.makeText(holder.itemView.getContext(), "데이터가 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) listener.onBookClick(book);
        });
    }

    @Override
    public int getItemCount() {
        return books == null ? 0 : books.size();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBookCover;
        TextView txtBookNo;

        BookViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBookCover = itemView.findViewById(R.id.imgBookCover);
            txtBookNo    = itemView.findViewById(R.id.txtBookNo);
        }
    }
}
