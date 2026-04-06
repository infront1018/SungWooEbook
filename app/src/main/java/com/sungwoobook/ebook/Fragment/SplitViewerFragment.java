package com.sungwoobook.ebook.Fragment;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Guideline;
import androidx.fragment.app.Fragment;

import com.sungwoobook.ebook.MainActivity;
import com.sungwoobook.ebook.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

/**
 * 전집(PDF)과 영상(Video)을 한 화면에 동시에 보여주는 분할 뷰 컴포넌트.
 * 분리선(Divider)을 드래그하여 비율을 조절할 수 있습니다.
 */
public class SplitViewerFragment extends Fragment {

    private static final String ARG_PDF_URL = "pdf_url";
    private static final String ARG_STORAGE_PATH = "storage_path";
    private static final String ARG_VIDEO_URLS = "video_urls";
    private static final String ARG_VIDEO_TITLES = "video_titles";

    private Guideline guideline;
    private View splitDivider;
    private View dividerHandle;
    private ChipGroup videoChipGroup;
    private View videoTabScroll;

    private float dX = 0f;
    private float dY = 0f;

    public static SplitViewerFragment newInstance(String pdfUrl, String storagePath, ArrayList<String> videoUrls, ArrayList<String> videoTitles) {
        SplitViewerFragment f = new SplitViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PDF_URL, pdfUrl);
        args.putString(ARG_STORAGE_PATH, storagePath);
        args.putStringArrayList(ARG_VIDEO_URLS, videoUrls);
        args.putStringArrayList(ARG_VIDEO_TITLES, videoTitles);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_split_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        guideline = view.findViewById(R.id.guideline);
        splitDivider = view.findViewById(R.id.splitDivider);
        dividerHandle = view.findViewById(R.id.dividerHandle);
        videoChipGroup = view.findViewById(R.id.videoChipGroup);
        videoTabScroll = view.findViewById(R.id.videoTabScroll);

        Bundle args = getArguments();
        if (args != null) {
            String pdfUrl = args.getString(ARG_PDF_URL, "");
            String storagePath = args.getString(ARG_STORAGE_PATH, "");
            ArrayList<String> videoUrls = args.getStringArrayList(ARG_VIDEO_URLS);
            ArrayList<String> videoTitles = args.getStringArrayList(ARG_VIDEO_TITLES);

            // 1. 좌측: 전자책 컨테이너 부착
            if (getChildFragmentManager().findFragmentById(R.id.bookContainer) == null) {
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.bookContainer, PdfViewerFragment.newInstance(pdfUrl, storagePath))
                        .commit();
            }

