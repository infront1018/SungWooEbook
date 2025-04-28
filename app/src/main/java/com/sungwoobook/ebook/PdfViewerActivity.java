package com.sungwoobook.ebook;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.barteksc.pdfviewer.PDFView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {
    private PDFView pdfView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        pdfView = findViewById(R.id.pdfView);

        String pdfPath = getIntent().getStringExtra("pdfUrl");  // 실제로는 path
        if (pdfPath == null || pdfPath.isEmpty()) {
            Toast.makeText(this, "PDF 경로가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPdfFromFirebase(pdfPath);
    }

    private void loadPdfFromFirebase(String pdfPath) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child(pdfPath);

        try {
            File localFile = File.createTempFile("tempPdf", ".pdf");

            storageRef.getFile(localFile)
                    .addOnSuccessListener(taskSnapshot -> {
                        pdfView.fromFile(localFile)
                                .enableSwipe(true)
                                .swipeHorizontal(false)
                                .load();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(PdfViewerActivity.this, "PDF 로드 실패", Toast.LENGTH_SHORT).show();
                        Log.e("PdfViewerActivity", "Error downloading PDF", e);
                    });
        } catch (Exception e) {
            Toast.makeText(this, "파일 생성 오류", Toast.LENGTH_SHORT).show();
            Log.e("PdfViewerActivity", "File creation failed", e);
        }
    }
}
