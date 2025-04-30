package com.sungwoobook.ebook.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

public class ZoomImageView extends AppCompatImageView {

    private Matrix matrix = new Matrix();
    private float scale = 1f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    private Bitmap currentBitmap;

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        currentBitmap = bm;
        post(this::centerAndScaleImage);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (currentBitmap != null) {
            post(this::centerAndScaleImage); // ✅ 회전 등 화면 크기 변경 시 재조정
        }
    }

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
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            scale *= factor;
            scale = Math.max(1f, Math.min(scale, 5f));
            matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            setImageMatrix(matrix);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (scale > 1f) {
                centerAndScaleImage(); // 초기화
            } else {
                matrix.postScale(2f, 2f, e.getX(), e.getY());
                scale = 2f;
                setImageMatrix(matrix);
            }
            return true;
        }
    }
}
