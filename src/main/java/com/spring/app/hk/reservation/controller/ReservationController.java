package com.spring.app.hk.reservation.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.spring.app.common.AES256;
import com.spring.app.hk.reservation.service.ReservationService;
import com.spring.app.jh.security.auth.domain.JwtPrincipalDTO;
import com.spring.app.jh.security.domain.CustomUserDetails;
import com.spring.app.jh.security.domain.Session_GuestDTO;
import com.spring.app.jh.security.domain.Session_MemberDTO;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final AES256 aes256;

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

        Map<String, Object> roomInfo = reservationService.getRoomInfo(room_type_id);

        Map<String, Object> loginInfo = resolveReservationLoginInfo(auth, session);

        String name = (String) loginInfo.get("name");
        String mobile = (String) loginInfo.get("mobile");
        String email = (String) loginInfo.get("email");
        Integer memberNo = (Integer) loginInfo.get("memberNo");

        if (memberNo == null) {
            model.addAttribute("message", "로그인 또는 비회원 로그인이 필요합니다.");
            model.addAttribute("loc", "/security/login");
            return "msg";
        }

        LocalDate inDate = LocalDate.parse(check_in.trim());
        LocalDate outDate = LocalDate.parse(check_out.trim());
        long nights = ChronoUnit.DAYS.between(inDate, outDate);

        int basePrice = ((Number) roomInfo.get("BASE_PRICE")).intValue();
        int maxCapacity = ((Number) roomInfo.get("MAX_CAPACITY")).intValue();
        int totalRoomPrice = basePrice * (int) nights;

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

        model.addAttribute("room_price", room_price);
        model.addAttribute("currency", currency);
        model.addAttribute("tax", tax);

        return "hk/reservation/form";
    }

    @PostMapping("/save")
    public String saveReservation(@RequestParam Map<String, String> map,
                                  HttpSession session,
                                  Model model) {

        System.out.println("결제 성공 UID: " + map.get("payment_imp_uid"));
        System.out.println("최종 결제 금액: " + map.get("applied_price"));
        System.out.println("프로모션 ID: " + map.get("promotion_id"));

        String reservationCode = reservationService.saveReservation(map, session);

        return "redirect:/reservation/complete?code=" + reservationCode;
    }

    @GetMapping("/complete")
    public String reservationComplete(@RequestParam("code") String code, Model model) {

        System.out.println("넘어온 code = " + code);

        Map<String, Object> reservation = reservationService.getReservationByCode(code);

        System.out.println("조회 결과 = " + reservation);

        if (reservation == null) {
            model.addAttribute("message", "예약 정보를 찾을 수 없습니다.");
            model.addAttribute("loc", "/index");
            return "msg";
        }

        model.addAttribute("reservation", reservation);

        return "hk/reservation/complete";
    }

    @GetMapping("/mypage")
    public String myReservationList(Authentication auth, HttpSession session, Model model) {

        Integer memberNo = resolveMemberNoForMemberOnly(auth, session);

        if (memberNo == null) {
            return "redirect:/security/login";
        }

        List<Map<String, Object>> reservationList = reservationService.selectMyReservationList(memberNo);
        model.addAttribute("reservationList", reservationList);

        return "hk/reservation/reservationList";
    }

    @PostMapping("/cancel")
    @ResponseBody
    public String cancelReservation(@RequestParam("reservation_id") Long reservationId) {

        int n = reservationService.cancelReservation(reservationId);

        if (n == 1) {
            return "success";
        }

        return "deadline";
    }

    @GetMapping("/guest")
    public String guestReservationSearchPage() {
        return "hk/reservation/guestSearch";
    }

    @PostMapping("/guest")
    public String guestReservationSearch(@RequestParam("name") String name,
                                         @RequestParam("phone") String phone,
                                         Model model) {

        List<Map<String, Object>> reservationList = reservationService.findGuestReservation(name, phone);
        model.addAttribute("reservationList", reservationList);

        return "hk/reservation/guestReservationList";
    }

    @PostMapping("/guestCancel")
    @ResponseBody
    public String cancelGuestReservation(@RequestParam("reservation_code") String reservationCode) {

        System.out.println("받은 예약코드 = " + reservationCode);

        int n = reservationService.cancelGuestReservation(reservationCode);

        System.out.println("update 결과 = " + n);

        if (n == 1) {
            return "success";
        }

        return "deadline";
    }

    private Integer resolveMemberNoForMemberOnly(Authentication auth, HttpSession session) {

        Object sessionMemberObj = session.getAttribute("sessionMemberDTO");
        if (sessionMemberObj instanceof Session_MemberDTO sessionMemberDTO
                && sessionMemberDTO.getMemberNo() != null) {
            return sessionMemberDTO.getMemberNo();
        }

        if (auth == null) {
            return null;
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getMemberDto().getMemberNo();
        }

        if (principal instanceof JwtPrincipalDTO jwtPrincipal) {
            if ("MEMBER".equalsIgnoreCase(jwtPrincipal.getPrincipalType())
                    && jwtPrincipal.getPrincipalNo() != null) {
                return jwtPrincipal.getPrincipalNo().intValue();
            }
            return null;
        }

        if (principal instanceof OAuth2User oauthUser) {
            String emailFromOauth = extractOauthEmail(oauthUser);

            if (emailFromOauth != null) {
                try {
                    String encryptedEmail = aes256.encrypt(emailFromOauth.trim());
                    Map<String, Object> member = reservationService.findMemberByEmail(encryptedEmail);

                    if (member != null) {
                        return ((Number) member.get("MEMBER_NO")).intValue();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private Map<String, Object> resolveReservationLoginInfo(Authentication auth, HttpSession session) {

        Map<String, Object> loginInfo = new HashMap<>();

        String name = null;
        String mobile = null;
        String email = null;
        Integer memberNo = null;

        Object sessionMemberObj = session.getAttribute("sessionMemberDTO");
        if (sessionMemberObj instanceof Session_MemberDTO sessionMemberDTO) {
            name = sessionMemberDTO.getName();
            memberNo = sessionMemberDTO.getMemberNo();
        }

        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            name = userDetails.getMemberDto().getName();
            mobile = userDetails.getMemberDto().getMobile();
            email = userDetails.getMemberDto().getEmail();
            memberNo = userDetails.getMemberDto().getMemberNo();
        }
        else if (auth != null && auth.getPrincipal() instanceof JwtPrincipalDTO jwtPrincipal) {
            if ("MEMBER".equalsIgnoreCase(jwtPrincipal.getPrincipalType())
                    && jwtPrincipal.getPrincipalNo() != null) {
                memberNo = jwtPrincipal.getPrincipalNo().intValue();
                if (jwtPrincipal.getName() != null) {
                    name = jwtPrincipal.getName();
                }
            }
        }
        else if (auth != null && auth.getPrincipal() instanceof OAuth2User oauthUser) {

            String emailFromOauth = extractOauthEmail(oauthUser);

            if (emailFromOauth != null) {
                try {
                    String encryptedEmail = aes256.encrypt(emailFromOauth.trim());
                    Map<String, Object> member = reservationService.findMemberByEmail(encryptedEmail);

                    if (member != null) {
                        name = (String) member.get("NAME");
                        mobile = (String) member.get("MOBILE");
                        email = (String) member.get("EMAIL");
                        memberNo = ((Number) member.get("MEMBER_NO")).intValue();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (memberNo == null) {
            Object guestObj = session.getAttribute("guestSession");

            if (guestObj instanceof Session_GuestDTO guest) {
                name = guest.getGuestName();
                mobile = guest.getGuestPhone();
                memberNo = guest.getMemberNo();
            }
        }

        loginInfo.put("name", name);
        loginInfo.put("mobile", mobile);
        loginInfo.put("email", email);
        loginInfo.put("memberNo", memberNo);

        return loginInfo;
    }

    @SuppressWarnings("unchecked")
    private String extractOauthEmail(OAuth2User oauthUser) {

        String emailFromOauth = null;

        Map<String, Object> response = (Map<String, Object>) oauthUser.getAttributes().get("response");
        if (response != null) {
            emailFromOauth = (String) response.get("email");
        }

        if (emailFromOauth == null) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) oauthUser.getAttributes().get("kakao_account");
            if (kakaoAccount != null) {
                emailFromOauth = (String) kakaoAccount.get("email");
            }
        }

        return emailFromOauth;
    }
}