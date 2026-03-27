package com.sungwoobook.ebook;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.sungwoobook.ebook.Fragment.HomeFragment;
import com.sungwoobook.ebook.model.FavoriteManager;

/**
 * 단일 Activity 컨테이너.
 * - FLAG_SECURE : 화면 캡처/녹화 원천 차단
 * - fragment_container 에 Fragment를 replace 하는 방식
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ✅ 테마 설정 (setContentView 및 super.onCreate 전에 호출)
        com.sungwoobook.ebook.model.ThemeManager.applyTheme(this);
        
        super.onCreate(savedInstanceState);

        // ✅ 화면 캡처 및 녹화 차단
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        setContentView(R.layout.activity_main);

        // 앱 최초 진입 시 HomeFragment 표시
        if (savedInstanceState == null) {
            navigateTo(new HomeFragment(), false);
        }

        // 🚀 미리보기 도서(전집별 01권) 선행 다운로드 시작
        com.sungwoobook.ebook.model.BookPrefetcher.start(this);

        setupGlobalNav();

        // ✅ 시스템 백버튼 핸들링: 프래그먼트 백스택 우선 확인 후 홈 이동
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    androidx.fragment.app.Fragment current = fm.findFragmentById(R.id.fragment_container);
                    if (current instanceof com.sungwoobook.ebook.Fragment.HomeFragment) {
                        // 🚪 홈 화면에서 뒤로가기 시 종료 확인 팝업
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle("앱 종료")
                            .setMessage("성우주니어 전자책 앱을 종료하시겠습니까?")
                            .setPositiveButton("예", (dialog, which) -> finish())
                            .setNegativeButton("아니오", null)
                            .show();
                    } else {
                        navigateTo(new com.sungwoobook.ebook.Fragment.HomeFragment(), false);
                    }
                }
            }
        });
    }

    private void setupGlobalNav() {
        android.view.View nav = findViewById(R.id.floatingNav);
        if (nav == null) return;

        // 터치 영역 확장을 위해 각 레이아웃 컨테이너에 클릭 리스너 설정
        findViewById(R.id.layoutHome).setOnClickListener(v -> {
            if (!(getSupportFragmentManager().findFragmentById(R.id.fragment_container) instanceof HomeFragment)) {
                setNavActive(NavItem.HOME);
                navigateTo(new HomeFragment(), false);
            }
        });

        findViewById(R.id.layoutFavorites).setOnClickListener(v -> {
            if (!(getSupportFragmentManager().findFragmentById(R.id.fragment_container) instanceof com.sungwoobook.ebook.Fragment.FavoritesFragment)) {
                setNavActive(NavItem.FAVORITE);
                navigateTo(new com.sungwoobook.ebook.Fragment.FavoritesFragment(), false);
            }
        });
        
        findViewById(R.id.layoutSettings).setOnClickListener(v -> {
            if (!(getSupportFragmentManager().findFragmentById(R.id.fragment_container) instanceof com.sungwoobook.ebook.Fragment.SettingsFragment)) {
                updateTitle("설정");
                setNavActive(NavItem.SETTINGS);
                navigateTo(new com.sungwoobook.ebook.Fragment.SettingsFragment(), true);
            }
        });
    }

    public void updateTitle(String title) {
        android.widget.TextView txtTitle = findViewById(R.id.txtHeaderTitle);
        if (txtTitle != null) {
            txtTitle.setText(title);
        }
    }

    public enum NavItem { HOME, FAVORITE, SETTINGS }

    public void setNavActive(NavItem item) {
        android.widget.ImageView h = findViewById(R.id.btnHome);
        android.widget.ImageView f = findViewById(R.id.btnFavorites);
        android.widget.ImageView s = findViewById(R.id.btnSettings);
        
        android.view.View ih = findViewById(R.id.indicatorHome);
        android.view.View ifav = findViewById(R.id.indicatorFavorites);
        android.view.View is = findViewById(R.id.indicatorSettings);
        
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.themeAccentColor, typedValue, true);
        int accentColor = typedValue.data;
        getTheme().resolveAttribute(R.attr.themeSubTextColor, typedValue, true);
        int inactiveColor = typedValue.data;

        // 아이콘 틴트 처리
        if (h != null) h.setColorFilter(item == NavItem.HOME ? accentColor : inactiveColor);
        if (f != null) f.setColorFilter(item == NavItem.FAVORITE ? android.graphics.Color.parseColor("#E11D48") : inactiveColor);
        if (s != null) s.setColorFilter(item == NavItem.SETTINGS ? accentColor : inactiveColor);

        // 인디케이터 가시성 처리
        if (ih != null) ih.setVisibility(item == NavItem.HOME ? android.view.View.VISIBLE : android.view.View.GONE);
        if (ifav != null) ifav.setVisibility(item == NavItem.FAVORITE ? android.view.View.VISIBLE : android.view.View.GONE);
        if (is != null) is.setVisibility(item == NavItem.SETTINGS ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        androidx.fragment.app.FragmentTransaction tx =
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out,
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                        )
                        .replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            tx.addToBackStack(null);
        }
        tx.commit();
    }

    /**
     * 전역 내비게이션 및 헤더 가시성 조절 (영상 재생 시 사용)
     */
    public void setGlobalUiVisibility(int visibility) {
        android.view.View nav    = findViewById(R.id.floatingNav);
        android.view.View header = findViewById(R.id.globalHeader);
        android.view.View bg     = findViewById(R.id.backgroundImage);
        android.view.View root   = findViewById(R.id.rootLayout);
        android.view.View cont   = findViewById(R.id.fragment_container);

        if (nav != null) nav.setVisibility(visibility);
        if (header != null) header.setVisibility(visibility);
        if (bg != null) bg.setVisibility(visibility);

        // 영상 재생 시(GONE) 배경을 검은색으로 고정하고 상단 여백 제거
        if (root != null) {
            root.setBackgroundColor(visibility == android.view.View.GONE ? 
                android.graphics.Color.BLACK : android.graphics.Color.TRANSPARENT);
        }

        if (cont != null && cont.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams lp = (android.view.ViewGroup.MarginLayoutParams) cont.getLayoutParams();
            lp.topMargin = (visibility == android.view.View.GONE) ? 0 : 
                (int) (64 * getResources().getDisplayMetrics().density);
            cont.setLayoutParams(lp);
        }
    }
}