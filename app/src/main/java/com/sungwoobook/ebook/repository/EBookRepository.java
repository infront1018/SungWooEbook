package com.sungwoobook.ebook.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sungwoobook.ebook.model.EBook;
import com.sungwoobook.ebook.model.EBookFirestoreModule;

import java.util.ArrayList;
import java.util.List;

public class EBookRepository {

    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "ebook_list";

    public EBookRepository() {
        // 싱글톤 모듈에서 sungwoo-db 인스턴스를 가져옵니다.
        this.db = EBookFirestoreModule.getInstance().getDb();
    }

    /**
     * 특정 categoryId를 기준으로 volume 오름차순 정렬된 도서 목록을 가져옵니다.
     * @param categoryId 조회할 카테고리 ID (예: "sci_complete")
     * @return EBook 리스트를 포함하는 LiveData
     */
    public LiveData<List<EBook>> getEBooksByCategory(String categoryId) {
        MutableLiveData<List<EBook>> liveData = new MutableLiveData<>();

        db.collection(COLLECTION_NAME)
                .whereEqualTo("categoryId", categoryId)
                .orderBy("volume", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<EBook> ebookList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            EBook ebook = document.toObject(EBook.class);
                            ebookList.add(ebook);
                        }
                        liveData.setValue(ebookList);
                    } else {
                        Log.e("EBookRepository", "Error getting documents: ", task.getException());
                        liveData.setValue(new ArrayList<>()); // 에러 발생 시 빈 리스트 반환
                    }
                });

        return liveData;
    }
}
