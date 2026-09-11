import React, { useMemo, useCallback } from 'react';
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import { ref, getDownloadURL } from 'firebase/storage';
import { storage } from '../config/firebase';
import { Heart } from 'lucide-react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RootStackParamList } from '../config/navigation';
import BottomSheet, { BottomSheetView, BottomSheetBackdrop } from '@gorhom/bottom-sheet';
import { Book } from '../types';
import { BookCover } from './BookCover';
import { getVideos } from '../constants/streaming';

interface PreviewBottomSheetProps {
  sheetRef: React.RefObject<BottomSheet | null>;
  selectedBook: Book | null;
  onClose: () => void;
}

/**
 * Android PreviewBottomSheet(Java/XML) 기능을 Expo React Native로 완벽히 포팅.
 * 책 상세 정보 노출 및 전자책/영상 재생 경로 분기 담당.
 */
export const PreviewBottomSheet: React.FC<PreviewBottomSheetProps> = ({ 
  sheetRef, 
  selectedBook,
  onClose
}) => {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const snapPoints = useMemo(() => ['75%'], []);

  const renderBackdrop = useCallback(
    (props: any) => (
      <BottomSheetBackdrop {...props} disappearsOnIndex={-1} appearsOnIndex={0} />
    ),
    []
  );

  if (!selectedBook) return null;

  // 영상 공급자 연동 (통합과학/꼬마과학 등 시리즈 판별)
  const videos = getVideos(selectedBook.seriesId, selectedBook.volume);

  return (
    <BottomSheet
      ref={sheetRef}
      index={-1}
      snapPoints={snapPoints}
      enablePanDownToClose
      backdropComponent={renderBackdrop}
      onClose={onClose}
      backgroundStyle={styles.background}
    >
      <BottomSheetView style={styles.content}>
        {/* 상단 액션 (즐겨찾기) */}
        <View style={styles.header}>
          <TouchableOpacity style={styles.heartBtn}>
            <Heart size={28} color="#EF4444" strokeWidth={2} {...({} as any)} />
          </TouchableOpacity>
        </View>

        {/* 도서 표지 (프리미엄 3:4 비율 반영) */}
        <View style={styles.coverWrapper}>
          <BookCover 
            storagePath={selectedBook.thumbnailUrl} 
            width={160} 
            height={213} 
            borderRadius={16} 
          />
        </View>

        {/* 책 제목 및 설명 */}
        <View style={styles.infoWrapper}>
          <Text style={styles.title}>{selectedBook.title}</Text>
          <Text style={styles.description}>
            본 전집에 대한 전문가의 생생한 해설과{"\n"}
            입체적인 3D 전자책 경험을 제공합니다.
          </Text>
        </View>

        {/* 버튼 그룹 (Android Layout 1:1 포팅) */}
        <View style={styles.buttonGroup}>
          <View style={styles.row}>
            <TouchableOpacity 
              style={[styles.btn, styles.btnPdf]}
              onPress={() => {
                if (selectedBook) {
                  onClose();
                  navigation.navigate('Viewer', { book: selectedBook });
                }
              }}
            >
              <Text style={styles.btnText}>📖 전자책 재생</Text>
            </TouchableOpacity>
            
            {(videos && videos.length > 0) && (
              <TouchableOpacity 
                style={[styles.btn, styles.btnVideo]}
                onPress={() => {
                  onClose();
                  navigation.navigate('VideoPlayer', { videoUrl: videos[0].url });
                }}
              >
                <Text style={styles.btnText}>🎬 {videos[0].type}</Text>
              </TouchableOpacity>
            )}
          </View>

          {/* 듀얼 뷰 킬러 기능 (함께 보기) */}
          {(videos && videos.length > 0) && (
            <TouchableOpacity 
              style={styles.btnTogether}
              onPress={() => {
                onClose();
                navigation.navigate('DualViewer', { book: selectedBook, videoUrl: videos[0].url });
              }}
            >
              <Text style={styles.btnTogetherText}>📖 ✨ 🎬 함께 보기 (분할 모드)</Text>
            </TouchableOpacity>
          )}
        </View>
      </BottomSheetView>
    </BottomSheet>
  );
};

const styles = StyleSheet.create({
  background: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
  },
  content: {
    padding: 24,
    alignItems: 'center',
    flex: 1,
  },
  header: {
    width: '100%',
    flexDirection: 'row',
    justifyContent: 'flex-end',
  },
  heartBtn: {
    padding: 8,
  },
  coverWrapper: {
    marginTop: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 20 },
    shadowOpacity: 0.25,
    shadowRadius: 30,
    elevation: 24,
  },
  infoWrapper: {
    marginTop: 24,
    alignItems: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1E293B',
    textAlign: 'center',
  },
  description: {
    marginTop: 12,
    fontSize: 15,
    color: '#64748B',
    textAlign: 'center',
    lineHeight: 22,
  },
  buttonGroup: {
    width: '100%',
    marginTop: 32,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  btn: {
    flex: 1,
    height: 56,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  btnPdf: {
    backgroundColor: '#1E293B',
    marginRight: 6,
  },
  btnVideo: {
    backgroundColor: '#1E293B',
    marginLeft: 6,
  },
  btnText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  btnTogether: {
    width: '100%',
    height: 60,
    backgroundColor: '#6366F1',
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 8,
    borderWidth: 1,
    borderColor: '#A5B4FC',
    elevation: 8,
  },
  btnTogetherText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  }
});
