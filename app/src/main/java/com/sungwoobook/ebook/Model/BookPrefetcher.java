package com.sungwoobook.ebook.model;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 전집별 01권을 백그라운드에서 미리 다운로드하는 클래스.
 * - 앱 기동 시 실행되어 8개 전집의 1권을 순차적으로 로컬 저장소에 저장.
 */
public class BookPrefetcher {
    private static final String TAG = "BookPrefetcher";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void start(Context context) {
        Log.i(TAG, "🚀 Starting Book Prefetching...");
        
        FirebaseManager.getInstance().getAllBooks(books -> {
            executor.execute(() -> {
                int count = 0;
                for (Book b : books) {
                    if (b.getVolume() == 1) {
                        prefetchBook(context, b);
                        count++;
                    }
                }
                Log.i(TAG, "✅ Prefetching task completed for " + count + " books.");
            });
        }, e -> Log.e(TAG, "Failed to get books for prefetching", e));
    }

    private static void prefetchBook(Context context, Book b) {
        if (b.getBookUrl() == null || b.getBookUrl().isEmpty()) return;

        // 📂 저장 경로: getFilesDir()/ebook_cache/ (영구 캐시)
        File dir = new File(context.getFilesDir(), "ebook_cache");
        if (!dir.exists()) dir.mkdirs();

        // 파일명 규칙: book_{bookUrl_hash}.pdf (PdfViewerFragment와 동일하게)
        int hash = Math.abs(b.getBookUrl().hashCode());
        String fileName = "book_" + hash + ".pdf";
        File file = new File(dir, fileName);

        if (file.exists() && file.length() > 1024) {
            Log.d(TAG, "Already cached: " + b.getTitle() + " (Hash: " + hash + ")");
            return;
        }

        Log.i(TAG, "Queued for prefetch: " + b.getTitle() + " (Hash: " + hash + ")");

        // Firebase Storage 경로를 URL로 해소 후 다운로드
        File tmpFile = new File(dir, fileName + ".tmp");
        if (tmpFile.exists()) {
            Log.d(TAG, "Already prefetching in background: " + b.getTitle());
            return;
        }

        FirebaseManager.getInstance().getDownloadUrl(b.getBookUrl(), uri -> {
            executor.execute(() -> {
                downloadFile(uri.toString(), file, tmpFile, b.getTitle());
            });
        }, e -> Log.w(TAG, "Failed to get download URL for prefetch: " + b.getTitle()));
    }

    private static void downloadFile(String urlString, File destFile, File tmpFile, String title) {
        Log.d(TAG, "⬇️ Prefetching: " + title);
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Server returned HTTP " + connection.getResponseCode() + " for prefetch");
                return;
            }

            InputStream input = connection.getInputStream();
            OutputStream output = new FileOutputStream(tmpFile);

            byte[] data = new byte[8192];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            // 완료 후 원자적 이름 변경
            if (tmpFile.renameTo(destFile)) {
                Log.i(TAG, "Successfully prefetched and finalized: " + title);
            } else {
                Log.e(TAG, "Failed to rename prefetch file: " + title);
            }
        } catch (Exception e) {
            Log.e(TAG, "Prefetch download failed for: " + title, e);
            if (tmpFile.exists()) tmpFile.delete();
        }
    }
}
