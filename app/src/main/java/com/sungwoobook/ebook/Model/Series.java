package com.sungwoobook.ebook.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

/**
 * Firestore "contents" 컬렉션과 매핑되는 전집(시리즈) 모델.
 */
public class Series {

    private String seriesId;

    @PropertyName("title")
    private String title;

    @PropertyName("series_no")
    private int seriesNo;

    @PropertyName("category")
    private String category;

    @PropertyName("last_accessed")
    private Timestamp lastAccessed;

    public Series() {}

    public String getSeriesId() { return seriesId; }
    public void setSeriesId(String seriesId) { this.seriesId = seriesId; }

    @PropertyName("title")
    public String getTitle() { return title; }
    @PropertyName("title")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("series_no")
    public int getSeriesNo() { return seriesNo; }
    @PropertyName("series_no")
    public void setSeriesNo(int seriesNo) { this.seriesNo = seriesNo; }

    @PropertyName("category")
    public String getCategory() { return category; }
    @PropertyName("category")
    public void setCategory(String category) { this.category = category; }

    @PropertyName("last_accessed")
    public Timestamp getLastAccessed() { return lastAccessed; }
    @PropertyName("last_accessed")
    public void setLastAccessed(Timestamp lastAccessed) { this.lastAccessed = lastAccessed; }
}
