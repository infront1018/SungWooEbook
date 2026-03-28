package com.sungwoobook.ebook.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.sungwoobook.ebook.R;

import java.io.File;
import java.io.IOException;

/**
 * PDF 책장 넘기기 뷰.
 * - PdfRenderer를 사용하여 페이지 비트맵을 생성.
 * - PageFlipRenderer를 사용하여 2.5D 효과를 렌더링.
 */
public class PageFlipView extends View {

    private static final String TAG = "PageFlipView";

    private PdfRenderer pdfRenderer;
    private File pdfFile;
    private int currentPageIndex = 0;
    private int totalPages = 0;

    private Bitmap currentPageBitmap;
    private Bitmap nextPageBitmap;

    private final PageFlipRenderer renderer = new PageFlipRenderer();
    private android.graphics.PointF dragPoint = new android.graphics.PointF(1.0f, 1.0f);
    private boolean isDragging = false;

    public PageFlipView(Context context) { super(context); init(); }
    public PageFlipView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }

    private android.media.MediaPlayer mediaPlayer;

    private void init() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    @SuppressWarnings("deprecation")
    private void playFlipSound() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            mediaPlayer = android.media.MediaPlayer.create(getContext(), R.raw.page_flip);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                Log.d("PageFlipView", "MediaPlayer Started Success");
            } else {
                Log.e("PageFlipView", "MediaPlayer Creation Failed");
            }
        } catch (Exception e) {
            Log.e("PageFlipView", "Error playing sound: " + e.getMessage());
        }
    }

    public synchronized void setPdfFile(File file) {
        this.pdfFile = file;
        try {
            if (pdfRenderer != null) {
                synchronized (pdfRenderer) {
                    pdfRenderer.close();
                }
            }
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(pfd);
            totalPages = pdfRenderer.getPageCount();
            
            // 초기 페이지 로딩 (두 페이지 선제 로드)
            loadInitialBitmaps();
        } catch (IOException e) {
            Log.e(TAG, "Error loading PDF file", e);
        }
    }

    private android.animation.ValueAnimator flipAnimator;

    private final java.util.concurrent.ExecutorService renderExecutor = 
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private boolean isRendering = false;

    private synchronized void loadInitialBitmaps() {
        if (pdfRenderer == null || isRendering) return;
        isRendering = true;
        
        int w = getWidth() > 0 ? getWidth() : 1080;
        int h = getHeight() > 0 ? getHeight() : 1920;
        boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;

        renderExecutor.execute(() -> {
            try {
                Bitmap p1 = isLandscape ? renderDoublePage((currentPageIndex / 2) * 2, w, h) : renderPage(currentPageIndex, w, h);
                int nextIdx = currentPageIndex + (isLandscape ? 2 : 1);
                Bitmap p2 = (nextIdx < totalPages) ? 
                        (isLandscape ? renderDoublePage((nextIdx / 2) * 2, w, h) : renderPage(nextIdx, w, h)) : null;

                post(() -> {
                    this.currentPageBitmap = p1;
                    this.nextPageBitmap = p2;
                    isRendering = false;
                    invalidate();
                });
            } catch (Exception e) {
                isRendering = false;
            }
        });
    }

    private synchronized void prefetchNextPageAsync() {
        if (pdfRenderer == null || isRendering) return;
        isRendering = true;

        boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int nextIdx = currentPageIndex + (isLandscape ? 2 : 1);
        if (nextIdx >= totalPages) {
            isRendering = false;
            return;
        }

        int w = getWidth() > 0 ? getWidth() : (isLandscape ? 2160 : 1080);
        int h = getHeight() > 0 ? getHeight() : (isLandscape ? 1620 : 1920);

        renderExecutor.execute(() -> {
            try {
                Bitmap nextUnder = isLandscape ? 
                        renderDoublePage((nextIdx / 2) * 2, w, h) : renderPage(nextIdx, w, h);
                
                post(() -> {
                    Bitmap oldNext = this.nextPageBitmap;
                    this.nextPageBitmap = nextUnder;
                    if (oldNext != null && oldNext != this.currentPageBitmap) oldNext.recycle();
                    isRendering = false;
                    invalidate();
                });
            } catch (Exception e) {
                isRendering = false;
            }
        });
    }

    private Bitmap renderDoublePage(int index, int w, int h) {
        if (index < 0 || index >= totalPages) return null;
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(android.graphics.Color.WHITE);
        renderPageToBitmap(bitmap, index, new android.graphics.Rect(0, 0, w / 2, h));
        if (index + 1 < totalPages) {
            renderPageToBitmap(bitmap, index + 1, new android.graphics.Rect(w / 2, 0, w, h));
        }
        return bitmap;
    }

    private Bitmap renderPage(int index, int w, int h) {
        if (index < 0 || index >= totalPages) return null;
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(android.graphics.Color.WHITE);
        renderPageToBitmap(bitmap, index, new android.graphics.Rect(0, 0, w, h));
        return bitmap;
    }

    private void renderPageToBitmap(Bitmap bitmap, int index, android.graphics.Rect rect) {
        if (pdfRenderer == null || index < 0 || index >= totalPages) return;
        synchronized (pdfRenderer) {
            PdfRenderer.Page page = pdfRenderer.openPage(index);
            page.render(bitmap, rect, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();
        }
    }

    private boolean isReverseFlip = false;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (currentPageBitmap == null) return;

        boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        
        if (isDragging || (flipAnimator != null && flipAnimator.isRunning())) {
            // 넘길 때만 하단 페이지(nextPageBitmap)를 같이 그림
            renderer.drawFlip(canvas, currentPageBitmap, nextPageBitmap, getWidth(), getHeight(), dragPoint, isLandscape, isReverseFlip);
        } else {
            // 정지 상태에선 현재 페지만 출력
            canvas.drawBitmap(currentPageBitmap, null, new android.graphics.Rect(0, 0, getWidth(), getHeight()), null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (flipAnimator != null && flipAnimator.isRunning()) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float x = event.getX();
                if (x > getWidth() * 0.72f) { // 다음 장 드래그
                    isReverseFlip = false;
                    isDragging = true;
                    prefetchNextPageAsync();
                } else if (x < getWidth() * 0.28f) { // 이전 장 드래그
                    isReverseFlip = true;
                    isDragging = true;
                    prefetchPrevPageAsync();
                }
                
                if (isDragging) {
                    dragPoint.set(x / getWidth(), event.getY() / getHeight());
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    dragPoint.set(event.getX() / getWidth(), event.getY() / getHeight());
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    // 드래그 임계값 판단
                    boolean forward = isReverseFlip ? (dragPoint.x > 0.38f) : (dragPoint.x < 0.62f);
                    
                    // 탭 넘김 지원
                    if (!forward) {
                        if (!isReverseFlip && event.getX() > getWidth() * 0.85f) forward = true;
                        if (isReverseFlip && event.getX() < getWidth() * 0.15f) forward = true;
                    }

                    startAnimation(forward);
                    isDragging = false;
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void startAnimation(boolean forward) {
        float startX = dragPoint.x;
        float endX;
        
        if (isReverseFlip) {
            endX = forward ? 2.2f : 0.0f; // 왼쪽에서 오른쪽으로 넘김
        } else {
            endX = forward ? -1.2f : 1.0f; // 오른쪽에서 왼쪽으로 넘김
        }

        // 🚀 터치 시에는 타협 없는 프리미엄을 위해 시간을 3000ms(3초)로 극대화
        long duration = (Math.abs(startX - (isReverseFlip ? 0.0f : 1.0f)) < 0.05f) ? 3000 : 1500;

        flipAnimator = android.animation.ValueAnimator.ofFloat(startX, endX);
        flipAnimator.setDuration(duration);
        
        // 🚀 핵심: 0에서 출발하여 웅장하게 넘어가는 AccelerateDecelerate로 복구
        flipAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        
        // 🚀 사운드 트리거: 넘김이 시작되는 순간 '사라락' 소리 재생
        playFlipSound();
        
        flipAnimator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            dragPoint.x = val;
            
            // 방향에 따른 진행률 계산 (0.0 ~ 1.0)
            float rawProgress = isReverseFlip ? (val / endX) : ((1.0f - val) / (1.0f - endX));
            float progress = Math.min(1.0f, Math.max(0f, rawProgress));
            
            // 🚀 한층 강화된 아치: 종이가 더 높고 입체적으로 들리도록 보정
            float liftAmount = (float) Math.sin(progress * Math.PI) * 0.48f;
            dragPoint.y = 0.82f - liftAmount; 
            
            invalidate();
        });

        flipAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (forward) {
                    if (isReverseFlip) goToPrevPage();
                    else goToNextPage();
                } else {
                    dragPoint.set(isReverseFlip ? 0.0f : 1.0f, 1.0f);
                    invalidate();
                }
            }
        });
        flipAnimator.start();
    }

    private void prefetchPrevPageAsync() {
        if (currentPageIndex <= 0 || isRendering) return;
        isRendering = true;
        
        int w = getWidth() > 0 ? getWidth() : 1080;
        int h = getHeight() > 0 ? getHeight() : 1920;

        renderExecutor.execute(() -> {
            try {
                boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
                int step = isLandscape ? 2 : 1;
                int targetIndex = Math.max(0, currentPageIndex - step);
                
                Bitmap bitmap = isLandscape ? renderDoublePage((targetIndex / 2) * 2, w, h) : renderPage(targetIndex, w, h);
                
                post(() -> {
                    Bitmap oldNext = this.nextPageBitmap;
                    this.nextPageBitmap = bitmap;
                    if (oldNext != null && oldNext != this.currentPageBitmap) oldNext.recycle();
                    isRendering = false;
                    invalidate();
                });
            } catch (Exception e) {
                isRendering = false;
            }
        });
    }

    private synchronized void goToPrevPage() {
        if (nextPageBitmap == null) return;
        
        boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int step = isLandscape ? 2 : 1;

        if (currentPageIndex - step >= 0) {
            currentPageIndex -= step;
            Bitmap oldCurrent = currentPageBitmap;
            currentPageBitmap = nextPageBitmap;
            nextPageBitmap = null;
            if (oldCurrent != null) oldCurrent.recycle();
            
            dragPoint.set(0.0f, 1.0f);
            invalidate();
            prefetchNextPageAsync(); // 다음 장도 다시 준비
            
            if (pageChangeListener != null) {
                pageChangeListener.onPageChanged(currentPageIndex, totalPages);
            }
        }
    }

    private synchronized void goToNextPage() {
        if (nextPageBitmap == null) return; // 아직 다음 장 로딩 중

        boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int step = isLandscape ? 2 : 1;
        
        if (currentPageIndex + step < totalPages) {
            currentPageIndex += step;
            
            // 🚀 핵심: 즉각적인 비트맵 승격 (심리스 스왑)
            Bitmap oldCurrent = currentPageBitmap;
            currentPageBitmap = nextPageBitmap;
            nextPageBitmap = null; // 백그라운드에서 다시 채울 예정
            
            if (oldCurrent != null) oldCurrent.recycle();
            
            dragPoint.set(1.0f, 1.0f);
            invalidate();
            
            // 백그라운드 선행 로딩 시작 (다음 다음 장 준비)
            prefetchNextPageAsync();
            
            if (pageChangeListener != null) {
                pageChangeListener.onPageChanged(currentPageIndex, totalPages);
            }
        } else {
            dragPoint.set(1.0f, 1.0f);
            invalidate();
        }
    }

    private OnPageChangeListener pageChangeListener;
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.pageChangeListener = listener;
    }

    public interface OnPageChangeListener {
        void onPageChanged(int currentPage, int totalPages);
    }

    public int getCurrentPage() { return currentPageIndex; }
    public int getTotalPages() { return totalPages; }

    public void recycle() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            renderExecutor.shutdown();
            if (pdfRenderer != null) {
                synchronized (pdfRenderer) {
                    pdfRenderer.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error recycling resources", e);
        }
        if (currentPageBitmap != null && !currentPageBitmap.isRecycled()) currentPageBitmap.recycle();
        if (nextPageBitmap != null && !nextPageBitmap.isRecycled()) nextPageBitmap.recycle();
    }
}
