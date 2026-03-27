package com.sungwoobook.ebook.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sungwoobook.ebook.MainActivity;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.adapter.AgeGroupAdapter;
import com.sungwoobook.ebook.adapter.SeriesAdapter;
import com.sungwoobook.ebook.dialog.PreviewBottomSheet;
import com.sungwoobook.ebook.model.Book;
import com.sungwoobook.ebook.model.FavoriteManager;
import com.sungwoobook.ebook.model.FirebaseManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 전집 목록(홈, 즐겨찾기)의 공통 로직을 담당하는 베이스 프래그먼트.
 */
public abstract class BaseGalleryFragment extends Fragment {

    protected final String TAG = getClass().getSimpleName();

    protected RecyclerView recyclerAll;
    protected AgeGroupAdapter ageGroupAdapter;
    protected final List<AgeGroupAdapter.AgeGroupSection> ageGroupSections = new ArrayList<>();
    
    protected boolean isFavoriteMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutResourceId(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerAll = view.findViewById(R.id.recyclerAllContents);
        
        setupMainRV();
        loadGalleryData();
        updateTitleAndNav();
    }

    protected abstract int getLayoutResourceId();
    protected abstract void updateTitleAndNav();

    protected void setupMainRV() {
        if (recyclerAll == null) return;
        ageGroupAdapter = new AgeGroupAdapter(ageGroupSections, this::onSeriesClicked);
        recyclerAll.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerAll.setAdapter(ageGroupAdapter);
    }

    public void loadGalleryData() {
        Log.d(TAG, "loadGalleryData called. isFavoriteMode=" + isFavoriteMode);
        FirebaseManager.getInstance().getAllBooks(
                books -> {
                    if (books == null || books.isEmpty()) {
                        Log.w(TAG, "No books found!");
                        return;
                    }

                    // 1. 전집별 그룹화 (categoryId 기준)
                    java.util.Map<String, List<Book>> groupedBySeries = new java.util.LinkedHashMap<>();
                    for (Book b : books) {
                        String sid = b.getSeriesId() != null ? b.getSeriesId() : "others";
                        if (!groupedBySeries.containsKey(sid)) {
                            groupedBySeries.put(sid, new ArrayList<>());
                        }
                        groupedBySeries.get(sid).add(b);
                    }

                    // 2. SeriesItem 리스트 생성
                    List<SeriesAdapter.SeriesItem> seriesItems = new ArrayList<>();
                    FavoriteManager favManager = FavoriteManager.getInstance(requireContext());
                    
                    if (isFavoriteMode) {
                        // 즐겨찾기 모드: 모든 개별 즐겨찾기된 도서를 각각 아이템으로 생성
                        for (Book b : books) {
                            if (favManager.isFavorite(b.getBookId())) {
                                String sName = extractSeriesName(b);
                                String displayTitle = sName + " " + (b.getTitle() != null ? b.getTitle() : "");
                                
                                // categoryId에 BOOK:접두사와 함께 도서ID|시리즈명|시리즈ID를 인코딩하여 저장
                                String encodedId = "BOOK:" + b.getBookId() + "|" + sName + "|" + b.getSeriesId();
                                seriesItems.add(new SeriesAdapter.SeriesItem(encodedId, displayTitle, 1, b.getThumbnailUrl()));
                            }
                        }
                    } else {
                        // 일반 모드: 전집 단위 그룹화 노출
                        for (java.util.Map.Entry<String, List<Book>> entry : groupedBySeries.entrySet()) {
                            String categoryId = entry.getKey();
                            List<Book> seriesBooks = entry.getValue();
                            if (seriesBooks.isEmpty()) continue;

                            String title = extractSeriesName(seriesBooks.get(0));
                            Book rep = findRepresentativeBook(seriesBooks, categoryId);
                            String seriesThumb = rep.getThumbnailUrl();
                            
                            seriesItems.add(new SeriesAdapter.SeriesItem(categoryId, title, seriesBooks.size(), seriesThumb));
                        }
                    }

                    // 3. 연령대별 그룹화 (공통 매핑 로직)
                    groupAndDisplay(seriesItems);
                },
                e -> Log.e(TAG, "데이터 로딩 실패", e)
        );
    }

