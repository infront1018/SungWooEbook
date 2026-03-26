package com.sungwoobook.ebook.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sungwoobook.ebook.Login.LoginActivity;
import com.sungwoobook.ebook.Model.ContentModel;
import com.sungwoobook.ebook.Viewer.PdfViewerActivity;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.Viewer.VideoViewerActivity;
import com.sungwoobook.ebook.adapter.FavoriteAdapter;

import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment {

    private RecyclerView recyclerView;
    private FavoriteAdapter adapter;
    private List<ContentModel> favoriteList = new ArrayList<>();

    private LinearLayout layoutLoginRequired;
    private Button btnGoLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        recyclerView = view.findViewById(R.id.recycler_favorites);
        layoutLoginRequired = view.findViewById(R.id.layout_login_required);
        btnGoLogin = view.findViewById(R.id.btn_go_login);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FavoriteAdapter(favoriteList, content -> {
            if (content.getType().equals("pdf")) {
                Intent intent = new Intent(getContext(), PdfViewerActivity.class);
                intent.putExtra("pdfUrl", content.getBookUrl());
                startActivity(intent);
            } else if (content.getType().equals("video")) {
                Intent intent = new Intent(getContext(), VideoViewerActivity.class);
                intent.putExtra("videoUrl", content.getVideoUrl());
                startActivity(intent);
            }
        });

        recyclerView.setAdapter(adapter);

        btnGoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), LoginActivity.class);
            startActivity(intent);
        });

        loadFavorites();
    }

    private void loadFavorites() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            recyclerView.setVisibility(View.GONE);
            layoutLoginRequired.setVisibility(View.VISIBLE);
            return;
        }

        recyclerView.setVisibility(View.VISIBLE);
        layoutLoginRequired.setVisibility(View.GONE);

        String userId = user.getUid();
        FirebaseFirestore.getInstance()
                .collection("favorites")
                .document(userId)
                .collection("items")
                .get()
                .addOnSuccessListener(snapshot -> {
                    favoriteList.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String title = doc.getString("title");
                        String type = doc.getString("type");
                        String url = doc.getString("url");
                        if (title != null && url != null && type != null) {
                            favoriteList.add(new ContentModel(title, type, url));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
