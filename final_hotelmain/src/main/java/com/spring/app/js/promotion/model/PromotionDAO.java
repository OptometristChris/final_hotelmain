package com.spring.app.js.promotion.model;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import com.spring.app.js.promotion.domain.PromotionDTO;

@Mapper
public interface PromotionDAO {

    // 1. 특정 호텔의 진행 중인 프로모션 목록 조회
    List<PromotionDTO> getPromotionList(Map<String, Object> paraMap);

    // 2. 프로모션 상세 조회 (필요 시)
    PromotionDTO getPromotionDetail(int promotionId);
    
 	// 3. 프로모션 마스터 등록 (PK를 추출하여 paraMap에 담음)
    int insertPromotionMaster(Map<String, String> paraMap);

    // 4. 프로모션 배너 등록 (위에서 추출한 PK를 FK로 사용)
    int insertPromotionBanner(Map<String, String> paraMap);
    
    // 5. 배너 테이블(자식) 삭제
    int deletePromotionBanner(int promotionId);

    // 6. 마스터 테이블(부모) 삭제
    int deletePromotionMaster(int promotionId);

    // 객실 예약 기본 정보 저장
	int insertBaseReservation(Map<String, String> paraMap);

	// 예약-프로모션 매핑 정보 저장
	int insertPromotionMapping(Map<String, Object> paraMap);

	// 프로모션 수정
	int updatePromotion(Map<String, String> paraMap);

	// 프로모션 배너 수정
	int updatePromotionBanner(Map<String, String> paraMap);

	// 프로모션 조건(타겟)에 맞는 객실 리스트만 조회
	List<Map<String, Object>> getAvailableRoomsForPromotion(PromotionDTO promo);

}