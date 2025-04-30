/**
 * 📌 파일 경로: com.sungwoobook.ebook.Viewer.VideoViewerActivity.java
 * 📌 설명: 영상 URL을 다운로드하여 앱 캐시에 저장 후 VideoView로 재생
 */

package com.sungwoobook.ebook.Viewer;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.sungwoobook.ebook.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class VideoViewerActivity extends AppCompatActivity {

    private static final String TAG = "VideoViewerActivity";
    private VideoView videoView;
    private String videoUrl;
    private File cachedVideoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_viewer);

        videoView = findViewById(R.id.videoView);
        videoUrl = getIntent().getStringExtra("videoUrl");

        Log.d(TAG, "Video URL: " + videoUrl);

        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "영상 URL이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cachedVideoFile = new File(getCacheDir(), "cached_video.mp4");

        if (cachedVideoFile.exists()) {
            Log.d(TAG, "캐시된 영상 있음 → 바로 재생");
            playVideoFromFile(cachedVideoFile);
        } else {
            Log.d(TAG, "캐시된 영상 없음 → 다운로드 시작");
            new Thread(() -> {
                try {
                    downloadVideoToCache(videoUrl, cachedVideoFile);
                    runOnUiThread(() -> playVideoFromFile(cachedVideoFile));
                } catch (Exception e) {
                    Log.e(TAG, "영상 다운로드 실패", e);
                    runOnUiThread(() -> Toast.makeText(this, "영상 다운로드에 실패했습니다.", Toast.LENGTH_SHORT).show());
                }
            }).start();
        }
    }

    private void downloadVideoToCache(String urlString, File targetFile) throws Exception {
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
        Log.d(TAG, "영상 다운로드 완료: " + targetFile.getAbsolutePath());
    }

    private void playVideoFromFile(File file) {
        videoView.setVideoURI(Uri.fromFile(file));
        videoView.setMediaController(new MediaController(this));
        videoView.setOnPreparedListener(mp -> {
            Log.d(TAG, "영상 준비 완료 → 재생 시작");
            videoView.start();
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "영상 재생 오류: what=" + what + ", extra=" + extra);
            Toast.makeText(this, "영상 재생 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            return true;
        });
        videoView.requestFocus();
    }
}