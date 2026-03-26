// MyApplication.java - 프로젝트 루트 패키지에 추가
package com.sungwoobook.ebook;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 파이어베이스 초기화
        FirebaseApp.initializeApp(this);
    }
}