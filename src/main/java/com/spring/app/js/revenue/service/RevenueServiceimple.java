package com.spring.app.js.revenue.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.spring.app.js.revenue.model.RevenueDAO;

@Service
public class RevenueServiceimple implements RevenueService {

    @Autowired
    private RevenueDAO rdao;

    // 1. 월간 수익 요약 정보 조회 (총 매출, 예약 건수, 취소 건수 등)
    @Override
    public Map<String, Object> getRevenueSummary(Map<String, Object> paraMap) {
        // selectRevenueSummary 매퍼 호출
        return rdao.selectRevenueSummary(paraMap);
    }

    // 2. 차트용 일별 매출 추이 데이터 조회 (Line Chart)
    @Override
    public List<Map<String, Object>> getDailyRevenue(Map<String, Object> paraMap) {
        // selectDailyRevenue 매퍼 호출
        return rdao.selectDailyRevenue(paraMap);
    }

    // 3. 객실 등급별 매출 비중 조회 (Doughnut Chart)
    @Override
    public List<Map<String, Object>> getRoomTypePortion(Map<String, Object> paraMap) {
        // selectRoomTypePortion 매퍼 호출
        return rdao.selectRoomTypePortion(paraMap);
    }

    // 4. 월간 결제 상세 내역 리스트 조회 (Table)
    @Override
    public List<Map<String, Object>> getMonthlyPaymentList(Map<String, Object> paraMap) {
        // selectMonthlyPaymentList 매퍼 호출
        return rdao.selectMonthlyPaymentList(paraMap);
    }
    
    // 5. 호텔 리스트 가져오기
    @Override
    public List<Map<String, String>> getHotelList() {
        return rdao.selectHotelList();
    }
}