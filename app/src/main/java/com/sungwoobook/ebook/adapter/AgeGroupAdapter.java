package com.sungwoobook.ebook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sungwoobook.ebook.R;

import java.util.List;

public class AgeGroupAdapter extends RecyclerView.Adapter<AgeGroupAdapter.AgeGroupViewHolder> {

    private final List<AgeGroupSection> sections;
    private final SeriesAdapter.OnSeriesClickListener listener;

    public static class AgeGroupSection {
        public final String name;
        public final List<SeriesAdapter.SeriesItem> items;

        public AgeGroupSection(String name, List<SeriesAdapter.SeriesItem> items) {
            this.name = name;
            this.items = items;
        }
    }

    public AgeGroupAdapter(List<AgeGroupSection> sections, SeriesAdapter.OnSeriesClickListener listener) {
        this.sections = sections;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AgeGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_age_group, parent, false);
        return new AgeGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AgeGroupViewHolder holder, int position) {
        AgeGroupSection section = sections.get(position);
        holder.txtTitle.setText(section.name);
        
        SeriesAdapter seriesAdapter = new SeriesAdapter(section.items, listener);
        holder.recyclerSeries.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        holder.recyclerSeries.setAdapter(seriesAdapter);
    }

    @Override
    public int getItemCount() {
        return sections == null ? 0 : sections.size();
    }

    static class AgeGroupViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle;
        RecyclerView recyclerSeries;

        AgeGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.textAgeGroupTitle);
            recyclerSeries = itemView.findViewById(R.id.recyclerSeries);
        }
    }
}
