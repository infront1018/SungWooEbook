import { useState, useEffect } from 'react';
import { collection, getDocs, query, where } from 'firebase/firestore';
import { db } from '../config/firebase';
import { Book } from '../types';

/**
 * 전집 리스트를 Firestore에서 가져오는 커스텀 훅.
 * 'sungwoo-db' 인스턴스의 'ebook_list' 컬렉션을 참조합니다.
 */
export const useBooks = (categoryId?: string) => {
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    const fetchBooks = async () => {
      setLoading(true);
      try {
        const ebookCollection = collection(db, 'ebook_list');
        const q = categoryId 
          ? query(ebookCollection, where('categoryId', '==', categoryId))
          : ebookCollection;

        const snapshot = await getDocs(q);
        const fetchedBooks: Book[] = snapshot.docs.map(doc => {
          const data = doc.data();
          return {
            id: doc.id,
            seriesId: data.categoryId,
            volume: data.volume || 0,
            title: `${data.categoryName} ${data.volume}권`,
            bookUrl: data.pdfPath,
            thumbnailUrl: data.thumbPath,
          };
        });

        // 권수 기준 정렬 (네이티브 로직 유지)
        fetchedBooks.sort((a, b) => a.volume - b.volume);
        
        setBooks(fetchedBooks);
      } catch (err) {
        console.error('Error fetching books:', err);
        setError(err as Error);
      } finally {
        setLoading(false);
      }
    };

    fetchBooks();
  }, [categoryId]);

  return { books, loading, error };
};
