export interface StreamingVideo {
  type: string;
  url: string;
}

export interface Book {
  id: string;
  seriesId: string;
  volume: number;
  title: string;
  bookUrl: string;       // PDF 경로 (Firebase Storage)
  thumbnailUrl: string;  // 표지 경로 (Firebase Storage)
}

export type SeriesName =
  | '꼬마과학'
  | '꼬마수학'
  | '꼬마사회'
  | '꿈틀이첫과학'
  | '릴라팝'
  | '통합과학뒤집기'
  | '사회뒤집기';
