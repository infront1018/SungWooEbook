package com.sungwoobook.ebook.model;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore / Storage 호출을 담당하는 싱글톤 매니저.
 */
public class FirebaseManager {

    private static FirebaseManager instance;

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    private static final String COL_EBOOK_LIST = "ebook_list";

    private FirebaseManager() {
        FirebaseFirestore tempDb;
        try {
            // Firestore Native 모드인 "sungwoo-db"를 우선 시도합니다.
            tempDb = com.google.firebase.firestore.FirebaseFirestore.getInstance("sungwoo-db");
        } catch (Exception e) {
            android.util.Log.e("FirebaseManager", "Failed to get 'sungwoo-db' instance. Falling back to default instance.", e);
            // 만약 sungwoo-db가 없다면 기본 인스턴스로 폴백합니다.
            tempDb = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        }
        this.db = tempDb;
        this.storage = com.google.firebase.storage.FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public void getAllBooks(OnSuccessListener<List<Book>> onSuccess,
                            OnFailureListener onFailure) {
        
        android.util.Log.d("FirebaseManager", "Fetching books from collection: " + COL_EBOOK_LIST);
        db.collection(COL_EBOOK_LIST)
                .get()
                .addOnSuccessListener(snapshot -> {
                    android.util.Log.d("FirebaseManager", "Success! Total documents: " + snapshot.size());
                    List<Book> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            String categoryId = doc.getString("categoryId");
                            if (categoryId != null && !categoryId.isEmpty()) {
                                String categoryName = doc.getString("categoryName");
                                Long volumeLong = doc.getLong("volume");
                                int volume = (volumeLong != null) ? volumeLong.intValue() : 0;
                                String pdfPath = doc.getString("pdfPath");
                                String thumbPath = doc.getString("thumbPath");

                                Book b = new Book();
                                b.setBookId(doc.getId()); // Document ID
                                b.setSeriesId(categoryId); // 카테고리 ID를 Series ID로
                                b.setTitle(categoryName + " " + volume + "권"); // 결합하여 타이틀 생성
                                b.setBookUrl(pdfPath);
                                b.setThumbnailUrl(thumbPath);
                                
                                list.add(b);
                            } else {
                                android.util.Log.w("FirebaseManager", "Missing categoryId in doc: " + doc.getId());
                            }
                        } catch (Exception e) {
                            android.util.Log.e("FirebaseManager", "Data mapping error on: " + doc.getId(), e);
                        }
                    }
                    android.util.Log.d("FirebaseManager", "Successfully mapped books: " + list.size());
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseManager", "Firestore GET failed. Error: " + e.getMessage());
                    if (e.getMessage() != null && e.getMessage().contains("Datastore Mode")) {
                        android.util.Log.e("FirebaseManager", "CRITICAL: (default) DB is in Datastore Mode. You MUST use a Native Mode database like 'sungwoo-db'.");
                    }
                    onFailure.onFailure(e);
                });
    }

    public void getDownloadUrl(@NonNull String storagePath,
                               OnSuccessListener<Uri> onSuccess,
                               OnFailureListener onFailure) {
        StorageReference ref = storage.getReference().child(storagePath);
        ref.getDownloadUrl()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}
