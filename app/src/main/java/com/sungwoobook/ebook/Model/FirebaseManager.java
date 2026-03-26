// 📌 파일 경로: com.sungwoobook.ebook.Model.FirebaseManager.java
// 📌 설명: 인증, Firestore, Storage 기능을 관리하는 싱글톤 클래스

package com.sungwoobook.ebook.Model;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseManager {
    private static FirebaseManager instance;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // ✅ 싱글톤 생성자 - 사용자 지정 DB 인스턴스 사용
    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance("defaultdb"); // ✅ 여기가 핵심
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void signIn(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public void signUp(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public void signOut() {
        auth.signOut();
    }

    public void getAllContents(OnSuccessListener<QuerySnapshot> successListener) {
        db.collection("contents").get().addOnSuccessListener(successListener);
    }

    public void getContentById(String contentId, OnSuccessListener<DocumentSnapshot> successListener) {
        db.collection("contents").document(contentId).get().addOnSuccessListener(successListener);
    }

    public void getUserData(String userId, OnSuccessListener<DocumentSnapshot> successListener) {
        db.collection("users").document(userId).get().addOnSuccessListener(successListener);
    }

    public void addToFavorites(String userId, String contentId) {
        db.collection("users").document(userId).collection("favorites")
                .document(contentId)
                .set(new ContentReference(contentId));
    }

    public void removeFromFavorites(String userId, String contentId) {
        db.collection("users").document(userId).collection("favorites")
                .document(contentId)
                .delete();
    }

    public void getAllBanners(OnSuccessListener<QuerySnapshot> successListener) {
        db.collection("banners").get().addOnSuccessListener(successListener);
    }

    public void getPdfUrl(String pdfPath, OnSuccessListener<Uri> successListener) {
        StorageReference pdfRef = storage.getReference().child(pdfPath);
        pdfRef.getDownloadUrl().addOnSuccessListener(successListener);
    }

    public void getVideoUrl(String videoPath, OnSuccessListener<Uri> successListener) {
        StorageReference videoRef = storage.getReference().child(videoPath);
        videoRef.getDownloadUrl().addOnSuccessListener(successListener);
    }

    private static class ContentReference {
        public String contentId;

        public ContentReference(String contentId) {
            this.contentId = contentId;
        }

        public ContentReference() {
        }
    }
}
