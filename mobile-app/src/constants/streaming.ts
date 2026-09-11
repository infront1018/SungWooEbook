import { StreamingVideo } from '../types';

/**
 * 전집 시리즈별 와우자(Wowza) HLS 스트리밍 URL 공급자.
 * 네이티브 Java 코드(StreamingProvider.java)를 TypeScript 버전으로 변환. (2026-04-06)
 */

const BASE_VOD_URL = "https://6273012c6ece4.streamlock.net/vod/mp4:";

const createUrl = (fileName: string) => `${BASE_VOD_URL}${fileName}.mp4/playlist.m3u8`;

const streamingData: Record<string, StreamingVideo[]> = {
  // 1. 꼬마과학 (1~5권)
  "꼬마과학_1": [{ type: "애니메이션", url: createUrl("GS-1-B") }, { type: "실험", url: createUrl("GS-1-T") }],
  "꼬마과학_2": [{ type: "애니메이션", url: createUrl("GS-2-B") }, { type: "실험", url: createUrl("GS-2-T") }],
  "꼬마과학_3": [{ type: "애니메이션", url: createUrl("GS-3-B") }, { type: "실험", url: createUrl("GS-3-T") }],
  "꼬마과학_4": [{ type: "애니메이션", url: createUrl("GS-4-B") }, { type: "실험", url: createUrl("GS-4-T") }],
  "꼬마과학_5": [{ type: "애니메이션", url: createUrl("GS-5-B") }, { type: "실험", url: createUrl("GS-5-T") }],

  // 2. 꼬마수학 (1~5권)
  "꼬마수학_1": [{ type: "애니메이션", url: createUrl("GM-1-A") }, { type: "놀이", url: createUrl("GM-1-T") }],
  "꼬마수학_2": [{ type: "애니메이션", url: createUrl("GM-2-A") }, { type: "놀이", url: createUrl("GM-2-T") }],
  "꼬마수학_3": [{ type: "애니메이션", url: createUrl("GM-3-A") }, { type: "놀이", url: createUrl("GM-3-T") }],
  "꼬마수학_4": [{ type: "애니메이션", url: createUrl("GM-4-A") }, { type: "놀이", url: createUrl("GM-4-T") }],
  "꼬마수학_5": [{ type: "애니메이션", url: createUrl("GM-5-A") }, { type: "놀이", url: createUrl("GM-5-T") }],

  // 3. 통합과학뒤집기 (1~5권)
  "통합과학뒤집기_1": [{ type: "강의", url: createUrl("CSCI_1_L") }, { type: "문제풀이", url: createUrl("CSCI_1_E") }],
  "통합과학뒤집기_2": [{ type: "강의", url: createUrl("CSCI_2_L") }, { type: "문제풀이", url: createUrl("CSCI_2_E") }],
  "통합과학뒤집기_3": [{ type: "강의", url: createUrl("CSCI_3_L") }, { type: "문제풀이", url: createUrl("CSCI_3_E") }],
  "통합과학뒤집기_4": [{ type: "강의", url: createUrl("CSCI_4_L") }, { type: "문제풀이", url: createUrl("CSCI_4_E") }],
  "통합과학뒤집기_5": [{ type: "강의", url: createUrl("CSCI_5_L") }, { type: "문제풀이", url: createUrl("CSCI_5_E") }],
};

/**
 * @param seriesTitle 시리즈 이름 (예: '꼬마과학')
 * @param volume 권수 (예: 1)
 */
export const getVideos = (seriesTitle: string, volume: number): StreamingVideo[] | null => {
  if (!seriesTitle) return null;
  const normalized = seriesTitle.replace(/\s/g, '').toLowerCase();

  // 시리즈 매칭로직
  let resolved: string | null = null;
  if (normalized.includes("통합과학")) resolved = "통합과학뒤집기";
  else if (normalized.includes("꼬마수학")) resolved = "꼬마수학";
  else if (normalized.includes("꼬마과학")) resolved = "꼬마과학";
  else if (normalized.includes("꼬마사회")) resolved = "꼬마사회";
  // ... 추가 시리즈 로직 확장 가능

  if (resolved) {
    const key = `${resolved}_${volume}`;
    return streamingData[key] || null;
  }
  return null;
};
