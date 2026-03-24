package com.spring.app.js.promotion.service;

import java.util.List;
import java.util.Map;

import com.spring.app.js.promotion.domain.PromotionDTO;

public interface PromotionService {

    // 특정 호텔의 현재 진행 중인 프로모션 목록 조회
    List<PromotionDTO> getPromotionList(Map<String, Object> paraMap);

    // 프로모션 상세 정보 조회
    PromotionDTO getPromotionDetail(int promotionId);
    
    // 프로모션 등록
    int insertPromotion(Map<String, String> paraMap);
    
    // 프로모션 삭제
    int deletePromotion(int promotionId);

    // 객실 예약 및 프로모션 할인 적용 (핵심 기능)
	int registerPackageReservation(Map<String, String> paraMap);

	// 프로모션 수정
	int updatePromotion(Map<String, String> paraMap);

	// 프로모션 조건(타겟)에 맞는 객실 리스트만 조회
	List<Map<String, Object>> getAvailableRoomsForPromotion(PromotionDTO promo);

	
	List<Map<String, String>> getHotelList();
	
}