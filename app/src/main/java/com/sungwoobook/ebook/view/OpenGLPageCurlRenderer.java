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

    private final float[] projMatrix = new float[16];
    private final float[] mvMatrix   = new float[16];
    private final float[] mvpMatrix  = new float[16];

    // 컬 상태 변수
    // curlX : 1.0(우측 끝, 시작) ~ -1.2(완전히 넘어감)
    // curlY : 터치 시작 Y (-1.0 = 하단 모서리 기준)
    // curlRadius : 원기둥 반지름 (클수록 완만하게 말림)
    private float curlX      = 1.0f;
    private float curlY      = -1.0f;
    private float curlRadius = 0.20f;

    // 80×80 메시 (GPU 전송용 사전 빌드)
    private static final int MESH_COLS = 80;
    private static final int MESH_ROWS = 80;
    private FloatBuffer meshBuffer;
    private int meshVertexCount;

    public OpenGLPageCurlRenderer(Context context) {
        this.context = context;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setCurlX(float x)      { this.curlX = x; }
    public void setCurlY(float y)      { this.curlY = y; }
    public void setCurlRadius(float r) { this.curlRadius = r; }
    public float getCurlX()            { return curlX; }
    public float getCurlY()            { return curlY; }

    // ── GLSurfaceView.Renderer ────────────────────────────────────────────────

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.95f, 0.94f, 0.92f, 1f);
        // Depth Test 비활성화: 드로쟉 순서(Pass1 하단 → Pass2 상단)로만 처리
        // Depth Test 사용 시 curlPage가 z=0 중복으로 bottomPage에 막히는 버그 발생
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        initShaders();
        buildMesh();
        initDummyTextures();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        // Stretch-to-Fit: 화면 가득 채우는 frustum
        float b = 3f / 4f;
        Matrix.frustumM(projMatrix, 0, -b, b, -b, b, 3f, 7f);
        Matrix.setLookAtM(mvMatrix, 0, 0f, 0f, 4f, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        
        // Culling 비활성화: 종이 앞뒤를 한 번에 렌더링하기 위함
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        // Pass 1: 하단(다음) 페이지 먼저 그림
        drawBottomPage();
        
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
     * - vIsBack = 0: 앞면 (textureCurrent 그대로)
     * - vIsBack = 1: 뒷면 (흰 종이 + 10% 비침)
     */
    private void drawCurlPage() {
        GLES20.glUseProgram(programCurl);

        int mvpLoc    = GLES20.glGetUniformLocation(programCurl, "uMVPMatrix");
        int texLoc    = GLES20.glGetUniformLocation(programCurl, "sTexture");
        int curlXLoc  = GLES20.glGetUniformLocation(programCurl, "uCurlX");
        int curlYLoc  = GLES20.glGetUniformLocation(programCurl, "uCurlY");
        int radiusLoc = GLES20.glGetUniformLocation(programCurl, "uRadius");
        int posLoc    = GLES20.glGetAttribLocation(programCurl, "vPosition");
        int uvLoc     = GLES20.glGetAttribLocation(programCurl, "vTexCoord");

        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniform1i(texLoc, 0);
        GLES20.glUniform1f(curlXLoc, curlX);
        GLES20.glUniform1f(curlYLoc, curlY);
        GLES20.glUniform1f(radiusLoc, curlRadius);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureCurrent);

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
        // 단순 패스스루. UV를 그대로 출력.
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
        // 폴드라인 근방의 UV X 좌표 기준으로 집중 드롭 쉐도우 적용
        String fsBottom =
            "precision highp float;" +
            "varying vec2 fTexCoord;" +
            "uniform sampler2D sTexture;" +
            "uniform float uCurlX;" +   // UV 공간에서 폴드 위치 (0~1)
            "void main() {" +
            "  vec4 color = texture2D(sTexture, fTexCoord);" +
            // 폴드라인 오른쪽만 그림자: 폴드 위치 가까울수록 어두움
            "  float dist = fTexCoord.x - uCurlX;" +
            "  float shadow = 0.0;" +
            "  if (dist > 0.0) {" +
            "    shadow = 0.55 * exp(-dist * 6.0);" +  // 지수 감쇠 그림자
            "  } else {" +
            // 폴드 왼쪽도 약한 중앙 책등(spine) 그림자
            "    shadow = 0.25 * exp(dist * 10.0) * (1.0 - uCurlX);" +
            "  }" +
            "  gl_FragColor = vec4(color.rgb * (1.0 - shadow), color.a);" +
            "}";

        // ── 컬 페이지 버텍스 셰이더 ───────────────────────────────────────────
        // 핵심: 원기둥형 컬 수학
        //
        //  [폴드라인 왼쪽 dist ≤ 0]  → 정적(변환 없음)
        //  [말리는 구간 0 < dist ≤ π×r] → 원기둥 곡면:
        //      new_x = foldX + r × sin(angle)
        //      new_z = r × (1 - cos(angle))    ← 종이가 들어올려짐
        //      vShadow 감소 (어두워짐)
        //  [뒷면 dist > π×r] → 완전히 뒤집힘:
        //      new_x = foldX - excess
        //      new_z = 2×r                     ← 뒤쪽 평면
        //      vIsBack = 1.0
        //
        //  대각선 폴드: foldX = curlX + (curlY - pos.y) × slant
        //  → Y 위치에 따라 폴드라인이 비스듬히 기울어짐
        String vsCurl =
            "uniform mat4 uMVPMatrix;" +
            "uniform float uCurlX;" +
            "uniform float uCurlY;" +
            "uniform float uRadius;" +
            "attribute vec4 vPosition;" +
            "attribute vec2 vTexCoord;" +
            "varying vec2 fTexCoord;" +
            "varying float vShadow;" +
            "varying float vIsBack;" +

            "void main() {" +
            "  const float PI = 3.14159265;" +
            "  vec4 pos = vPosition;" +
            "  vShadow  = 1.0;" +
            "  vIsBack  = 0.0;" +

            // 대각선 폴드라인: Y가 낮을수록(하단) 폴드가 오른쪽에 위치
            // slant 값이 클수록 대각선이 급격해짐
            "  float slant = 0.18;" +
            "  float foldX = uCurlX + (uCurlY - pos.y) * slant;" +

            "  float dist = pos.x - foldX;" +

            "  if (dist > 0.0) {" +
            "    float angle = dist / uRadius;" +

            "    if (angle <= PI * 0.5) {" +
            // ── 앞면 원기둥 말림 (0° ~ 90°) ──
            "      pos.x = foldX + uRadius * sin(angle);" +
            "      pos.z = uRadius * (1.0 - cos(angle));" +
            "      vShadow = 0.5 + 0.5 * cos(angle);" + // 1.0 ~ 0.5

            "    } else if (angle <= PI) {" +
            // ── 뒷면 커링 (90° ~ 180°) ──
            "      pos.x = foldX + uRadius * sin(angle);" +
            "      pos.z = uRadius * (1.0 - cos(angle));" +
            "      vIsBack = 1.0;" +
            "      vShadow = 0.5 + 0.5 * abs(cos(angle));" + // 0.5 ~ 1.0

            "    } else {" +
            // ── 완전히 넘어가서 하단 페이지가 보여야 할 구역 ──
            "      float excess = dist - PI * uRadius;" +
            "      pos.x = foldX - excess;" +
            "      pos.z = -1.0;" + // 하단 페이지(Z=0) 뒤로 숨김
            "      vIsBack = 2.0;" + // 프래그먼트 셰이더에서 Alpha=0 처리용
            "      vShadow = 0.0;" +
            "    }" +
            "  }" +

            "  fTexCoord = vTexCoord;" +
            "  gl_Position = uMVPMatrix * pos;" +
            "}";

        // ── 컬 페이지 프래그먼트 셰이더 ──────────────────────────────────────
        // vIsBack == 0: 앞면 → textureCurrent × vShadow
        // vIsBack == 1: 뒷면 → 순수 종이 흰색 (texture 샘플링 없음 → 블랙 버그 방지)
        String fsCurl =
            "precision highp float;" +
            "varying vec2 fTexCoord;" +
            "varying float vShadow;" +
            "varying float vIsBack;" +
            "uniform sampler2D sTexture;" +

            "void main() {" +
            "  vec4 color;" +
            "  float alpha = 1.0;" +

            "  if (vIsBack > 1.5) {" +
            // 완전히 넘어가서 하단 페이지가 보여야 하는 구역: 투명 처리
            "    alpha = 0.0;" +
            "    color = vec4(0.0);" +
            "  } else if (vIsBack > 0.5) {" +
            // 뒷면: 순수 오프화이트 종이색
            "    color = vec4(0.96, 0.95, 0.93, 1.0);" +
            "    color.rgb *= vShadow;" +
            "  } else {" +
            // 앞면: 텍스처 그대로 + 조명
            "    color = texture2D(sTexture, fTexCoord);" +
            "    color.rgb *= vShadow;" +
            "  }" +

            "  gl_FragColor = vec4(color.rgb, color.a * alpha);" +
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
