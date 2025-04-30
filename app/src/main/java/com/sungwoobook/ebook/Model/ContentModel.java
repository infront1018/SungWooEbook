/**
 * 📌 파일 경로: com.sungwoobook.ebook.Model.ContentModel.java
 * 📌 설명: Firestore contents 컬렉션과 매핑되는 콘텐츠 데이터 모델 클래스
 */

package com.sungwoobook.ebook.Model;

import java.util.Date;
import java.util.List;

public class ContentModel {
    private String title;
    private String thumbnailUrl;
    private String bookUrl;
    private String videoUrl;
    private String type; // "book", "video", "both"
    private List<String> tags;
    private Date createdAt;

    // ✅ 기본 생성자 (Firestore 역직렬화에 필요)
    public ContentModel() {
    }

    // ✅ 전체 필드를 포함한 생성자
    public ContentModel(String title, String thumbnailUrl, String bookUrl, String videoUrl, String type, List<String> tags, Date createdAt) {
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.bookUrl = bookUrl;
        this.videoUrl = videoUrl;
        this.type = type;
        this.tags = tags;
        this.createdAt = createdAt;
    }

    // ✅ Getter/Setter
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getBookUrl() {
        return bookUrl;
    }

    public void setBookUrl(String bookUrl) {
        this.bookUrl = bookUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}