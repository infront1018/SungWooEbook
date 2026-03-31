package com.sungwoobook.ebook.view;

import java.io.File;

/**
 * 페이지 플립 렌더링 엔진(2.5D, 3D)의 공통 인터페이스.
 * 하이브리드 엔진 교체를 가능하게 함.
 */
public interface IPageFlip {
    
    /**
     * PDF 파일을 설정하고 렌더링을 준비합니다.
     * @param file PDF 파일 객체
     */
    void setPdfFile(File file);

    /**
     * 페이지 변경 리스너를 설정합니다.
     * @param listener 페이지 변경 이벤트 리스너
     */
    void setOnPageChangeListener(OnPageChangeListener listener);

    /**
     * 전체 페이지 수를 반환합니다.
     * @return 전체 페이지 수
     */
    int getTotalPages();

    /**
     * 현재 페이지 인덱스를 반환합니다.
     * @return 현재 페이지 인덱스
     */
    int getCurrentPage();

    /**
     * 자원을 해제하고 렌더링을 중단합니다.
     */
    void recycle();

    /**
     * 페이지 변경 콜백 인터페이스.
     */
    interface OnPageChangeListener {
        void onPageChanged(int currentPage, int totalPages);
    }
}
