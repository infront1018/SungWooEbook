import * as THREE from 'three';
import { shaderMaterial } from '@react-three/drei';

/**
 * Android OpenGLPageCurlRenderer의 Vertex/Fragment 셰이더를 R3F(Three.js)용으로 이관.
 * 기존의 Cylinder 기반 컬링 수학 공식과 그림자 효과를 그대로 구현합니다.
 */
export const PageCurlMaterial = shaderMaterial(
  {
    uTextureCurrent: new THREE.Texture(),
    uTextureNext: new THREE.Texture(),
    uCurlX: 1.0, // 컬 진행 위치 (오른쪽 1.0에서 왼쪽 -1.0으로)
    uCurlY: 0.0, // 컬 각도/방향
    uRadius: 0.18, // 휘어지는 원기둥의 반지름 (반응형 대응)
  },
  // Vertex Shader
  `
    varying vec2 vUv;
    varying vec2 vOrigPos;
    
    uniform float uCurlX;
    uniform float uCurlY;
    uniform float uRadius;

    void main() {
      vUv = uv;
      vOrigPos = position.xy;

      // 0.18 고정 기울기 (안드로이드와 동일)
      float slant = 0.18;
      float foldX = uCurlX + (uCurlY - position.y) * slant;
      float dist = position.x - foldX;

      vec3 pos = position;

      if (dist > 0.0) {
        float angle = dist / uRadius;
        const float PI = 3.14159265;
        
        if (angle <= PI) {
          pos.x = foldX + uRadius * sin(angle);
          pos.z = uRadius * (1.0 - cos(angle));
        } else {
          float excess = dist - PI * uRadius;
          pos.x = foldX - excess;
          // Android의 Z 왜곡 픽스 적용 (곡면 연장)
          pos.z = uRadius * 2.0; 
        }
      }

      gl_Position = projectionMatrix * modelViewMatrix * vec4(pos, 1.0);
    }
  `,
  // Fragment Shader
  `
    varying vec2 vUv;
    varying vec2 vOrigPos;
    
    uniform sampler2D uTextureCurrent;
    uniform sampler2D uTextureNext;
    uniform float uCurlX;
    uniform float uCurlY;
    uniform float uRadius;

    void main() {
      const float PI = 3.14159265;
      float slant = 0.18;
      float foldX = uCurlX + (uCurlY - vOrigPos.y) * slant;
      float dist = vOrigPos.x - foldX;

      vec4 color;

      if (dist > 0.0) {
        float angle = dist / uRadius;
        if (angle > PI * 0.5) {
          // 넘어간 페이지 뒷면 (거울 반전 필요)
          vec2 mirroredUV = vec2(1.0 - vUv.x, vUv.y);
          color = texture2D(uTextureNext, mirroredUV);
          float shadow = 0.5 + 0.5 * abs(cos(min(angle, PI)));
          color.rgb *= shadow;
        } else {
          // 접혀 올라가는 앞면 (현재 페이지)
          float shadow = 0.5 + 0.5 * cos(angle);
          color = texture2D(uTextureCurrent, vUv);
          color.rgb *= shadow;
        }
      } else {
        // 평평한 앞면부
        color = texture2D(uTextureCurrent, vUv);
      }

      gl_FragColor = color;
    }
  `
);
