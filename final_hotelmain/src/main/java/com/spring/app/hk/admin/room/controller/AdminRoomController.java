package com.spring.app.hk.admin.room.controller;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.spring.app.common.FileManager;
import com.spring.app.hk.admin.room.service.AdminRoomService;
import com.spring.app.hk.room.domain.RoomTypeDTO;
import com.spring.app.jh.security.domain.CustomAdminDetails;
import com.spring.app.jh.security.domain.Session_AdminDTO;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/room")
public class AdminRoomController {

	private final AdminRoomService roomService;

	// 이미지 업로드 위해 추가
	private final FileManager fileManager;

	@Value("${file.images-dir}")
	private String imagesDir;

	// ==========================
	// 지점 관리자 객실 관리 페이지
	// ==========================

	// 지점 객실 조회
	@PreAuthorize("hasRole('ADMIN_BRANCH')")
	@GetMapping("/branch/list")
	public String roomList(Model model, HttpSession session) {

		// System.out.println("컨트롤러 세션 ID = " + session.getId());
		// System.out.println("sessionAdminDTO = " +
		// session.getAttribute("sessionAdminDTO"));

		// 로그인 사용자 가져오기
		Session_AdminDTO loginAdmin = (Session_AdminDTO) session.getAttribute("sessionAdminDTO");

		if (loginAdmin == null) {
			return "redirect:/admin/login";
		}

		Integer adminNo = loginAdmin.getAdmin_no();

		// 히스토리 조회
		List<Map<String, Object>> historyList = roomService.getBranchApprovalHistoryList(adminNo);

		// 지점 객실 조회
		// 전체 객실 조회
		List<RoomTypeDTO> roomList = roomService.getRoomListByManager(adminNo);

		// 상태별 분리
		List<RoomTypeDTO> approvedList = roomList.stream().filter(r -> "APPROVED".equals(r.getApprove_status()))
				.toList();

		List<RoomTypeDTO> pendingList = roomList.stream().filter(r -> "PENDING".equals(r.getApprove_status())).toList();

		List<RoomTypeDTO> rejectedList = roomList.stream().filter(r -> "REJECTED".equals(r.getApprove_status()))
				.toList();

		List<RoomTypeDTO> inactiveList = roomService.getInactiveRoomListByManager(adminNo);

		model.addAttribute("approvedList", approvedList);
		model.addAttribute("pendingList", pendingList);
		model.addAttribute("rejectedList", rejectedList);
		model.addAttribute("inactiveList", inactiveList);
		model.addAttribute("historyList", historyList);

		if (!roomList.isEmpty()) {
			model.addAttribute("hotelName", roomList.get(0).getHotel_name());
		}

		return "hk/branch/room/roomlist";
	}

	// 운영중 객실 수정
	@PostMapping("/modify")
	@PreAuthorize("hasRole('ADMIN_BRANCH')")
	public String modifyApprovedRoom(@RequestParam Map<String, String> map,
			@RequestParam(value = "roomImage", required = false) MultipartFile roomImage) {

		roomService.modifyApprovedRoom(map, roomImage);

		return "redirect:/admin/room/branch/list";
	}

	// 객실 등록 페이지 진입
	@PreAuthorize("hasRole('ADMIN_BRANCH')")
	@GetMapping("/register")
	public String roomRegisterPage() {

		return "hk/branch/room/roomRegister";
	}

	// 지점 객실 등록 신청 (외부경로. 이미지까지)
	@PreAuthorize("hasRole('ADMIN_BRANCH')")
	@PostMapping("register")
	public String registerRoom(@RequestParam Map<String, String> map,
			@RequestParam("roomImage") MultipartFile roomImage, Authentication authentication) {

		try {

			// 로그인한 관리자 정보 가져오기
			CustomAdminDetails loginAdmin = (CustomAdminDetails) authentication.getPrincipal();

			Integer adminNo = loginAdmin.getAdminDto().getAdmin_no();
			String adminType = loginAdmin.getAdminDto().getAdmin_type();

			// 관리자 번호
			Map<String, Object> paraMap = new HashMap<>(map);

			paraMap.put("admin_no", adminNo);

			// 승인 상태
			if ("HQ".equals(adminType)) {
				paraMap.put("approve_status", "APPROVED");
			} else {
				paraMap.put("approve_status", "PENDING");
			}

			// 이미지 업로드
			if (!roomImage.isEmpty()) {

				// 업로드 위치 : file_images/room/파일명.jpg
				String roomPath = imagesDir + File.separator + "room";

				String savedName = fileManager.doFileUpload(roomImage.getBytes(), roomImage.getOriginalFilename(),
						roomPath);

				paraMap.put("image_url", "/file_images/room/" + savedName);
			}

			roomService.saveRoom(paraMap);

		} catch (Exception e) {
			e.printStackTrace();

		}

		// 등록 후 객실 리스트 이동
		return "redirect:/admin/room/branch/list";

	}

