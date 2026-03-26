package com.sungwoobook.ebook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.storage.StorageReference;

// TODO: 프로젝트에 생성된 ViewBinding 클래스로 변경해주세요. (예: ItemEbookBinding)
// import com.sungwoobook.ebook.databinding.ItemEbookBinding;

import com.sungwoobook.ebook.model.EBook;
import com.sungwoobook.ebook.model.EBookFirestoreModule;

public class EBookAdapter extends ListAdapter<EBook, EBookAdapter.EBookViewHolder> {

    private final OnEBookClickListener clickListener;

    public interface OnEBookClickListener {
        void onEBookClicked(String pdfPath);
    }

    public EBookAdapter(OnEBookClickListener clickListener) {
        super(new EBookDiffCallback());
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public EBookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // TODO: 실제 프로젝트의 ViewBinding을 사용하여 LayoutInflater 연결
        // ItemEbookBinding binding = ItemEbookBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        // return new EBookViewHolder(binding, clickListener);
        return null; // 실제 Binding 코드로 대체 시 제거
    }

    @Override
    public void onBindViewHolder(@NonNull EBookViewHolder holder, int position) {
        EBook ebook = getItem(position);
        holder.bind(ebook);
    }

    static class EBookViewHolder extends RecyclerView.ViewHolder {

        // private final ItemEbookBinding binding;
        private final OnEBookClickListener clickListener;

        // public EBookViewHolder(ItemEbookBinding binding, OnEBookClickListener clickListener) {
        //     super(binding.getRoot());
        //     this.binding = binding;
        //     this.clickListener = clickListener;
        // }
        // 임시 생성자 (빌드 에러 방지용)
        public EBookViewHolder(View view, OnEBookClickListener clickListener) {
            super(view);
            this.clickListener = clickListener;
        }

        public void bind(EBook ebook) {
            // 텍스트 바인딩 예시
            // binding.titleTextView.setText(ebook.getCategoryName() + " " + ebook.getVolume() + "권");

            // StorageReference 생성 (FirebaseStorage Singleton 모듈 활용)
            if (ebook.getThumbPath() != null && !ebook.getThumbPath().isEmpty()) {
                StorageReference storageRef = EBookFirestoreModule.getInstance().getStorage().getReference(ebook.getThumbPath());

                // 참고: StorageReference를 직접 Glide에 넣기 위해서는 FirebaseUI-Storage 라이브러리가 필요합니다.
                /* Glide.with(itemView.getContext())
                        .load(storageRef)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.thumbnailImageView); */
            }

            // 클릭 시 pdfPath 전달
            itemView.setOnClickListener(v -> {
                if (clickListener != null && ebook.getPdfPath() != null) {
                    clickListener.onEBookClicked(ebook.getPdfPath());
                }
            });
        }
    }

    static class EBookDiffCallback extends DiffUtil.ItemCallback<EBook> {
        @Override
        public boolean areItemsTheSame(@NonNull EBook oldItem, @NonNull EBook newItem) {
            // Document ID가 서버에 있으면 사용, 없으면 PDF 경로 등의 고유값 사용
            if (oldItem.getDocumentId() != null && newItem.getDocumentId() != null) {
                return oldItem.getDocumentId().equals(newItem.getDocumentId());
            }
            return oldItem.getPdfPath().equals(newItem.getPdfPath());
        }

        @Override
        public boolean areContentsTheSame(@NonNull EBook oldItem, @NonNull EBook newItem) {
            return oldItem.getVolume() == newItem.getVolume() &&
                   oldItem.getCategoryName().equals(newItem.getCategoryName());
        }
    }
}
