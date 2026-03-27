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

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_home;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // 배너 섹션 초기화
        heroSection = view.findViewById(R.id.heroSection);
        if (heroSection != null) heroSection.setVisibility(View.VISIBLE);
        
        super.onViewCreated(view, savedInstanceState);
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
