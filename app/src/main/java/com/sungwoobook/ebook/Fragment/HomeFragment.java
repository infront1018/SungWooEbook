/**
 * 📌 파일 경로: com.sungwoobook.ebook.Fragment.HomeFragment.java
 * 📌 설명: 홈 프래그먼트 - 배너 + 콘텐츠 로딩 + 자동 썸네일 생성 (전체 콘텐츠 가로 리사이클러뷰로 표시)
 */

package com.sungwoobook.ebook.Fragment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sungwoobook.ebook.Model.ContentModel;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.adapter.AllContentAdapter;
import com.sungwoobook.ebook.adapter.BannerAdapter;
import com.sungwoobook.ebook.adapter.RecentAdapter;
import com.sungwoobook.ebook.Utils.PdfThumbnailHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import com.sungwoobook.ebook.adapter.AllContentAdapter.OnContentClickListener;

public class HomeFragment extends Fragment {

    private ViewPager2 bannerViewPager;
    private TabLayout bannerIndicator;
    private RecyclerView recyclerRecent, recyclerAllContents;

    private BannerAdapter bannerAdapter;
    private RecentAdapter recentAdapter;
    private AllContentAdapter allContentAdapter;

    private List<String> bannerImages = new ArrayList<>();
    private List<ContentModel> recentContents = new ArrayList<>();
    private List<ContentModel> allContents = new ArrayList<>();

    private ProgressDialog progressDialog;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // ✅ UI 바인딩
        bannerViewPager = view.findViewById(R.id.bannerViewPager);
        bannerIndicator = view.findViewById(R.id.bannerIndicator);
        recyclerRecent = view.findViewById(R.id.recyclerRecent);
        recyclerAllContents = view.findViewById(R.id.recyclerAllContents);

        setupAdapters();

        //showLoadingDialog(); // ✅ 썸네일 생성 안내, 현재 사용하지 않음.

        checkAndGenerateMissingThumbnails(); // ✅ 저장된 썸네일이 없으면, 썸네일 PDF를 통해 자동 생성

        loadContentData(); // ✅ 콘텐츠 로딩

        return view;
    }

    private void setupAdapters() {
        bannerAdapter = new BannerAdapter(bannerImages);
        bannerViewPager.setAdapter(bannerAdapter);

        recyclerRecent.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // ✅ 최근 콘텐츠 갱신을 위한 리스너 연결 -> 최근 콘텐츠 클릭 시
        recentAdapter = new RecentAdapter(recentContents, content -> {
            for (int i = 0; i < recentContents.size(); i++) {
                if (recentContents.get(i).getId().equals(content.getId())) {
                    recentContents.remove(i);
                    break;
                }
            }
            recentContents.add(0, content);
            recentAdapter.notifyDataSetChanged();
        });
        recyclerRecent.setAdapter(recentAdapter);

        recyclerAllContents.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // ✅ 최근 콘텐츠 갱신을 위한 리스너 연결 -> 전체 콘텐츠 클릭 시
        allContentAdapter = new AllContentAdapter(allContents, content -> {
            // 동일 ID 제거 후 맨 앞으로 추가
            for (int i = 0; i < recentContents.size(); i++) {
                if (recentContents.get(i).getId().equals(content.getId())) {
                    recentContents.remove(i);
                    break;
                }
            }
            recentContents.add(0, content);
            recentAdapter.notifyDataSetChanged();
        });

        recyclerAllContents.setAdapter(allContentAdapter);
    }

    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("썸네일 생성 중...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void checkAndGenerateMissingThumbnails() {
        // ✅ 사용자 지정 DB 이름 사용
        FirebaseFirestore.getInstance("defaultdb")
                .collection("contents")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    AtomicInteger totalToGenerate = new AtomicInteger();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String contentId = doc.getId();
                        String thumbnailUrl = doc.getString("thumbnailUrl");

                        if (thumbnailUrl == null || thumbnailUrl.trim().isEmpty()) {
                            totalToGenerate.getAndIncrement();
                            String thumbPdfUrl = "https://firebasestorage.googleapis.com/v0/b/your-app.appspot.com/o/thumb_pdf%2F" + contentId + ".pdf?alt=media";

                            PdfThumbnailHelper.generateAndUploadThumbnail(
                                    getContext(),
                                    thumbPdfUrl,
                                    contentId,
                                    url -> {
                                        Log.d("AutoThumbnail", "썸네일 생성 완료: " + url);
                                        totalToGenerate.getAndDecrement();
                                        if (totalToGenerate.get() <= 0) hideLoadingDialog();
                                    }
                            );
                        }
                    }

                    if (totalToGenerate.get() == 0) {
                        hideLoadingDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "썸네일 자동 생성 실패", Toast.LENGTH_SHORT).show();
                    Log.e("AutoThumbnail", "에러 발생: ", e);
                    hideLoadingDialog();
                });
    }

    private void loadContentData() {
        // ✅ 사용자 지정 DB 이름 사용
        FirebaseFirestore.getInstance("defaultdb")
                .collection("contents")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("🔥FirestoreDebug", "문서 수: " + queryDocumentSnapshots.size());

                    allContents.clear();
                    recentContents.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Log.d("🔥FirestoreDebug", "Document ID: " + doc.getId());

                        ContentModel content = doc.toObject(ContentModel.class);

                        if (content != null) {
                            Log.d("🔥FirestoreDebug", "Title: " + content.getTitle());
                            Log.d("🔥FirestoreDebug", "Thumbnail URL: " + content.getThumbnailUrl());
                            content.setId(doc.getId()); // 🔴 여기 꼭 추가
                            allContents.add(content);
                            recentContents.add(content);
                        } else {
                            Log.w("🔥FirestoreDebug", "toObject(ContentModel.class) 반환값이 null입니다.");
                        }
                    }

                    Log.d("🔥FirestoreDebug", "allContents size: " + allContents.size());
                    Log.d("🔥FirestoreDebug", "recentContents size: " + recentContents.size());

                    recentAdapter.notifyDataSetChanged();
                    allContentAdapter.notifyDataSetChanged();

                    // ✅ 썸네일 캐싱 preload (Glide) - 썸네일 미리 캐시하여 앱 진입 시 즉시 표시
                    for (ContentModel content : allContents) {
                        Glide.with(requireContext())
                                .load(content.getThumbnailUrl())
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                                .preload();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("🔥FirestoreDebug", "Firestore 데이터 로딩 실패", e);
                });
    }
}
