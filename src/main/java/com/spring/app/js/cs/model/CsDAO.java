package com.spring.app.js.cs.model;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CsDAO {

    // 호텔별 FAQ 목록 조회 (XML의 id="getFaqListByHotel"와 매칭)
    List<Map<String, String>> getFaqListByHotel(String hotelId);

    // QnA 총 개수 조회 (XML의 id="getQnaTotalCount"와 매칭)
    int getQnaTotalCount(Map<String, String> paraMap);

    // QnA 목록 조회 (XML의 id="getQnaListWithPaging"와 매칭)
    List<Map<String, String>> getQnaListWithPaging(Map<String, String> paraMap);

    // QnA 상세 조회 (XML의 id="getQnaDetail"와 매칭)
    Map<String, String> getQnaDetail(String qnaId);

    // QnA 작성
	int insertQna(Map<String, String> paraMap);

	// QnA 수정
	int updateQna(Map<String, String> paraMap);
	
	// QnA 삭제
	int deleteQna(String qnaId);

	// 어드민 답변 테이블 인서트
	int insertAnswer(Map<String, String> paraMap);

	// qna 테이블 상태 업데이트
	int updateQnaStatus(Map<String, String> paraMap);

	// 어드민 답변 업데이트
	int updateAnswer(Map<String, String> paraMap);

	// FAQ 등록 구현
	int insertFaq(Map<String, String> paraMap);

	// FAQ 삭제 구현
	int deleteFaq(String faqId);

	// 호텔리스트(notice service 대신)
	List<Map<String, String>> getHotelList();


}