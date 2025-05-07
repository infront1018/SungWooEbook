/**
 * 파일 경로: com.sungwoobook.ebook.Viewer.PdfViewerActivity.java
 * 설명: PDF 다운로드 + 캐시 저장 + 책장 넘기기 + 로딩 + 확대 + 더블탭 Zoom + 전체화면 + 페이지 호수 + 로딩 진행률 표시
 */

package com.sungwoobook.ebook.Viewer;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.adapter.PdfPageAdapter;
import com.sungwoobook.ebook.anim.BookFlipPageTransformer;
import com.sungwoobook.ebook.view.ZoomStateListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class PdfViewerActivity extends AppCompatActivity {

    private static final String TAG = "PdfViewerActivity";
    private TextView zoomInfoText;
    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private TextView pageNumberText;
    private TextView progressText; // ✅ 렌더링 진행 텍스트 표시용
    private String pdfUrl;
    private File cachedPdfFile;

    private ParcelFileDescriptor fileDescriptor;
    private PdfRenderer pdfRenderer;
    private List<Bitmap> pageBitmaps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ✅ 전체화면 모드 적용
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_pdf_viewer);

        // ✅ UI 요소 초기화
        zoomInfoText = findViewById(R.id.zoomInfoText);
        viewPager = findViewById(R.id.viewPager);
        viewPager.setVisibility(View.INVISIBLE); // 📌 렌더 전에는 숨김

        progressBar = findViewById(R.id.progressBar);
        pageNumberText = findViewById(R.id.pageNumberText);
        progressText = findViewById(R.id.progressText);

        pdfUrl = getIntent().getStringExtra("pdfUrl");
        Log.d(TAG, "PDF URL: " + pdfUrl);

        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Toast.makeText(this, "PDF URL이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ✨ URL 해시 기반으로 고유한 캐시 파일 생성
        String cacheFileName = getCacheFileName(pdfUrl);
        cachedPdfFile = new File(getCacheDir(), cacheFileName);

        // ✅ 상위 디렉토리 자동 생성
        File parentDir = cachedPdfFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            Log.d(TAG, "📁 디렉토리 생성됨: " + created + " → " + parentDir.getAbsolutePath());
        }

        if (cachedPdfFile.exists()) {
            Log.d(TAG, "📄 캐시 파일 존재 → 바로 렌더링");
            renderPdfWithFlipEffect(cachedPdfFile);
        } else {
            Log.d(TAG, "⬇️ 캐시 없음 → 다운로드 시작");
            progressBar.setVisibility(View.VISIBLE);
            new Thread(() -> {
                try {
                    downloadPdfToFile(pdfUrl, cachedPdfFile);
                    runOnUiThread(() -> renderPdfWithFlipEffect(cachedPdfFile));
                } catch (Exception e) {
                    Log.e(TAG, "❌ PDF 다운로드 실패", e);
                    runOnUiThread(() -> Toast.makeText(this, "PDF 로딩 실패", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }
    }

    private void renderPdfWithFlipEffect(File file) {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);
            pageBitmaps.clear();

            int totalPages = pdfRenderer.getPageCount();

            runOnUiThread(() -> {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(false);
                progressBar.setMax(totalPages);
                progressText.setVisibility(View.VISIBLE);
                progressText.setText("로딩 중... 0 / " + totalPages);
            });

            new Thread(() -> {
                for (int i = 0; i < totalPages; i++) {
                    try {
                        PdfRenderer.Page page = pdfRenderer.openPage(i);
                        Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        page.close();
                        pageBitmaps.add(bitmap);

                        int current = i + 1;
                        runOnUiThread(() -> {
                            progressBar.setProgress(current);
                            progressText.setText("로딩 중... " + current + " / " + totalPages);
                            Log.i(TAG, "로딩 중 ... " + current + " / " + totalPages);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "페이지 렌더링 실패: " + i, e);
                    }
                }

                runOnUiThread(() -> {
                    PdfPageAdapter adapter = new PdfPageAdapter(pageBitmaps, new ZoomStateListener() {
                        @Override
                        public void onZoomStarted() {
                            viewPager.setUserInputEnabled(false);
                            zoomInfoText.setVisibility(View.VISIBLE);
                            zoomInfoText.setText("확대를 종료하면 페이지 넘기기가 가능합니다");
                        }

                        @Override
                        public void onZoomEnded() {
                            viewPager.setUserInputEnabled(true);
                            zoomInfoText.setVisibility(View.GONE);
                        }
                    });

                    viewPager.setAdapter(adapter);
                    viewPager.setPageTransformer(new BookFlipPageTransformer());

                    viewPager.setVisibility(View.VISIBLE); // ✅ 렌더 완료 후 표시
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);

                    pageNumberText.setVisibility(View.VISIBLE);
                    pageNumberText.setText("1 / " + pageBitmaps.size());

                    viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                        @Override
                        public void onPageSelected(int position) {
                            pageNumberText.setText((position + 1) + " / " + pageBitmaps.size());
                        }
                    });

                    Log.d(TAG, "✅ PDF 렌더링 완료");
                });

            }).start();

        } catch (Exception e) {
            Log.e(TAG, "❌ PDF 렌더링 실패", e);
            Toast.makeText(this, "PDF 렌더링 오류 발생", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadPdfToFile(String urlString, File targetFile) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();

        try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }

        Log.d(TAG, "✅ PDF 다운로드 완료: " + targetFile.getAbsolutePath());
    }

    private String getCacheFileName(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(url.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "pdf_" + hex.toString() + ".pdf";
        } catch (Exception e) {
            Log.e(TAG, "해시 생성 실패", e);
            return "pdf_default_cache.pdf";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (fileDescriptor != null) fileDescriptor.close();
        } catch (Exception ignored) {}
    }
}
