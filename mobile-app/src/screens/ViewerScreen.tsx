import React, { useState, Suspense, useRef } from 'react';
import { StyleSheet, View, ActivityIndicator, TouchableOpacity, Text, Dimensions } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { ChevronLeft } from 'lucide-react-native';
import { BookViewer } from '../components/viewer/BookViewer';
import Animated, { useSharedValue, withSpring, runOnJS } from 'react-native-reanimated';
import { GestureDetector, Gesture } from 'react-native-gesture-handler';
import { usePreventScreenCapture } from 'expo-screen-capture';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

const TEST_PAGES = [
  'https://cdn.pixabay.com/photo/2015/12/01/20/28/road-1072821_1280.jpg',
  'https://cdn.pixabay.com/photo/2015/06/19/21/24/avenue-815297_1280.jpg',
  'https://cdn.pixabay.com/photo/2014/02/27/16/10/tree-276014_1280.jpg'
];

/**
 * 3D 페이지 넘김 기능이 포함된 메인 전자책 뷰어 화면.
 * [Android PdfViewerFragment 대응]
 */
export default function ViewerScreen() {
  const navigation = useNavigation();
  const [currentPage, setCurrentPage] = useState(0);
  const progress = useSharedValue(0);

  // 🚀 보안 기능: 화면 캡처 및 녹화 방지 (Phase 4)
  usePreventScreenCapture();

  const handlePageComplete = () => {
    if (currentPage < TEST_PAGES.length - 2) {
      setCurrentPage((prev: number) => prev + 1);
    }
    progress.value = 0;
  };

  const panGesture = Gesture.Pan()
    .onUpdate((event) => {
      const moveX = -event.translationX / (SCREEN_WIDTH * 0.7);
      progress.value = Math.max(0, Math.min(1, moveX));
    })
    .onEnd((event) => {
      if (progress.value > 0.4 || event.velocityX < -800) {
        progress.value = withSpring(1, { damping: 20 }, (finished) => {
          if (finished) runOnJS(handlePageComplete)();
        });
      } else {
        progress.value = withSpring(0);
      }
    });

  return (
    <View style={styles.container}>
      {/* 상단 툴바 */}
      <View style={styles.toolbar}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
          <ChevronLeft stroke="#fff" size={28} {...({} as any)} />
        </TouchableOpacity>
        <Text style={styles.pageIndicator}>{currentPage + 1} / {TEST_PAGES.length}</Text>
      </View>

      <GestureDetector gesture={panGesture}>
        <View style={styles.content}>
          <BookViewer 
            currentUrl={TEST_PAGES[currentPage]} 
            nextUrl={TEST_PAGES[currentPage + 1]} 
            progress={progress} 
          />
        </View>
      </GestureDetector>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  toolbar: {
    height: 100,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 40,
    backgroundColor: 'rgba(0,0,0,0.7)',
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 10,
  },
  backBtn: {
    padding: 8,
  },
  pageIndicator: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
    marginLeft: 12,
  },
  content: {
    flex: 1,
  },
});