	// 객실 반려 후 수정 (이미지 교체 처리)
	@PreAuthorize("hasRole('ADMIN_BRANCH')")
	@PostMapping("/update")
	public String updateRoom(@RequestParam Map<String, String> map,
			@RequestParam(value = "roomImage", required = false) MultipartFile roomImage) {

		roomService.updateRoom(map, roomImage);

		return "redirect:/admin/room/branch/list";
	}

	// 반려 후 재상신
	@PostMapping("/resubmit")
	@PreAuthorize("hasRole('ADMIN_BRANCH')")
	public String resubmitRoom(@RequestParam("roomTypeId") int roomTypeId) {

		roomService.resubmitRoom(roomTypeId);

		return "redirect:/admin/room/branch/list";
	}

	// 객실 비활성화
	@PostMapping("/deactivate")
	@PreAuthorize("hasRole('ADMIN_BRANCH')")
	public String deactivateRoom(@RequestParam("roomTypeId") int roomTypeId) {

		roomService.deactivateRoom(roomTypeId);

		return "redirect:/admin/room/branch/list";
	}

	// 객실 복구 (활성화)
	@PostMapping("/restore")
	@PreAuthorize("hasRole('ADMIN_BRANCH')")
	public String restoreRoom(@RequestParam("roomTypeId") int roomTypeId) {

		roomService.restoreRoom(roomTypeId);

		return "redirect:/admin/room/branch/list";
	}

	// ==========================
	// 총괄 관리자 객실 관리 페이지 (승인/반려)
	// ==========================
	// 승인요청 (지점관리자) 목록 조회
	@PreAuthorize("hasRole('ADMIN_HQ')")
	@GetMapping("/pending")
	public String roomApprovalPage(Model model) {

		List<RoomTypeDTO> roomList = roomService.getRoomApprovalList();

		List<RoomTypeDTO> pendingList = roomList.stream().filter(r -> "PENDING".equals(r.getApprove_status())).toList();

		List<RoomTypeDTO> approvedList = roomList.stream().filter(r -> "APPROVED".equals(r.getApprove_status()))
				.toList();

		List<RoomTypeDTO> rejectedList = roomList.stream().filter(r -> "REJECTED".equals(r.getApprove_status()))
				.toList();

		// 전체 승인 히스토리 조회용
		List<Map<String, Object>> historyList = roomService.getApprovalHistoryList();

		// 승인대기
		model.addAttribute("pendingList", pendingList);

		// 승인완료
		model.addAttribute("approvedList", approvedList);

		// 반려
		model.addAttribute("rejectedList", rejectedList);

		model.addAttribute("historyList", historyList);

		return "hk/admin/room/pendingRoomList";
	}

	// 객실 승인 처리
	@PreAuthorize("hasRole('ADMIN_HQ')")
	@PostMapping("/approve")
	public String approveRoom(@RequestParam("roomTypeId") int roomTypeId, HttpSession session) {

		// 로그인 사용자 가져오기
		Session_AdminDTO loginAdmin = (Session_AdminDTO) session.getAttribute("sessionAdminDTO");

		int adminId = loginAdmin.getAdmin_no();

		// 객실 승인
		roomService.approveRoom(roomTypeId, adminId);

		return "redirect:/admin/room/pending";
	}

	// 객실 반려 처리
	@PreAuthorize("hasRole('ADMIN_HQ')")
	@PostMapping("/reject")
	public String rejectRoom(@RequestParam("roomTypeId") int roomTypeId, @RequestParam("reason") String reason,
			HttpSession session) {

		// 로그인 사용자 가져오기
		Session_AdminDTO loginAdmin = (Session_AdminDTO) session.getAttribute("sessionAdminDTO");

		int adminId = loginAdmin.getAdmin_no();

		// 객실 반려
		roomService.rejectRoom(roomTypeId, adminId, reason);

		return "redirect:/admin/room/pending";
	}

	// 객실 승인 히스토리 조회
	@PreAuthorize("hasRole('ADMIN_HQ')")
	@GetMapping("/history")
	@ResponseBody
	public List<Map<String, Object>> getHistory(@RequestParam("roomTypeId") int roomTypeId) {

		return roomService.getRoomApprovalHistory(roomTypeId);

	}

}