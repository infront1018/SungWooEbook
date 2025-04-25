package com.sungwoobook.ebook.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sungwoobook.ebook.Model.ContentModel;
import com.sungwoobook.ebook.R;
import java.util.List;

public class RecycleAdapterContents extends RecyclerView.Adapter<RecycleAdapterContents.ViewHolder> {
    private Context context;
    private List<ContentModel> contentList;

    public RecycleAdapterContents(Context context, List<ContentModel> contentList) {
        this.context = context;
        this.contentList = contentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_content, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContentModel content = contentList.get(position);
        holder.title.setText(content.getTitle());
        holder.type.setText(content.getType());

        holder.itemView.setOnClickListener(v -> {
            // PDF 또는 영상 뷰어 호출 (다음 단계에서 구현)
        });
    }

    @Override
    public int getItemCount() {
        return contentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, type;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            type = itemView.findViewById(R.id.text_type);
        }
    }
}
