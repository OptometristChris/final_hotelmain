package com.spring.app.jh.security.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spring.app.jh.ops.admin.common.domain.AdminDashboardKpiDTO;
import com.spring.app.jh.ops.admin.common.domain.MonthlyReservationSummaryDTO;
import com.spring.app.jh.ops.admin.service.AdminDashboardService;
import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.domain.Session_AdminDTO;
import com.spring.app.jh.security.service.AdminService;
import org.springframework.security.core.Authentication;

import com.spring.app.jh.security.auth.domain.JwtPrincipalDTO;
import com.spring.app.jh.security.domain.Session_AdminDTO;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/* ===== (#스프링시큐리티07-ADMIN-HQ) ===== */
@Controller
@RequiredArgsConstructor
@RequestMapping(value="/admin/hq/")
@PreAuthorize("hasRole('ADMIN_HQ')")  // ★ HQ 전용 컨트롤러(클래스 레벨로 고정)
public class AdminHqController {

	private final AdminService adminService;
	private final AdminDashboardService adminDashboardService;

	/*
	   HQ 전용 기능

	   1) HQ 대시보드
	      - /admin/hq/dashboard

	   2) HQ 내 정보(프로필)
	      - /admin/hq/account/myInfo
	      - /admin/hq/account/profileEdit (GET/POST)

	   3) BRANCH 관리자 계정 발급/관리
	      - /admin/hq/admins/**

	   ※ 로그인 성공 후 세션 저장/redirect는 AdminAuthenticationSuccessHandler 에서 처리한다.
	*/


	// ============================================================
	// 0. HQ 대시보드
	// ============================================================

	@GetMapping("dashboard")
	public String dashboard(Model model) {

        AdminDashboardKpiDTO kpi = adminDashboardService.getHqDashboardKpi();
        MonthlyReservationSummaryDTO monthlySummary = adminDashboardService.getHqMonthlyReservationSummary();
        
        model.addAttribute("kpi_occupancy", kpi.getOccupancyRate());
        model.addAttribute("kpi_sales", kpi.getMonthlySales());
        model.addAttribute("kpi_cancelRate", kpi.getCancelRate());
        model.addAttribute("kpi_revpar", kpi.getRevpar());
        
        model.addAttribute("monthlySummary", monthlySummary);
        model.addAttribute("reservationSummary", monthlySummary);

        return "admin/hq/hq_dashboard";
    }
	
	
	// ============================================================
		// 1. HQ 내 정보(프로필) 보기/수정
		// ============================================================

	@GetMapping("account/myInfo")
	public String myInfo(HttpSession session, Model model, Authentication authentication){

	    Integer adminNo = resolveAdminNo(session, authentication);

	    if(adminNo == null) {
	        return "redirect:/admin/login";
	    }

	    AdminDTO adminDto = adminService.getAdminDetail(adminNo);
	    model.addAttribute("adminDto", adminDto);

	    return "admin/hq/account/myInfo";
	}


	@GetMapping("account/profileEdit")
	public String profileEditForm(HttpSession session, Model model, Authentication authentication){

	    Integer adminNo = resolveAdminNo(session, authentication);

	    if(adminNo == null) {
	        return "redirect:/admin/login";
	    }

	    AdminDTO adminDto = adminService.getAdminDetail(adminNo);
	    model.addAttribute("adminDto", adminDto);

	    return "admin/hq/account/profileEditForm";
	}


	@PostMapping("account/profileEdit")
	public String profileEditEnd(AdminDTO adminDto,
	                             HttpSession session,
	                             Model model,
	                             Authentication authentication){

	    Integer adminNo = resolveAdminNo(session, authentication);

	    if(adminNo == null) {
	        return "redirect:/admin/login";
	    }

	    adminDto.setAdmin_no(adminNo);

	    int n = adminService.updateAdminProfile(adminDto);
	    model.addAttribute("result", n);

	    return "admin/hq/account/profileEditResult";
	}


		
		private Integer resolveAdminNo(HttpSession session, Authentication authentication) {

		    if (session != null) {
		        Session_AdminDTO sad = (Session_AdminDTO) session.getAttribute("sessionAdminDTO");
		        if (sad != null && sad.getAdmin_no() != null) {
		            return sad.getAdmin_no();
		        }
		    }

		    if (authentication != null && authentication.getPrincipal() instanceof JwtPrincipalDTO jwtPrincipal) {
		        if (jwtPrincipal.getPrincipalNo() != null) {
		            return jwtPrincipal.getPrincipalNo().intValue();
		        }
		    }

		    return null;
		}
	
	



}