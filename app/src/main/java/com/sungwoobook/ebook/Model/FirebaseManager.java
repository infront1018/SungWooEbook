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
        db.collection(COL_EBOOK_LIST)
                .get()
                .addOnSuccessListener(snapshot -> {
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
                                b.setBookId(doc.getId());
                                b.setSeriesId(categoryId);
                                b.setVolume(volume);
                                b.setTitle(categoryName + " " + volume + "권");
                                b.setBookUrl(pdfPath);
                                b.setThumbnailUrl(thumbPath);
                                list.add(b);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("FirebaseManager", "Data mapping error", e);
                        }
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }

    public void getBooksByCategory(String categoryId, 
                                   OnSuccessListener<List<Book>> onSuccess,
                                   OnFailureListener onFailure) {
        db.collection(COL_EBOOK_LIST)
                .whereEqualTo("categoryId", categoryId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Book> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            String categoryName = doc.getString("categoryName");
                            Long volumeLong = doc.getLong("volume");
                            int volume = (volumeLong != null) ? volumeLong.intValue() : 0;
                            String pdfPath = doc.getString("pdfPath");
                            String thumbPath = doc.getString("thumbPath");

                            Book b = new Book();
                            b.setBookId(doc.getId());
                            b.setSeriesId(categoryId);
                            b.setVolume(volume);
                            b.setTitle(categoryName + " " + volume + "권");
                            b.setBookUrl(pdfPath);
                            b.setThumbnailUrl(thumbPath);
                            list.add(b);
                        } catch (Exception e) {
                            android.util.Log.e("FirebaseManager", "Mapping error", e);
                        }
                    }
                    // 권수 기준 정렬
                    java.util.Collections.sort(list, (b1, b2) -> {
                        int v1 = com.sungwoobook.ebook.Fragment.HomeFragment.extractVolumeNo(b1);
                        int v2 = com.sungwoobook.ebook.Fragment.HomeFragment.extractVolumeNo(b2);
                        return Integer.compare(v1, v2);
                    });
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
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