    protected void groupAndDisplay(List<SeriesAdapter.SeriesItem> seriesItems) {
        ageGroupSections.clear();
        
        List<SeriesAdapter.SeriesItem> age05Items = new ArrayList<>();
        List<SeriesAdapter.SeriesItem> age59Items = new ArrayList<>();
        List<SeriesAdapter.SeriesItem> age913Items = new ArrayList<>();

        addItemByKeyword(seriesItems, "릴라팝", age05Items);
        addItemByKeyword(seriesItems, "꿈틀이첫과학", age05Items);
        
        addItemByKeyword(seriesItems, "꼬마과학뒤집기", age59Items);
        addItemByKeyword(seriesItems, "꼬마사회뒤집기", age59Items);
        addItemByKeyword(seriesItems, "꼬마수학뒤집기", age59Items);

        addItemByKeyword(seriesItems, "사회뒤집기기본", age913Items);
        addItemByKeyword(seriesItems, "통합과학뒤집기", age913Items);
        addItemByKeyword(seriesItems, "과학뒤집기완성", age913Items);

        for (SeriesAdapter.SeriesItem item : seriesItems) {
            if (!age05Items.contains(item) && !age59Items.contains(item) && !age913Items.contains(item)) {
                age913Items.add(item);
            }
        }

        if (!age05Items.isEmpty()) ageGroupSections.add(new AgeGroupAdapter.AgeGroupSection("영유아 (0~5세)", age05Items));
        if (!age59Items.isEmpty()) ageGroupSections.add(new AgeGroupAdapter.AgeGroupSection("유치 (5~9세)", age59Items));
        if (!age913Items.isEmpty()) ageGroupSections.add(new AgeGroupAdapter.AgeGroupSection("초등 (9~13세)", age913Items));

        if (ageGroupAdapter != null) ageGroupAdapter.notifyDataSetChanged();
    }

