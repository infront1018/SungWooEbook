package com.sungwoobook.ebook;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2초

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView logoText = findViewById(R.id.logoText); // ✅ ID로 참조

        // ✅ 텍스트에 페이드 인 애니메이션 적용
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(1000);       // 1초 동안 서서히 나타남
        fadeIn.setFillAfter(true);      // 애니메이션 후 상태 유지
        logoText.startAnimation(fadeIn);

        // ✅ 2초 후 MainActivity로 이동
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // SplashActivity 종료
        }, SPLASH_DELAY);
    }
}
