package com.spring.app.jh.ops.admin.service;

import java.util.List;

import com.spring.app.jh.ops.admin.common.domain.AdminDashboardKpiDTO;
import com.spring.app.jh.ops.admin.common.domain.BranchDashboardQnaDTO;
import com.spring.app.jh.ops.admin.common.domain.BranchDashboardReservationDTO;
import com.spring.app.jh.ops.admin.common.domain.MonthlyReservationSummaryDTO;

public interface AdminDashboardService {

    AdminDashboardKpiDTO getBranchDashboardKpi(int hotelId);

    AdminDashboardKpiDTO getHqDashboardKpi();

    MonthlyReservationSummaryDTO getBranchMonthlyReservationSummary(int hotelId);

    MonthlyReservationSummaryDTO getHqMonthlyReservationSummary();

    int getBranchTodayReservationCount(int hotelId);

    int getBranchTodayCancelCount(int hotelId);

    List<BranchDashboardQnaDTO> getBranchPendingQnaTopList(int hotelId);

    List<BranchDashboardReservationDTO> getBranchTodayOperationReservationList(int hotelId);
}