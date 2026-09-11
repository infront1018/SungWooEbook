import React, { useState } from 'react';
import { StyleSheet, View, Dimensions, TouchableOpacity, Text, ScrollView } from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import Animated, { useSharedValue, useAnimatedStyle, withSpring } from 'react-native-reanimated';
import { BookViewer } from '../components/viewer/BookViewer';
import { VideoPlayer } from '../components/viewer/VideoPlayer';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { ChevronLeft } from 'lucide-react-native';
import { RootStackParamList } from '../config/navigation';
import { getVideos } from '../constants/streaming';
import { usePreventScreenCapture } from 'expo-screen-capture';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

const TEST_PAGES = [
  'https://cdn.pixabay.com/photo/2015/12/01/20/28/road-1072821_1280.jpg',
  'https://cdn.pixabay.com/photo/2015/06/19/21/24/avenue-815297_1280.jpg',
];

/**
 * 📖 + 🎬 하이브리드 동시 보기 화면.
 * [Android SplitViewerFragment 완벽 이관]
 */
export default function SplitViewerScreen() {
  const navigation = useNavigation();
  const route = useRoute<RouteProp<RootStackParamList, 'DualViewer'>>();
  const { book, videoUrl: initialUrl } = route.params;

  const [currentVideoUrl, setCurrentVideoUrl] = useState(initialUrl);
  const splitPercent = useSharedValue(0.5);
  const bookProgress = useSharedValue(0);

  // 🚀 보안 기능: 화면 캡처 및 녹화 방지 (Phase 4)
  usePreventScreenCapture();

  // 시리즈 내 모든 영상 리스트 가져오기 (탭 전환용)
  const allVideos = getVideos(book.seriesId, book.volume) || [];

  const dragGesture = Gesture.Pan()
    .onUpdate((event) => {
      const newPercent = event.absoluteX / SCREEN_WIDTH;
      splitPercent.value = Math.max(0.2, Math.min(0.8, newPercent));
    });

  const bookStyle = useAnimatedStyle(() => ({
    flex: splitPercent.value,
  }));

  const videoStyle = useAnimatedStyle(() => ({
    flex: 1 - splitPercent.value,
  }));

  const bookPanGesture = Gesture.Pan()
    .onUpdate((event) => {
      const moveX = -event.translationX / (SCREEN_WIDTH * 0.4);
      bookProgress.value = Math.max(0, Math.min(1, moveX));
    })
    .onEnd(() => {
      bookProgress.value = withSpring(0);
    });

  return (
    <View style={styles.container}>
      {/* 툴바 */}
      <View style={styles.toolbar}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
          <ChevronLeft stroke="#fff" size={24} {...({} as any)} />
        </TouchableOpacity>
        <Text style={styles.title}>{book.title} - 함께 보기</Text>
      </View>

      <View style={styles.splitWrapper}>
        <Animated.View style={[styles.section, bookStyle]}>
          <GestureDetector gesture={bookPanGesture}>
            <View style={{ flex: 1 }}>
              <BookViewer 
                currentUrl={TEST_PAGES[0]} 
                nextUrl={TEST_PAGES[1]} 
                progress={bookProgress} 
              />
            </View>
          </GestureDetector>
        </Animated.View>

        <GestureDetector gesture={dragGesture}>
          <View style={styles.divider}>
            <View style={styles.handle} />
          </View>
        </GestureDetector>

        <Animated.View style={[styles.section, videoStyle]}>
          <VideoPlayer url={currentVideoUrl} />
          
          {/* 🚀 영상 전환 플로팅 탭 (Android 구현체 그대로 이식) */}
          {allVideos.length > 1 && (
            <View style={styles.tabContainer}>
              <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                {allVideos.map((v) => (
                  <TouchableOpacity 
                    key={v.url} 
                    style={[
                      styles.chip, 
                      currentVideoUrl === v.url && styles.chipActive
                    ]}
                    onPress={() => setCurrentVideoUrl(v.url)}
                  >
                    <Text style={[
                      styles.chipText, 
                      currentVideoUrl === v.url && styles.chipTextActive
                    ]}>🎬 {v.type}</Text>
                  </TouchableOpacity>
                ))}
              </ScrollView>
            </View>
          )}
        </Animated.View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  toolbar: {
    height: 80,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingTop: 30,
    backgroundColor: '#1E293B',
  },
  backBtn: {
    padding: 4,
  },
  title: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
    marginLeft: 12,
  },
  splitWrapper: {
    flex: 1,
    flexDirection: 'row',
  },
  section: {
    overflow: 'hidden',
    position: 'relative',
  },
  divider: {
    width: 20,
    height: '100%',
    backgroundColor: '#334155',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 10,
  },
  handle: {
    width: 4,
    height: 48,
    backgroundColor: '#94A3B8',
    borderRadius: 2,
  },
  tabContainer: {
    position: 'absolute',
    top: 16,
    right: 16,
    left: 16,
    alignItems: 'flex-end',
  },
  chip: {
    backgroundColor: 'rgba(255,255,255,0.2)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    marginLeft: 8,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.3)',
  },
  chipActive: {
    backgroundColor: '#FACC15',
    borderColor: '#EAB308',
  },
  chipText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  chipTextActive: {
    color: '#1E293B',
  }
});
