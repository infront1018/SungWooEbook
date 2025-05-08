package com.sungwoobook.ebook.dialog;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.sungwoobook.ebook.Model.ContentModel;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.Viewer.PdfViewerActivity;
import com.sungwoobook.ebook.Viewer.VideoViewerActivity;

/**
 * 책 / 영상 선택 다이얼로그
 */
public class ContentChoiceDialog {

    public static void show(Context context, ContentModel item) {
        // ✅ Activity 유효성 체크
        if (!(context instanceof AppCompatActivity)) return;
        AppCompatActivity activity = (AppCompatActivity) context;
        if (activity.isFinishing() || activity.isDestroyed()) {
            Log.e("Dialog", "Activity가 종료되어 Dialog를 띄울 수 없습니다.");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_content_choice, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        Button btnBook = dialogView.findViewById(R.id.btnBook);
        Button btnVideo = dialogView.findViewById(R.id.btnVideo);

        btnBook.setOnClickListener(v -> {
            if (item.getBookUrl() != null && !item.getBookUrl().isEmpty()) {
                Intent intent = new Intent(context, PdfViewerActivity.class);
                intent.putExtra("pdfUrl", item.getBookUrl());
                context.startActivity(intent);
                dialog.dismiss();
            } else {
                Toast.makeText(context, "책 URL이 없습니다", Toast.LENGTH_SHORT).show();
            }
        });

        btnVideo.setOnClickListener(v -> {
            if (item.getVideoUrl() != null && !item.getVideoUrl().isEmpty()) {
                Intent intent = new Intent(context, VideoViewerActivity.class);
                intent.putExtra("videoUrl", item.getVideoUrl());
                context.startActivity(intent);
                dialog.dismiss();
            } else {
                Toast.makeText(context, "영상 URL이 없습니다", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
