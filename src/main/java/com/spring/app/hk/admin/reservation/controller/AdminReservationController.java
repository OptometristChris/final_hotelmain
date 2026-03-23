package com.spring.app.hk.admin.reservation.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import com.spring.app.hk.admin.reservation.service.AdminReservationService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reservation")
public class AdminReservationController {

	private final AdminReservationService reservationService;

	// ======= 지점 관리자 ========= //
	/*
	 * // 예약 캘린더 (객실 배정)
	 * 
	 * @PreAuthorize("hasRole('ADMIN_BRANCH')")
	 * 
	 * @GetMapping("/calendar") public String reservationCalendar() {
	 * 
	 * return "hk/admin/reservation/calendar"; }
	 */

	// 예약 관리 페이지
	@PreAuthorize("hasRole('ADMIN_BRANCH')")
	@GetMapping("/manage")
	public String reservationManage(Model model) {

	    List<Map<String, Object>> checkinList = reservationService.getTodayCheckinList();
	    List<Map<String, Object>> checkoutList = reservationService.getTodayCheckoutList();
	    List<Map<String, Object>> stayList = reservationService.getStayList();
	    List<Map<String, Object>> checkoutCompleteList = reservationService.getCheckoutCompleteList();
	    List<Map<String, Object>> overdueList = reservationService.getOverdueList();

	    // ===== KPI =====

	    // 🔥 오늘 체크인 "전체 대상 수" (변하지 않는 값)
	    int todayCheckinTotal = reservationService.getTodayCheckinTotalCount();

	    // 🔥 오늘 체크인 완료 수
	    int todayCheckinDone = reservationService.getTodayCheckinDoneCount();

	    int todayCheckoutCount = checkoutList.size();
	    int stayCount = stayList.size();
	    int overdueCount = overdueList.size();

	    int totalRoomCount = 100;
	    int occupancyRate = 0;

	    if(totalRoomCount > 0) {
	        occupancyRate = (int)(((double) stayCount / totalRoomCount) * 100);
	    }

	    // 🔥 체크인 진행률 (이제 절대 안깨짐)
	    int checkinProgress = 0;

	    if(todayCheckinTotal > 0) {
	        checkinProgress = (int)(((double) todayCheckinDone / todayCheckinTotal) * 100);
	    }

	    // ===== model =====
	    model.addAttribute("checkinList", checkinList);
	    model.addAttribute("checkoutList", checkoutList);
	    model.addAttribute("stayList", stayList);
	    model.addAttribute("checkoutCompleteList", checkoutCompleteList);
	    model.addAttribute("overdueList", overdueList);

	    model.addAttribute("todayCheckinReserved", todayCheckinTotal); // 이름 유지
	    model.addAttribute("todayCheckinDone", todayCheckinDone);
	    model.addAttribute("todayCheckoutCount", todayCheckoutCount);
	    model.addAttribute("stayCount", stayCount);
	    model.addAttribute("occupancyRate", occupancyRate);
	    model.addAttribute("overdueCount", overdueCount);
	    model.addAttribute("checkinProgress", checkinProgress);

	    return "hk/admin/reservation/reservationManage";
	}

	// 체크인 처리
	@PreAuthorize("hasRole('ADMIN_BRANCH')")
	@PostMapping("/checkin")
	public String checkin(@RequestParam("reservationId") int reservationId) {

		reservationService.checkinReservation(reservationId);

		return "redirect:/admin/reservation/manage";
	}

	// 체크아웃 처리
	@PreAuthorize("hasRole('ADMIN_BRANCH')")
	@PostMapping("/checkout")
	public String checkout(@RequestParam("reservationId") int reservationId) {

		reservationService.checkoutReservation(reservationId);

		return "redirect:/admin/reservation/manage";
	}

	// ======= 총괄 관리자 ========= //
	// 전체 객실 예약 리스트 조회 + 검색
	@PreAuthorize("hasRole('ADMIN_HQ')")
	@GetMapping("/list")
	public String adminReservationList(
	        @RequestParam(value="name", required=false) String name,
	        @RequestParam(value="status", required=false) String status,
	        @RequestParam(value="hotelId", required=false) String hotelId,
	        Model model) {

	    Map<String,Object> param = new HashMap<>();
	    param.put("name", name);
	    param.put("status", status);
	    param.put("hotelId", hotelId);

	    List<Map<String,Object>> reservationList =
	            reservationService.selectAdminReservationList(param);

	    // 호텔 목록 추가
	    List<Map<String,Object>> hotelList =
	            reservationService.selectHotelList();

	    model.addAttribute("reservationList", reservationList);
	    model.addAttribute("hotelList", hotelList);

	    model.addAttribute("name", name);
	    model.addAttribute("status", status);
	    model.addAttribute("hotelId", hotelId);

	    return "hk/admin/reservation/adminreservationList";
	}
	
	
	// 엑셀 다운로드
	@PreAuthorize("hasRole('ADMIN_HQ')")
	@GetMapping("/excel")
	public void downloadExcel(
	        @RequestParam(value="name", required=false) String name,
	        @RequestParam(value="status", required=false) String status,
	        HttpServletResponse response) throws Exception {
		
	    Map<String,Object> param = new HashMap<>();
	    param.put("name", name);
	    param.put("status", status);

	    List<Map<String,Object>> reservationList =
	            reservationService.selectAdminReservationList(param);

	    Workbook wb = new XSSFWorkbook();
	    Sheet sheet = wb.createSheet("예약목록");

	    int rowNo = 0;

	    Row header = sheet.createRow(rowNo++);
	    header.createCell(0).setCellValue("예약번호");
	    header.createCell(1).setCellValue("회원");
	    header.createCell(2).setCellValue("호텔");
	    header.createCell(3).setCellValue("객실타입");
	    header.createCell(4).setCellValue("체크인");
	    header.createCell(5).setCellValue("체크아웃");
	    header.createCell(6).setCellValue("가격");
	    header.createCell(7).setCellValue("결제상태");
	    header.createCell(8).setCellValue("예약상태");

	    for(Map<String,Object> r : reservationList){

	        Row row = sheet.createRow(rowNo++);

	        row.createCell(0).setCellValue(String.valueOf(r.get("RESERVATION_ID")));
	        row.createCell(1).setCellValue(String.valueOf(r.get("NAME")));
	        row.createCell(2).setCellValue(String.valueOf(r.get("HOTEL_NAME")));
	        row.createCell(3).setCellValue(String.valueOf(r.get("ROOM_TYPE_ID")));
	        row.createCell(4).setCellValue(String.valueOf(r.get("CHECKIN_DATE")));
	        row.createCell(5).setCellValue(String.valueOf(r.get("CHECKOUT_DATE")));
	        row.createCell(6).setCellValue(String.valueOf(r.get("TOTAL_PRICE")));
	        row.createCell(7).setCellValue(String.valueOf(r.get("PAYMENT_STATUS")));
	        row.createCell(8).setCellValue(String.valueOf(r.get("RESERVATION_STATUS")));
	    }

	    response.setContentType("application/vnd.ms-excel");
	    response.setHeader("Content-Disposition",
	            "attachment;filename=reservation_list.xlsx");

	    wb.write(response.getOutputStream());
	    wb.close();
	}

}