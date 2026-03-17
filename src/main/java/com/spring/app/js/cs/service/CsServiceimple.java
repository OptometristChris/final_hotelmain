package com.spring.app.js.cs.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

	@Override
	public int insertQna(Map<String, String> paraMap) {
		return dao.insertQna(paraMap);
	}

	@Override
	public int deleteQna(String qnaId) {
		return dao.deleteQna(qnaId);
	}
}