    protected void onSeriesClicked(SeriesAdapter.SeriesItem item) {
        if (item.categoryId.startsWith("BOOK:")) {
            // 개별 도서 즐겨찾기 아이템 클릭 처리
            String raw = item.categoryId.substring(5); // "bookId|seriesName|seriesId"
            String[] parts = raw.split("\\|");
            if (parts.length >= 3) {
                String bookId = parts[0];
                String seriesName = parts[1];
                String seriesId = parts[2];
                
                // 해당 시리즈의 도서 목록을 불러와서 특정 도서 팝업 노출
                FirebaseManager.getInstance().getBooksByCategory(seriesId, books -> {
                    if (books != null) {
                        for (Book b : books) {
                            if (b.getBookId().equals(bookId)) {
                                int vol = extractVolumeNo(b);
                                PreviewBottomSheet sheet = PreviewBottomSheet.newInstance(b.getBookId(), b.getTitle(), vol, seriesName, b.getThumbnailUrl(), b.getBookUrl());
                                sheet.show(getChildFragmentManager(), "PreviewBottomSheet");
                                return;
                            }
                        }
                    }
                }, e -> Log.e(TAG, "Failed to load direct book", e));
            }
            return;
        }

        if (isFavoriteMode) {
            // 이 경로는 이제 하위 호환성이나 예외 처리를 위해 남겨둡니다. 
            // 실제로는 위의 BOOK: 분기에서 대부분 처리됩니다.
            FirebaseManager.getInstance().getBooksByCategory(item.categoryId,
                    books -> {
                        if (books != null && !books.isEmpty()) {
                            // 즐겨찾기된 도서 중 첫 번째를 미리보기 대상으로 선택
                            com.sungwoobook.ebook.model.Book previewBook = books.get(0);
                            for (com.sungwoobook.ebook.model.Book b : books) {
                                if (com.sungwoobook.ebook.model.FavoriteManager.getInstance(requireContext()).isFavorite(b.getBookId())) {
                                    previewBook = b;
                                    break;
                                }
                            }
                            
                            int previewVol = extractVolumeNo(previewBook);
                            if (previewVol >= 1 && previewVol <= 5) {
                                PreviewBottomSheet sheet = PreviewBottomSheet.newInstance(previewBook.getBookId(), previewBook.getTitle(), previewVol, item.title, previewBook.getThumbnailUrl(), previewBook.getBookUrl());
                                sheet.show(getChildFragmentManager(), "PreviewBottomSheet");
                            } else {
                                android.widget.Toast.makeText(getContext(), "미리보기 자료는 01~05권만 제공됩니다", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    e -> android.util.Log.e(TAG, "Failed to load books for preview", e)
            );
        } else {
            SeriesDetailFragment fragment = SeriesDetailFragment.newInstance(item.categoryId, item.title);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(fragment, true);
            }
        }
    }

    protected Book findRepresentativeBook(List<Book> books, String categoryId) {
        Book bestMatch = null;
        String seriesNameFromFirst = extractSeriesName(books.get(0));
        
        for (Book b : books) {
            String bId = b.getBookId();
            String bTitle = b.getTitle();
            int bVol = extractVolumeNo(b);

            if (bId != null && bId.equals(categoryId)) return b;

            if (bTitle != null) {
                if (bTitle.equals(seriesNameFromFirst) || bTitle.contains("전체") || bTitle.contains("표지") || bTitle.contains("전집")) {
                    bestMatch = b;
                    if (bTitle.equals(seriesNameFromFirst)) break;
                }
            }
            if (bestMatch == null && bVol == 0) bestMatch = b;
            if (bestMatch == null && bTitle != null && !bTitle.contains("권")) bestMatch = b;
        }
        return (bestMatch != null) ? bestMatch : books.get(0);
    }

    protected void addItemByKeyword(List<SeriesAdapter.SeriesItem> source, String keyword, List<SeriesAdapter.SeriesItem> target) {
        for (SeriesAdapter.SeriesItem item : source) {
            if (item.title.replace(" ", "").contains(keyword.replace(" ", ""))) {
                if (!target.contains(item)) {
                    target.add(item);
                }
            }
        }
    }

    protected String extractSeriesName(Book book) {
        if (book.getTitle() != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.*?)\\s*\\d+권").matcher(book.getTitle());
            if (m.find()) return m.group(1).trim();
        }
        String id = book.getBookId();
        if (id != null && id.contains("_")) return id.split("_")[0] + " 전집";
        return "기타 전집";
    }

    public static int extractVolumeNo(Book book) {
        if (book == null) return 999;
        String id = book.getBookId();
        String title = book.getTitle();
        
        // 1. ID에서 추출 (_1, -1 등)
        if (id != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[_-](\\d+)").matcher(id);
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
            }
        }
        
        // 2. 제목에서 추출 (1권, 01, (1) 등)
        if (title != null) {
            // "1권" 형태
            java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("(\\d+)권").matcher(title);
            if (m1.find()) {
                try { return Integer.parseInt(m1.group(1)); } catch (Exception ignored) {}
            }
            // " 01" 또는 " 1" 형태 (공백 뒤 숫자)
            java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("\\s+(\\d+)").matcher(title);
            if (m2.find()) {
                try { return Integer.parseInt(m2.group(1)); } catch (Exception ignored) {}
            }
        }
        
        // 3. 최후의 수단: 숫자만 추출 (첫 번째 발견되는 숫자)
        if (title != null) {
            java.util.regex.Matcher m3 = java.util.regex.Pattern.compile("(\\d+)").matcher(title);
            if (m3.find()) {
                try { return Integer.parseInt(m3.group(1)); } catch (Exception ignored) {}
            }
        }

        return 999;
    }
}
