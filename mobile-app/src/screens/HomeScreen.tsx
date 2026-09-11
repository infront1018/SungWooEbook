import React, { useState, useRef } from 'react';
import { 
  StyleSheet, 
  Text, 
  View, 
  FlatList, 
  TouchableOpacity, 
  SafeAreaView, 
  ActivityIndicator, 
  ScrollView
} from 'react-native';
import { useBooks } from '../hooks/useBooks';
import { BookCover } from '../components/BookCover';
import { HeroBanner } from '../components/HeroBanner';
import { PreviewBottomSheet } from '../components/PreviewBottomSheet';
import BottomSheet from '@gorhom/bottom-sheet';
import { Book } from '../types';

/**
 * 전집 리스트를 보여주는 메인 홈 화면.
 * [Android HomeFragment & BaseGalleryFragment 대응]
 */
export default function HomeScreen() {
  const [selectedSeries, setSelectedSeries] = useState<string | undefined>(undefined);
  const [selectedBook, setSelectedBook] = useState<Book | null>(null);
  const bottomSheetRef = useRef<BottomSheet>(null);

  const { books, loading, error } = useBooks(selectedSeries);

  const handleBookSelect = (book: Book) => {
    setSelectedBook(book);
    bottomSheetRef.current?.expand();
  };

  const series = [
    { id: undefined, name: '전체' },
    { id: '꼬마과학', name: '꼬마과학' },
    { id: '꼬마수학', name: '꼬마수학' },
    { id: '통합과학', name: '통합과학' },
    { id: '꼬마사회', name: '꼬마사회' },
    { id: '사회뒤집기', name: '사회뒤집기' },
  ];

  const renderBook = ({ item }: { item: Book }) => (
    <TouchableOpacity 
      style={styles.bookCard} 
      activeOpacity={0.75}
      onPress={() => handleBookSelect(item)}
    >
      <BookCover 
        storagePath={item.thumbnailUrl} 
        width={100} 
        height={140} 
        borderRadius={12} 
      />
      <Text style={styles.bookTitle} numberOfLines={2}>
        {item.title}
      </Text>
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.container}>
      {/* 시리즈 필터 탭 */}
      <View style={styles.filterWrapper}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.filterScroll}>
          {series.map((s) => (
            <TouchableOpacity 
              key={s.name} 
              style={[
                styles.chip, 
                selectedSeries === s.id && styles.chipActive
              ]} 
              onPress={() => setSelectedSeries(s.id)}
            >
              <Text style={[
                styles.chipText, 
                selectedSeries === s.id && styles.chipTextActive
              ]}>
                {s.name}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      {/* 배너 및 로딩상태 감싸는 스크롤 */}
      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color="#4F46E5" />
        </View>
      ) : error ? (
        <View style={styles.center}>
          <Text style={styles.errorText}>데이터를 불러올 수 없습니다.</Text>
        </View>
      ) : (
        <FlatList
          data={books}
          renderItem={renderBook}
          keyExtractor={(item) => item.id}
          numColumns={3}
          columnWrapperStyle={styles.row}
          contentContainerStyle={styles.listContent}
          showsVerticalScrollIndicator={false}
          ListHeaderComponent={
            <View style={{ marginBottom: 24 }}>
              <HeroBanner />
            </View>
          }
        />
      )}

      {/* 미리보기 바텀 시트 */}
      <PreviewBottomSheet 
        sheetRef={bottomSheetRef} 
        selectedBook={selectedBook}
        onClose={() => setSelectedBook(null)}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FAF5EF', // 안드로이드의 나무 책장 느낌을 살릴 수 있는 웜톤 베이스
  },
  filterWrapper: {
    paddingVertical: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#E2E8F0',
  },
  filterScroll: {
    paddingHorizontal: 16,
  },
  chip: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#F1F5F9',
    marginRight: 8,
  },
  chipActive: {
    backgroundColor: '#4F46E5',
  },
  chipText: {
    fontSize: 14,
    color: '#64748B',
    fontWeight: '600',
  },
  chipTextActive: {
    color: '#fff',
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  errorText: {
    color: '#EF4444',
  },
  listContent: {
    padding: 16,
    paddingBottom: 40,
  },
  row: {
    justifyContent: 'flex-start',
    marginBottom: 40, // 책 등 간격 벌리기
    // 책장 선(Shelf Line) 효과
    borderBottomWidth: 4,
    borderBottomColor: '#D4B895', // 우드톤 선
    paddingBottom: 8,
  },
  bookCard: {
    width: '30%',
    marginHorizontal: '1.6%',
    alignItems: 'center',
    // 책이 선반에 세워진 느낌을 위한 그림자
    shadowColor: '#000',
    shadowOffset: { width: 4, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 5,
    elevation: 8,
  },
  bookTitle: {
    marginTop: 12,
    fontSize: 12,
    color: '#1E293B',
    fontWeight: '600',
    textAlign: 'center',
    backgroundColor: '#fff',
    paddingHorizontal: 4,
    borderRadius: 4,
  }
});
