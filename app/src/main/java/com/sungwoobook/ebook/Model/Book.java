package com.sungwoobook.ebook.model;

import com.google.firebase.firestore.PropertyName;
import java.util.List;

/**
 * Firestore "contents/{seriesId}/books" 서브컬렉션과 매핑되는 도서 모델.
 */
public class Book {

    private String bookId;
    private String seriesId;

    @PropertyName("bookUrl")
    private String bookUrl;

    @PropertyName("tags")
    private List<String> tags;

    @PropertyName("thumbnailUrl")
    private String thumbnailUrl;

    @PropertyName("title")
    private String title;

    @PropertyName("type")
    private String type;

    @PropertyName("videoUrl")
    private String videoUrl;

    private int volume;

    public Book() {}

    public int getVolume() { return volume; }
    public void setVolume(int volume) { this.volume = volume; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getSeriesId() { return seriesId; }
    public void setSeriesId(String seriesId) { this.seriesId = seriesId; }

    @PropertyName("bookUrl")
    public String getBookUrl() { return bookUrl; }
    @PropertyName("bookUrl")
    public void setBookUrl(String bookUrl) { this.bookUrl = bookUrl; }

    @PropertyName("tags")
    public List<String> getTags() { return tags; }
    @PropertyName("tags")
    public void setTags(List<String> tags) { this.tags = tags; }

    @PropertyName("thumbnailUrl")
    public String getThumbnailUrl() { return thumbnailUrl; }
    @PropertyName("thumbnailUrl")
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    @PropertyName("title")
    public String getTitle() { return title; }
    @PropertyName("title")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("type")
    public String getType() { return type; }
    @PropertyName("type")
    public void setType(String type) { this.type = type; }

    @PropertyName("videoUrl")
    public String getVideoUrl() { return videoUrl; }
    @PropertyName("videoUrl")
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
}
