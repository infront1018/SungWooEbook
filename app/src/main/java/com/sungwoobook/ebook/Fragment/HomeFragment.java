package com.sungwoobook.ebook.Fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sungwoobook.ebook.MainActivity;
import com.sungwoobook.ebook.R;

/**
 * 전용 홈 화면 프래그먼트.
 * - 히어로 배너(Hero Section)를 포함합니다.
 */
public class HomeFragment extends BaseGalleryFragment {

    private View heroSection;
    private androidx.viewpager2.widget.ViewPager2 bannerViewPager;
    private android.widget.LinearLayout bannerIndicator;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_home;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // 배너 섹션 초기화
        heroSection = view.findViewById(R.id.heroSection);
        bannerViewPager = view.findViewById(R.id.bannerViewPager);
        bannerIndicator = view.findViewById(R.id.bannerIndicator);

        if (heroSection != null) heroSection.setVisibility(View.VISIBLE);
        
        setupBanner();
        super.onViewCreated(view, savedInstanceState);
    }

    private void setupBanner() {
        if (bannerViewPager == null) return;

        java.util.List<Integer> images = new java.util.ArrayList<>();
        images.add(R.drawable.banner_1);
        images.add(R.drawable.banner_2);
        images.add(R.drawable.banner_3);

        com.sungwoobook.ebook.adapter.BannerAdapter adapter = new com.sungwoobook.ebook.adapter.BannerAdapter(images);
        bannerViewPager.setAdapter(adapter);

        setupIndicator(images.size());

        bannerViewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicator(position);
            }
        });
    }

    private void setupIndicator(int count) {
        if (bannerIndicator == null) return;
        bannerIndicator.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                (int) (8 * getResources().getDisplayMetrics().density),
                (int) (8 * getResources().getDisplayMetrics().density)
            );
            params.setMargins(8, 4, 8, 4);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.dot_indicator);
            dot.setSelected(i == 0);
            bannerIndicator.addView(dot);
        }
    }

    private void updateIndicator(int position) {
        if (bannerIndicator == null) return;
        for (int i = 0; i < bannerIndicator.getChildCount(); i++) {
            bannerIndicator.getChildAt(i).setSelected(i == position);
        }
    }

    @Override
    protected void updateTitleAndNav() {
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            main.updateTitle("성우주니어");
            main.setNavActive(MainActivity.NavItem.HOME);
        }
    }

    /**
     * 외부(MainActivity)에서 홈 모드로 강제 전환 시 호출
     */
    public void resetToHome() {
        this.isFavoriteMode = false;
        if (heroSection != null) heroSection.setVisibility(View.VISIBLE);
        loadGalleryData();
        updateTitleAndNav();
    }
}
