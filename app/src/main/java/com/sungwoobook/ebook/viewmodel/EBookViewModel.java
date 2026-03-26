package com.sungwoobook.ebook.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.sungwoobook.ebook.model.EBook;
import com.sungwoobook.ebook.repository.EBookRepository;

import java.util.List;

public class EBookViewModel extends ViewModel {

    private final EBookRepository repository;
    private final MutableLiveData<String> categoryIdInput = new MutableLiveData<>();
    private final LiveData<List<EBook>> eBooksLiveData;

    public EBookViewModel() {
        this.repository = new EBookRepository();

        // categoryIdInput 값이 변경될 때마다 Repository를 통해 새로운 LiveData를 가져오도록 연결
        this.eBooksLiveData = Transformations.switchMap(categoryIdInput, categoryId -> 
                repository.getEBooksByCategory(categoryId)
        );
    }

    /**
     * 특정 카테고리의 도서 목록을 로드하도록 트리거합니다.
     * @param categoryId 로드할 카테고리 ID
     */
    public void loadEBooks(String categoryId) {
        categoryIdInput.setValue(categoryId);
    }

    /**
     * UI에서 관찰할 LiveData를 반환합니다.
     */
    public LiveData<List<EBook>> getEBooks() {
        return eBooksLiveData;
    }
}
