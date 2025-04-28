package com.sungwoobook.ebook.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sungwoobook.ebook.R;

import java.util.List;

/**
 * 📌 파일 경로: com.sungwoobook.ebook.adapter.BannerAdapter.java
 * 📌 설명: 상단 배너용 ViewPager2 어댑터
 */
public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private List<String> bannerImages;

    public BannerAdapter(List<String> bannerImages) {
        this.bannerImages = bannerImages;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        String imageUrl = bannerImages.get(position);

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.default_banner)  // 기본 배너 이미지
                .error(R.drawable.default_banner)
                .into(holder.imageBanner);
    }

    @Override
    public int getItemCount() {
        return bannerImages.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageBanner;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageBanner = itemView.findViewById(R.id.imageBanner);
        }
    }
}
