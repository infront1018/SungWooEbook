/**
 * 파일 경로: com.sungwoobook.ebook.Viewer.PdfViewerActivity.java
 * 설명: PDF 다운로드 + 캐시 저장 + 책장 넘기기 + 로딩 진행차화 + 확대 + 더블탭 Zoom + 전체화면 + 페이지 번호 표시 포함 통합 버전
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PdfViewerActivity extends AppCompatActivity {

    private static final String TAG = "PdfViewerActivity";
    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private TextView pageNumberText;
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

        viewPager = findViewById(R.id.viewPager);
        progressBar = findViewById(R.id.progressBar);
        pageNumberText = findViewById(R.id.pageNumberText);

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

            for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
                PdfRenderer.Page page = pdfRenderer.openPage(i);
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                pageBitmaps.add(bitmap);
            }

            PdfPageAdapter adapter = new PdfPageAdapter(pageBitmaps);
            viewPager.setAdapter(adapter);
            viewPager.setPageTransformer(new BookFlipPageTransformer());
            progressBar.setVisibility(View.GONE);

            // ✅ 페이지 번호 표시
            pageNumberText.setVisibility(View.VISIBLE);
            pageNumberText.setText("1 / " + pageBitmaps.size());

            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    pageNumberText.setText((position + 1) + " / " + pageBitmaps.size());
                }
            });

            Log.d(TAG, "✅ PDF 렌더링 성공");
            Log.d(TAG, "✅ 책장 넘김 효과 + 줌 + 전체화면 + 페이지 번호 적용 완료");

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (fileDescriptor != null) fileDescriptor.close();
        } catch (Exception ignored) {}
    }
}
