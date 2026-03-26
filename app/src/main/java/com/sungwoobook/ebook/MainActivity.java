package com.sungwoobook.ebook;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.sungwoobook.ebook.Fragment.HomeFragment;

/**
 * 단일 Activity 컨테이너.
 * - FLAG_SECURE : 화면 캡처/녹화 원천 차단
 * - fragment_container 에 Fragment를 replace 하는 방식
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
    }

    /**
     * Fragment 전환 헬퍼.
     * @param fragment  표시할 Fragment
     * @param addToBackStack  true이면 백스택에 추가 (뒤로가기 가능)
     */
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

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("앱 종료")
                    .setMessage("정말 종료하시겠습니까?")
                    .setPositiveButton("예", (dialog, which) -> finish())
                    .setNegativeButton("아니오", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }
}