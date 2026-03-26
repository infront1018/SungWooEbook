package com.sungwoobook.ebook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.model.Book;
import com.sungwoobook.ebook.model.Series;

import java.util.ArrayList;
import java.util.List;

/**
 * 전집 행(Row) 관리 어댑터 (세로 RecyclerView용).
 * - 모든 가로 RV가 RecycledViewPool을 공유 → 메모리 절약
 * - 각 행에 BookHorizontalAdapter 를 연결
 */
public class MainVerticalAdapter extends RecyclerView.Adapter<MainVerticalAdapter.SeriesViewHolder> {

    private final List<SeriesRow> rows;
    private final BookHorizontalAdapter.OnBookClickListener bookClickListener;

    // 모든 가로 RecyclerView가 공유하는 RecycledViewPool
    private final RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();

    public MainVerticalAdapter(List<SeriesRow> rows,
                               BookHorizontalAdapter.OnBookClickListener bookClickListener) {
        this.rows = rows;
        this.bookClickListener = bookClickListener;
    }

    // ── 데이터 구조 ──────────────────────────────────────────────────────────

    public static class SeriesRow {
        public final Series series;
        public final List<Book> books;

        public SeriesRow(Series series, List<Book> books) {
            this.series = series;
            this.books  = books;
        }
    }

    // ── Adapter 구현 ─────────────────────────────────────────────────────────

    @NonNull
    @Override
    public SeriesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_section_with_horizontal, parent, false);
        return new SeriesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeriesViewHolder holder, int position) {
        SeriesRow row = rows.get(position);

        holder.txtSeriesTitle.setText(row.series.getTitle());

        // 가로 RecyclerView 설정
        BookHorizontalAdapter bookAdapter =
                new BookHorizontalAdapter(row.books, bookClickListener);

        holder.recyclerBooks.setLayoutManager(
                new LinearLayoutManager(holder.itemView.getContext(),
                        LinearLayoutManager.HORIZONTAL, false));

        // ✅ RecycledViewPool 공유 (메모리 최적화)
        holder.recyclerBooks.setRecycledViewPool(sharedPool);
        holder.recyclerBooks.setAdapter(bookAdapter);
        holder.recyclerBooks.setHasFixedSize(true);
        holder.recyclerBooks.setNestedScrollingEnabled(false);
    }

    @Override
    public int getItemCount() {
        return rows == null ? 0 : rows.size();
    }

    // ── ViewHolder ───────────────────────────────────────────────────────────

    static class SeriesViewHolder extends RecyclerView.ViewHolder {
        TextView txtSeriesTitle;
        RecyclerView recyclerBooks;

        SeriesViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSeriesTitle = itemView.findViewById(R.id.textSectionTitle);
            recyclerBooks  = itemView.findViewById(R.id.recyclerSection);
        }
    }
}
