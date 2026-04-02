// MyApplication.java - 프로젝트 루트 패키지에 추가
package com.sungwoobook.ebook;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        // 파이어베이스 초기화
        FirebaseApp.initializeApp(this);

        // App Check 초기화 (Play Integrity)
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());

        // 익명 로그인으로 Storage/Firestore 접근 권한 획득 처리 (no auth token 해결)
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInAnonymously:success");
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                    }
                });
        }
    }
}