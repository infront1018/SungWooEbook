package com.sungwoobook.ebook.model;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore / Storage 호출을 담당하는 싱글톤 매니저.
 */
public class FirebaseManager {

    private static FirebaseManager instance;

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    private static final String COL_CONTENTS = "contents";
    private static final String COL_BOOKS    = "books";
    private static final String COL_BANNER   = "banner";

    private FirebaseManager() {
        db      = FirebaseFirestore.getInstance("defaultdb");
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public void getAllSeries(OnSuccessListener<List<Series>> onSuccess,
                             OnFailureListener onFailure) {
        db.collection(COL_CONTENTS)
                .orderBy("series_no", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Series> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Series s = doc.toObject(Series.class);
                        if (s != null) {
                            s.setSeriesId(doc.getId());
                            list.add(s);
                        }
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }

    public void getRecentSeries(int limit,
                                OnSuccessListener<List<Series>> onSuccess,
                                OnFailureListener onFailure) {
        db.collection(COL_CONTENTS)
                .whereGreaterThan("last_accessed", new Timestamp(0, 0))
                .orderBy("last_accessed", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Series> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Series s = doc.toObject(Series.class);
                        if (s != null) {
                            s.setSeriesId(doc.getId());
                            list.add(s);
                        }
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }

    public void updateLastAccessed(@NonNull String seriesId) {
        Map<String, Object> update = new HashMap<>();
        update.put("last_accessed", Timestamp.now());
        db.collection(COL_CONTENTS).document(seriesId).update(update);
    }

    public void getAllBooks(OnSuccessListener<List<Book>> onSuccess,
                            OnFailureListener onFailure) {
        db.collection(COL_CONTENTS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Book> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        // banner 등 다른 문서가 있다면 Book 캐스팅 실패할 수도 있으므로 try-catch 혹은 안전하게 처리
                        try {
                            Book b = doc.toObject(Book.class);
                            if (b != null && b.getBookUrl() != null) { // book_url이나 title 등 필수로 있는 필드를 체크해서 일반 내용인지 도서인지 구분
                                b.setBookId(doc.getId());
                                list.add(b);
                            } else if (b != null && b.getTitle() != null) {
                                b.setBookId(doc.getId());
                                list.add(b);
                            }
                        } catch (Exception e) {
                            // Book 형식이 아닌 문서 무시
                        }
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }

    public void getBanners(OnSuccessListener<QuerySnapshot> onSuccess,
                           OnFailureListener onFailure) {
        db.collection(COL_BANNER)
                .get()
                .addOnSuccessListener(onSuccess)
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
