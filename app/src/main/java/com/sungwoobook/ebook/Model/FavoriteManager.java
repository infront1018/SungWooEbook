package com.sungwoobook.ebook.model;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * 전집 즐겨찾기 상태를 관리하는 매니저.
 * SharedPreferences를 사용하여 로컬에 저장합니다.
 */
public class FavoriteManager {
    private static final String PREF_NAME = "favorite_prefs";
    private static final String KEY_FAVORITES = "favorite_series_ids";
    
    private static FavoriteManager instance;
    private final SharedPreferences prefs;

    private FavoriteManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized FavoriteManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoriteManager(context);
        }
        return instance;
    }

    /**
     * 특정 전집이 즐겨찾기 상태인지 확인
     */
    public boolean isFavorite(String seriesId) {
        if (seriesId == null) return false;
        Set<String> favorites = prefs.getStringSet(KEY_FAVORITES, new HashSet<>());
        return favorites.contains(seriesId);
    }

    /**
     * 즐겨찾기 상태 토글
     */
    public boolean toggleFavorite(String seriesId) {
        if (seriesId == null) return false;
        Set<String> favorites = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
        boolean isNowFavorite;
        if (favorites.contains(seriesId)) {
            favorites.remove(seriesId);
            isNowFavorite = false;
        } else {
            favorites.add(seriesId);
            isNowFavorite = true;
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
        return isNowFavorite;
    }

    /**
     * 모든 즐겨찾기 전집 ID 반환
     */
    public Set<String> getFavoriteSeriesIds() {
        return prefs.getStringSet(KEY_FAVORITES, new HashSet<>());
    }
}
