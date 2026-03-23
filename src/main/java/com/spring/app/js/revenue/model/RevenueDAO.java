package com.spring.app.js.revenue.model;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface RevenueDAO {

    /**
     * 월간 요약 통계 (총 매출액, 예약 건수, 취소 건수 등)
     * 반환 Key: TOTAL_REVENUE, TOTAL_COUNT, CANCEL_COUNT
     */
	Map<String, Object> selectRevenueSummary(Map<String, Object> paraMap);

    /**
     * 차트용 일별 매출 추이
     * 반환 Key: PAID_DAY, DAILY_REVENUE
     */
	List<Map<String, Object>> selectDailyRevenue(Map<String, Object> paraMap);

    /**
     * 객실 등급별 매출 비중 (도넛 차트용)
     * 반환 Key: ROOM_TYPE, TYPE_REVENUE
     */
	List<Map<String, Object>> selectRoomTypePortion(Map<String, Object> paraMap);

    /**
     * 결제 상세 내역 (테이블용)
     * 반환 Key: PAID_AT, PAYMENT_ID, MEMBER_NAME, PAYMENT_AMOUNT, PAYMENT_STATUS
     */
	List<Map<String, Object>> selectMonthlyPaymentList(Map<String, Object> paraMap);

	// 호텔 목록 조회
	List<Map<String, String>> selectHotelList();
}