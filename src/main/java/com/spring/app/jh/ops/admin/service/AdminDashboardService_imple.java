package com.spring.app.jh.ops.admin.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.spring.app.jh.ops.admin.common.domain.AdminDashboardKpiDTO;
import com.spring.app.jh.ops.admin.common.domain.BranchDashboardQnaDTO;
import com.spring.app.jh.ops.admin.common.domain.BranchDashboardReservationDTO;
import com.spring.app.jh.ops.admin.common.domain.MonthlyReservationSummaryDTO;
import com.spring.app.jh.ops.admin.model.AdminDashboardDAO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardService_imple implements AdminDashboardService {

    private final AdminDashboardDAO adminDashboardDAO;

    @Override
    public AdminDashboardKpiDTO getBranchDashboardKpi(int hotelId) {

        AdminDashboardKpiDTO dto = new AdminDashboardKpiDTO();

        dto.setOccupancyRate(adminDashboardDAO.selectBranchOccupancyRate(hotelId));
        dto.setMonthlySales(adminDashboardDAO.selectBranchMonthlySales(hotelId));
        dto.setCancelRate(adminDashboardDAO.selectBranchCancelRate(hotelId));
        dto.setRevpar(adminDashboardDAO.selectBranchRevpar(hotelId));

        if (dto.getOccupancyRate() == null) dto.setOccupancyRate("0");
        if (dto.getMonthlySales() == null) dto.setMonthlySales("0");
        if (dto.getCancelRate() == null) dto.setCancelRate("0");
        if (dto.getRevpar() == null) dto.setRevpar("0");

        return dto;
    }

    @Override
    public AdminDashboardKpiDTO getHqDashboardKpi() {

        AdminDashboardKpiDTO dto = new AdminDashboardKpiDTO();

        dto.setOccupancyRate(adminDashboardDAO.selectHqOccupancyRate());
        dto.setMonthlySales(adminDashboardDAO.selectHqMonthlySales());
        dto.setCancelRate(adminDashboardDAO.selectHqCancelRate());
        dto.setRevpar(adminDashboardDAO.selectHqRevpar());

        if (dto.getOccupancyRate() == null) dto.setOccupancyRate("0");
        if (dto.getMonthlySales() == null) dto.setMonthlySales("0");
        if (dto.getCancelRate() == null) dto.setCancelRate("0");
        if (dto.getRevpar() == null) dto.setRevpar("0");

        return dto;
    }

    @Override
    public MonthlyReservationSummaryDTO getBranchMonthlyReservationSummary(int hotelId) {

        MonthlyReservationSummaryDTO dto = adminDashboardDAO.selectBranchMonthlyReservationSummary(hotelId);

        if (dto == null) {
            dto = new MonthlyReservationSummaryDTO();
        }

        if (dto.getPaidCount() == null) dto.setPaidCount(0);
        if (dto.getPrevPaidCount() == null) dto.setPrevPaidCount(0);
        if (dto.getPaidDiffCount() == null) dto.setPaidDiffCount(0);
        if (dto.getPaidDiffRate() == null) dto.setPaidDiffRate("0");

        if (dto.getCancelCount() == null) dto.setCancelCount(0);
        if (dto.getPrevCancelCount() == null) dto.setPrevCancelCount(0);
        if (dto.getCancelDiffCount() == null) dto.setCancelDiffCount(0);
        if (dto.getCancelDiffRate() == null) dto.setCancelDiffRate("0");

        if (dto.getNetCount() == null) dto.setNetCount(0);

        return dto;
    }

    @Override
    public MonthlyReservationSummaryDTO getHqMonthlyReservationSummary() {

        MonthlyReservationSummaryDTO dto = adminDashboardDAO.selectHqMonthlyReservationSummary();

        if (dto == null) {
            dto = new MonthlyReservationSummaryDTO();
        }

        if (dto.getPaidCount() == null) dto.setPaidCount(0);
        if (dto.getPrevPaidCount() == null) dto.setPrevPaidCount(0);
        if (dto.getPaidDiffCount() == null) dto.setPaidDiffCount(0);
        if (dto.getPaidDiffRate() == null) dto.setPaidDiffRate("0");

        if (dto.getCancelCount() == null) dto.setCancelCount(0);
        if (dto.getPrevCancelCount() == null) dto.setPrevCancelCount(0);
        if (dto.getCancelDiffCount() == null) dto.setCancelDiffCount(0);
        if (dto.getCancelDiffRate() == null) dto.setCancelDiffRate("0");

        if (dto.getNetCount() == null) dto.setNetCount(0);

        return dto;
    }

    @Override
    public int getBranchTodayReservationCount(int hotelId) {
        Integer n = adminDashboardDAO.selectBranchTodayReservationCount(hotelId);
        return n == null ? 0 : n;
    }

    @Override
    public int getBranchTodayCancelCount(int hotelId) {
        Integer n = adminDashboardDAO.selectBranchTodayCancelCount(hotelId);
        return n == null ? 0 : n;
    }

    @Override
    public List<BranchDashboardQnaDTO> getBranchPendingQnaTopList(int hotelId) {
        List<BranchDashboardQnaDTO> list = adminDashboardDAO.selectBranchPendingQnaTopList(hotelId);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public List<BranchDashboardReservationDTO> getBranchTodayOperationReservationList(int hotelId) {
        List<BranchDashboardReservationDTO> list = adminDashboardDAO.selectBranchTodayOperationReservationList(hotelId);
        return list == null ? Collections.emptyList() : list;
    }
}