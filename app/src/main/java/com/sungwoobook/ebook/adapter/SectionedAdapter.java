package com.sungwoobook.ebook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sungwoobook.ebook.Model.ContentModel;
import com.sungwoobook.ebook.R;

import java.util.List;

public class SectionedAdapter extends RecyclerView.Adapter<SectionedAdapter.SectionViewHolder> {

    public static class Section {
        public String title;
        public List<ContentModel> contents;

        public Section(String title, List<ContentModel> contents) {
            this.title = title;
            this.contents = contents;
        }
    }

    private final List<Section> sections;
    private final AllContentAdapter.OnContentClickListener listener;

    public SectionedAdapter(List<Section> sections, AllContentAdapter.OnContentClickListener listener) {
        this.sections = sections;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_section_with_horizontal, parent, false);
        return new SectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SectionViewHolder holder, int position) {
        Section section = sections.get(position);
        holder.sectionTitle.setText(section.title);

        AllContentAdapter adapter = new AllContentAdapter(section.contents, listener);
        holder.sectionRecyclerView.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        holder.sectionRecyclerView.setAdapter(adapter);

        // 🔧 nested scroll 문제 해결
        holder.sectionRecyclerView.setNestedScrollingEnabled(false);
        holder.sectionRecyclerView.setHasFixedSize(true);
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {
        TextView sectionTitle;
        RecyclerView sectionRecyclerView;

        public SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            sectionTitle = itemView.findViewById(R.id.textSectionTitle);
            sectionRecyclerView = itemView.findViewById(R.id.recyclerSection);
        }
    }
}
