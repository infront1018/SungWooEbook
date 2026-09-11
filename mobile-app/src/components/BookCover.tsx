import React, { useState, useEffect } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { Image } from 'expo-image';
import { ref, getDownloadUrl } from 'firebase/storage';
import { storage } from '../config/firebase';

interface BookCoverProps {
  storagePath: string;
  width?: number;
  height?: number;
  borderRadius?: number;
}

/**
 * Firebase Storage 경로를 다운로드 URL로 변환하여 이미지를 렌더링하는 컴포넌트.
 * [Android Glide 대응] - expo-image를 사용하여 고성능 캐싱 지원.
 */
export const BookCover: React.FC<BookCoverProps> = ({ 
  storagePath, 
  width = 120, 
  height = 160, 
  borderRadius = 8 
}) => {
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!storagePath) return;

    if (storagePath.startsWith('http')) {
      setImageUrl(storagePath);
      setLoading(false);
      return;
    }

    const fetchUrl = async () => {
      try {
        const imageRef = ref(storage, storagePath);
        const url = await getDownloadUrl(imageRef);
        setImageUrl(url);
      } catch (error) {
        console.error('Error fetching image URL:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchUrl();
  }, [storagePath]);

  return (
    <View style={[styles.container, { width, height, borderRadius }]}>
      {loading ? (
        <ActivityIndicator size="small" color="#94A3B8" />
      ) : (
        <Image
          source={{ uri: imageUrl }}
          style={styles.image}
          contentFit="cover"
          transition={300}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#F1F5F9',
    justifyContent: 'center',
    alignItems: 'center',
    overflow: 'hidden',
  },
  image: {
    width: '100%',
    height: '100%',
  },
});
