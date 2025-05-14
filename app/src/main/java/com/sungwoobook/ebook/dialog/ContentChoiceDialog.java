package com.sungwoobook.ebook.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sungwoobook.ebook.Model.ContentModel;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.Viewer.PdfViewerActivity;
import com.sungwoobook.ebook.Viewer.VideoViewerActivity;

/**
 * 책 / 영상 선택 다이얼로그 (v2)
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

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sample_selector);

        // ✅ 배경 둥글게
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, context.getResources().getDisplayMetrics()), // width
                    WindowManager.LayoutParams.WRAP_CONTENT // height: wrap_content로 변경
            );
        }

        // ✅ 버튼 참조 (참조 코드에서 수정된 ID 사용)
        Button btnPdf = dialog.findViewById(R.id.btnBook);
        Button btnVideo = dialog.findViewById(R.id.btnVideo);

        btnPdf.setOnClickListener(v -> {
            if (item.getBookUrl() != null && !item.getBookUrl().isEmpty()) {
                Intent intent = new Intent(context, PdfViewerActivity.class);
                intent.putExtra("pdfUrl", item.getBookUrl()); // 기존 키 유지
                context.startActivity(intent);
                dialog.dismiss();
            } else {
                Toast.makeText(context, "책 URL이 없습니다", Toast.LENGTH_SHORT).show();
            }
        });

        btnVideo.setOnClickListener(v -> {
            if (item.getVideoUrl() != null && !item.getVideoUrl().isEmpty()) {
                Intent intent = new Intent(context, VideoViewerActivity.class); // 기존 ViewerActivity 유지
                intent.putExtra("videoUrl", item.getVideoUrl()); // 기존 키 유지
                context.startActivity(intent);
                dialog.dismiss();
            } else {
                Toast.makeText(context, "영상 URL이 없습니다", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
