package com.spring.app.jh.ops.admin.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spring.app.jh.ops.admin.common.domain.MonthlyReservationSummaryDTO;

@Mapper
public interface AdminDashboardDAO {

    // ===== BRANCH =====
    String selectBranchOccupancyRate(@Param("hotelId") Integer hotelId);

    String selectBranchMonthlySales(@Param("hotelId") Integer hotelId);

    String selectBranchCancelRate(@Param("hotelId") Integer hotelId);

    String selectBranchRevpar(@Param("hotelId") Integer hotelId);

    // ===== HQ =====
    String selectHqOccupancyRate();

    String selectHqMonthlySales();

    String selectHqCancelRate();

    String selectHqRevpar();
    
    // ===== 월간 예약 요약 =====
    MonthlyReservationSummaryDTO selectBranchMonthlyReservationSummary(@Param("hotelId") Integer hotelId);
    MonthlyReservationSummaryDTO selectHqMonthlyReservationSummary();

    // ===== BRANCH 오늘 기준 카드 =====
    Integer selectBranchTodayReservationCount(@Param("hotelId") Integer hotelId);
    Integer selectBranchTodayCancelCount(@Param("hotelId") Integer hotelId);
}