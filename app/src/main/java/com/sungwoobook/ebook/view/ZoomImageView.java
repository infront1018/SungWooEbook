package com.sungwoobook.ebook.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

public class ZoomImageView extends AppCompatImageView {

    private Matrix matrix = new Matrix();         // 이미지에 적용할 변형 행렬
    private float scale = 1f;                      // 현재 스케일 상태
    private Bitmap currentBitmap;                  // 현재 표시 중인 이미지

    private ScaleGestureDetector scaleDetector;    // 핀치 줌 제스처 처리
    private GestureDetector gestureDetector;       // 더블탭 제스처 처리
    private ZoomStateListener zoomListener;        // 확대 상태 콜백 처리

    private PointF lastTouchPoint = new PointF();  // 드래그 시작 위치 저장
    private boolean isDragging = false;            // 드래그 중 여부

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void setZoomListener(ZoomStateListener listener) {
        this.zoomListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        // 드래그 처리를 위한 추가 로직
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchPoint.set(event.getX(), event.getY());
                isDragging = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (scale > 1f) { // 확대된 상태일 때만 드래그 허용
                    float dx = event.getX() - lastTouchPoint.x;
                    float dy = event.getY() - lastTouchPoint.y;
                    matrix.postTranslate(dx, dy);
                    setImageMatrix(matrix);
                    lastTouchPoint.set(event.getX(), event.getY());
                    isDragging = true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }

        return true; // 이벤트 소비
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        this.currentBitmap = bm;
        post(this::centerAndScaleImage);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (currentBitmap != null) {
            post(this::centerAndScaleImage);
        }
    }

    /**
     * 이미지의 초기 확대 상태 및 위치를 중앙 정렬로 설정함
     */
    private void centerAndScaleImage() {
        if (currentBitmap == null) return;

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float imageWidth = currentBitmap.getWidth();
        float imageHeight = currentBitmap.getHeight();

        float scaleX = viewWidth / imageWidth;
        float scaleY = viewHeight / imageHeight;
        scale = Math.min(scaleX, scaleY);

        float dx = (viewWidth - imageWidth * scale) / 2f;
        float dy = (viewHeight - imageHeight * scale) / 2f;

        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);

        if (zoomListener != null) zoomListener.onZoomEnded();
    }

    /**
     * 핀치 줌을 위한 리스너
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            scale *= factor;
            scale = Math.max(1f, Math.min(scale, 5f)); // 스케일 제한: 1x ~ 5x

            matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            setImageMatrix(matrix);

            if (zoomListener != null) zoomListener.onZoomStarted();
            return true;
        }
    }

    /**
     * 더블탭 확대 또는 초기화 처리
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (scale > 1f) {
                centerAndScaleImage();
            } else {
                matrix.postScale(2f, 2f, e.getX(), e.getY());
                scale = 2f;
                setImageMatrix(matrix);
                if (zoomListener != null) zoomListener.onZoomStarted();
            }
            return true;
        }
    }
}
