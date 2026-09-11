import React, { useState, useEffect, useRef } from 'react';
import { View, Text, StyleSheet, Dimensions, FlatList, NativeSyntheticEvent, NativeScrollEvent } from 'react-native';
import { Image } from 'expo-image';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

// 가상의 배너 데이터
const BANNERS = [
  { id: '1', title: '성우주니어 신간 출시!', imageUrl: 'https://cdn.pixabay.com/photo/2015/11/19/21/10/glasses-1052010_1280.jpg' },
  { id: '2', title: '꼬마과학뒤집기 전집', imageUrl: 'https://cdn.pixabay.com/photo/2018/07/01/20/01/book-3510326_1280.jpg' },
  { id: '3', title: '프리미엄 3D 뷰어 시작', imageUrl: 'https://cdn.pixabay.com/photo/2015/07/27/20/16/book-863418_1280.jpg' }
];

export const HeroBanner = () => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const flatListRef = useRef<FlatList>(null);

  // 3초마다 자동 슬라이드
  useEffect(() => {
    const timer = setInterval(() => {
      let nextIndex = currentIndex + 1;
      if (nextIndex >= BANNERS.length) nextIndex = 0;
      flatListRef.current?.scrollToIndex({ index: nextIndex, animated: true });
      setCurrentIndex(nextIndex);
    }, 3000);
    return () => clearInterval(timer);
  }, [currentIndex]);

  const onScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
    const slideSize = event.nativeEvent.layoutMeasurement.width;
    const index = event.nativeEvent.contentOffset.x / slideSize;
    const roundIndex = Math.round(index);
    if (roundIndex !== currentIndex) {
      setCurrentIndex(roundIndex);
    }
  };

  const renderItem = ({ item }: { item: typeof BANNERS[0] }) => {
    return (
      <View style={styles.slide}>
        <Image source={{ uri: item.imageUrl }} style={styles.image} contentFit="cover" />
        <View style={styles.overlay}>
          <Text style={styles.title}>{item.title}</Text>
        </View>
      </View>
    );
  };

  return (
    <View style={styles.container}>
      <FlatList
        ref={flatListRef}
        data={BANNERS}
        renderItem={renderItem}
        keyExtractor={item => item.id}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        onScroll={onScroll}
        scrollEventThrottle={16}
      />
      {/* 인디케이터 */}
      <View style={styles.indicatorContainer}>
        {BANNERS.map((_, idx) => (
          <View key={idx} style={[styles.dot, currentIndex === idx && styles.activeDot]} />
        ))}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: '100%',
    aspectRatio: 16 / 9, // Android와 동일한 16:9 반응형 지정
    borderRadius: 20,
    overflow: 'hidden',
    backgroundColor: '#E2E8F0',
    elevation: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.15,
    shadowRadius: 10,
  },
  slide: {
    width: SCREEN_WIDTH - 32, // HomeScreen.tsx 패딩 고려 (16+16)
    height: '100%',
  },
  image: {
    width: '100%',
    height: '100%',
  },
  overlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.3)',
    justifyContent: 'flex-end',
    padding: 16,
  },
  title: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
  indicatorContainer: {
    position: 'absolute',
    bottom: 16,
    alignSelf: 'center',
    flexDirection: 'row',
    backgroundColor: 'rgba(0,0,0,0.5)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: 'rgba(255,255,255,0.4)',
    marginHorizontal: 3,
  },
  activeDot: {
    backgroundColor: '#ffffff',
    width: 12,
  }
});
