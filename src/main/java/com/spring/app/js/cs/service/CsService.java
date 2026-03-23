package com.spring.app.js.cs.service;

import java.util.List;
import java.util.Map;

public interface CsService {
    // 호텔별 FAQ 목록 조회
    List<Map<String, String>> getFaqListByHotel(String hotelId);

    // QnA 총 개수 조회 (페이징용)
    int getQnaTotalCount(Map<String, String> paraMap);

    // QnA 목록 조회 (페이징 포함)
    List<Map<String, String>> getQnaListWithPaging(Map<String, String> paraMap);

    // QnA 상세 조회
    Map<String, String> getQnaDetail(String qnaId);

    // Qna 작성
	int insertQna(Map<String, String> paraMap);
	
	// Qna 수정
	int updateQna(Map<String, String> paraMap);

	// Qna 삭제
	int deleteQna(String qnaId);
	
	// 어드민 답변 등록
	int updateQnaAnswer(Map<String, String> paraMap);

	// faq 등록
	int insertFaq(Map<String, String> paraMap);

	// faq 삭제
	int deleteFaq(String faqId);
}