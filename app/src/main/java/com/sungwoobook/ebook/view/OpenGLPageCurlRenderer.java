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
 * 원기둥형 페이지 컬(Cylindrical Page Curl) 렌더러.
 *
 * 핵심 원리 (StPageFlip / 실제 책 넘김과 동일):
 *  - 버텍스 쉐이더에서 sin/cos 기반 원기둥 수학으로 종이 굴곡 계산
 *  - dist(폴드라인까지 거리) ≤ π×r : 종이가 둥글게 말리는 구간
 *  - dist > π×r               : 완전히 뒤집혀 뒷면이 보이는 구간
 *  - 대각선 폴드라인 (curlY로 제어)
 *  - 하단 페이지에 집중 드롭 쉐도우
 *  - 뒷면: 흰 종이색 + 앞면 내용 10% 비침
 */
public class OpenGLPageCurlRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "OpenGLPageCurlRenderer";

    @SuppressWarnings("unused")
    private final Context context;

    // 셰이더 (Pass1: 하단 페이지, Pass2: 상단 컬 페이지)
    private int programBottom;
    private int programCurl;

    private int textureCurrent;
    private int textureNext;
    private final int[] curWidth  = new int[2];
    private final int[] curHeight = new int[2];
    private float contentAspectRatio = 1.0f; // 가로/세로 비율 (1.0 = 정사각형)
    private int lastWidth, lastHeight;

    private final float[] projMatrix = new float[16];
    private final float[] mvMatrix   = new float[16];
    private final float[] mvpMatrix  = new float[16];

    // 컬 상태 변수
    // curlX : 1.0(우측 끝, 시작) ~ -1.2(완전히 넘어감)
    // curlY : 터치 시작 Y (-1.0 = 하단 모서리 기준)
    // curlRadius : 원기둥 반지름 (클수록 완만하게 말림)
    private float curlX      = 1.0f;
    private float curlY      = 1.0f;
    private float curlRadius = 0.20f;

    // 120×120 메시 (GPU 전송용 사전 빌드) - 정밀도 상향 (80->120) 🛑
    private static final int MESH_COLS = 120;
    private static final int MESH_ROWS = 120;
    private FloatBuffer meshBuffer;
    private int meshVertexCount;

    public OpenGLPageCurlRenderer(Context context) {
        this.context = context;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setCurlX(float x)      { this.curlX = x; }
    public void setCurlY(float y)      { this.curlY = y; }
    public void setCurlRadius(float r) { this.curlRadius = r; }
    public void setContentAspectRatio(float ratio) { 
        this.contentAspectRatio = ratio;
        // 🚀 즉시 갱신 (onSurfaceChanged를 기다리지 않고 바로 반영)
        updateProjectionMatrix(lastWidth, lastHeight);
    }
    public float getCurlX()            { return curlX; }
    public float getCurlY()            { return curlY; }

    // ── GLSurfaceView.Renderer ────────────────────────────────────────────────

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        // Depth Test 활성화: 상단/하단 절반이 겹치면서 깨지는 Perspective 분리 현상 방지
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        initShaders();
        buildMesh();
        initDummyTextures();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.lastWidth = width;
        this.lastHeight = height;
        GLES20.glViewport(0, 0, width, height);
        updateProjectionMatrix(width, height);
    }

    private void updateProjectionMatrix(int width, int height) {
        if (width <= 0 || height <= 0) return;
        
        float screenRatio = (float) width / height;
        float left, right, bottom, top;

        if (width > height) {
            // 🚀 가로 모드 (Rollback): 사용자가 선호하는 '전체 화면' 꽉 찬 느낌 복구
            // 비율을 무시하고 하드웨어 영역을 최대한 활용하던 이전 로직으로 회귀
            float b = 0.75f; 
            left = -b; right = b; bottom = -b; top = b;
        } else {
            // 🚀 세로 모드 (Zoom): 현재보다 더 크게 보이도록 확대 배율 적용
            // 7년 차 개발자의 '시각적 타협': 비율을 100% 지키기보다 화면 몰입감을 위해 18% 확대 (Zoom-in)
            float zoomFactor = 1.18f; 
            float adjustedRatio = contentAspectRatio / zoomFactor;

            if (screenRatio > adjustedRatio) {
                // 가로 여백 발생 시
                top = 1.0f;
                bottom = -1.0f;
                left = -screenRatio / adjustedRatio;
                right = screenRatio / adjustedRatio;
            } else {
                // 세로 여백 발생 시 (보통의 폰)
                left = -1.0f;
                right = 1.0f;
                top = adjustedRatio / screenRatio;
                bottom = -adjustedRatio / screenRatio;
            }
        }

        // frustumM: 시야각에 따른 원근 투영 (Near 3.0f, Far 7.0f로 컬 효과의 깊이감 극대화)
        Matrix.frustumM(projMatrix, 0, left, right, bottom, top, 3.0f, 7.0f);
        Matrix.setLookAtM(mvMatrix, 0, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0);
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        // Culling 비활성화: 종이 앞뒤를 한 번에 렌더링하기 위함
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        // Pass 1: 하단(다음) 페이지 먼저 그림
        drawBottomPage();
        
        // Pass 2 직전 매우 중요: Depth Buffer를 초기화해야 bottomPage(z=0)와 curlPage의 겹침 파괴 버그 차단
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        // Pass 2: 상단(현재) 페이지 컬 효과 (Pass1 위에 그려짐)
        drawCurlPage();
    }

    // ── Drawing Methods ───────────────────────────────────────────────────────

    /**
     * 하단(다음) 페이지 렌더링.
     * 컬 위치 근방만 집중적으로 어두어지는 드롭 쉐도우 적용.
     */
    private void drawBottomPage() {
        GLES20.glUseProgram(programBottom);

        int mvpLoc      = GLES20.glGetUniformLocation(programBottom, "uMVPMatrix");
        int texLoc      = GLES20.glGetUniformLocation(programBottom, "sTexture");
        int curlXLoc    = GLES20.glGetUniformLocation(programBottom, "uCurlX");
        int posLoc      = GLES20.glGetAttribLocation(programBottom, "vPosition");
        int uvLoc       = GLES20.glGetAttribLocation(programBottom, "vTexCoord");

        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1i(texLoc, 0);
        // UV 좌표계로 curlX 변환 (GL -1~1 → UV 0~1)
        GLES20.glUniform1f(curlXLoc, (curlX + 1f) / 2f);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNext);

        bindMeshAndDraw(posLoc, uvLoc);
    }

    /**
     * 상단(현재) 페이지 컬 렌더링.
     * 버텍스 쉐이더에서 원기둥형 컬 수학 처리.
     */
    private void drawCurlPage() {
        GLES20.glUseProgram(programCurl);

        int mvpLoc     = GLES20.glGetUniformLocation(programCurl, "uMVPMatrix");
        int texLoc     = GLES20.glGetUniformLocation(programCurl, "sTexture");
        int texNextLoc = GLES20.glGetUniformLocation(programCurl, "sTextureNext");
        int curlXLoc   = GLES20.glGetUniformLocation(programCurl, "uCurlX");
        int curlYLoc   = GLES20.glGetUniformLocation(programCurl, "uCurlY");
        int radiusLoc  = GLES20.glGetUniformLocation(programCurl, "uRadius");
        int posLoc     = GLES20.glGetAttribLocation(programCurl, "vPosition");
        int uvLoc      = GLES20.glGetAttribLocation(programCurl, "vTexCoord");

        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0);
        
        // 현재 프리 텍스처 바인딩 (앞면 용도)
        GLES20.glUniform1i(texLoc, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureCurrent);

        // 다음 텍스처 바인딩 (넘어가는 뒷면 용도)
        GLES20.glUniform1i(texNextLoc, 1);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNext);

        GLES20.glUniform1f(curlXLoc, curlX);
        GLES20.glUniform1f(curlYLoc, curlY);
        GLES20.glUniform1f(radiusLoc, curlRadius);

        bindMeshAndDraw(posLoc, uvLoc);
    }

    private void bindMeshAndDraw(int posLoc, int uvLoc) {
        meshBuffer.position(0);
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 5 * 4, meshBuffer);
        GLES20.glEnableVertexAttribArray(posLoc);

        meshBuffer.position(3);
        GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, 5 * 4, meshBuffer);
        GLES20.glEnableVertexAttribArray(uvLoc);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, meshVertexCount);

        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(uvLoc);
    }

    // ── Shader Initialization ─────────────────────────────────────────────────

    private void initShaders() {
        // ── 하단 페이지 버텍스 셰이더 ──────────────────────────────────────────
        String vsBottom =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "attribute vec2 vTexCoord;" +
            "varying vec2 fTexCoord;" +
            "void main() {" +
            "  fTexCoord = vTexCoord;" +
            "  gl_Position = uMVPMatrix * vPosition;" +
            "}";

        // ── 하단 페이지 프래그먼트 셰이더 ─────────────────────────────────────
        String fsBottom =
            "precision highp float;" +
            "varying vec2 fTexCoord;" +
            "uniform sampler2D sTexture;" +
            "uniform float uCurlX;" +   
            "void main() {" +
            "  vec4 color = texture2D(sTexture, fTexCoord);" +
            "  float dist = fTexCoord.x - uCurlX;" +
            "  float shadow = 0.0;" +
            "  if (dist > 0.0) {" +
            "    shadow = 0.55 * exp(-dist * 6.0);" +
            "  } else {" +
            "    shadow = 0.25 * exp(dist * 10.0) * (1.0 - uCurlX);" +
            "  }" +
            "  gl_FragColor = vec4(color.rgb * (1.0 - shadow), color.a);" +
            "}";

        // ── 컬 페이지 버텍스 셰이더 ───────────────────────────────────────────
        String vsCurl =
            "uniform mat4 uMVPMatrix;" +
            "uniform float uCurlX;" +
            "uniform float uCurlY;" +
            "uniform float uRadius;" +
            "attribute vec4 vPosition;" +
            "attribute vec2 vTexCoord;" +
            "varying vec2 fTexCoord;" +
            "varying vec2 fOrigPos;" +

            "void main() {" +
            "  const float PI = 3.14159265;" +
            "  vec4 pos = vPosition;" +
            "  fOrigPos = vPosition.xy;" +

            // 기존의 안정적이고 부드러운 0.18 기울기 고정형 원기둥
            "  float slant = 0.18;" +
            "  float foldX = uCurlX + (uCurlY - pos.y) * slant;" +
            "  float dist = pos.x - foldX;" +

            "  if (dist > 0.0) {" +
            "    float angle = dist / uRadius;" +
            "    if (angle <= PI) {" +
            "      pos.x = foldX + uRadius * sin(angle);" +
            "      pos.z = uRadius * (1.0 - cos(angle));" +
            "    } else {" +
            "      float excess = dist - PI * uRadius;" +
            "      pos.x = foldX - excess;" +
            // 🐛 결정적 버그 픽스: Z값을 -0.5로 급격히 꺾는 대신 곡면을 그대로 연장(2*r)시켜 Perspective 왜곡 파괴 현상 차단!
            "      pos.z = uRadius * 2.0;" +
            "    }" +
            "  }" +

            "  fTexCoord = vTexCoord;" +
            "  gl_Position = uMVPMatrix * pos;" +
            "}";

        // ── 컬 페이지 프래그먼트 셰이더 ──────────────────────────────────────
        String fsCurl =
            "precision highp float;" +
            "varying vec2 fTexCoord;" +
            "varying vec2 fOrigPos;" +
            "uniform sampler2D sTexture;" +
            "uniform sampler2D sTextureNext;" +
            "uniform float uCurlX;" +
            "uniform float uCurlY;" +
            "uniform float uRadius;" +

            "void main() {" +
            "  vec4 color;" +
            "  const float PI = 3.14159265;" +
            "  float slant = 0.18;" +
            "  float foldX = uCurlX + (uCurlY - fOrigPos.y) * slant;" +
            "  float dist = fOrigPos.x - foldX;" +

            "  if (dist > 0.0) {" +
            "    float angle = dist / uRadius;" +
            "    if (angle > PI * 0.5) {" +
            // 넘어간 부분 뒷면 (원기둥 뒤쪽부터 손에 쥐고 있는 평평한 뒷면 전체 끝까지)
            // 투명(alpha)으로 자르지 않아야 페이지 전체가 사용자 손끝까지 매끄럽게 보입니다.
            "      vec2 mirroredUV = vec2(1.0 - fTexCoord.x, fTexCoord.y);" +
            "      color = texture2D(sTextureNext, mirroredUV);" +
            "      float shadow = 0.5 + 0.5 * abs(cos(min(angle, PI)));" +
            "      color.rgb *= shadow;" +
            "    } else {" +
            // 앞면
            "      float shadow = 0.5 + 0.5 * cos(angle);" +
            "      color = texture2D(sTexture, fTexCoord);" +
            "      color.rgb *= shadow;" +
            "    }" +
            "  } else {" +
            "    color = texture2D(sTexture, fTexCoord);" +
            "  }" +

            "  gl_FragColor = color;" +
            "}";

        programBottom = buildProgram(vsBottom, fsBottom);
        programCurl   = buildProgram(vsCurl,   fsCurl);
    }

    // ── Mesh Builder ──────────────────────────────────────────────────────────

    /**
     * 80×80 메시를 CPU에서 한 번 생성, GPU 버퍼에 저장.
     * 버텍스 쉐이더가 컬 변환을 처리하므로 메시는 항상 평면 상태.
     * 각 정점: (x, y, z=0, u, v) - 5 floats
     */
    private void buildMesh() {
        int cols = MESH_COLS;
        int rows = MESH_ROWS;
        // 각 셀 = 2 삼각형 = 6 정점
        int totalVertices = cols * rows * 6;
        float[] verts = new float[totalVertices * 5];
        int idx = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // GL 좌표: -1 ~ +1
                float x0 = (float) c       / cols * 2f - 1f;
                float x1 = (float) (c + 1) / cols * 2f - 1f;
                float y0 = (float) r       / rows * 2f - 1f;
                float y1 = (float) (r + 1) / rows * 2f - 1f;

                // UV: x=0~1(좌→우), y=0~1(상→하, 주의 GL Y 반전)
                float u0 = (x0 + 1f) / 2f;
                float u1 = (x1 + 1f) / 2f;
                float v0 = 1f - (y0 + 1f) / 2f;  // GL Y 반전
                float v1 = 1f - (y1 + 1f) / 2f;

                // Triangle 1 (↖↗↘)
                verts[idx++] = x0; verts[idx++] = y1; verts[idx++] = 0f; verts[idx++] = u0; verts[idx++] = v1;
                verts[idx++] = x1; verts[idx++] = y1; verts[idx++] = 0f; verts[idx++] = u1; verts[idx++] = v1;
                verts[idx++] = x0; verts[idx++] = y0; verts[idx++] = 0f; verts[idx++] = u0; verts[idx++] = v0;

                // Triangle 2 (↗↘↙)
                verts[idx++] = x1; verts[idx++] = y1; verts[idx++] = 0f; verts[idx++] = u1; verts[idx++] = v1;
                verts[idx++] = x1; verts[idx++] = y0; verts[idx++] = 0f; verts[idx++] = u1; verts[idx++] = v0;
                verts[idx++] = x0; verts[idx++] = y0; verts[idx++] = 0f; verts[idx++] = u0; verts[idx++] = v0;
            }
        }

        meshVertexCount = totalVertices;
        meshBuffer = ByteBuffer.allocateDirect(verts.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        meshBuffer.put(verts).position(0);
    }

    // ── Texture Management ────────────────────────────────────────────────────

    public synchronized void updateTextures(Bitmap current, Bitmap next) {
        textureCurrent = updateTexture(current, textureCurrent, 0);
        textureNext    = updateTexture(next,    textureNext,    1);
    }

    /**
     * 역방향 플립(Backwards) 준비: 
     * - textureCurrent: 이전 페이지 (왼쪽에서 나타남)
     * - textureNext: 현재 페이지 (덮여질 페이지)
     */
    public synchronized void updateTexturesForReverseFlip(Bitmap prevPage, Bitmap currentPage) {
        textureCurrent = updateTexture(prevPage,    textureCurrent, 0);
        textureNext    = updateTexture(currentPage, textureNext,    1);
    }

    public synchronized void swapTextures() {
        // GL ID 교환: 두 텍스처가 항상 별도 GL 객체를 유지
        // (같은 ID를 공유하면 loadBitmaps가 두 텍스처를 동시에 덮어쓰는 버그 방지)
        int tmpId = textureCurrent;
        textureCurrent = textureNext;
        textureNext = tmpId;
        // 크기 추적도 함께 교환
        int tmpW = curWidth[0];  curWidth[0]  = curWidth[1];  curWidth[1]  = tmpW;
        int tmpH = curHeight[0]; curHeight[0] = curHeight[1]; curHeight[1] = tmpH;
    }

    public synchronized void updateNextTexture(Bitmap next) {
        textureNext = updateTexture(next, textureNext, 1);
    }

    public void recycle() {
        GLES20.glDeleteTextures(2, new int[]{ textureCurrent, textureNext }, 0);
        textureCurrent = 0;
        textureNext    = 0;
    }

    private int updateTexture(Bitmap bmp, int oldId, int index) {
        if (bmp == null || bmp.isRecycled()) return oldId;

        int id = oldId;
        if (id == 0) {
            int[] t = new int[1];
            GLES20.glGenTextures(1, t, 0);
            id = t[0];
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        // LINEAR_MIPMAP_LINEAR + generateMipmap → 고해상도 안티엘리어싱
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,     GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,     GLES20.GL_CLAMP_TO_EDGE);

        if (bmp.getWidth() != curWidth[index] || bmp.getHeight() != curHeight[index]) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
            curWidth[index]  = bmp.getWidth();
            curHeight[index] = bmp.getHeight();
            Log.d(TAG, "Texture reallocated [" + index + "]: " + curWidth[index] + "×" + curHeight[index]);
        } else {
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bmp);
        }
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        return id;
    }

    // ── GL Helpers ────────────────────────────────────────────────────────────

    private int buildProgram(String vsCode, String fsCode) {
        int vs = compileShader(GLES20.GL_VERTEX_SHADER,   vsCode);
        int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsCode);
        int pg = GLES20.glCreateProgram();
        GLES20.glAttachShader(pg, vs);
        GLES20.glAttachShader(pg, fs);
        GLES20.glLinkProgram(pg);
        int[] status = new int[1];
        GLES20.glGetProgramiv(pg, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "Program link error: " + GLES20.glGetProgramInfoLog(pg));
        }
        return pg;
    }

    private void initDummyTextures() {
        Bitmap dummy = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        dummy.eraseColor(android.graphics.Color.WHITE);
        updateTextures(dummy, dummy);
        dummy.recycle();
    }

    private int compileShader(int type, String code) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, code);
        GLES20.glCompileShader(s);
        int[] status = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            Log.e(TAG, "Shader compile error (" + (type == GLES20.GL_VERTEX_SHADER ? "VS" : "FS") + "): " + GLES20.glGetShaderInfoLog(s));
        }
        return s;
    }
}
