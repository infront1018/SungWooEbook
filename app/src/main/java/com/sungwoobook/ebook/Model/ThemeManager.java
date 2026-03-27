package com.sungwoobook.ebook.model;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.sungwoobook.ebook.R;

public class ThemeManager {
    private static final String PREF_NAME = "ThemePrefs";
    private static final String KEY_THEME = "selected_theme";

    public static final int THEME_DEFAULT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_WHITE = 2;

    public static void setTheme(Context context, int theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, theme).apply();
    }

    public static int getTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_DEFAULT);
    }

    public static void applyTheme(Activity activity) {
        int theme = getTheme(activity);
        switch (theme) {
            case THEME_DARK:
                activity.setTheme(R.style.AppTheme_Dark);
                break;
            case THEME_WHITE:
                activity.setTheme(R.style.AppTheme_White);
                break;
            default:
                activity.setTheme(R.style.AppTheme_Default);
                break;
        }
    }
}
