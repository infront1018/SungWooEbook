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

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.sungwoobook.ebook.R;

/**
 * PDF 미리보기 Fragment.
 * - barteksc AndroidPdfViewer 라이브러리 사용 (fromUrl 스트리밍)
 * - 좌우 스와이프 모드 (swipeHorizontal = true)
 * - onDestroyView() 에서 pdfView.recycle() 반드시 호출
 */
public class PdfViewerFragment extends Fragment {

    private static final String TAG      = "PdfViewerFragment";
    private static final String ARG_URL  = "pdf_url";
    private static final String ARG_PATH = "storage_path";

    private PDFView     pdfView;
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

        pdfView          = view.findViewById(R.id.pdfView);
        layoutLoading    = view.findViewById(R.id.layoutLoading);
        txtLoadingStatus = view.findViewById(R.id.txtLoadingStatus);
        txtPageNumber    = view.findViewById(R.id.txtPageNumber);
        ImageButton btnClose = view.findViewById(R.id.btnClose);

        btnClose.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

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
                // Storage 경로(고정값)를 기반으로 캐시 파일명 생성하여 중복 다운로드 방지
                java.io.File cacheDir = requireContext().getCacheDir();
                java.io.File pdfFile = new java.io.File(cacheDir, "book_" + Math.abs(storagePath.hashCode()) + ".pdf");
                
                if (!pdfFile.exists() || pdfFile.length() < 100) { 
                    Log.d(TAG, "Downloading PDF: " + urlString);
                    java.net.URL url = new java.net.URL(urlString);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.connect();

                    if (connection.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
                        throw new Exception("Server HTTP " + connection.getResponseCode());
                    }

                    java.io.InputStream input = connection.getInputStream();
                    java.io.OutputStream output = new java.io.FileOutputStream(pdfFile);

                    byte[] data = new byte[8192];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();
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
        txtLoadingStatus.setText("PDF 로딩 중...");

        pdfView.fromFile(pdfFile)
                .defaultPage(0)
                .swipeHorizontal(true)   // 좌우 스와이프
                .pageSnap(true)          // 페이지 단위로 걸림
                .autoSpacing(true)
                .pageFling(true)
                .enableSwipe(true)
                .enableDoubletap(true)
                .scrollHandle(new DefaultScrollHandle(requireContext()))
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        layoutLoading.setVisibility(View.GONE);
                        txtPageNumber.setVisibility(View.VISIBLE);
                        txtPageNumber.setText("1 / " + nbPages);
                        Log.d(TAG, "PDF 로딩 완료: " + nbPages + "페이지");
                    }
                })
                .onPageChange(new OnPageChangeListener() {
                    @Override
                    public void onPageChanged(int page, int pageCount) {
                        txtPageNumber.setText((page + 1) + " / " + pageCount);
                    }
                })
                .onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        layoutLoading.setVisibility(View.GONE);
                        Log.e(TAG, "PDF 렌더링 실패", t);
                        Toast.makeText(getContext(), "PDF 로딩 실패", Toast.LENGTH_SHORT).show();
                    }
                })
                .load();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            // PDF 뷰어에서는 기기 센서 설정에 따라 가로/세로 자동 회전 허용
            getActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        hideSystemUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
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
        if (pdfView != null) {
            pdfView.recycle();
            pdfView = null;
        }
    }
}
