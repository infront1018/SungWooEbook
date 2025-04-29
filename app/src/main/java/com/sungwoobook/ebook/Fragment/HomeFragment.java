package com.sungwoobook.ebook.Fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sungwoobook.ebook.Model.ContentModel;
import com.sungwoobook.ebook.Model.FirebaseManager; // ✅ 추가
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.adapter.BannerAdapter;
import com.sungwoobook.ebook.adapter.HorizontalAdapter;
import com.sungwoobook.ebook.adapter.RecentAdapter;
import com.sungwoobook.ebook.adapter.VerticalAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 📌 파일 경로: com.sungwoobook.ebook.Fragment.HomeFragment.java
 * 📌 설명: 홈 탭 전체 제어 (배너 + 최근 콘텐츠 + 세로/가로 리스트)
 */
public class HomeFragment extends Fragment {

    private ViewPager2 bannerViewPager;
    private TabLayout bannerIndicator;
    private RecyclerView recyclerRecent, recyclerVertical;

    private BannerAdapter bannerAdapter;
    private RecentAdapter recentAdapter;
    private VerticalAdapter verticalAdapter;

    private Handler bannerHandler = new Handler();

    private List<ContentModel> allContents = new ArrayList<>();
    private List<String> bannerImages = new ArrayList<>();
    private List<ContentModel> recentContents = new ArrayList<>();

    private Runnable bannerRunnable = new Runnable() {
        @Override
        public void run() {
            if (bannerImages.size() > 0) {
                int currentItem = bannerViewPager.getCurrentItem();
                int nextItem = (currentItem + 1) % bannerImages.size();
                bannerViewPager.setCurrentItem(nextItem, true);
                bannerHandler.postDelayed(this, 3000);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        bannerViewPager = view.findViewById(R.id.bannerViewPager);
        bannerIndicator = view.findViewById(R.id.bannerIndicator);
        recyclerRecent = view.findViewById(R.id.recyclerRecent);
        recyclerVertical = view.findViewById(R.id.recyclerVertical);

        setupAdapters();

        // 🔥 데이터 불러오는 순서
        loadBannerImages();        // (1) 배너 전용 이미지 가져오기
        loadContentData();         // (2) 책 콘텐츠 데이터 가져오기

        return view;
    }

    private void setupAdapters() {
        bannerAdapter = new BannerAdapter(bannerImages);
        bannerViewPager.setAdapter(bannerAdapter);

        recyclerRecent.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recentAdapter = new RecentAdapter(recentContents);
        recyclerRecent.setAdapter(recentAdapter);

        recyclerVertical.setLayoutManager(new LinearLayoutManager(getContext()));
        verticalAdapter = new VerticalAdapter(allContents);
        recyclerVertical.setAdapter(verticalAdapter);
    }

    /**
     * 📌 배너 전용 이미지 로드 (FirebaseManager 사용)
     */
    private void loadBannerImages() {
        FirebaseFirestore db = FirebaseFirestore.getInstance(); // ✅ 여기서 db 새로 가져오자
        db.collection("contents")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        bannerImages.clear();

                        android.util.Log.d("HomeFragment", "queryDocumentSnapshots :: " + queryDocumentSnapshots);
                        android.util.Log.d("HomeFragment", "queryDocumentSnapshots size :: " + queryDocumentSnapshots.size()); // (1)

                        List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
                        for (int i = 0; i < documents.size(); i++) {
                            DocumentSnapshot doc = documents.get(i);
                            String imageUrl = doc.getString("url");

                            // ✅ 디버깅 로그 추가 (진짜 어디서 막히는지 확인)
                            if (imageUrl != null) {
                                android.util.Log.d("HomeFragment", "imageUrl" + imageUrl);
                            } else {
                                android.util.Log.d("HomeFragment", "imageUrl is null !!!");
                            }

                            if (imageUrl != null && !imageUrl.trim().isEmpty()) { // ⭐ 공백 자동 제거
                                bannerImages.add(imageUrl);
                                // ✅ [1] 가져온 URL 출력
                                android.util.Log.d("HomeFragment", "Banner URL: " + imageUrl);
                            } else {
                                android.util.Log.w("HomeFragment", "Banner URL is null or empty");
                            }
                        }

                        bannerAdapter.notifyDataSetChanged();
                        android.util.Log.d("HomeFragment", "Banner Images size: " + bannerImages.size()); // ✅ [2] 리스트 개수 출력

                        // 배너 Indicator 연결
                        new TabLayoutMediator(bannerIndicator, bannerViewPager,
                                new TabLayoutMediator.TabConfigurationStrategy() {
                                    @Override
                                    public void onConfigureTab(TabLayout.Tab tab, int position) {
                                        // 아무 동작 없음
                                    }
                                }).attach();

                        // 자동 슬라이드 시작
                        bannerHandler.postDelayed(bannerRunnable, 3000);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "배너 이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 📌 책 콘텐츠 데이터 로드 (contents 컬렉션)
     */
    private void loadContentData() {
        FirebaseManager.getInstance().getAllContents(queryDocumentSnapshots -> { // ✅ 수정
            allContents.clear();
            recentContents.clear();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                ContentModel content = doc.toObject(ContentModel.class);
                allContents.add(content);
                recentContents.add(content);
            }
            recentAdapter.notifyDataSetChanged();
            verticalAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bannerHandler.removeCallbacks(bannerRunnable);
    }
}
