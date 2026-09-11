import React, { Suspense } from 'react';
import { StyleSheet, View, ActivityIndicator, Dimensions } from 'react-native';
import { Canvas, useFrame } from '@react-three/fiber/native';
import { useTexture } from '@react-three/drei/native';
import { PageFlip } from './PageFlip';
import Animated from 'react-native-reanimated';

/**
 * 3D 페이지 플립 렌더러만 포함하는 모듈형 컴포넌트.
 * [Split View 및 Full Viewer에서 공용 사용]
 */
export const BookViewer: React.FC<{ 
  currentUrl: string, 
  nextUrl: string, 
  progress: any 
}> = ({ currentUrl, nextUrl, progress }) => {
  return (
    <View style={styles.container}>
      <Suspense fallback={<ActivityIndicator size="large" color="#444" />}>
        <Canvas camera={{ position: [0, 0, 5], fov: 45 }}>
          <ambientLight intensity={0.8} />
          <pointLight position={[10, 10, 10]} />
          
          <Scene 
            currentUrl={currentUrl} 
            nextUrl={nextUrl} 
            progress={progress} 
          />
        </Canvas>
      </Suspense>
    </View>
  );
};

function Scene({ currentUrl, nextUrl, progress }: { currentUrl: string, nextUrl: string, progress: any }) {
  const [p1, p2] = useTexture([currentUrl, nextUrl]);
  const materialRef = React.useRef<any>(null);

  useFrame(() => {
    if (materialRef.current) {
        const p = progress.value;
        const curlX = 1.0 - (p * 2.1);
        materialRef.current.uCurlX = curlX;
        materialRef.current.uCurlY = p * 0.15;
    }
  });

  return <PageFlip currentTexture={p1} nextTexture={p2} progress={0} />;
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FAF5EF',
  }
});
