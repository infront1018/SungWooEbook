package com.sungwoobook.ebook;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class VideoPlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.playerView);
        String videoPath = getIntent().getStringExtra("videoPath");

        if (videoPath == null || videoPath.isEmpty()) {
            Toast.makeText(this, "비디오 경로가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initPlayer();
        loadVideoFromFirebase(videoPath);
    }

    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
    }

    private void loadVideoFromFirebase(String videoPath) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference videoRef = storage.getReference().child(videoPath);

        videoRef.getDownloadUrl()
                .addOnSuccessListener(uri -> playVideo(uri))
                .addOnFailureListener(e -> {
                    Toast.makeText(VideoPlayerActivity.this, "비디오 로드 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void playVideo(Uri uri) {
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
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
