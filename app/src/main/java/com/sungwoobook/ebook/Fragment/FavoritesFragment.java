package com.sungwoobook.ebook.Fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sungwoobook.ebook.MainActivity;
import com.sungwoobook.ebook.R;

/**
 * 전용 즐겨찾기 화면 프래그먼트.
 * - 히어로 배너 없이 즐겨찾기 목록만 보여줍니다.
 */
public class FavoritesFragment extends BaseGalleryFragment {

    public FavoritesFragment() {
        this.isFavoriteMode = true;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_home; // 동일한 레이아웃 재사용 (배너는 숨김 처리)
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // 배너 섹션 숨김 (즐겨찾기 전용)
        View heroSection = view.findViewById(R.id.heroSection);
        if (heroSection != null) heroSection.setVisibility(View.GONE);
        
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected void updateTitleAndNav() {
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            main.updateTitle("즐겨찾기");
            main.setNavActive(MainActivity.NavItem.FAVORITE);
        }
    }
}
