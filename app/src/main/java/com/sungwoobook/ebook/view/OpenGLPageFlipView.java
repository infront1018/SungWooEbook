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
        // 🚀 회전이나 크기 변경 시 비트맵을 즉시 다시 생성하여 '해상도 최적화' 🛑
        if (w > 0 && h > 0 && pdfRenderer != null) {
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
                final float ratio = (float) curBitmap.getWidth() / curBitmap.getHeight();
                
                queueEvent(() -> {
                    renderer.setContentAspectRatio(ratio);
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
        if (pdfRenderer == null || index < 0 || index >= totalPages) return null;

        // 🚀 7년 차 개발자의 '다이내믹 해상도' 전략: 원본 PDF 페이지 크기를 읽어와 비율을 계산 🛑
        int origW, origH;
        synchronized (pdfRenderer) {
            PdfRenderer.Page probePage = pdfRenderer.openPage(index);
            origW = probePage.getWidth();
            origH = probePage.getHeight();
            probePage.close();
        }

        boolean isLandscape = getWidth() > getHeight();
        
        // 성능과 메모리를 고려한 Target Size 결정 (최대 2048px 제한으로 선명도는 챙기되 OOM 차단)
        float scale = Math.min(2048f / Math.max(origW, origH), 1.5f); // 저해상도 PDF 대비 약간의 업스케일 허용
        int tw = (int) (origW * scale);
        int th = (int) (origH * scale);
        
        int targetW = isLandscape ? tw * 2 : tw;
        int targetH = th;
        
        // 🚀 7년 차 개발자의 재사용 공식: 기존 비트맵이 있고 크기가 같으면 그대로 사용
        Bitmap bitmap = reuse;
        if (bitmap == null || bitmap.getWidth() != targetW || bitmap.getHeight() != targetH || bitmap.isRecycled()) {
            bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        }
        
        bitmap.eraseColor(android.graphics.Color.WHITE);
        synchronized (pdfRenderer) {
            // 왼쪽 페이지 (또는 유일한 페이지)
            renderSinglePageToBitmap(bitmap, index, isLandscape ? new android.graphics.Rect(0, 0, tw, th) : null);
            
            // 가로 모드일 때만 오른쪽 페이지 추가
            if (isLandscape && index + 1 < totalPages) {
                renderSinglePageToBitmap(bitmap, index + 1, new android.graphics.Rect(tw, 0, tw * 2, th));
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

    private float touchStartX  = 0f;   // 터치 시작 X (0.0~1.0 화면 비율)
    private float touchStartY  = -1.0f; // GL 좌표계 Y
    private boolean isDragging = false;
    // 드래그로 인정하는 최소 이동 거리: 화면 너비의 8%
    private static final float DRAG_THRESHOLD = 0.08f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x   = event.getX() / getWidth();
        float y   = event.getY() / getHeight();
        float glX = x * 2.0f - 1.0f;
        float glY = -(y * 2.0f - 1.0f);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = x;
                touchStartY = Math.max(-1.0f, Math.min(0.3f, glY));
                isDragging  = false;
                renderer.setCurlY(touchStartY);
                
                // 🚀 7년 차 개발자의 직관: 드래그 시작 시점에 역방향이면 텍스처 미리 준비 🛑
                if (touchStartX < 0.3f) {
                    prepareBackwardsTextures();
                } else {
                    renderer.setCurlX(1.0f);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                float dragDist = Math.abs(x - touchStartX);
                if (dragDist >= DRAG_THRESHOLD) {
                    isDragging = true;
                }
                if (isDragging) {
                    // 역방향일 때는 -1.2f 근처에서 시작하게 보정
                    float currentCurlX = (touchStartX < 0.3f) ? Math.min(glX, 1.0f) : glX;
                    renderer.setCurlX(currentCurlX);
                    requestRender();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    float totalDrag = Math.abs(x - touchStartX);
                    boolean forward = (touchStartX > 0.5f); // 오른쪽에서 시작한 드래그 = 앞으로
                    
                    if (totalDrag >= 0.25f) {
                        startFlipAnimation(forward);
                    } else {
                        snapBack(forward); // 충분히 드래그 못했으면 원래 상태로
                    }
                }
                isDragging = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    /** 드래그가 충분하지 않을 때 페이지를 원위치로 부드럽게 되돌림 */
    private void snapBack(boolean forward) {
        float target = forward ? 1.0f : -1.2f;
        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(renderer.getCurlX(), target);
        animator.setDuration(250);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        animator.addUpdateListener(anim -> {
            renderer.setCurlX((float) anim.getAnimatedValue());
            requestRender();
        });
        
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (!forward) {
                    // 역방향 스냅 시, 다시 정방향을 위해 텍스처 복구 필요 시 처리 (선택사항)
                    loadBitmaps(); 
                }
                renderer.setCurlX(1.0f); // 최종적으로는 항상 1.0f(정상 상태) 유지
                requestRender();
            }
        });
        animator.start();
    }
    
    /** 🚀 7년 차 개발자의 노하우: 역방향 텍스처 사전 구성 🛑 */
    private void prepareBackwardsTextures() {
        boolean isLandscape = getWidth() > getHeight();
        int step = isLandscape ? 2 : 1;
        final int prevIndex = Math.max(currentPageIndex - step, 0);
        
        if (prevIndex == currentPageIndex) return;

        renderExecutor.execute(() -> {
            Bitmap prevBitmap = renderPage(prevIndex, null);
            Bitmap curBitmap  = renderPage(currentPageIndex, null);
            if (prevBitmap == null || curBitmap == null) return;
            
            final float ratio = (float) curBitmap.getWidth() / curBitmap.getHeight();
            queueEvent(() -> {
                renderer.setContentAspectRatio(ratio);
                renderer.updateTexturesForReverseFlip(prevBitmap, curBitmap);
                renderer.setCurlX(-1.2f); // 왼쪽 바깥에서 시작 준비
                requestRender();
            });
        });
    }

    private void startFlipAnimation(boolean forward) {
        playFlipSound();

        boolean isLandscape = getWidth() > getHeight();
        int step = isLandscape ? 2 : 1;
        int nextIndex = forward
                ? Math.min(currentPageIndex + step, totalPages - 1)
                : Math.max(currentPageIndex - step, 0);

        // 🚀 애니메이션 목표 설정: 정방향이면 왼쪽(-1.2), 역방향이면 오른쪽(1.0) 🛑 
        float targetX = forward ? -1.2f : 1.0f;
        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofFloat(renderer.getCurlX(), targetX);

        animator.setDuration(600);
        animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

        final boolean[] preloaded = {false};

        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            renderer.setCurlX(progress);
            requestRender();

            // 🚀 정방향일 때만 스트리밍 프리패칭 (Next -> Next+1)
            if (forward && !preloaded[0] && animation.getAnimatedFraction() >= 0.4f) {
                preloaded[0] = true;
                renderExecutor.execute(() -> {
                    if (pdfRenderer == null) return;
                    Bitmap futureBitmap = renderPage(nextIndex, null);
                    queueEvent(() -> {
                        renderer.updateNextTexture(futureBitmap);
                        requestRender();
                    });
                });
            }
        });

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // 페이지 인덱스 변경
                currentPageIndex = nextIndex;

                if (pageChangeListener != null) {
                    pageChangeListener.onPageChanged(currentPageIndex, totalPages);
                }

                queueEvent(() -> {
                    if (forward) {
                        renderer.swapTextures();
                    }
                    // 역방향은 이미 updateTexturesForReverseFlip에서 위치가 잡혀 있으므로 스왑 불필요
                    renderer.setCurlX(1.0f);
                    renderer.setCurlY(-1.0f);
                    requestRender();
                    
                    // 다음 상태를 위한 비트맵 로딩
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
