package com.sungwoobook.ebook.Fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.firestore.DocumentSnapshot;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.adapter.BannerAdapter;
import com.sungwoobook.ebook.adapter.BookHorizontalAdapter;
import com.sungwoobook.ebook.adapter.MainVerticalAdapter;
import com.sungwoobook.ebook.dialog.PreviewBottomSheet;
import com.sungwoobook.ebook.model.Book;
import com.sungwoobook.ebook.model.FirebaseManager;
import com.sungwoobook.ebook.model.Series;

import java.util.ArrayList;
import java.util.List;

/**
 * 홈 화면 Fragment.
 * - 상단 자동 슬라이딩 배너 (ViewPager2)
 * - 최근 이용한 전집 퀵 리스트 (Horizontal RV)
 * - 전집 8종 중첩 RecyclerView (Vertical + Horizontal)
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // ── 전집 리스트 (중첩 RV) ─────────────────────────────────────────────────
    private RecyclerView           recyclerSeries;
    private MainVerticalAdapter    verticalAdapter;
    private final List<MainVerticalAdapter.SeriesRow> seriesRows = new ArrayList<>();

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerSeries  = view.findViewById(R.id.recyclerAllContents);

        setupSeriesRV();
        loadAllSeries();
    }



    // ── 전집 중첩 리스트 ───────────────────────────────────────────────────────

    private void setupSeriesRV() {
        verticalAdapter = new MainVerticalAdapter(seriesRows, this::showPreviewSheet);
        recyclerSeries.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerSeries.setNestedScrollingEnabled(false);
        recyclerSeries.setAdapter(verticalAdapter);
    }

    private void loadAllSeries() {
        FirebaseManager.getInstance().getAllBooks(
                books -> {
                    seriesRows.clear();

                    if (books == null || books.isEmpty()) {
                        // books가 비어있어도 더미 데이터를 표시하기 위해 return 생략 (혹은 초기화)
                        books = new java.util.ArrayList<>();
                    }

                    // 🛠️ 임시 UI 스크롤 테스트용 더미 데이터 생성 기능 (배포 시 삭제/false 처리 요망)
                    boolean ENABLE_DUMMY_DATA = true; 
                    if (ENABLE_DUMMY_DATA) {
                        for (int i = 1; i <= 5; i++) {
                            for (int j = 1; j <= 20; j++) {
                                Book dummy = new Book();
                                dummy.setBookId("TEST_" + i + "_" + j);
                                dummy.setTitle("샘플 전집 " + i + " " + j + "권");
                                
                                // 파이어베이스에서 불러온 실제 도서가 있다면, 표지(썸네일) 이미지를 순환해서 적용
                                if (!books.isEmpty()) {
                                    dummy.setThumbnailUrl(books.get((i + j) % books.size()).getThumbnailUrl());
                                }
                                books.add(dummy);
                            }
                        }
                    }

                    // 1. 책들을 전집 이름별로 그룹화
                    java.util.Map<String, java.util.List<Book>> groupedFiles = new java.util.HashMap<>();
                    for (Book b : books) {
                        String seriesName = extractSeriesName(b);
                        if (!groupedFiles.containsKey(seriesName)) {
                            groupedFiles.put(seriesName, new java.util.ArrayList<>());
                        }
                        groupedFiles.get(seriesName).add(b);
                    }

                    // 2. 그룹화된 전집들을 SeriesRow로 묶고 권수 기준으로 수평 정렬
                    for (java.util.Map.Entry<String, java.util.List<Book>> entry : groupedFiles.entrySet()) {
                        java.util.List<Book> seriesBooks = entry.getValue();

                        // 🔴 권수 기준 오름차순 정렬 (1, 2, 3...)
                        java.util.Collections.sort(seriesBooks, (b1, b2) -> Integer.compare(extractVolumeNo(b1), extractVolumeNo(b2)));

                        Series series = new Series();
                        series.setTitle(entry.getKey());
                        seriesRows.add(new MainVerticalAdapter.SeriesRow(series, seriesBooks));
                    }

                    // 3. 전집 자체도 이름순 정렬
                    java.util.Collections.sort(seriesRows, (r1, r2) -> r1.series.getTitle().compareTo(r2.series.getTitle()));

                    verticalAdapter.notifyDataSetChanged();
                },
                e -> {
                    Log.e(TAG, "전체 도서 로딩 실패", e);
                    Toast.makeText(getContext(), "데이터 로딩 실패", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private String extractSeriesName(Book book) {
        if (book.getTitle() != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.*?)\\s*\\d+권").matcher(book.getTitle());
            if (m.find()) return m.group(1).trim();
        }
        
        String id = book.getBookId();
        if (id != null && id.contains("_")) {
            return id.split("_")[0] + " 전집";
        }
        return "기타 전집";
    }

    public static int extractVolumeNo(Book book) {
        String id = book.getBookId();
        if (id != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(".*?_(\\d+)").matcher(id);
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
            }
        }
        if (book.getTitle() != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)권").matcher(book.getTitle());
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
            }
        }
        return 999;
    }

    // ── 프리뷰 BottomSheet ────────────────────────────────────────────────────

    private void showPreviewSheet(Book book) {
        // last_accessed 업데이트 (seriesId가 세팅된 경우)
        if (book.getSeriesId() != null && !book.getSeriesId().isEmpty()) {
            FirebaseManager.getInstance().updateLastAccessed(book.getSeriesId());
        }

        // 시리즈 제목 찾기
        String seriesTitle = "";
        for (MainVerticalAdapter.SeriesRow row : seriesRows) {
            for (Book b : row.books) {
                if (b.getBookId() != null && b.getBookId().equals(book.getBookId())) {
                    seriesTitle = row.series.getTitle();
                    break;
                }
            }
        }

        PreviewBottomSheet sheet = PreviewBottomSheet.newInstance(book, seriesTitle);
        sheet.show(getChildFragmentManager(), "PreviewBottomSheet");
    }

    // ── onDestroyView ─────────────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
