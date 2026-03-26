package com.sungwoobook.ebook.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

public class EBook {

    @DocumentId
    private String documentId;

    @PropertyName("categoryId")
    private String categoryId;

    @PropertyName("categoryName")
    private String categoryName;

    @PropertyName("volume")
    private int volume;

    @PropertyName("pdfPath")
    private String pdfPath;

    @PropertyName("thumbPath")
    private String thumbPath;

    @ServerTimestamp
    @PropertyName("updatedAt")
    private Timestamp updatedAt;

    public EBook() {}

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    @PropertyName("categoryId")
    public String getCategoryId() { return categoryId; }
    @PropertyName("categoryId")
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    @PropertyName("categoryName")
    public String getCategoryName() { return categoryName; }
    @PropertyName("categoryName")
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    @PropertyName("volume")
    public int getVolume() { return volume; }
    @PropertyName("volume")
    public void setVolume(int volume) { this.volume = volume; }

    @PropertyName("pdfPath")
    public String getPdfPath() { return pdfPath; }
    @PropertyName("pdfPath")
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

    @PropertyName("thumbPath")
    public String getThumbPath() { return thumbPath; }
    @PropertyName("thumbPath")
    public void setThumbPath(String thumbPath) { this.thumbPath = thumbPath; }

    @PropertyName("updatedAt")
    public Timestamp getUpdatedAt() { return updatedAt; }
    @PropertyName("updatedAt")
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
