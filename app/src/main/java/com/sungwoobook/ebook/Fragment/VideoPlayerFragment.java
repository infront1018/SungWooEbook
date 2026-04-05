package com.sungwoobook.ebook.Fragment;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;
import com.sungwoobook.ebook.R;

/**
 * 영상 플레이어 Fragment.
 * - ExoPlayer + ProgressiveMediaSource (프로그레시브 스트리밍)
 * - 가로 방향 전환 시 전체화면 전환
 * - onDestroyView() 에서 player.release() 반드시 호출
 */
public class VideoPlayerFragment extends Fragment {

    private static final String TAG     = "VideoPlayerFragment";
    private static final String ARG_URL = "video_url";

    private PlayerView  playerView;
    private ExoPlayer   player;

    // ── 정적 팩토리 ──────────────────────────────────────────────────────────

    public static VideoPlayerFragment newInstance(String videoUrl) {
        VideoPlayerFragment f = new VideoPlayerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, videoUrl);
        f.setArguments(args);
        return f;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerView = view.findViewById(R.id.playerView);

        // ✅ 영상 재생 시 전역 UI (내비게이션, 헤더) 숨김
        if (getActivity() instanceof com.sungwoobook.ebook.MainActivity) {
            ((com.sungwoobook.ebook.MainActivity) getActivity()).setGlobalUiVisibility(android.view.View.GONE);
        }

        String videoUrl = getArguments() != null ? getArguments().getString(ARG_URL) : null;
        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(getContext(), "영상 경로가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 가로 모드에서 시작하면 즉시 전체화면
        if (requireActivity().getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullScreen();
        }

        // 🛠️ Firebase Storage 경로 처리 로직 추가
        if (!videoUrl.startsWith("http")) {
            Log.d(TAG, "Resolving Video Storage Path: " + videoUrl);
            com.sungwoobook.ebook.model.FirebaseManager.getInstance().getDownloadUrl(videoUrl,
                    uri -> {
                        Log.d(TAG, "Resolved Video URL: " + uri.toString());
                        initPlayer(uri.toString());
                    },
                    e -> {
                        Log.e(TAG, "Failed to resolve Video URL for: " + videoUrl, e);
                        Toast.makeText(getContext(), "영상 경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            initPlayer(videoUrl);
        }
    }

    private void initPlayer(String url) {
        if (player != null) {
            player.release();
        }
        
        player = new ExoPlayer.Builder(requireContext()).build();
        playerView.setPlayer(player);

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(20000);

        try {
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
            // HLS(m3u8) 및 Progressive(mp4) 자동 감지
            MediaSource mediaSource = new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                    .createMediaSource(mediaItem);

            player.setMediaSource(mediaSource);
            player.prepare();
            
            // 영상 진입 시 항상 자동 재생
            player.setPlayWhenReady(true);
        } catch (Exception e) {
            Log.e(TAG, "Error creating MediaSource: " + url, e);
            Toast.makeText(getContext(), "영상 재생 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }

        player.addListener(new androidx.media3.common.Player.Listener() {
            @Override
            public void onPlayerError(@NonNull androidx.media3.common.PlaybackException error) {
                Log.e(TAG, "Player Error: " + error.getMessage() + " (Code: " + error.errorCode + ")", error);
                Toast.makeText(getContext(), "재생 오류: " + error.getErrorCodeName(), Toast.LENGTH_LONG).show();
            }
        });

        Log.d(TAG, "▶️ 영상 준비 완료: " + url);
    }



    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullScreen();
        } else {
            exitFullScreen();
        }
    }

    private void enterFullScreen() {
        if (getActivity() == null) return;
        getActivity().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void exitFullScreen() {
        if (getActivity() == null) return;
        getActivity().getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            // 영상 시청 시 가로/세로 자동 회전 허용
            getActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        if (requireActivity().getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullScreen();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            // 영상 뷰어를 벗어나면 다시 메인 설정(세로 모드)으로 복구
            getActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // ✅ 영상 종료 시 전역 UI 다시 표시
        if (getActivity() instanceof com.sungwoobook.ebook.MainActivity) {
            ((com.sungwoobook.ebook.MainActivity) getActivity()).setGlobalUiVisibility(android.view.View.VISIBLE);
        }

        // ✅ 반드시 리소스 해제
        if (player != null) {
            player.release();
            player = null;
        }
        if (playerView != null) {
            playerView.setPlayer(null);
            playerView = null;
        }
    }
}
