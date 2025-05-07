/**
 * 📌 파일 경로: com.sungwoobook.ebook.Viewer.VideoViewerActivity.java
 * 📌 설명: ExoPlayer 기반 Progressive Streaming + 각 영상별 캐시 저장 구조 + 전체화면 전환 (자동 + 버튼)
 */

package com.sungwoobook.ebook.Viewer;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.sungwoobook.ebook.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public class VideoViewerActivity extends AppCompatActivity {

    private static final String TAG = "VideoViewerActivity";
    private PlayerView playerView;
    private ExoPlayer player;
    private String videoUrl;
    private File cachedVideoFile;
    private boolean isFullScreen = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_viewer);

        playerView = findViewById(R.id.player_view);
        //fullscreenButton = findViewById(R.id.btn_fullscreen);
        videoUrl = getIntent().getStringExtra("videoUrl");

        Log.d(TAG, "Video URL: " + videoUrl);

        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "영상 URL이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 가로 모드면 자동 전체화면
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullScreen();
        }

        // 1️⃣ 영상 URL 해시로 고유한 캐시 파일 생성
        String cacheFileName = getCacheFileName(videoUrl);
        cachedVideoFile = new File(getCacheDir(), cacheFileName);

        if (cachedVideoFile.exists()) {
            Log.d(TAG, "📆 캐시된 영상 있음 → 로컴 재생");
            playVideoFromUri(Uri.fromFile(cachedVideoFile));
        } else {
            Log.d(TAG, "🚀 캐시 없음 → ExoPlayer로 URL 스트리믹 + 백관량드 다운로드");
            streamVideoFromUrl(videoUrl);

            new Thread(() -> {
                try {
                    downloadVideoToCache(videoUrl, cachedVideoFile);
                    Log.d(TAG, "✅ 백그라운드 다운로드 완료");
                } catch (Exception e) {
                    Log.e(TAG, "❌ 백그라운드 다운로드 실패", e);
                }
            }).start();
        }
    }

    /**
     * 🔁 ExoPlayer로 Progressive 스트리믹 재생
     */
    private void streamVideoFromUrl(String url) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(8000)
                .setReadTimeoutMs(15000);

        MediaItem mediaItem = MediaItem.fromUri(url);
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);

        player.setMediaSource(mediaSource);
        player.prepare();
        player.play();

        Log.d(TAG, "▶️ 스트리믹 재생 시작: " + url);
    }

    /**
     * 📆 로컴 캐시 파일에서 영상 재생
     */
    private void playVideoFromUri(Uri uri) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        Log.d(TAG, "▶️ 로컴 영상 재생 시작: " + uri);
    }

    /**
     * ⬇️ 영상 다운로드 후 캐시에 저장
     */
    private void downloadVideoToCache(String urlString, File targetFile) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
        Log.d(TAG, "📁 영상 다운로드 완료: " + targetFile.getAbsolutePath());
    }

    /**
     * 🧐 URL을 기반으로 고유한 캐시 파일명 생성 (SHA-1 해시)
     */
    private String getCacheFileName(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(url.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "video_" + hex.toString() + ".mp4";
        } catch (Exception e) {
            Log.e(TAG, "해시 생성 실패", e);
            return "video_default_cache.mp4";
        }
    }

    /**
     * 🧱 전체화면 진입
     */
    private void enterFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    /**
     * ⬅️ 전체화면 해제
     */
    private void exitFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterFullScreen();
        } else {
            exitFullScreen();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
