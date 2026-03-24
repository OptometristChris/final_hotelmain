package com.spring.app.js.promotion.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.app.hk.reservation.model.ReservationDAO;
import com.spring.app.js.promotion.domain.PromotionDTO;
import com.spring.app.js.promotion.model.PromotionDAO;

@Service
public class PromotionServiceimple implements PromotionService {

    @Autowired
    private PromotionDAO proDao;
    
    @Autowired
    private ReservationDAO rdao; // 예약 관련 DAO 주입

    // 특정 호텔의 현재 진행 중인 프로모션 목록 조회
    @Override
    public List<PromotionDTO> getPromotionList(Map<String, Object> paraMap) {
        return proDao.getPromotionList(paraMap);
    }

    // 프로모션 상세 정보 조회
    @Override
    public PromotionDTO getPromotionDetail(int promotionId) {
        // 특정 프로모션의 상세 내용을 조회합니다.
        return proDao.getPromotionDetail(promotionId);
    }
    
    // 프로모션 등록
    @Transactional
    public int insertPromotion(Map<String, String> paraMap) {
    	
    	// 3. 프로모션 마스터 등록 (PK를 추출하여 paraMap에 담음)
        int result1 = proDao.insertPromotionMaster(paraMap);
        
        // 4. 프로모션 배너 등록 (위에서 추출한 PK를 FK로 사용)
        int result2 = 0;
        if(result1 > 0) {
            result2 = proDao.insertPromotionBanner(paraMap);
        }
        
        return result2;
    }
    
    // 프로모션 삭제
    @Override
    @Transactional // 둘 중 하나라도 실패하면 롤백
    public int deletePromotion(int promotionId) {
        // 자식 테이블(Banner) 먼저 삭제
        int n1 = proDao.deletePromotionBanner(promotionId);
        // 부모 테이블(Master) 삭제
        int n2 = proDao.deletePromotionMaster(promotionId);
        
        return n2; // 최종 성공 여부 반환
    }

    // 객실 예약 및 프로모션 할인 적용 (핵심 기능)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int registerPackageReservation(Map<String, String> paraMap) {
        
        // 데이터 타입을 Map<String, Object>로 변환 (DAO 사양에 맞춤)
        Map<String, Object> dbMap = new HashMap<>(paraMap);

        // 1. 결제 정보 저장 (payment_id 추출)
        int n1 = rdao.insertPayment(dbMap);
        
        // 2. 예약 정보 저장 (reservation_id 추출)
        int n2 = 0;
        if(n1 > 0) {
            n2 = rdao.insertReservation(dbMap);
        }
        
        // 3. 프로모션 매핑 저장 (할인 근거 남기기)
        int n3 = 0;
        if(n2 > 0) {
            n3 = proDao.insertPromotionMapping(dbMap);
        }

        return (n1 > 0 && n2 > 0 && n3 > 0) ? 1 : 0;
    }
    
    //프로모션 수정
    @Override
    @Transactional // 두 테이블을 동시에 수정하므로 트랜잭션 처리가 필수입니다!
    public int updatePromotion(Map<String, String> paraMap) {
        
        // 1. PROMOTION_MASTER 테이블 수정 (제목, 기간, 할인율, 적용객실 등)
        int n1 = proDao.updatePromotion(paraMap);
        
        // 2. PROMOTION_BANNER 테이블 수정 (서브타이틀, 특별혜택(benefits), 이미지 등)
        int n2 = proDao.updatePromotionBanner(paraMap);
        
        // 두 테이블 수정이 모두 성공해야 1을 반환, 아니면 0 반환
        return (n1 > 0 && n2 > 0) ? 1 : 0;
    }
    
    // 프로모션 조건(타겟)에 맞는 객실 리스트만 조회
    public List<Map<String, Object>> getAvailableRoomsForPromotion(PromotionDTO promo) {
        return proDao.getAvailableRoomsForPromotion(promo);
    }
    
    @Override
    public List<Map<String, String>> getHotelList() {
        return proDao.getHotelList();
    }
    
    
}