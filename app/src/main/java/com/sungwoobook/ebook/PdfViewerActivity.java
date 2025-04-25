package com.sungwoobook.ebook;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.github.barteksc.pdfviewer.PDFView;
import com.sungwoobook.ebook.R;

public class PdfViewerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        PDFView pdfView = findViewById(R.id.pdfView);

        String pdfUrl = getIntent().getStringExtra("pdfUrl");

        // Firebase Storage 다운로드 및 표시 (간단화)
        // 실제 구현 시 FirebaseStorage로 파일 다운로드 후 표시
        // pdfView.fromFile(localFile).load();
    }
}
