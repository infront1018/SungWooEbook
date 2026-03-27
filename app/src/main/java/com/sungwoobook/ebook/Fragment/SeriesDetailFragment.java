package com.sungwoobook.ebook.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.adapter.BookGridAdapter;
import com.sungwoobook.ebook.dialog.PreviewBottomSheet;
import com.sungwoobook.ebook.model.Book;
import com.sungwoobook.ebook.model.FirebaseManager;

import java.util.ArrayList;
import java.util.List;

public class SeriesDetailFragment extends Fragment {

    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_SERIES_TITLE = "series_title";

    private String categoryId;
    private String seriesTitle;

    private BookGridAdapter adapter;
    private final List<Book> bookList = new ArrayList<>();

    public static SeriesDetailFragment newInstance(String categoryId, String seriesTitle) {
        SeriesDetailFragment fragment = new SeriesDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_ID, categoryId);
        args.putString(ARG_SERIES_TITLE, seriesTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryId = getArguments().getString(ARG_CATEGORY_ID);
            seriesTitle = getArguments().getString(ARG_SERIES_TITLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_series_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 상단 타이틀바 문구를 '브랜드관'으로 변경
        if (getActivity() instanceof com.sungwoobook.ebook.MainActivity) {
            ((com.sungwoobook.ebook.MainActivity) getActivity()).updateTitle("브랜드관");
        }

        TextView txtTitle = view.findViewById(R.id.txtSeriesTitle);
        txtTitle.setText(seriesTitle);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerBooks);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new BookGridAdapter(bookList, this::showPreviewSheet);
        recyclerView.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        FirebaseManager.getInstance().getBooksByCategory(categoryId,
                books -> {
                    bookList.clear();
                    if (books != null) {
                        bookList.addAll(books);
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                },
                e -> {
                    android.util.Log.e("SeriesDetail", "Failed to load books", e);
                }
        );
    }

    private void showPreviewSheet(Book book) {
        // 미리보기 권수 제한 (01~05권만 제공)
        int vol = com.sungwoobook.ebook.Fragment.BaseGalleryFragment.extractVolumeNo(book);
        if (vol >= 1 && vol <= 5) {
            PreviewBottomSheet sheet = PreviewBottomSheet.newInstance(book.getBookId(), book.getTitle(), vol, seriesTitle, book.getThumbnailUrl(), book.getBookUrl());
            sheet.show(getChildFragmentManager(), "PreviewBottomSheet");
        } else {
            android.widget.Toast.makeText(getContext(), "미리보기 자료는 01~05권만 제공됩니다", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
