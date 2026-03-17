package com.spring.app.jh.ops.admin.controller;

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

import com.spring.app.hk.admin.hotel.service.HotelService;
import com.spring.app.jh.ops.admin.service.AdminHqOpsService;
import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.domain.Session_AdminDTO;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/*
    ===== OPS (ADMIN-HQ) =====
    - "업무" 영역 전용 컨트롤러
    - security 패키지 서비스/DAO를 재사용하지 않는다.
*/
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/hq/")
@PreAuthorize("hasRole('ADMIN_HQ')")
public class AdminHqOpsController {

	private final AdminHqOpsService adminHqOpsService;
	
	private final HotelService hotelService;


	// ============================================================
	// HQ 전용: BRANCH 계정 발급/목록/상세/수정/활성화
	// ============================================================

	@GetMapping("admins")
	public String branchAdminList(@RequestParam Map<String, String> paraMap, Model model) {

		// pageNo 기본값
		String pageNo = paraMap.get("pageNo");
		if (pageNo == null || pageNo.trim().isEmpty()) paraMap.put("pageNo", "1");

		// sizePerPage 기본값
		String sizePerPage = paraMap.get("sizePerPage");
		if (sizePerPage == null || sizePerPage.trim().isEmpty()) paraMap.put("sizePerPage", "10");

		// enabled 기본값
		if (!paraMap.containsKey("enabled")) paraMap.put("enabled", "ALL");

		List<AdminDTO> adminList = adminHqOpsService.getBranchAdminList(paraMap);
		int totalCount = adminHqOpsService.getBranchAdminTotalCount(paraMap);

		model.addAttribute("adminList", adminList);
		model.addAttribute("totalCount", totalCount);
		model.addAttribute("paraMap", paraMap);

		return "admin/hq/admin_list";
	}

	// 수정
	@GetMapping("admins/new")
	public String branchAdminCreateForm(Model model) {

	    List<Map<String,Object>> hotelList =
	            hotelService.getApprovedHotelList();

	    model.addAttribute("hotelList", hotelList);

	    return "admin/hq/admin_create_form";
	}

	
	
	@PostMapping("admins")
	public String branchAdminCreateEnd(AdminDTO adminDto, Model model) {

		try {
			int n = adminHqOpsService.insertBranchAdmin(adminDto);

			if (n == 1) model.addAttribute("message", "지점 관리자 계정 발급이 정상적으로 처리되었습니다.");
			else        model.addAttribute("message", "지점 관리자 계정 발급이 실패했습니다.");

		} catch (Exception e) {
			model.addAttribute("message", e.getMessage() != null ? e.getMessage()
					: "지점 관리자 계정 발급 중 장애가 발생했습니다.");
			e.printStackTrace();
		}

		return "admin/hq/admin_create_result";
	}

	@GetMapping("admins/detail/{admin_no}")
	public String adminDetail(@PathVariable("admin_no") int admin_no, Model model) {

		AdminDTO adminDto = adminHqOpsService.getAdminDetail(admin_no);
		List<String> authorityList = adminHqOpsService.getAdminAuthorities(admin_no);

		model.addAttribute("adminDto", adminDto);
		model.addAttribute("authorityList", authorityList);

		return "admin/hq/admin_detail";
	}

	@PostMapping("admins/update")
	public String adminUpdateEnd(AdminDTO adminDto, Model model) {

		int n = adminHqOpsService.updateAdminByHq(adminDto);
		model.addAttribute("result", n);

		return "admin/hq/admin_update_result";
	}

	@PostMapping("admins/{admin_no}/enabled")
	@ResponseBody
	public Map<String, Object> updateAdminEnabled(@PathVariable("admin_no") int admin_no,
												  @RequestParam("enabled") String enabled) {

		Map<String, Object> paraMap = new HashMap<>();
		paraMap.put("admin_no", admin_no);
		paraMap.put("enabled", enabled);

		int n = adminHqOpsService.updateAdminEnabled(paraMap);

		Map<String, Object> result = new HashMap<>();
		result.put("result", n);
		return result;
	}


	// ============================================================
	// HQ 전용: 회원 전체 조회
	// ============================================================

	@GetMapping("members")
	public String memberAllInfo(Model model) {

		List<MemberDTO> memberDtoList = adminHqOpsService.getAllMember();
		model.addAttribute("memberDtoList", memberDtoList);

		return "admin/hq/memberAllInfo";
	}

}