            // 2. 우측: 최초 영상 컨테이너 부착 및 탭 생성
            if (videoUrls != null && !videoUrls.isEmpty()) {
                if (getChildFragmentManager().findFragmentById(R.id.videoContainer) == null) {
                    getChildFragmentManager().beginTransaction()
                            .replace(R.id.videoContainer, VideoPlayerFragment.newInstance(videoUrls.get(0)))
                            .commit();
                }

                // 영상이 2개 이상일 때만 탭 표시
                if (videoUrls.size() > 1 && videoTitles != null && videoTitles.size() == videoUrls.size()) {
                    videoTabScroll.setVisibility(View.VISIBLE);
                    setupVideoTabs(videoUrls, videoTitles);
                } else {
                    videoTabScroll.setVisibility(View.GONE);
                }
            }
        }

        setupDividerDrag(view);
        applyOrientation(getResources().getConfiguration().orientation);
    }

    private void setupVideoTabs(ArrayList<String> urls, ArrayList<String> titles) {
        videoChipGroup.removeAllViews();
        
        for (int i = 0; i < urls.size(); i++) {
            final String url = urls.get(i);
            String title = titles.get(i);
            
            Chip chip = new Chip(requireContext());
            chip.setText("🎬 " + title);
            chip.setCheckable(true);
            chip.setClickable(true);
            
            // 첫 번째 탭 기본 선택
            if (i == 0) {
                chip.setChecked(true);
            }
            
            // 🚀 7년 차의 자연스러운 UX: 탭 클릭 시 플레이어 교체
            chip.setOnClickListener(v -> {
                getChildFragmentManager().beginTransaction()
                        // 커스텀 애니메이션으로 부드러운 전환 효과
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.videoContainer, VideoPlayerFragment.newInstance(url))
                        .commit();
            });
            
            videoChipGroup.addView(chip);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDividerDrag(View root) {
        splitDivider.setOnTouchListener((v, event) -> {
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) guideline.getLayoutParams();

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (isLandscape) {
                        dX = v.getX() - event.getRawX();
                    } else {
                        dY = v.getY() - event.getRawY();
                    }
                    dividerHandle.setAlpha(0.6f);
                    break;

                case MotionEvent.ACTION_MOVE:
                    float percent;
                    if (isLandscape) {
                        float newX = event.getRawX() + dX;
                        percent = newX / root.getWidth();
                    } else {
                        float newY = event.getRawY() + dY;
                        percent = newY / root.getHeight();
                    }

                    // 안전 범위를 20% ~ 80% 로 고정하여 화면이 사라지는 것 방지
                    percent = Math.max(0.2f, Math.min(percent, 0.8f));
                    layoutParams.guidePercent = percent;
                    guideline.setLayoutParams(layoutParams);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dividerHandle.setAlpha(1.0f);
                    break;
            }
            return true;
        });
    }

    private void applyOrientation(int orientation) {
        ConstraintLayout root = (ConstraintLayout) getView();
        if (root == null) return;

        ConstraintSet set = new ConstraintSet();
        set.clone(root);

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 가로 모드 (좌측: 책, 우측: 영상)
            set.create(R.id.guideline, ConstraintSet.VERTICAL_GUIDELINE);
            set.setGuidelinePercent(R.id.guideline, 0.5f);

            // bookContainer (좌측)
            set.clear(R.id.bookContainer);
            set.constrainWidth(R.id.bookContainer, 0); // 0dp (match_constraint)
            set.constrainHeight(R.id.bookContainer, 0);
            set.connect(R.id.bookContainer, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            set.connect(R.id.bookContainer, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            set.connect(R.id.bookContainer, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            set.connect(R.id.bookContainer, ConstraintSet.END, R.id.splitDivider, ConstraintSet.START);

            // videoContainer (우측)
            set.clear(R.id.videoContainer);
            set.constrainWidth(R.id.videoContainer, 0);
            set.constrainHeight(R.id.videoContainer, 0);
            set.connect(R.id.videoContainer, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            set.connect(R.id.videoContainer, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            set.connect(R.id.videoContainer, ConstraintSet.START, R.id.splitDivider, ConstraintSet.END);
            set.connect(R.id.videoContainer, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

            // splitDivider (중앙 세로바)
            set.clear(R.id.splitDivider);
            set.constrainWidth(R.id.splitDivider, (int) (6 * getResources().getDisplayMetrics().density));
            set.constrainHeight(R.id.splitDivider, 0);
            set.connect(R.id.splitDivider, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            set.connect(R.id.splitDivider, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            set.connect(R.id.splitDivider, ConstraintSet.START, R.id.guideline, ConstraintSet.START);
            set.connect(R.id.splitDivider, ConstraintSet.END, R.id.guideline, ConstraintSet.START);

            // dividerHandle (분리바 손잡이)
            set.clear(R.id.dividerHandle);
            set.constrainWidth(R.id.dividerHandle, (int) (16 * getResources().getDisplayMetrics().density));
            set.constrainHeight(R.id.dividerHandle, (int) (48 * getResources().getDisplayMetrics().density));
            set.connect(R.id.dividerHandle, ConstraintSet.TOP, R.id.splitDivider, ConstraintSet.TOP);
            set.connect(R.id.dividerHandle, ConstraintSet.BOTTOM, R.id.splitDivider, ConstraintSet.BOTTOM);
            set.connect(R.id.dividerHandle, ConstraintSet.START, R.id.splitDivider, ConstraintSet.START);
            set.connect(R.id.dividerHandle, ConstraintSet.END, R.id.splitDivider, ConstraintSet.END);

        } else {
            // 세로 모드 (상단: 영상, 하단: 책)
            set.create(R.id.guideline, ConstraintSet.HORIZONTAL_GUIDELINE);
            set.setGuidelinePercent(R.id.guideline, 0.4f);

            // videoContainer (상단)
            set.clear(R.id.videoContainer);
            set.constrainWidth(R.id.videoContainer, 0);
            set.constrainHeight(R.id.videoContainer, 0);
            set.connect(R.id.videoContainer, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            set.connect(R.id.videoContainer, ConstraintSet.BOTTOM, R.id.splitDivider, ConstraintSet.TOP);
            set.connect(R.id.videoContainer, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            set.connect(R.id.videoContainer, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

            // bookContainer (하단)
            set.clear(R.id.bookContainer);
            set.constrainWidth(R.id.bookContainer, 0);
            set.constrainHeight(R.id.bookContainer, 0);
            set.connect(R.id.bookContainer, ConstraintSet.TOP, R.id.splitDivider, ConstraintSet.BOTTOM);
            set.connect(R.id.bookContainer, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
            set.connect(R.id.bookContainer, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            set.connect(R.id.bookContainer, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);

            // splitDivider (중앙 가로바)
            set.clear(R.id.splitDivider);
            set.constrainWidth(R.id.splitDivider, 0);
            set.constrainHeight(R.id.splitDivider, (int) (6 * getResources().getDisplayMetrics().density));
            set.connect(R.id.splitDivider, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            set.connect(R.id.splitDivider, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            set.connect(R.id.splitDivider, ConstraintSet.TOP, R.id.guideline, ConstraintSet.TOP);
            set.connect(R.id.splitDivider, ConstraintSet.BOTTOM, R.id.guideline, ConstraintSet.TOP);

            // dividerHandle (분리바 손잡이)
            set.clear(R.id.dividerHandle);
            set.constrainWidth(R.id.dividerHandle, (int) (48 * getResources().getDisplayMetrics().density));
            set.constrainHeight(R.id.dividerHandle, (int) (16 * getResources().getDisplayMetrics().density));
            set.connect(R.id.dividerHandle, ConstraintSet.TOP, R.id.splitDivider, ConstraintSet.TOP);
            set.connect(R.id.dividerHandle, ConstraintSet.BOTTOM, R.id.splitDivider, ConstraintSet.BOTTOM);
            set.connect(R.id.dividerHandle, ConstraintSet.START, R.id.splitDivider, ConstraintSet.START);
            set.connect(R.id.dividerHandle, ConstraintSet.END, R.id.splitDivider, ConstraintSet.END);
        }

        set.applyTo(root);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyOrientation(newConfig.orientation);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setGlobalUiVisibility(View.GONE);
            getActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setGlobalUiVisibility(View.VISIBLE);
            getActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }
}
