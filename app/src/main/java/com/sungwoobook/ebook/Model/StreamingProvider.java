package com.sungwoobook.ebook.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 전집 시리즈별 와우자(Wowza) HLS 스트리밍 URL 공급자.
 * 사용자 제공 최신 데이터 및 정규화된 타이틀 매칭 로직 (2026-03-27)
 */
public class StreamingProvider {

    public static class StreamingVideo {
        public final String type;
        public final String url;

        public StreamingVideo(String type, String url) {
            this.type = type;
            this.url = url;
        }
    }

    private static final Map<String, List<StreamingVideo>> streamingData = new HashMap<>();

    static {
        // 1. 꼬마과학 (1~5권) - 애니메이션(B), 실험(T)
        addTwoVideos("꼬마과학", 1, "애니메이션", "GS-1-B", "실험", "GS-1-T");
        addTwoVideos("꼬마과학", 2, "애니메이션", "GS-2-B", "실험", "GS-2-T");
        addTwoVideos("꼬마과학", 3, "애니메이션", "GS-3-B", "실험", "GS-3-T");
        addTwoVideos("꼬마과학", 4, "애니메이션", "GS-4-B", "실험", "GS-4-T");
        addTwoVideos("꼬마과학", 5, "애니메이션", "GS-5-B", "실험", "GS-5-T");

        // 2. 꼬마수학 (1~5권) - 애니메이션(A), 놀이(T)
        addTwoVideos("꼬마수학", 1, "애니메이션", "GM-1-A", "놀이", "GM-1-T");
        addTwoVideos("꼬마수학", 2, "애니메이션", "GM-2-A", "놀이", "GM-2-T");
        addTwoVideos("꼬마수학", 3, "애니메이션", "GM-3-A", "놀이", "GM-3-T");
        addTwoVideos("꼬마수학", 4, "애니메이션", "GM-4-A", "놀이", "GM-4-T");
        addTwoVideos("꼬마수학", 5, "애니메이션", "GM-5-A", "놀이", "GM-5-T");

        // 3. 꼬마사회 (1~5권) - 애니메이션(A)
        addOneVideo("꼬마사회", 1, "애니메이션", "GS_01_A");
        addOneVideo("꼬마사회", 2, "애니메이션", "GS_02_A");
        addOneVideo("꼬마사회", 3, "애니메이션", "GS_03_A");
        addOneVideo("꼬마사회", 4, "애니메이션", "GS_04_A");
        addOneVideo("꼬마사회", 5, "애니메이션", "GS_05_A");

        // 4. 꿈틀이첫과학 (1~5권) - 애니메이션
        addOneVideo("꿈틀이첫과학", 1, "애니메이션", "01_feel");
        addOneVideo("꿈틀이첫과학", 2, "애니메이션", "02_breath");
        addOneVideo("꿈틀이첫과학", 3, "애니메이션", "03_eat");
        addOneVideo("꿈틀이첫과학", 4, "애니메이션", "04_poop");
        addOneVideo("꿈틀이첫과학", 5, "애니메이션", "05_move");

        // 5. 릴라팝 (1~5권) - 애니메이션
        addOneVideo("릴라팝", 1, "애니메이션", "ani9");
        addOneVideo("릴라팝", 2, "애니메이션", "ani10");
        addOneVideo("릴라팝", 3, "애니메이션", "ani11");
        addOneVideo("릴라팝", 4, "애니메이션", "ani12");
        addOneVideo("릴라팝", 5, "애니메이션", "ani13");

        // 6. 통합과학뒤집기 (1~5권) - 강의(L), 문제풀이(E)
        addTwoVideos("통합과학뒤집기", 1, "강의", "CSCI_1_L", "문제풀이", "CSCI_1_E");
        addTwoVideos("통합과학뒤집기", 2, "강의", "CSCI_2_L", "문제풀이", "CSCI_2_E");
        addTwoVideos("통합과학뒤집기", 3, "강의", "CSCI_3_L", "문제풀이", "CSCI_3_E");
        addTwoVideos("통합과학뒤집기", 4, "강의", "CSCI_4_L", "문제풀이", "CSCI_4_E");
        addTwoVideos("통합과학뒤집기", 5, "강의", "CSCI_5_L", "문제풀이", "CSCI_5_E");

        // 7. 사회뒤집기 (1~5권) - 강의
        addOneVideo("사회뒤집기", 1, "강의", "nsoci_1");
        addOneVideo("사회뒤집기", 2, "강의", "nsoci_2");
        addOneVideo("사회뒤집기", 3, "강의", "nsoci_3");
        addOneVideo("사회뒤집기", 4, "강의", "nsoci_4");
        addOneVideo("사회뒤집기", 5, "강의", "nsoci_5");
    }

