package com.sungwoobook.ebook.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;

/**
 * 2.5D Page Flip 렌더링 핵심 로직 클래스.
 * - Canvas.clipPath()를 사용하여 페이지의 물리적 굴곡과 쉐도우를 표현.
 */
public class PageFlipRenderer {

    private final Paint shadowPaint;
    private final android.graphics.Matrix shaderMatrix;
    private final Paint meshPaint;

    // 3D Mesh 설정: 40x40 격자로 극강의 부드러움 구현
    private static final int MESH_WIDTH = 40;
    private static final int MESH_HEIGHT = 40;
    private static final int VERTS_COUNT = (MESH_WIDTH + 1) * (MESH_HEIGHT + 1) * 2;
    private final float[] vertsFront = new float[VERTS_COUNT];
    private final float[] vertsBack = new float[VERTS_COUNT];

    private LinearGradient spineGrad;
    private LinearGradient dropGrad;

    public PageFlipRenderer() {
        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        meshPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        shaderMatrix = new android.graphics.Matrix();
    }

    public void drawFlip(Canvas canvas, Bitmap current, Bitmap next, int width, int height, PointF dragPoint, boolean isLandscape, boolean isReverse) {
        if (current == null || next == null) return;

        // 🚀 양방향의 핵심: 진행률을 방향에 따라 반전
        float progress = isReverse ? dragPoint.x : (1.0f - dragPoint.x);
        float lift = (1.0f - dragPoint.y) * 0.45f;

        // 1. 하단 페이지 (Next/Prev Page) 그리기
        canvas.drawBitmap(next, null, new Rect(0, 0, width, height), null);
        if (isLandscape) drawSpineShadow(canvas, width, height);

        // 2. 물리 메쉬 연산 (Front & Back) - 방향성 반영
        calculatePhysicsMesh(progress, lift, width, height, isReverse);
        
        // 3. 렌더링 패스 (하이브리드 클리핑 가드 추가)
        canvas.save();
        
        // [Pass 1] 앞면 메쉬 (말려 올라가는 부분)
        canvas.drawBitmapMesh(current, MESH_WIDTH, MESH_HEIGHT, vertsFront, 0, null, 0, meshPaint);
        
        // [Pass 2] 뒷면 메쉬 (말려 올라간 부분의 안쪽) 및 그림자
        meshPaint.setAlpha(150); 
        canvas.drawBitmapMesh(current, MESH_WIDTH, MESH_HEIGHT, vertsBack, 0, null, 0, meshPaint);
        meshPaint.setAlpha(255);
        
        canvas.restore();
        
        // 4. 동적 폴드 그림자 (바닥면 입체감 채움)
        drawFoldShadow(canvas, progress, width, height, isReverse);
    }

    private void calculatePhysicsMesh(float progress, float lift, int width, int height, boolean isReverse) {
        float pivotX = width * (1.0f - progress);
        
        // 🚀 두께감 강화: 곡률 반경을 0.19f로 키워 두꺼운 양장본 용지의 묵직함을 구현
        float curlRadius = width * 0.19f; 
        float piR = (float) Math.PI * curlRadius;

        int index = 0;
        for (int y = 0; y <= MESH_HEIGHT; y++) {
            float fy = (float) y / MESH_HEIGHT * height;
            for (int x = 0; x <= MESH_WIDTH; x++) {
                float fx = (float) x / MESH_WIDTH * width;
                
                // 🚀 프리미엄 고도화: Y 좌표에 따른 비대칭 대각선 틸트(Diagonal Tilt) 적용
                float tilt = (fy / height - 0.5f) * 0.12f * progress;
                float localPivotX = pivotX + (tilt * width);
                
                float d = fx - localPivotX;
                float angle = d / curlRadius;

                float vx, vy, bx, by;

                if (fx > localPivotX) {
                    if (angle < Math.PI) {
                        vx = localPivotX + (float) (Math.sin(angle) * curlRadius);
                        vy = fy - (float) ((1.0 - Math.cos(angle)) * curlRadius * lift);
                        bx = localPivotX - (float) (Math.sin(angle) * curlRadius * 0.95);
                        by = fy - (float) (angle * curlRadius * lift * 0.45);
                    } else {
                        vx = localPivotX;
                        vy = fy;
                        bx = localPivotX - piR - (d - piR);
                        by = fy;
                    }
                } else {
                    vx = fx;
                    vy = fy;
                    bx = localPivotX;
                    by = fy;
                }

                // 🚀 최종 좌표 결정: 역방향일 경우 좌우 반전
                if (isReverse) {
                    vertsFront[index * 2] = width - vx;
                    vertsFront[index * 2 + 1] = vy;
                    vertsBack[index * 2] = width - bx;
                    vertsBack[index * 2 + 1] = by;
                } else {
                    vertsFront[index * 2] = vx;
                    vertsFront[index * 2 + 1] = vy;
                    vertsBack[index * 2] = bx;
                    vertsBack[index * 2 + 1] = by;
                }
                index++;
            }
        }
    }

    private void drawSpineShadow(Canvas canvas, int width, int height) {
        int spineWidth = 60;
        if (spineGrad == null) {
            spineGrad = new LinearGradient(0, 0, spineWidth * 2, 0,
                    new int[]{Color.TRANSPARENT, Color.argb(60, 0, 0, 0), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP);
        }
        shaderMatrix.reset();
        shaderMatrix.setTranslate(width / 2.0f - spineWidth, 0);
        spineGrad.setLocalMatrix(shaderMatrix);
        shadowPaint.setShader(spineGrad);
        canvas.drawRect(width / 2.0f - spineWidth, 0, width / 2.0f + spineWidth, height, shadowPaint);
    }

    private void drawFoldShadow(Canvas canvas, float progress, int width, int height, boolean isReverse) {
        float x = width * (1.0f - progress);
        if (isReverse) x = width - x;

        if (dropGrad == null) {
            dropGrad = new LinearGradient(0, 0, 1, 0,
                    new int[]{Color.argb(80, 0, 0, 0), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP);
        }
        shaderMatrix.reset();
        // 🚀 두께감 강화: 그림자 범위를 0.22f로 넓혀 웅장한 입체감 부여
        shaderMatrix.setScale(width * 0.22f * (isReverse ? -1 : 1), height);
        shaderMatrix.postTranslate(x, 0);
        dropGrad.setLocalMatrix(shaderMatrix);
        shadowPaint.setShader(dropGrad);
        canvas.drawRect(0, 0, width, height, shadowPaint);
    }
}
