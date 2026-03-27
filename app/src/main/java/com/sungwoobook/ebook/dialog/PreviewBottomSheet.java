package com.sungwoobook.ebook.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.sungwoobook.ebook.MainActivity;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.Fragment.VideoPlayerFragment;
import com.sungwoobook.ebook.Fragment.PdfViewerFragment;
import com.sungwoobook.ebook.Fragment.BaseGalleryFragment;
import com.sungwoobook.ebook.model.Book;
import com.sungwoobook.ebook.model.FavoriteManager;
import com.sungwoobook.ebook.model.StreamingProvider;

import java.util.List;

/**
 * 도서 상세 정보 및 미리보기(PDF/영상) 팝업.
 */
public class PreviewBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_BOOK_ID         = "book_id";
    private static final String ARG_BOOK_TITLE      = "book_title";
    private static final String ARG_THUMBNAIL_URL   = "thumbnail_url";
    private static final String ARG_BOOK_URL        = "book_url";
    private static final String ARG_SERIES_NAME     = "series_name";

    public static PreviewBottomSheet newInstance(String bookId, String title, int volume, String seriesName, String thumbnailUrl, String bookUrl) {
        PreviewBottomSheet f = new PreviewBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_BOOK_ID, bookId);
        args.putString(ARG_BOOK_TITLE, title);
        args.putString(ARG_SERIES_NAME, seriesName);
        args.putString(ARG_THUMBNAIL_URL, thumbnailUrl);
        args.putString(ARG_BOOK_URL, bookUrl);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        String bookId       = args.getString(ARG_BOOK_ID, "");
        String bookTitle    = args.getString(ARG_BOOK_TITLE, "");
        String seriesName   = args.getString(ARG_SERIES_NAME, "");
        String thumbnailUrl = args.getString(ARG_THUMBNAIL_URL, "");
        String bookUrl      = args.getString(ARG_BOOK_URL, "");

        TextView txtTitle    = view.findViewById(R.id.txtBookTitle);
        TextView txtDesc     = view.findViewById(R.id.txtSeriesDescription);
        android.widget.ImageView imgCover = view.findViewById(R.id.imgBottomSheetCover);
        MaterialButton btnPdf      = view.findViewById(R.id.btnPreviewPdf);
        MaterialButton btnVideo1   = view.findViewById(R.id.btnPreviewVideo);
        MaterialButton btnVideo2   = view.findViewById(R.id.btnPreviewVideo2);
        
        // 초기 상태: 영상 버튼 숨김
        btnVideo1.setVisibility(View.GONE);
        btnVideo2.setVisibility(View.GONE);

        txtTitle.setText(bookTitle);
        txtDesc.setText(getSeriesDescription(seriesName, bookTitle));

        // 🖼️ 도서 표지 로딩 (Firebase Storage 경로 지원)
        if (imgCover != null && !thumbnailUrl.isEmpty()) {
            android.util.Log.d("PreviewBottomSheet", "Resolving Thumbnail: " + thumbnailUrl);
            if (!thumbnailUrl.startsWith("http")) {
                com.sungwoobook.ebook.model.FirebaseManager.getInstance().getDownloadUrl(thumbnailUrl,
                        uri -> {
                            if (isAdded() && getContext() != null) {
                                com.bumptech.glide.Glide.with(requireContext())
                                        .load(uri)
                                        .centerCrop()
                                        .placeholder(R.drawable.default_thumbnail)
                                        .error(R.drawable.default_thumbnail)
                                        .into(imgCover);
                            }
                        },
                        e -> {
                            android.util.Log.e("PreviewBottomSheet", "Thumbnail resolve failed", e);
                            if (isAdded()) imgCover.setImageResource(R.drawable.default_thumbnail);
                        });
            } else {
                com.bumptech.glide.Glide.with(requireContext())
                        .load(thumbnailUrl)
                        .centerCrop()
                        .placeholder(R.drawable.default_thumbnail)
                        .error(R.drawable.default_thumbnail)
                        .into(imgCover);
            }
        }

        // 🎬 스트리밍 영상 연동
        int volume = BaseGalleryFragment.extractVolumeNo(new Book() {{ setBookId(bookId); setTitle(bookTitle); }});
        List<StreamingProvider.StreamingVideo> videos = StreamingProvider.getVideos(seriesName, volume);
        
        if (videos != null && !videos.isEmpty()) {
            btnVideo1.setVisibility(View.VISIBLE);
            btnVideo1.setText("🎬 " + videos.get(0).type);
            btnVideo1.setOnClickListener(v -> {
                dismiss();
                navigateTo(VideoPlayerFragment.newInstance(videos.get(0).url));
            });

            if (videos.size() > 1) {
                btnVideo2.setVisibility(View.VISIBLE);
                btnVideo2.setText("🎬 " + videos.get(1).type);
                btnVideo2.setOnClickListener(v -> {
                    dismiss();
                    navigateTo(VideoPlayerFragment.newInstance(videos.get(1).url));
                });
            }
        }

        // 📖 전자책 PDF 버튼 (실제 bookUrl 사용)
        btnPdf.setOnClickListener(v -> {
            if (bookUrl == null || bookUrl.isEmpty()) {
                Toast.makeText(getContext(), "PDF 경로가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            dismiss();
            // PdfViewerFragment.newInstance(pdfUrl, storagePath)
            navigateTo(com.sungwoobook.ebook.Fragment.PdfViewerFragment.newInstance(bookUrl, bookId));
        });
        
        // 즐겨찾기 버튼 로직
        android.widget.ImageButton btnFav = view.findViewById(R.id.btnToggleFavorite);
        boolean currentIsFav = FavoriteManager.getInstance(requireContext()).isFavorite(bookId);
        btnFav.setImageResource(currentIsFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        if (currentIsFav) btnFav.setColorFilter(android.graphics.Color.parseColor("#E11D48"));
        else btnFav.clearColorFilter();

        btnFav.setOnClickListener(v -> {
            boolean nowFav = FavoriteManager.getInstance(requireContext()).toggleFavorite(bookId);
            btnFav.setImageResource(nowFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            if (nowFav) btnFav.setColorFilter(android.graphics.Color.parseColor("#E11D48"));
            else btnFav.clearColorFilter();
            Toast.makeText(getContext(), nowFav ? "즐겨찾기에 추가되었습니다." : "즐겨찾기에서 제거되었습니다.", Toast.LENGTH_SHORT).show();
        });
    }

    private String getSeriesDescription(String seriesName, String title) {
        if (seriesName.contains("꼬마과학") || title.contains("과학")) return "꼬마과학뒤집기(59권 + 동영상2종)\n본책 59권 + 애니메이션 + 실험영상";
        if (seriesName.contains("꼬마수학") || title.contains("수학")) return "꼬마수학뒤집기(50권 + 동영상2종)\n본책 50권 + 애니메이션 + 놀이영상";
        if (seriesName.contains("꼬마사회") || title.contains("사회")) return "꼬마사회뒤집기(40권 + 동영상40편)\n본책 40권 + 애니메이션 영상";
        if (seriesName.contains("꿈틀") || title.contains("꿈틀")) return "꿈틀이 첫 과학(43권 + 동요)\n본책 43권 + 세이펜 + 동요 40곡";
        if (seriesName.contains("릴라") || title.contains("릴라")) return "릴라팝 패키지(53권 + 동영상2종)\n총 54종(핑거/바디/스토리/플레이팝)";
        if (seriesName.contains("통합과학")) return "통합과학뒤집기(1~5권)\n강의 + 문제풀이 영상";
        return "성우주니어 브랜드관 엄선 도서";
    }

    private void navigateTo(Fragment fragment) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateTo(fragment, true);
        }
    }
}
