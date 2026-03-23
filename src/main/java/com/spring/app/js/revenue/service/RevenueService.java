package com.spring.app.js.revenue.service;

import java.util.List;
import java.util.Map;

public interface RevenueService {
    
    // 1. 월간 수익 요약 정보 조회 (총 매출, 예약 건수, 취소 건수 등)
    Map<String, Object> getRevenueSummary(Map<String, Object> paraMap);
    
    // 2. 차트용 일별 매출 추이 데이터 조회 (Line Chart)
    List<Map<String, Object>> getDailyRevenue(Map<String, Object> paraMap);
    
    // 3. 객실 등급별 매출 비중 조회 (Doughnut Chart)
    List<Map<String, Object>> getRoomTypePortion(Map<String, Object> paraMap);
    
    // 4. 월간 결제 상세 내역 리스트 조회 (Table)
    List<Map<String, Object>> getMonthlyPaymentList(Map<String, Object> paraMap);
    
    // 5. 호텔 리스트 가져오기
    List<Map<String, String>> getHotelList();
}