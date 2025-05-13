package com.sungwoobook.ebook;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sungwoobook.ebook.Fragment.*;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.menu_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.menu_favorite) {
                selectedFragment = new FavoriteFragment();
            } else if (itemId == R.id.menu_qr) {
                selectedFragment = new QrFragment();
            } else if (itemId == R.id.menu_search) {
                selectedFragment = new SearchFragment();
            } else if (itemId == R.id.menu_more) {
                selectedFragment = new MoreFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();

            return true;
        });

        // 첫 화면 기본 프래그먼트
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();

            // 상단 상태바 & 하단 내비게이션 바 투명 처리
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);

            // 전체 화면 레이아웃이 시스템 바 아래로 확장되도록 설정
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("앱 종료")
                .setMessage("정말 종료하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> finish())
                .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                .show();
    }

}