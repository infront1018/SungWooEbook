package com.sungwoobook.ebook.Model;

public class ContentModel {
    private String title;
    private String type;    // "pdf" 또는 "video"
    private String url;     // Firebase Storage URL

    public ContentModel() {
        // Firestore용 기본 생성자
    }

    public ContentModel(String title, String type, String url) {
        this.title = title;
        this.type = type;
        this.url = url;
    }

    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getUrl() { return url; }
}
