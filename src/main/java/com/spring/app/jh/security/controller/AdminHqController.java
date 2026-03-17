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
		public String myInfo(HttpSession session, Model model){

			Session_AdminDTO sad = (Session_AdminDTO) session.getAttribute("sessionAdminDTO");

			// 세션이 없다면 비정상 -> 다시 로그인
			if(sad == null) {
				return "redirect:/admin/login";
			}

			AdminDTO adminDto = adminService.getAdminDetail(sad.getAdmin_no()); // TODO
			model.addAttribute("adminDto", adminDto);

			return "admin/hq/account/myInfo";
			// src/main/resources/templates/admin/hq/account/myInfo.html
		}


		@GetMapping("account/profileEdit")
		public String profileEditForm(HttpSession session, Model model){

			Session_AdminDTO sad = (Session_AdminDTO) session.getAttribute("sessionAdminDTO");
			if(sad == null) {
				return "redirect:/admin/login";
			}

			AdminDTO adminDto = adminService.getAdminDetail(sad.getAdmin_no()); // TODO
			model.addAttribute("adminDto", adminDto);

			return "admin/hq/account/profileEditForm";
			// src/main/resources/templates/admin/hq/account/profileEditForm.html
		}


		@PostMapping("account/profileEdit")
		public String profileEditEnd(AdminDTO adminDto, HttpSession session, Model model){

			/*
			   ★ 핵심: 어떤 관리자인지(PK=admin_no)는 세션에서 강제 주입해야 한다.
			   - 사용자가 form에서 admin_no를 조작하는 것을 막기 위함.
			*/
			Session_AdminDTO sad = (Session_AdminDTO) session.getAttribute("sessionAdminDTO");
			if(sad == null) {
				return "redirect:/admin/login";
			}

			adminDto.setAdmin_no(sad.getAdmin_no());

			int n = adminService.updateAdminProfile(adminDto); // TODO (name/email/mobile 등)
			model.addAttribute("result", n);

			return "admin/hq/account/profileEditResult";
			// src/main/resources/templates/admin/hq/account/profileEditResult.html
		}


		
	
	
	



}