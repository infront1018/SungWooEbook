package com.sungwoobook.ebook.util;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * 앱 전체 버튼 Push(눌림) 효과 통합 유틸리티.
 *
 * 사용법:
 *   ButtonEffectUtil.apply(view);           // 기본 효과 (scale 0.93)
 *   ButtonEffectUtil.apply(view, 0.88f);    // 커스텀 스케일
 *
 * 적용 원리:
 *   - DOWN : scaleX/scaleY → targetScale (눌림 느낌)
 *   - UP/CANCEL : scaleX/scaleY → 1.0f (원상복귀)
 *   - 기존 OnClickListener를 덮어쓰지 않음 (performClick() 호출)
 */
public class ButtonEffectUtil {

    private static final float DEFAULT_SCALE    = 0.93f;
    private static final long  PRESS_DURATION   = 90L;
    private static final long  RELEASE_DURATION = 140L;

    /** 기본 스케일(0.93)로 Push 효과 적용 */
    public static void apply(View view) {
        apply(view, DEFAULT_SCALE);
    }

    /** 커스텀 스케일로 Push 효과 적용 */
    public static void apply(View view, float targetScale) {
        if (view == null) return;

        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pressDown(v, targetScale);
                    break;
                case MotionEvent.ACTION_UP:
                    pressRelease(v);
                    v.performClick();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    pressRelease(v);
                    break;
            }
            return true; // true: 이벤트 소비 (OnClickListener는 performClick으로 수동 호출)
        });
    }

    /**
     * OnClickListener를 유지하면서 Push 효과 추가 (권장 방식).
     * OnTouchListener가 true를 반환하지 않으므로 기존 클릭 이벤트가 그대로 동작.
     */
    public static void applyWithClick(View view) {
        applyWithClick(view, DEFAULT_SCALE);
    }

    public static void applyWithClick(View view, float targetScale) {
        if (view == null) return;

        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    pressDown(v, targetScale);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    pressRelease(v);
                    break;
            }
            return false; // false: 기존 OnClickListener 유지
        });
    }

    /** 여러 뷰에 한꺼번에 기본 효과 적용 */
    public static void applyAll(View... views) {
        for (View v : views) {
            applyWithClick(v);
        }
    }

    // ── 내부 애니메이션 ──────────────────────────────────────────────────

    private static void pressDown(View v, float scale) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(v, "scaleX", scale),
                ObjectAnimator.ofFloat(v, "scaleY", scale)
        );
        set.setDuration(PRESS_DURATION);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }

    private static void pressRelease(View v) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(v, "scaleX", 1.0f),
                ObjectAnimator.ofFloat(v, "scaleY", 1.0f)
        );
        set.setDuration(RELEASE_DURATION);
        set.setInterpolator(new DecelerateInterpolator(1.5f));
        set.start();
    }
}
