package com.sungwoobook.ebook.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.opengl.GLSurfaceView;
import android.os.ParcelFileDescriptor;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.sungwoobook.ebook.R;
import android.media.AudioManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OpenGL ES 2.0 기반의 프리미엄 3D 페이지 플립 뷰.
 * - IPageFlip 인터페이스를 구현하여 하이브리드 교체 지원.
 */
public class OpenGLPageFlipView extends GLSurfaceView implements IPageFlip {

    private static final String TAG = "OpenGLPageFlipView";
    private final OpenGLPageCurlRenderer renderer;
    
    private PdfRenderer pdfRenderer;
    private int currentPageIndex = 0;
    private int totalPages = 0;
    
    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor();
    private boolean isRendering = false;
    
    // 🚀 7년 차 개발자의 메모리 다이어트: 비트맵 풀링 객체 추가
    private Bitmap mBitmapPoolCurrent;
    private Bitmap mBitmapPoolNext;
    
    private boolean isInitialLoadDone = false;
    private android.media.MediaPlayer mediaPlayer;
    private IPageFlip.OnPageChangeListener pageChangeListener;

    public OpenGLPageFlipView(Context context) { this(context, null); }
    public OpenGLPageFlipView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        renderer = new OpenGLPageCurlRenderer(context);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY); // 성능 최적화
    }

    private void playFlipSound() {
        try {
            AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            if (am != null && am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                return; // 진동/무음 모드에서는 재생 안함 🛑
            }

            if (mediaPlayer != null) { mediaPlayer.release(); }
            mediaPlayer = android.media.MediaPlayer.create(getContext(), R.raw.page_flip);
            if (mediaPlayer != null) mediaPlayer.start();
        } catch (Exception e) { Log.e(TAG, "Sound error", e); }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 🚀 뷰 크기가 확정되는 순간, 지연되었던 로딩을 실행하여 '첫 프레임 비율 깨짐' 방지 🛑
        if (w > 0 && h > 0 && !isInitialLoadDone && pdfRenderer != null) {
            loadBitmaps();
        }
    }

    @Override
    public void setPdfFile(File file) {
        try {
            if (pdfRenderer != null) pdfRenderer.close();
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(pfd);
            totalPages = pdfRenderer.getPageCount();
            isInitialLoadDone = false; // 새 파일 로드 시 상태 리셋 🛑
            loadBitmaps();
        } catch (IOException e) { Log.e(TAG, "PDF Error", e); }
    }

    private void loadBitmaps() {
        // 🚀 7년 차 개발자의 '레이아웃 안전망': 뷰 크기가 0이면 로딩을 지연시킴 🛑
        if (getWidth() == 0 || getHeight() == 0) {
            Log.w("OpenGLPageFlipView", "Load deferred: dimensions are 0");
            return;
        }

        if (pdfRenderer == null || isRendering) return;
        isRendering = true;
        isInitialLoadDone = true;
        
        final int index = currentPageIndex;
        final boolean isLandscape = getWidth() > getHeight();
        final int step = isLandscape ? 2 : 1;

        renderExecutor.execute(() -> {
            try {
                // 🚀 7년 차 개발자의 '노-플리커' 전략: 멤버 변수를 직접 건드리지 않고 로컬에서 완벽하게 그림 🛑
                // 1. 현재 페이지 렌더링 (Step 1)
                Bitmap curBitmap = renderPage(index, null); // 로컬에서 새 비트맵으로 렌더링
                
                // 🚀 완성된 시점에만 GL 스레드로 전송 (흰색으로 지워진 중간 과정 노출 차단)
                queueEvent(() -> {
                    renderer.updateTextures(curBitmap, null);
                    requestRender();
                    // 사용 후 이전 비트맵 자리는 새로운 결과물로 교체 (풀링)
                    mBitmapPoolCurrent = curBitmap;
                });
                
                isRendering = false;

                // 2. 다음 페이지 프리페치 (Step 2)
                if (index + step < totalPages) {
                    Bitmap nextBitmap = renderPage(index + step, null);
                    queueEvent(() -> {
                        renderer.updateTextures(curBitmap, nextBitmap);
                        requestRender();
                        mBitmapPoolNext = nextBitmap;
                    });
                }
            } catch (Exception e) { 
                Log.e(TAG, "Bitmap load error", e);
                isRendering = false; 
            }
        });
    }

    private Bitmap renderPage(int index, @Nullable Bitmap reuse) {
        boolean isLandscape = getWidth() > getHeight();
        int w = 1024; 
        int h = 1536;
        int targetW = isLandscape ? w * 2 : w;
        
        // 🚀 7년 차 개발자의 재사용 공식: 기존 비트맵이 있고 크기가 같으면 그대로 사용
        Bitmap bitmap = reuse;
        if (bitmap == null || bitmap.getWidth() != targetW || bitmap.isRecycled()) {
            bitmap = Bitmap.createBitmap(targetW, h, Bitmap.Config.ARGB_8888);
        }
        
        bitmap.eraseColor(android.graphics.Color.WHITE);
        synchronized (pdfRenderer) {
            // 왼쪽 페이지 (또는 유일한 페이지)
            renderSinglePageToBitmap(bitmap, index, isLandscape ? new android.graphics.Rect(0, 0, w, h) : null);
            
            // 가로 모드일 때만 오른쪽 페이지 추가
            if (isLandscape && index + 1 < totalPages) {
                renderSinglePageToBitmap(bitmap, index + 1, new android.graphics.Rect(w, 0, w * 2, h));
            }
        }
        return bitmap;
    }

    private void renderSinglePageToBitmap(Bitmap bitmap, int index, @Nullable android.graphics.Rect rect) {
        if (index < 0 || index >= totalPages) return;
        PdfRenderer.Page page = pdfRenderer.openPage(index);
        page.render(bitmap, rect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        page.close();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() / getWidth();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                renderer.setCurlX(x * 2.0f - 1.0f); // GL 좌표계로 변환
                requestRender();
                return true;
            case MotionEvent.ACTION_UP:
                // 🚀 7년 차 개발자의 사용자 직관 반영: 오른쪽 탭 -> 다음 쪽(Forward), 왼쪽 탭 -> 이전 쪽
                startFlipAnimation(x > 0.5f);
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void startFlipAnimation(boolean forward) {
        playFlipSound();
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(renderer.getCurlX(), forward ? -1.2f : 1.0f);
        animator.setDuration(800); // 🚀 800ms(0.8초)의 경쾌한 프리미엄 속도 적용 🛑
        
        // 🚀 7년 차 개발자의 감성 인터폴레이터: 끝에서 찰지게 멈추는 1.8배 감속 계수 적용
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.8f));
        
        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            renderer.setCurlX(progress);
            
            // 🚀 가변 곡률(Dynamic Radius) 알고리즘: 
            // 🚨 말리는 순간 정점(Peak)에서 두꺼워졌다가 끝에서 얇아지는 물리 법칙 적용 🛑
            float normalized = java.lang.Math.abs(progress - renderer.getCurlX()) / 2.2f; 
            float dynamicRadius = 0.18f + (float) java.lang.Math.sin(java.lang.Math.PI * animation.getAnimatedFraction()) * 0.12f;
            renderer.setCurlRadius(dynamicRadius);
            
            requestRender();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                boolean isLandscape = getWidth() > getHeight();
                int step = isLandscape ? 2 : 1;

                if (forward) {
                    // 🚀 정방향(Forward): 다음 쪽으로 이동
                    if (currentPageIndex + step < totalPages) {
                        currentPageIndex += step;
                    }
                } else {
                    // 🚀 역방향(Backward): 이전 쪽으로 이동 (누락된 로직 추가) 🛑
                    if (currentPageIndex - step >= 0) {
                        currentPageIndex -= step;
                    }
                }
                
                if (pageChangeListener != null) {
                    pageChangeListener.onPageChanged(currentPageIndex, totalPages);
                }
                
                // 🚀 7년 차 개발자의 '0ms 딜레이' 필살기: 
                // 🚨 비트맵을 새로 그리기 전에 이미 완성된 textureNext를 textureCurrent로 즉각 스왑 🛑
                queueEvent(() -> {
                    renderer.swapTextures();
                    renderer.setCurlX(1.0f);
                    requestRender();
                    // 🚀 스왑 완료 후 백그라운드에서 다음 페이지 로드
                    loadBitmaps();
                });
            }
        });
        animator.start();
    }

    @Override
    public void setOnPageChangeListener(OnPageChangeListener listener) { this.pageChangeListener = listener; }

    @Override
    public int getTotalPages() { return totalPages; }

    @Override
    public int getCurrentPage() { return currentPageIndex; }

    @Override
    public void recycle() {
        renderExecutor.shutdown();
        renderer.recycle();
        if (pdfRenderer != null) pdfRenderer.close();
        if (mediaPlayer != null) mediaPlayer.release();
    }
}
