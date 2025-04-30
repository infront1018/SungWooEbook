/**
 * 📌 파일 경로: com.sungwoobook.ebook.Viewer.PdfViewerActivity.java
 * 📌 설명: PDF 캐시 재활용 + 디렉토리 생성(mkdirs) 오류 방지 최종 버전
 */

package com.sungwoobook.ebook.Viewer;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;
import com.sungwoobook.ebook.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PdfViewerActivity extends AppCompatActivity {

    private static final String TAG = "PdfViewerActivity";
    private PDFView pdfView;
    private String pdfUrl;
    private File cachedPdfFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        pdfView = findViewById(R.id.pdfView);
        pdfUrl = getIntent().getStringExtra("pdfUrl");

        Log.d(TAG, "PDF URL: " + pdfUrl);

        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Toast.makeText(this, "PDF URL이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ✅ URL에서 파일 이름 추출 및 경로 생성
        String fileName = Uri.parse(pdfUrl).getLastPathSegment();
        if (fileName == null || fileName.isEmpty()) {
            fileName = "cached_pdf.pdf";
        }

        cachedPdfFile = new File(getCacheDir(), fileName);

        // ✅ 상위 디렉토리 자동 생성 (중첩 경로 포함 가능)
        File parentDir = cachedPdfFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            Log.d(TAG, "📁 디렉토리 생성됨: " + created + " → " + parentDir.getAbsolutePath());
        }

        if (cachedPdfFile.exists()) {
            Log.d(TAG, "📄 캐시 파일 존재 → 바로 렌더링");
            renderPdfFromFile(cachedPdfFile);
        } else {
            Log.d(TAG, "⬇️ 캐시 없음 → 다운로드 시작");
            new Thread(() -> {
                try {
                    downloadPdfToFile(pdfUrl, cachedPdfFile);
                    runOnUiThread(() -> renderPdfFromFile(cachedPdfFile));
                } catch (Exception e) {
                    Log.e(TAG, "❌ PDF 다운로드 실패", e);
                    runOnUiThread(() -> Toast.makeText(this, "PDF 로딩 실패", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }
    }

    private void renderPdfFromFile(File file) {
        pdfView.fromFile(file)
                .enableSwipe(true)
                .enableDoubletap(true)
                .swipeHorizontal(false)
                .load();
        Log.d(TAG, "✅ PDF 렌더링 성공");
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
}