package com.sungwoobook.ebook.Fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sungwoobook.ebook.view.PageFlipView;
import com.sungwoobook.ebook.R;

/**
 * PDF 미리보기 Fragment.
 * - 프리미엄 2.5D Page Flip UI 적용 (PdfRenderer 사용)
 * - 2단 보기(Landscape) 및 자동 넘기기 애니메이션 지원
 */
public class PdfViewerFragment extends Fragment {

    private static final String TAG      = "PdfViewerFragment";
    private static final String ARG_URL  = "pdf_url";
    private static final String ARG_PATH = "storage_path";

    private PageFlipView pageFlipView;
    private LinearLayout layoutLoading;
    private TextView    txtLoadingStatus;
    private TextView    txtPageNumber;

    // ── 정적 팩토리 ──────────────────────────────────────────────────────────

    public static PdfViewerFragment newInstance(String pdfUrl, String storagePath) {
        PdfViewerFragment f = new PdfViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, pdfUrl);
        args.putString(ARG_PATH, storagePath);
        f.setArguments(args);
        return f;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pdf_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pageFlipView     = view.findViewById(R.id.pageFlipView);
        layoutLoading    = view.findViewById(R.id.layoutLoading);
        txtLoadingStatus = view.findViewById(R.id.txtLoadingStatus);
        txtPageNumber    = view.findViewById(R.id.txtPageNumber);
        // PDF 로드 시작

        String pdfUrl  = getArguments() != null ? getArguments().getString(ARG_URL) : null;
        String pdfPath = getArguments() != null ? getArguments().getString(ARG_PATH) : "temp_pdf";
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Toast.makeText(getContext(), "PDF 경로가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        loadPdf(pdfUrl, pdfPath);
    }

    private void loadPdf(String urlString, String storagePath) {
        layoutLoading.setVisibility(View.VISIBLE);
        txtLoadingStatus.setText("PDF 다운로드 중...");

        // 🛠️ Firebase Storage 경로 처리 및 URL 해소
        if (!urlString.startsWith("http")) {
            Log.d(TAG, "Resolving PDF Storage Path: " + urlString);
            com.sungwoobook.ebook.model.FirebaseManager.getInstance().getDownloadUrl(urlString,
                    uri -> {
                        Log.d(TAG, "Resolved PDF URL: " + uri.toString());
                        loadPdfInternal(uri.toString(), storagePath);
                    },
                    e -> {
                        Log.e(TAG, "Failed to resolve PDF URL for: " + urlString, e);
                        Toast.makeText(getContext(), "PDF 경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        layoutLoading.setVisibility(View.GONE);
                    });
        } else {
            loadPdfInternal(urlString, storagePath);
        }
    }

    private void loadPdfInternal(String urlString, String storagePath) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 📂 저장 경로 통일: getFilesDir()/ebook_cache/ (BookPrefetcher와 동일)
                java.io.File dir = new java.io.File(requireContext().getFilesDir(), "ebook_cache");
                if (!dir.exists()) dir.mkdirs();

                java.io.File pdfFile = new java.io.File(dir, "book_" + Math.abs(storagePath.hashCode()) + ".pdf");
                
                if (!pdfFile.exists() || pdfFile.length() < 1024) { 
                    Log.d(TAG, "Downloading PDF: " + urlString);
                    java.net.URL url = new java.net.URL(urlString);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.connect();

                    if (connection.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
                        throw new Exception("Server HTTP " + connection.getResponseCode());
                    }

                    java.io.InputStream input = connection.getInputStream();
                    int totalSize = connection.getContentLength();
                    java.io.File tmpFile = new java.io.File(dir, pdfFile.getName() + ".tmp");
                    java.io.OutputStream output = new java.io.FileOutputStream(tmpFile);

                    byte[] data = new byte[8192];
                    int count;
                    long totalDownloaded = 0;
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                        totalDownloaded += count;
                        
                        if (totalSize > 0) {
                            final int progress = (int) (totalDownloaded * 100 / totalSize);
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                if (isAdded()) txtLoadingStatus.setText("PDF 다운로드 중... (" + progress + "%)");
                            });
                        }
                    }
                    output.flush();
                    output.close();
                    input.close();
                    
                    // 완료 후 안전하게 이름 변경 (원자적 쓰기)
                    if (tmpFile.renameTo(pdfFile)) {
                        Log.i(TAG, "Download completed and renamed: " + pdfFile.getName());
                    } else {
                        throw new Exception("Failed to rename temporary PDF file");
                    }
                } else {
                    Log.d(TAG, "Using cached PDF: " + pdfFile.getAbsolutePath());
                }

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (isAdded()) {
                        renderPdfFromFile(pdfFile);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "PDF 다운로드 실패", e);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (isAdded()) {
                        layoutLoading.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "PDF 다운로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void renderPdfFromFile(java.io.File pdfFile) {
        txtLoadingStatus.setText("프리미엄 뷰어 준비 중...");

        pageFlipView.setPdfFile(pdfFile);
        pageFlipView.setOnPageChangeListener((currentPage, totalPages) -> {
            txtPageNumber.setText((currentPage + 1) + " / " + totalPages);
        });

        // 초기 페이지 정보 설정
        txtPageNumber.setVisibility(View.VISIBLE);
        txtPageNumber.setText("1 / " + pageFlipView.getTotalPages());
        layoutLoading.setVisibility(View.GONE);
        
        Log.d(TAG, "PageFlipView 렌더링 시작: " + pageFlipView.getTotalPages() + "페이지");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof com.sungwoobook.ebook.MainActivity) {
            com.sungwoobook.ebook.MainActivity main = (com.sungwoobook.ebook.MainActivity) getActivity();
            // 전역 UI 영역 숨김 (상단바, 하단바, 배경 이미지 등)
            main.setGlobalUiVisibility(android.view.View.GONE);
            
            // PDF 뷰어에서는 기기 센서 설정에 따라 가로/세로 자동 회전 허용
            getActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        hideSystemUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof com.sungwoobook.ebook.MainActivity) {
            com.sungwoobook.ebook.MainActivity main = (com.sungwoobook.ebook.MainActivity) getActivity();
            // 전역 UI 영역 복구
            main.setGlobalUiVisibility(android.view.View.VISIBLE);
            
            // PDF 뷰어를 벗어나면 다시 메인 설정(세로 모드)으로 복구
            getActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        showSystemUI();
    }

    private void hideSystemUI() {
        if (getActivity() != null && getActivity().getWindow() != null) {
            View decorView = getActivity().getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    private void showSystemUI() {
        if (getActivity() != null && getActivity().getWindow() != null) {
            View decorView = getActivity().getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // ✅ 반드시 리소스 해제
        if (pageFlipView != null) {
            pageFlipView.recycle();
            pageFlipView = null;
        }
    }
}
