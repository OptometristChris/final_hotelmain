package com.spring.app.jh.security.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.spring.app.jh.ops.admin.common.domain.AdminDashboardKpiDTO;
import com.spring.app.jh.ops.admin.common.domain.MonthlyReservationSummaryDTO;
import com.spring.app.jh.ops.admin.service.AdminDashboardService;
import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.Session_AdminDTO;
import com.spring.app.jh.security.service.AdminService;
import org.springframework.security.core.Authentication;

import com.spring.app.jh.security.auth.domain.JwtPrincipalDTO;
import com.spring.app.jh.security.domain.Session_AdminDTO;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/* ===== (#스프링시큐리티07-ADMIN-BRANCH) ===== */
@Controller
@RequiredArgsConstructor
@RequestMapping(value="/admin/branch/")
@PreAuthorize("hasRole('ADMIN_BRANCH')")  // ★ BRANCH 전용 컨트롤러(클래스 레벨로 고정)
public class AdminBranchController {

	private final AdminService adminService;
	private final AdminDashboardService adminDashboardService;

	/*
	   BRANCH 전용 기능

	   1) BRANCH 대시보드
	      - /admin/branch/dashboard

	   2) BRANCH 내 정보(프로필)
	      - /admin/branch/account/myInfo
	      - /admin/branch/account/profileEdit (GET/POST)
	*/


	// ============================================================
	// 0. BRANCH 대시보드
	// ============================================================

	@GetMapping("dashboard")
	public String dashboard(HttpSession session, Model model, Authentication authentication) {

	    Integer hotelId = null;

	    Session_AdminDTO sad = (Session_AdminDTO) session.getAttribute("sessionAdminDTO");
	    if (sad != null && sad.getFk_hotel_id() != null) {
	        hotelId = sad.getFk_hotel_id();
	    }
	    else if (authentication != null
	          && authentication.getPrincipal() instanceof JwtPrincipalDTO jwtPrincipal) {

	        if ("ADMIN".equals(jwtPrincipal.getPrincipalType()) || jwtPrincipal.getAdminType() != null) {
	            if (jwtPrincipal.getHotelId() != null) {
	                hotelId = jwtPrincipal.getHotelId().intValue();
	            }
	        }
	    }

	    if (hotelId == null) {
	        return "redirect:/admin/login";
	    }

	    AdminDashboardKpiDTO kpi = adminDashboardService.getBranchDashboardKpi(hotelId);
	    MonthlyReservationSummaryDTO monthlySummary = adminDashboardService.getBranchMonthlyReservationSummary(hotelId);

	    model.addAttribute("kpi_occupancy", kpi.getOccupancyRate());
	    model.addAttribute("kpi_sales", kpi.getMonthlySales());
	    model.addAttribute("kpi_cancelRate", kpi.getCancelRate());
	    model.addAttribute("kpi_revpar", kpi.getRevpar());

	    model.addAttribute("monthlySummary", monthlySummary);
	    model.addAttribute("todayReservationCount", adminDashboardService.getBranchTodayReservationCount(hotelId));
	    model.addAttribute("todayCancelCount", adminDashboardService.getBranchTodayCancelCount(hotelId));
	    
	    // 추가
	    model.addAttribute("qnaList", adminDashboardService.getBranchPendingQnaTopList(hotelId));
	    model.addAttribute("reservationList", adminDashboardService.getBranchTodayOperationReservationList(hotelId));

	    return "admin/branch/branch_dashboard";
	}


	

}