package com.sungwoobook.ebook.anim;

import android.view.View;
import androidx.viewpager2.widget.ViewPager2;

public class BookFlipPageTransformer implements ViewPager2.PageTransformer {
    @Override
    public void transformPage(View page, float position) {
        page.setCameraDistance(20000);

        if (position < -1) {
            page.setAlpha(0f);
        } else if (position <= 0) {
            page.setAlpha(1f);
            page.setPivotX(page.getWidth());
            page.setRotationY(90 * position);
        } else if (position <= 1) {
            page.setAlpha(1f);
            page.setPivotX(0f);
            page.setRotationY(90 * position);
        } else {
            page.setAlpha(0f);
        }
    }
}