    private static void addOneVideo(String series, int vol, String type, String fileName) {
        String key = (series + "_" + vol).replace(" ", "");
        String url = "https://6273012c6ece4.streamlock.net/vod/mp4:" + fileName + ".mp4/playlist.m3u8";
        List<StreamingVideo> list = new ArrayList<>();
        list.add(new StreamingVideo(type, url));
        streamingData.put(key, list);
    }

    private static void addTwoVideos(String series, int vol, String t1, String f1, String t2, String f2) {
        String key = (series + "_" + vol).replace(" ", "");
        String u1 = "https://6273012c6ece4.streamlock.net/vod/mp4:" + f1 + ".mp4/playlist.m3u8";
        String u2 = "https://6273012c6ece4.streamlock.net/vod/mp4:" + f2 + ".mp4/playlist.m3u8";
        List<StreamingVideo> list = new ArrayList<>();
        list.add(new StreamingVideo(t1, u1));
        list.add(new StreamingVideo(t2, u2));
        streamingData.put(key, list);
    }

    /**
     * @param seriesTitle 갤러리 엑티비티에서 표시되는 전집 타이틀
     * @param volume 도서의 권수 (1~5)
     */
    public static List<StreamingVideo> getVideos(String seriesTitle, int volume) {
        if (seriesTitle == null) return null;
        
        // 정규화 (공백 제거, 소문자)
        String normalized = seriesTitle.replace(" ", "").toLowerCase();
        
        // 🚨 1. 제외 대상: 완성편은 절대 영상이 나오면 안됨
        if (normalized.contains("완성")) {
            return null;
        }

        String resolvedSeries = null;

        // 🚨 2. 타이틀 기반 엄격 매칭 (갤러리 카테고리 명칭 기준)
        if (normalized.contains("통합과학")) resolvedSeries = "통합과학뒤집기";
        else if (normalized.contains("꼬마사회")) resolvedSeries = "꼬마사회";
        else if (normalized.contains("사회뒤집기")) resolvedSeries = "사회뒤집기";
        else if (normalized.contains("꼬마수학")) resolvedSeries = "꼬마수학";
        else if (normalized.contains("꼬마과학")) resolvedSeries = "꼬마과학";
        else if (normalized.contains("꿈틀이")) resolvedSeries = "꿈틀이첫과학";
        else if (normalized.contains("릴라팝")) resolvedSeries = "릴라팝";

        // 🚨 3. 매칭된 시리즈가 있으면 권수와 조합하여 데이터 반환
        if (resolvedSeries != null) {
            String fullKey = (resolvedSeries + "_" + volume).replace(" ", "");
            if (streamingData.containsKey(fullKey)) {
                return streamingData.get(fullKey);
            }
        }
        
        // 🚨 4. 백업: ID 등 다른 입력값이 들어왔을 경우를 위한 유연한 매칭
        if (normalized.startsWith("gs-")) resolvedSeries = "꼬마과학";
        else if (normalized.startsWith("gs_")) resolvedSeries = "꼬마사회";
        else if (normalized.startsWith("gm-")) resolvedSeries = "꼬마수학";
        else if (normalized.startsWith("csci")) resolvedSeries = "통합과학뒤집기";
        else if (normalized.startsWith("nsoci")) resolvedSeries = "사회뒤집기";
        else if (normalized.startsWith("ani")) resolvedSeries = "릴라팝";
        else if (normalized.contains("feel") || normalized.contains("dream")) resolvedSeries = "꿈틀이첫과학";

        if (resolvedSeries != null) {
            String fullKey = (resolvedSeries + "_" + volume).replace(" ", "");
            if (streamingData.containsKey(fullKey)) {
                return streamingData.get(fullKey);
            }
        }

        return null;
    }
}
