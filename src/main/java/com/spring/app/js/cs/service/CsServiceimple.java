package com.spring.app.js.cs.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.app.js.cs.model.CsDAO;

@Service
public class CsServiceimple implements CsService {

    @Autowired
    private CsDAO dao;

    /**
     * 호텔별 FAQ 목록 조회 (조회수 높은 순 5개)
     */
    @Override
    public List<Map<String, String>> getFaqListByHotel(String hotelId) {
        return dao.getFaqListByHotel(hotelId);
    }

    /**
     * QnA 총 게시물 수 조회 (페이징 처리를 위한 카운트)
     */
    @Override
    public int getQnaTotalCount(Map<String, String> paraMap) {
        return dao.getQnaTotalCount(paraMap);
    }

    /**
     * QnA 목록 조회 (페이징 적용)
     */
    @Override
    public List<Map<String, String>> getQnaListWithPaging(Map<String, String> paraMap) {
        return dao.getQnaListWithPaging(paraMap);
    }

    /**
     * QnA 상세 조회 (질문 내용 + 관리자 답변 포함)
     */
    @Override
    public Map<String, String> getQnaDetail(String qnaId) {
        // 상세 조회 시 조회수 증가 로직이 필요하다면 여기에 추가할 수 있습니다.
        return dao.getQnaDetail(qnaId);
    }

    // Qna 작성
	@Override
	public int insertQna(Map<String, String> paraMap) {
		return dao.insertQna(paraMap);
	}
	
	// Qna 수정
	@Override
	public int updateQna(Map<String, String> paraMap) {
	    // 보안을 위해 DB에서 한 번 더 답변 여부를 확인합니다.
	    Map<String, String> qna = dao.getQnaDetail(paraMap.get("qnaId"));
	    
	    // 답변(ANS_CONTENT)이 이미 존재한다면 수정을 막고 0을 반환합니다.
	    if (qna != null && qna.get("ANS_CONTENT") != null) {
	        return 0; 
	    }
	    
	    return dao.updateQna(paraMap);
	}

	// Qna 삭제
	@Override
	public int deleteQna(String qnaId) {
		return dao.deleteQna(qnaId);
	}
	
	// 어드민 답변 등록
	@Override
	@Transactional
	public int updateQnaAnswer(Map<String, String> paraMap) {
	    // 1. 기존 답변이 있는지 확인 (상세조회 로직 재활용 가능)
	    Map<String, String> qna = dao.getQnaDetail(paraMap.get("qnaId"));
	    
	    int result = 0;
	    
	    // ANS_CONTENT가 null이 아니면 이미 답변이 있는 상태 -> '수정'
	    if (qna != null && qna.get("ANS_CONTENT") != null) {
	        // 답변 테이블 업데이트 (또는 QNA 테이블의 답변 컬럼 업데이트)
	        result = dao.updateAnswer(paraMap); 
	    } 
	    // 답변이 없는 상태 -> '최초 등록'
	    else {
	        int n1 = dao.insertAnswer(paraMap); 
	        int n2 = dao.updateQnaStatus(paraMap); // 상태를 '답변완료'로 변경
	        result = (n1 + n2 == 2) ? 1 : 0;
	    }
	    
	    return result;
	}
	
	// FAQ 등록 구현
    @Override
    public int insertFaq(Map<String, String> paraMap) {
        return dao.insertFaq(paraMap);
    }

    // FAQ 삭제 구현
    @Override
    public int deleteFaq(String faqId) {
        return dao.deleteFaq(faqId);
    }
    
}



