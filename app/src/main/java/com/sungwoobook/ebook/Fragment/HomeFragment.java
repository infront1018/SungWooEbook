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

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sungwoobook.ebook.Model.ContentModel;
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

    private FirebaseFirestore db;
    private Handler bannerHandler = new Handler();

    private List<ContentModel> allContents = new ArrayList<>();
    private List<String> bannerImages = new ArrayList<>();
    private List<ContentModel> recentContents = new ArrayList<>();

    private Runnable bannerRunnable = new Runnable() {
        @Override
        public void run() {
            int currentItem = bannerViewPager.getCurrentItem();
            int nextItem = (currentItem + 1) % bannerImages.size();
            bannerViewPager.setCurrentItem(nextItem, true);
            bannerHandler.postDelayed(this, 3000);
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
        loadDataFromFirestore();

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

    private void loadDataFromFirestore() {
        db = FirebaseFirestore.getInstance();
        db.collection("contents")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allContents.clear();
                    recentContents.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ContentModel content = doc.toObject(ContentModel.class);
                        allContents.add(content);
                        recentContents.add(content);
                        bannerImages.add(content.getUrl() != null ? content.getUrl() : ""); // URL 없으면 나중에 기본 이미지로
                    }
                    bannerAdapter.notifyDataSetChanged();
                    recentAdapter.notifyDataSetChanged();
                    verticalAdapter.notifyDataSetChanged();

                    new TabLayoutMediator(bannerIndicator, bannerViewPager,
                            (tab, position) -> {}).attach();
                    bannerHandler.postDelayed(bannerRunnable, 3000);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bannerHandler.removeCallbacks(bannerRunnable);
    }
}
