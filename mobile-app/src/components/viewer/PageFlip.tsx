import React, { useRef } from 'react';
import { useFrame, extend } from '@react-three/fiber';
import * as THREE from 'three';
import { PageCurlMaterial } from './PageCurlMaterial';

// custom shader material을 three-js 엘리먼트로 등록
extend({ PageCurlMaterial });

declare global {
  namespace JSX {
    interface IntrinsicElements {
      pageCurlMaterial: any;
    }
  }
}

interface PageFlipProps {
  currentTexture: THREE.Texture;
  nextTexture: THREE.Texture;
  progress: number; // 0.0 (시작) ~ 1.0 (완료)
}

/**
 * R3F 전용 페이지 플립 컴포넌트.
 * [Android OpenGLPageCurlRenderer 대응]
 * 0.0 ~ 1.0 사이의 progress 값을 받아 uCurlX(-1.0 ~ 1.0)로 변환하여 셰이더에 전달합니다.
 */
export const PageFlip: React.FC<PageFlipProps> = ({ 
  currentTexture, 
  nextTexture, 
  progress 
}) => {
  const materialRef = useRef<any>(null);

  useFrame(() => {
    if (materialRef.current) {
      // 🚀 안드로이드 공식: CurlX는 1.0(우측)에서 -1.0(좌측)으로 이동
      // progress 0.0 -> 1.0, progress 1.0 -> -1.0
      const curlX = 1.0 - (progress * 2.0);
      materialRef.current.uCurlX = curlX;
      
      // 약간의 사선넘김 효과 (uCurlY)
      materialRef.current.uCurlY = progress * 0.1;
    }
  });

  return (
    <mesh position={[0, 0, 0]}>
      {/* 2:3 또는 3:4 비율의 평면 지오메트리 (세밀한 곡면을 위해 세그먼트를 32 이상으로 설정) */}
      <planeGeometry args={[2.5, 3.5, 64, 64]} />
      
      <pageCurlMaterial
        ref={materialRef}
        uTextureCurrent={currentTexture}
        uTextureNext={nextTexture}
        transparent
        side={THREE.DoubleSide}
      />
    </mesh>
  );
};
