package com.spring.app.hk.reservation.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spring.app.common.AES256;
import com.spring.app.hk.reservation.service.ReservationService;
import com.spring.app.jh.security.domain.CustomUserDetails;
import com.spring.app.jh.security.domain.Session_GuestDTO;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

	private final ReservationService reservationService;
	private final AES256 aes256; // 추가

	// 예약 정보 입력 페이지 (객실 정보 조회, 숙박일 계산, 총 객실 기본요금 계산)
	@GetMapping("/form")
	public String reservationForm(@RequestParam("room_type_id") int room_type_id,
			@RequestParam("check_in") String check_in,
			@RequestParam("check_out") String check_out,
			@RequestParam("room_price") int room_price,
			@RequestParam(value = "currency", required = false) String currency,
			@RequestParam(value = "tax", required = false) Boolean tax,
			Authentication auth,
			HttpSession session,
			Model model) {

		// 객실 기본 정보 조회 (DAO → ROOM + HOTEL JOIN)
		Map<String, Object> roomInfo = reservationService.getRoomInfo(room_type_id);

		// ===============================
		// 로그인 사용자 정보 가져오기
		// 회원 로그인 / 비회원 로그인 모두 처리
		// ===============================
		String name = null;
		String mobile = null;
		String email = null;
		Integer memberNo = null;

		// 1️⃣ 회원 로그인 (Spring Security)
		if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {

			CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

			name = userDetails.getMemberDto().getName();
			mobile = userDetails.getMemberDto().getMobile();
			email = userDetails.getMemberDto().getEmail();
			memberNo = userDetails.getMemberDto().getMemberNo();
		}
		
		// ⭐ 소셜 로그인 추가 (네이버 / 카카오)
		else if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauthUser) {

		    System.out.println("oauth 전체 = " + oauthUser.getAttributes());

		    String emailFromOauth = null;

		    // 네이버
		    Map<String, Object> response = (Map<String, Object>) oauthUser.getAttributes().get("response");
		    if (response != null) {
		        emailFromOauth = (String) response.get("email");
		    }

		    // 카카오
		    if (emailFromOauth == null) {
		        Map<String, Object> kakaoAccount = (Map<String, Object>) oauthUser.getAttributes().get("kakao_account");

		        if (kakaoAccount != null) {
		            emailFromOauth = (String) kakaoAccount.get("email");
		        }
		    }

		    if (emailFromOauth != null) {

		        emailFromOauth = emailFromOauth.trim();
		        
		        try {
		            emailFromOauth = aes256.encrypt(emailFromOauth); // 추가
		        } catch (Exception e) {
		            e.printStackTrace();
		        }
		        Map<String, Object> member = reservationService.findMemberByEmail(emailFromOauth);

		        if (member != null) {
		            name = (String) member.get("NAME");
		            mobile = (String) member.get("MOBILE");
		            email = (String) member.get("EMAIL");
		            memberNo = ((Number) member.get("MEMBER_NO")).intValue();
		        }
		    }
		}

		// 2️⃣ 비회원 로그인 (세션 guestSession)
		if (name == null) {

			Session_GuestDTO guest = (Session_GuestDTO) session.getAttribute("guestSession");

			if (guest != null) {
				name = guest.getGuestName();
				mobile = guest.getGuestPhone();
				memberNo = guest.getMemberNo();
			}
		}
		
		// 추가
		if(memberNo == null){

		    model.addAttribute("message","로그인 또는 비회원 로그인이 필요합니다.");
		    model.addAttribute("loc","/security/login");

		    return "msg";
		}

		// 숙박일 계산
		LocalDate inDate = LocalDate.parse(check_in.trim());
		LocalDate outDate = LocalDate.parse(check_out.trim());
		long nights = ChronoUnit.DAYS.between(inDate, outDate);

		// 기본 객실 요금 계산 (1박 요금 * 숙박일)
		int basePrice = ((Number) roomInfo.get("BASE_PRICE")).intValue();
		int maxCapacity = ((Number) roomInfo.get("MAX_CAPACITY")).intValue();
		int totalRoomPrice = basePrice * (int) nights;

		// ===============================
		// 모델 데이터 전달
		// ===============================
		model.addAttribute("memberName", name);
		model.addAttribute("memberMobile", mobile);
		model.addAttribute("memberEmail", email);
		model.addAttribute("memberNo", memberNo);

		model.addAttribute("room_type_id", room_type_id);
		model.addAttribute("check_in", check_in);
		model.addAttribute("check_out", check_out);
		model.addAttribute("nights", nights);

		model.addAttribute("hotel_name", roomInfo.get("HOTEL_NAME"));
		model.addAttribute("room_name", roomInfo.get("ROOM_NAME"));
		model.addAttribute("max_capacity", maxCapacity);
		model.addAttribute("base_price", totalRoomPrice);

		model.addAttribute("room_price", room_price); // 추가 (가격 합)
		model.addAttribute("currency", currency); // 추가 (환율, 세금)
		model.addAttribute("tax", tax);

		return "hk/reservation/form";
	}

	// 예약 저장용
	@PostMapping("/save")
	public String saveReservation(@RequestParam Map<String, String> map,
								  HttpSession session,
								  Model model) {

		// 1. 넘어온 결제 정보 확인 (디버깅용)
		System.out.println("결제 성공 UID: " + map.get("payment_imp_uid"));
		System.out.println("최종 결제 금액: " + map.get("applied_price"));
		System.out.println("프로모션 ID: " + map.get("promotion_id"));

		// 2. 서비스 단에서 예약 정보 저장
		String reservationCode = reservationService.saveReservation(map, session);

		// 3. 성공 시 완료 페이지로 이동
		return "redirect:/reservation/complete?code=" + reservationCode;
	}

	// 예약 완료 페이지
	@GetMapping("/complete")
	public String reservationComplete(@RequestParam("code") String code, Model model) {

		System.out.println("넘어온 code = " + code);

		Map<String, Object> reservation = reservationService.getReservationByCode(code);

		System.out.println("조회 결과 = " + reservation);

		model.addAttribute("reservation", reservation);

		return "hk/reservation/complete";
	}

	// =======================================================
	// 마이페이지 : 로그인 회원의 예약 목록 조회
	// =======================================================
	@GetMapping("/mypage")
	public String myReservationList(Authentication auth, Model model) {

	    Integer memberNo = null;

	    // 1️⃣ 일반 로그인
	    if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
	        memberNo = userDetails.getMemberDto().getMemberNo();
	    }

	    // 2️⃣ 소셜 로그인
	    else if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauthUser) {

	        String email = null;

	        Map<String, Object> response = (Map<String, Object>) oauthUser.getAttributes().get("response");
	        if (response != null) {
	            email = (String) response.get("email");
	        }

	        if (email == null) {
	            Map<String, Object> kakao = (Map<String, Object>) oauthUser.getAttributes().get("kakao_account");
	            if (kakao != null) {
	                email = (String) kakao.get("email");
	            }
	        }

	        if (email != null) {
	            try {
	                email = aes256.encrypt(email.trim());
	            } catch (Exception e) {
	                e.printStackTrace();
	            }

	            Map<String, Object> member = reservationService.findMemberByEmail(email);

	            if (member != null) {
	                memberNo = ((Number) member.get("MEMBER_NO")).intValue();
	            }
	        }
	    }

	    // ❗ 로그인 안된 경우
	    if (memberNo == null) {
	        return "redirect:/security/login";
	    }

	    List<Map<String, Object>> reservationList =
	            reservationService.selectMyReservationList(memberNo);

	    model.addAttribute("reservationList", reservationList);

	    return "hk/reservation/reservationList";
	}

	// ======================================
	// 예약 취소
	// ======================================
	@PostMapping("/cancel")
	@ResponseBody
	public String cancelReservation(@RequestParam("reservation_id") Long reservationId){

	    int n = reservationService.cancelReservation(reservationId);

	    if(n == 1){
	        return "success";
	    }

	    return "deadline";
	}
	
	
	// ======================================
	// 비회원 예약 조회 페이지
	// ======================================
	@GetMapping("/guest")
	public String guestReservationSearchPage(){
	    return "hk/reservation/guestSearch";
	}


	// ======================================
	// 비회원 예약 조회 처리
	// ======================================
	@PostMapping("/guest")
	public String guestReservationSearch(
	        @RequestParam("name") String name,
	        @RequestParam("phone") String phone,
	        Model model){

	    List<Map<String,Object>> reservationList =
	            reservationService.findGuestReservation(name, phone);

	    model.addAttribute("reservationList", reservationList);

	    return "hk/reservation/guestReservationList";
	}
	
	// ======================================
	// 비회원 예약 취소
	// ======================================
	@PostMapping("/guestCancel")
	@ResponseBody
	public String cancelGuestReservation(@RequestParam("reservation_code") String reservationCode){

		  System.out.println("받은 예약코드 = " + reservationCode);
		
	    int n = reservationService.cancelGuestReservation(reservationCode);

	    System.out.println("update 결과 = " + n);
	    
	    if(n == 1){
	        return "success";
	    }

	    return "deadline";
	}
	
}