// 📌 파일 경로: com.sungwoobook.ebook.adapter.BannerAdapter.java
package com.sungwoobook.ebook.adapter;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
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

        // ✅ [3] 로드할 URL 확인
        android.util.Log.d("BannerAdapter", "Loading URL: " + imageUrl);

        // ✅ 기존 Glide 로드 + 디버깅용 리스너 추가
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.default_banner)  // 기본 배너 이미지
                .error(R.drawable.default_banner)
                .listener(new RequestListener<Drawable>() { // 🔥 디버깅용 listener 추가
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        if (e != null) {
                            Log.e("🔥GlideBanner", "배너 이미지 로딩 실패: " + model, e);
                            for (Throwable t : e.getRootCauses()) {
                                Log.e("🔥GlideBanner", "원인: " + t.getMessage(), t);
                            }
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        // 성공 시 특별히 할 건 없음
                        return false;
                    }
                })
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
