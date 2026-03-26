package com.sungwoobook.ebook.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.Fragment.PdfViewerFragment;
import com.sungwoobook.ebook.Fragment.VideoPlayerFragment;
import com.sungwoobook.ebook.model.Book;

/**
 * 도서 클릭 시 표시되는 BottomSheet: PDF 미리보기 / 영상 보기 선택.
 */
public class PreviewBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_BOOK_TITLE     = "book_title";
    private static final String ARG_THUMBNAIL_URL  = "thumbnail_url";
    private static final String ARG_PDF_PATH       = "pdf_path";
    private static final String ARG_VIDEO_PATH     = "video_path";
    private static final String ARG_HAS_VIDEO      = "has_video";

    public static PreviewBottomSheet newInstance(Book book, String seriesTitle) {
        PreviewBottomSheet sheet = new PreviewBottomSheet();
        Bundle args = new Bundle();
        
        // 시리즈명 + 도서명 조합
        String fullTitle = seriesTitle;
        if (book.getTitle() != null && !book.getTitle().isEmpty()) {
            fullTitle += " - " + book.getTitle();
        }
        
        args.putString(ARG_BOOK_TITLE,    fullTitle);
        args.putString(ARG_THUMBNAIL_URL, book.getThumbnailUrl());
        args.putString(ARG_PDF_PATH,      book.getBookUrl());
        args.putString(ARG_VIDEO_PATH,    book.getVideoUrl());
        boolean hasVideo = book.getVideoUrl() != null && !book.getVideoUrl().isEmpty();
        args.putBoolean(ARG_HAS_VIDEO,    hasVideo);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        String bookTitle    = args.getString(ARG_BOOK_TITLE, "");
        String thumbnailUrl = args.getString(ARG_THUMBNAIL_URL, "");
        String pdfPath      = args.getString(ARG_PDF_PATH, "");
        String videoPath    = args.getString(ARG_VIDEO_PATH, "");
        boolean hasVideo    = args.getBoolean(ARG_HAS_VIDEO, false);

        // UI 바인딩
        TextView txtTitle    = view.findViewById(R.id.txtBookTitle);
        android.widget.ImageView imgCover = view.findViewById(R.id.imgBottomSheetCover);
        android.widget.Button btnPdf      = view.findViewById(R.id.btnPreviewPdf);
        android.widget.Button btnVideo    = view.findViewById(R.id.btnPreviewVideo);

        txtTitle.setText(bookTitle);

        // 표지 이미지 로딩 (Glide 사용)
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.default_thumbnail)
                    .error(R.drawable.default_thumbnail)
                    .into(imgCover);
        }

        // 영상 없으면 버튼 숨김
        btnVideo.setVisibility(hasVideo ? View.VISIBLE : View.GONE);

        btnPdf.setOnClickListener(v -> {
            if (pdfPath == null || pdfPath.isEmpty()) {
                Toast.makeText(getContext(), "PDF 경로가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            dismiss();
            navigateTo(PdfViewerFragment.newInstance(pdfPath));
        });

        btnVideo.setOnClickListener(v -> {
            if (videoPath == null || videoPath.isEmpty()) {
                Toast.makeText(getContext(), "영상 경로가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            dismiss();
            navigateTo(VideoPlayerFragment.newInstance(videoPath));
        });
    }

    private void navigateTo(androidx.fragment.app.Fragment fragment) {
        if (getActivity() == null) return;
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
