package com.sungwoobook.ebook.Model;

import java.util.List;

public class UserModel {
    private String userId;
    private List<String> favoriteContentIds;

    public UserModel() {}

    public UserModel(String userId, List<String> favoriteContentIds) {
        this.userId = userId;
        this.favoriteContentIds = favoriteContentIds;
    }

    public String getUserId() { return userId; }
    public List<String> getFavoriteContentIds() { return favoriteContentIds; }
}
