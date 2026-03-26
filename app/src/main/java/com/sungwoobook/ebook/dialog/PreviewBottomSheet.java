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
    private static final String ARG_BOOK_SUBTITLE  = "book_subtitle";
    private static final String ARG_PDF_PATH       = "pdf_path";
    private static final String ARG_VIDEO_PATH     = "video_path";
    private static final String ARG_HAS_VIDEO      = "has_video";

    /**
     * 정적 팩토리: Book 객체로부터 BottomSheet 인스턴스 생성.
     * (Book을 직접 Bundle에 Serializable/Parcelable로 넣는 대신
     *  필요 필드만 분리하여 의존성을 낮춤)
     */
    public static PreviewBottomSheet newInstance(Book book, String seriesTitle) {
        PreviewBottomSheet sheet = new PreviewBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_BOOK_TITLE,    seriesTitle);
        args.putString(ARG_BOOK_SUBTITLE, book.getTitle() != null ? book.getTitle() : "");
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
        String bookSubtitle = args.getString(ARG_BOOK_SUBTITLE, "");
        String pdfPath      = args.getString(ARG_PDF_PATH, "");
        String videoPath    = args.getString(ARG_VIDEO_PATH, "");
        boolean hasVideo    = args.getBoolean(ARG_HAS_VIDEO, false);

        ((TextView) view.findViewById(R.id.txtBookTitle)).setText(bookTitle);
        ((TextView) view.findViewById(R.id.txtBookSubtitle)).setText(bookSubtitle);

        Button btnPdf   = view.findViewById(R.id.btnPreviewPdf);
        Button btnVideo = view.findViewById(R.id.btnPreviewVideo);

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
