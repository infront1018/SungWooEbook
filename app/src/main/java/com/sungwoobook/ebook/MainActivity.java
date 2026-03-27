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
                        finish();
                    } else {
                        navigateTo(new com.sungwoobook.ebook.Fragment.HomeFragment(), false);
                    }
                }
            }
        });
    }

    private void setupGlobalNav() {
        android.view.View nav = findViewById(R.id.floatingNav);
        if (nav == null) {
            android.util.Log.e("MainActivity", "❌ Critical: floatingNav view not found in activity_main layout!");
            return;
        }

        android.widget.ImageView btnHome = findViewById(R.id.btnHome);
        android.widget.ImageView btnFavorites = findViewById(R.id.btnFavorites);
        android.widget.ImageView btnSettings = findViewById(R.id.btnSettings);

        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(current instanceof com.sungwoobook.ebook.Fragment.HomeFragment)) {
                    navigateTo(new com.sungwoobook.ebook.Fragment.HomeFragment(), false);
                }
            });
        }

        if (btnFavorites != null) {
            btnFavorites.setOnClickListener(v -> {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (!(current instanceof com.sungwoobook.ebook.Fragment.FavoritesFragment)) {
                    navigateTo(new com.sungwoobook.ebook.Fragment.FavoritesFragment(), false);
                }
            });
        }
        
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                updateTitle("설정");
                setNavActive(NavItem.SETTINGS);
                navigateTo(new com.sungwoobook.ebook.Fragment.SettingsFragment(), true);
            });
        }
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
        
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.themeAccentColor, typedValue, true);
        int accentColor = typedValue.data;
        
        getTheme().resolveAttribute(R.attr.themeSubTextColor, typedValue, true);
        int inactiveColor = typedValue.data;

        int favActiveColor = android.graphics.Color.parseColor("#E11D48");

        if (h != null) h.setColorFilter(item == NavItem.HOME ? accentColor : inactiveColor);
        if (f != null) f.setColorFilter(item == NavItem.FAVORITE ? favActiveColor : inactiveColor);
        if (s != null) s.setColorFilter(item == NavItem.SETTINGS ? accentColor : inactiveColor);
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
                (int) (80 * getResources().getDisplayMetrics().density);
            cont.setLayoutParams(lp);
        }
    }
}