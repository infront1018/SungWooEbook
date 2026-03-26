/**
 * 📌 파일 경로: com.sungwoobook.ebook.util.PdfThumbnailHelper.java
 * 📌 설명: 썸네일 PDF를 다운로드하여 첫 페이지를 이미지로 렌더링하고 Firebase Storage에 업로드한 뒤 Firestore에 URL 저장
 */

package com.sungwoobook.ebook.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PdfThumbnailHelper {

    /**
     * ✅ 전체 처리 로직:
     * 1. Firebase Storage에서 썸네일용 PDF 다운로드
     * 2. 첫 페이지를 비트맵으로 렌더링
     * 3. Firebase Storage에 PNG 파일 업로드
     * 4. Firestore 'contents' 문서에 thumbnailUrl 업데이트
     */
    public static void generateAndUploadThumbnail(Context context, String thumbPdfUrl, String contentId, OnSuccessListener<String> onComplete) {
        new Thread(() -> {
            File pdfFile = null;
            File thumbFile = null;

            try {
                // 1. 썸네일 PDF 다운로드
                URL url = new URL(thumbPdfUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // 2. PDF 파일을 캐시 디렉토리에 저장
                pdfFile = new File(context.getCacheDir(), contentId + "_thumb.pdf");
                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(pdfFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = input.read(buffer)) > 0) {
                    output.write(buffer, 0, len);
                }
                input.close();
                output.close();

                // 3. 첫 페이지 렌더링
                Bitmap bitmap = renderFirstPage(pdfFile);
                if (bitmap == null) {
                    Log.w("PdfHelper", "비트맵 생성 실패: " + contentId);
                    return;
                }

                // 4. PNG 썸네일로 저장
                thumbFile = new File(context.getCacheDir(), contentId + ".png");
                FileOutputStream thumbOut = new FileOutputStream(thumbFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, thumbOut);
                thumbOut.close();

                // 5. Firebase Storage 업로드
                StorageReference storageRef = FirebaseStorage.getInstance()
                        .getReference()
                        .child("thumbnails/" + contentId + ".png");

                UploadTask uploadTask = storageRef.putFile(Uri.fromFile(thumbFile));
                uploadTask.addOnSuccessListener(taskSnapshot -> {
                    // 6. 다운로드 URL 받아오기
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String thumbnailUrl = uri.toString();

                        // 7. Firestore 업데이트
                        FirebaseFirestore.getInstance("defaultdb")
                                .collection("contents")
                                .document(contentId)
                                .update("thumbnailUrl", thumbnailUrl)
                                .addOnSuccessListener(unused -> {
                                    Log.d("PdfHelper", "Firestore 업데이트 완료: " + contentId);
                                    if (onComplete != null) onComplete.onSuccess(thumbnailUrl);
                                });
                    });
                }).addOnFailureListener(e -> {
                    Log.e("PdfHelper", "썸네일 업로드 실패: " + contentId, e);
                });

            } catch (Exception e) {
                Log.e("PdfHelper", "처리 중 에러 발생: " + contentId, e);
            } finally {
                // 8. 캐시 파일 정리
                if (pdfFile != null && pdfFile.exists()) pdfFile.delete();
                if (thumbFile != null && thumbFile.exists()) thumbFile.delete();
            }
        }).start();
    }

    /**
     * ✅ PDF 첫 페이지를 비트맵으로 렌더링
     */
    private static Bitmap renderFirstPage(File pdfFile) throws Exception {
        ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
        PdfRenderer renderer = new PdfRenderer(fileDescriptor);
        PdfRenderer.Page page = renderer.openPage(0);
        Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        page.close();
        renderer.close();
        return bitmap;
    }
}