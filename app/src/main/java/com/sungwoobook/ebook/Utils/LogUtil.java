package com.sungwoobook.ebook.Utils;

import android.util.Log;

public class LogUtil {
    private static final String DEFAULT_TAG = "EBOOK_APP";
    private static boolean isDebug = true;  // 추후 릴리즈 시 false로 변경

    public static void d(String message) {
        if (isDebug) Log.d(DEFAULT_TAG, message);
    }

    public static void d(String tag, String message) {
        if (isDebug) Log.d(tag, message);
    }

    public static void e(String message) {
        if (isDebug) Log.e(DEFAULT_TAG, message);
    }

    public static void e(String tag, String message) {
        if (isDebug) Log.e(tag, message);
    }

    public static void i(String message) {
        if (isDebug) Log.i(DEFAULT_TAG, message);
    }

    public static void i(String tag, String message) {
        if (isDebug) Log.i(tag, message);
    }

    public static void v(String message) {
        if (isDebug) Log.v(DEFAULT_TAG, message);
    }

    public static void v(String tag, String message) {
        if (isDebug) Log.v(tag, message);
    }

    public static void w(String message) {
        if (isDebug) Log.w(DEFAULT_TAG, message);
    }

    public static void w(String tag, String message) {
        if (isDebug) Log.w(tag, message);
    }

    // 에러 출력과 함께 Exception 로그 찍기
    public static void printStackTrace(Exception e) {
        if (isDebug && e != null) {
            Log.e(DEFAULT_TAG, "Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 로그 사용 ON/OFF 설정
    public static void setDebug(boolean debug) {
        isDebug = debug;
    }
}
