package com.sungwoobook.ebook.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sungwoobook.ebook.Model.ContentModel;
import com.sungwoobook.ebook.R;
import com.sungwoobook.ebook.adapter.RecycleAdapterContents;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private RecycleAdapterContents adapter;
    private List<ContentModel> contentList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recycler_home);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecycleAdapterContents(getContext(), contentList);
        recyclerView.setAdapter(adapter);

        loadContentsFromFirestore();

        return view;
    }

    private void loadContentsFromFirestore() {
        db = FirebaseFirestore.getInstance();
        db.collection("contents")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    contentList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ContentModel content = doc.toObject(ContentModel.class);
                        contentList.add(content);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
