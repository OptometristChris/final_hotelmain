package com.spring.app.jh.ops.admin.service;

import com.spring.app.jh.ops.admin.common.domain.AdminDashboardKpiDTO;
import com.spring.app.jh.ops.admin.common.domain.MonthlyReservationSummaryDTO;

public interface AdminDashboardService {

    AdminDashboardKpiDTO getBranchDashboardKpi(int hotelId);

    AdminDashboardKpiDTO getHqDashboardKpi();
    
    MonthlyReservationSummaryDTO getBranchMonthlyReservationSummary(int hotelId);

    MonthlyReservationSummaryDTO getHqMonthlyReservationSummary();

    int getBranchTodayReservationCount(int hotelId);

    int getBranchTodayCancelCount(int hotelId);
}