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
 * 전집별 01~05권을 백그라운드에서 미리 다운로드하는 클래스.
 * - 앱 기동 시 실행되어 8개 전집의 1~5권을 순차적으로 로컬 저장소에 저장.
 */
public class BookPrefetcher {
    private static final String TAG = "BookPrefetcher";
    private static final ExecutorService executor = Executors.newFixedThreadPool(3); // 다운로드 병렬성 확보

    public static void start(Context context) {
        // 🚀 7년 차 개발자의 '3D 최우선' 전략: 초기 3.6초간은 프리페칭을 지연시켜 메인 엔진에 자원 집중 🛑
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Log.i(TAG, "🚀 Starting Delayed Book Prefetching (Vol 1-5)...");
            
            FirebaseManager.getInstance().getAllBooks(books -> {
                executor.execute(() -> {
                    // 스레드 우선순위 낮춤 (UI 방해 금지) 🛑
                    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                    int count = 0;
                    for (Book b : books) {
                        if (b.getVolume() >= 1 && b.getVolume() <= 5) {
                            prefetchBook(context, b, false);
                            count++;
                        }
                    }
                    Log.i(TAG, "✅ Prefetching task completed for " + count + " books.");
                });
            }, e -> Log.e(TAG, "Failed to get books for prefetching", e));
        }, 3600); // 3.6초 금쪽같은 시간 확보 🛑
    }
    
    // 🚀 7년 차 개발자의 '하이패스': 사용자가 클릭한 책을 즉시 최우선 다운로드 큐에 배치 🛑
    public static void prioritize(Context context, String bookUrl, String title) {
        Log.i(TAG, "⭐ Prioritizing download for: " + title);
        Book target = new Book();
        target.setBookUrl(bookUrl);
        target.setTitle(title);
        prefetchBook(context, target, true); // 우선순위 모드로 즉각 개시
    }

    private static void prefetchBook(Context context, Book b, boolean isPriority) {
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
        if (!isPriority && tmpFile.exists()) {
            Log.d(TAG, "Already prefetching in background: " + b.getTitle());
            return;
        }

        FirebaseManager.getInstance().getDownloadUrl(b.getBookUrl(), uri -> {
            executor.execute(() -> {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY); // 개별 다운로드 스레드도 저우선순위 🛑
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
