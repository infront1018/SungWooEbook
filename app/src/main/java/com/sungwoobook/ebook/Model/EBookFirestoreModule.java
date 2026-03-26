package com.sungwoobook.ebook.model;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

/**
 * 기본 DB가 아닌 "sungwoo-db" Firestore 인스턴스를 가져오기 위한 싱글톤 객체
 */
public class EBookFirestoreModule {

    private static EBookFirestoreModule instance;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    private EBookFirestoreModule() {
        // Firebase Firestore SDK 24.11.0 이상부터 getInstance(FirebaseApp, databaseId) 지원
        db = FirebaseFirestore.getInstance(com.google.firebase.FirebaseApp.getInstance(), "sungwoo-db");
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized EBookFirestoreModule getInstance() {
        if (instance == null) {
            instance = new EBookFirestoreModule();
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public FirebaseStorage getStorage() {
        return storage;
    }
}
