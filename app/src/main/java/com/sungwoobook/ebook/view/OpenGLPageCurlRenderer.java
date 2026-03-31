package com.sungwoobook.ebook.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 사용자 정의 페이지 컬용 OpenGL 렌더러.
 * - Vertex Shader에서 Cylinder Roll 물리 연산 수행.
 * - 그림자 및 하이라이트 광원 효과 지원.
 * - 뒷면(Back-side) 거울 반전 렌더링 지원.
 */
public class OpenGLPageCurlRenderer implements GLSurfaceView.Renderer {

    private final Context context;
    private int program;
    
    private int textureCurrent;
    private int textureNext;
    private final int[] curWidth = new int[2];  // 🚀 텍스처 크기 추적 (0: Current, 1: Next)
    private final int[] curHeight = new int[2]; // 🚨 1281 에러 방지의 핵심 🛑
    
    private FloatBuffer vertexBuffer;
    private final float[] projectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    
    // 페이지 컬 상태 변수
    private float curlX = 1.0f; // 1.0 (우측) ~ -1.2 (완전히 넘어감)
    private float curlRadius = 0.18f;
    
    public OpenGLPageCurlRenderer(Context context) {
        this.context = context;
    }

    private void checkGlError(String label) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("GL_ERROR", label + ": glError " + error);
        }
    }

    public void setCurlX(float x) {
        this.curlX = x;
    }

    // 🚀 7년 차 개발자의 물리 인터페이스: 종이의 말림 두께를 애니메이션 중에 동적으로 조절 🛑
    public void setCurlRadius(float r) {
        this.curlRadius = r;
    }

    public float getCurlX() {
        return curlX;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 🚀 7년 차 개발자의 색상 정밀 튜닝: 'Papyrus(0.96, 0.945, 0.902)' 대신 
        // 🚨 실제 PDF의 배경과 가장 유사한 화이트-세피아(1.0, 1.0, 0.98)로 보정하여 블랙 플래시 차단 🛑
        GLES20.glClearColor(1.0f, 1.0f, 0.98f, 1.0f); 
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        
        initShaders();
        initMesh();
    }

    private void initShaders() {
        String vertexShaderCode = 
            "uniform mat4 uMVPMatrix;" +
            "uniform float uCurlX;" +
            "uniform float uRadius;" +
            "attribute vec4 vPosition;" +
            "attribute vec2 vTexCoord;" +
            "varying vec2 fTexCoord;" +
            "varying float vShadow;" +
            "varying float vIsBackSide;" +
            "varying float vSpineShadow;" +
            "void main() {" +
            "  vec4 pos = vPosition;" +
            "  float dist = pos.x - uCurlX;" +
            "  vShadow = 1.0;" +
            "  vIsBackSide = 0.0;" +
            "  vSpineShadow = 1.0 - (0.4 * exp(-8.0 * abs(pos.x)));" + // 🚀 중앙 책등(Spine) 그림자 계산
            "  if (dist > 0.0) {" +
            "    float angle = dist / uRadius;" +
            "    if (angle <= 3.14159) {" +
            "      pos.x = uCurlX + uRadius * sin(angle);" +
            "      pos.z = uRadius * (1.0 - cos(angle));" +
            "      vShadow = 0.5 + 0.5 * cos(angle * 0.8);" + // 🚀 곡면 명암비 강화 (웹 퀄리티 조준)
            "    } else {" +
            "      pos.x = uCurlX - (dist - uRadius * 3.14159);" +
            "      pos.z = uRadius * 2.0;" +
            "      vIsBackSide = 1.0;" +
            "      vShadow = 0.6;" + // 🚀 뒷면 진하기 최적화
            "    }" +
            "  }" +
            "  fTexCoord = vTexCoord;" +
            "  gl_Position = uMVPMatrix * pos;" +
            "}";

        String fragmentShaderCode = 
            "precision mediump float;" +
            "varying vec2 fTexCoord;" +
            "varying float vShadow;" +
            "varying float vIsBackSide;" +
            "varying float vSpineShadow;" +
            "uniform sampler2D sTexture;" +
            "uniform float uCastShadow;" + // 🚀 실시간 투영 그림자 농도
            "void main() {" +
            "  vec2 tex = fTexCoord;" +
            "  if (vIsBackSide > 0.5) tex.x = 1.0 - tex.x;" +
            "  vec4 color = texture2D(sTexture, tex);" +
            "  float finalShadow = vShadow * vSpineShadow;" +
            "  if (vIsBackSide < 0.5) finalShadow *= uCastShadow;" +
            "  gl_FragColor = vec4(color.rgb * finalShadow, color.a);" +
            "}";

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
    }

    private void initMesh() {
        int rows = 40;
        int cols = 40;
        float[] vertices = new float[rows * cols * 5 * 6];
        int idx = 0;
        
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols - 1; c++) {
                float x1 = (float)c / (cols - 1) * 2.0f - 1.0f;
                float y1 = (float)r / (rows - 1) * 2.0f - 1.0f;
                float x2 = (float)(c+1) / (cols - 1) * 2.0f - 1.0f;
                float y2 = (float)(r+1) / (rows - 1) * 2.0f - 1.0f;
                
                // Triangle 1
                vertices[idx++] = x1; vertices[idx++] = y1; vertices[idx++] = 0; vertices[idx++] = (x1+1)/2; vertices[idx++] = (1-y1)/2;
                vertices[idx++] = x2; vertices[idx++] = y1; vertices[idx++] = 0; vertices[idx++] = (x2+1)/2; vertices[idx++] = (1-y1)/2;
                vertices[idx++] = x1; vertices[idx++] = y2; vertices[idx++] = 0; vertices[idx++] = (x1+1)/2; vertices[idx++] = (1-y2)/2;
                
                // Triangle 2
                vertices[idx++] = x2; vertices[idx++] = y1; vertices[idx++] = 0; vertices[idx++] = (x2+1)/2; vertices[idx++] = (1-y1)/2;
                vertices[idx++] = x2; vertices[idx++] = y2; vertices[idx++] = 0; vertices[idx++] = (x2+1)/2; vertices[idx++] = (1-y2)/2;
                vertices[idx++] = x1; vertices[idx++] = y2; vertices[idx++] = 0; vertices[idx++] = (x1+1)/2; vertices[idx++] = (1-y2)/2;
            }
        }
        
        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertices).position(0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        
        // 🚀 7년 차 개발자의 '절대적 전체 화면' 공식 복구 🛑
        // 🚨 비율(ratio)을 무시하고 강제로 [-1, 1] 범위를 화면 끝까지 늘림 (Stretch-to-Fit)
        // 🚨 near 면에서의 가시 범위가 좌표계 [-1, 1]과 일치하도록 frustum 고정
        
        float nearRatio = 3.0f / 4.0f; // near(3) / cameraZ(4) = 0.75
        float tightBound = 1.0f * nearRatio; // 0.75: 책의 끝(-1, 1)이 화면 끝에 닿도록 정밀 조준 🛑
        
        Matrix.frustumM(projectionMatrix, 0, -tightBound, tightBound, -tightBound, tightBound, 3, 7);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        // 🚀 카메라를 전진(Z=4) 시켜 책을 더 크게 렌더링 (줌-인)
        Matrix.setLookAtM(modelViewMatrix, 0, 0, 0, 4, 0, 0, 0, 0, 1, 0);
        float[] mvpMatrix = new float[16];
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);
        
        GLES20.glUseProgram(program);
        
        int posHandle = GLES20.glGetAttribLocation(program, "vPosition");
        int texHandle = GLES20.glGetAttribLocation(program, "vTexCoord");
        int mvpHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        int curlHandle = GLES20.glGetUniformLocation(program, "uCurlX");
        int radiusHandle = GLES20.glGetUniformLocation(program, "uRadius");
        int uTextureLoc = GLES20.glGetUniformLocation(program, "sTexture");

        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(curlHandle, curlX);
        GLES20.glUniform1f(radiusHandle, curlRadius);
        GLES20.glUniform1i(uTextureLoc, 0); // 🚨 텍스처 유닛 0번 강제 바인딩

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 5 * 4, vertexBuffer);
        GLES20.glEnableVertexAttribArray(posHandle);

        vertexBuffer.position(3);
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 5 * 4, vertexBuffer);
        GLES20.glEnableVertexAttribArray(texHandle);

        // 🚀 실시간 투영 그림자(Cast Shadow) 계산: 
        // 페이지가 넘어갈수록 바닥 페이지에 지는 그림자 농도 조절
        int castShadowHandle = GLES20.glGetUniformLocation(program, "uCastShadow");
        float castShadowFactor = 1.0f;
        if (curlX < 0.5f) {
            // 컬링이 진행될수록 바닥면 그림자 강화
            castShadowFactor = 0.7f + 0.3f * Math.max(0.0f, curlX);
        }

        // 1. 하단 페이지 (다음 페이지) + 투영 그림자 효과
        GLES20.glUniform1f(castShadowHandle, castShadowFactor);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNext);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexBuffer.capacity() / 5);

        // 2. 상단 페이지 (현재 페이지) 넘김 그리기 (뒷면 포함)
        GLES20.glUniform1f(castShadowHandle, 1.0f); // 상단 페이지는 자기 그림자 영향 없음
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureCurrent);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexBuffer.capacity() / 5);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    public synchronized void updateTextures(Bitmap current, Bitmap next) {
        textureCurrent = updateTextureContent(current, textureCurrent, 0);
        textureNext = updateTextureContent(next, textureNext, 1);
    }
    
    // 🚀 페이지 전환 딜레이 제거를 위한 즉시 스왑 로직
    public synchronized void swapTextures() {
        // 🚨 7년 차 개발자의 '잔상 유지' 필살기: 
        // 🚨 textureNext(이미 넘어간 페이지)를 textureCurrent로 승격시킴
        textureCurrent = textureNext;
        
        // 🚨 textureNext에 textureCurrent(같은 값)를 그대로 유지하여 
        // 🚨 다음 로드가 완료될 때까지 하단 레이어가 비어있지 않게 함 🛑
    }

    private int updateTextureContent(Bitmap bitmap, int oldId, int index) {
        if (bitmap == null || bitmap.isRecycled()) return oldId;
        
        int textureId = oldId;
        
        // 1. 🚨 텍스처 풀링 및 생성
        if (textureId == 0) {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            textureId = textures[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            
            curWidth[index] = bitmap.getWidth();
            curHeight[index] = bitmap.getHeight();
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            
            // 🚨 7년 차 개발자의 미학: 비트맵 크기(가로/세로 모드 전환 등)가 달라졌으면 재할당 🛑
            // 🚨 이 로직이 없으면 GL_INVALID_VALUE (1281) 에러가 발생하며 화면이 깨집니다. 🛑
            if (bitmap.getWidth() != curWidth[index] || bitmap.getHeight() != curHeight[index]) {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                curWidth[index] = bitmap.getWidth();
                curHeight[index] = bitmap.getHeight();
                Log.d("OpenGLRenderer", "Texture reallocated for index " + index + ": " + curWidth[index] + "x" + curHeight[index]);
            } else {
                GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap);
            }
        }
        
        return textureId;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
    
    public void recycle() {
        int[] textures = {textureCurrent, textureNext};
        GLES20.glDeleteTextures(2, textures, 0);
    }
}
