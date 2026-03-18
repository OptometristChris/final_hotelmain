package com.spring.app.jh.ops.user.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.spring.app.jh.ops.user.domain.ShuttleConfirmPageDTO;
import com.spring.app.jh.ops.user.domain.ShuttleReservePageDTO;
import com.spring.app.jh.ops.user.service.ShuttleOpsService;
import com.spring.app.jh.security.auth.domain.JwtPrincipalDTO;
import com.spring.app.jh.security.domain.CustomUserDetails;
import com.spring.app.jh.security.domain.Session_GuestDTO;
import com.spring.app.jh.security.domain.Session_MemberDTO;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/shuttle")
public class ShuttleOpsController {

    private final ShuttleOpsService shuttleService;

    @GetMapping("/reserve")
    public String reserve(@RequestParam("reservationId") long reservationId,
                          HttpSession session,
                          Model model,
                          Authentication authentication) {

        Integer memberNo = resolveMemberNo(session, authentication);
        if (memberNo == null) return "redirect:/security/login";

        ShuttleReservePageDTO page = shuttleService.getReservePage(reservationId, memberNo);
        model.addAttribute("page", page);

        return "shuttle/reserve";
    }


    @PostMapping("/confirm")
    public String confirm(@RequestParam("reservationId") long reservationId,
				  		  @RequestParam(value = "toTimetableIds", required = false) List<Long> toTimetableIds,
				          @RequestParam(value = "toQtys", required = false) List<Integer> toQtys,
				          @RequestParam(value = "fromTimetableIds", required = false) List<Long> fromTimetableIds,
				          @RequestParam(value = "fromQtys", required = false) List<Integer> fromQtys,
                          HttpSession session,
                          Authentication authentication) {

        Integer memberNo = resolveMemberNo(session, authentication);
        if (memberNo == null) return "redirect:/security/login";

        shuttleService.confirm(reservationId, memberNo,
                toTimetableIds, toQtys,
                fromTimetableIds, fromQtys);

        return "redirect:/shuttle/confirm/view";
    }

    @GetMapping("/confirm/view")
    public String confirmView(HttpSession session, Model model, Authentication authentication) {

        Integer memberNo = resolveMemberNo(session, authentication);
        if (memberNo == null) return "redirect:/security/login";

        ShuttleConfirmPageDTO page = shuttleService.getConfirmPage(memberNo);

        model.addAttribute("memberName", page.getMemberName());
        model.addAttribute("memberNo", memberNo);
        model.addAttribute("validCount", page.getValidCount());
        model.addAttribute("cards", page.getCards());

        return "shuttle/confirm_view";
    }

    /*
        [중요]
        - 1순위: sessionMemberDTO
        - 2순위: 현재 Authentication principal
        - 즉, 세션 기반 + JWT/CustomUserDetails fallback 구조로 본다.
     */
    private Integer resolveMemberNo(HttpSession session, Authentication authentication) {

        if (session != null) {
        	// 일반 회원 세션
            Object obj = session.getAttribute("sessionMemberDTO");
            if (obj instanceof Session_MemberDTO dto) {
                return dto.getMemberNo();
            }
            
            // 게스트(비회원) 세션
            Object guestObj = session.getAttribute("guestSession");
            if (guestObj instanceof Session_GuestDTO guestDto) {
                return guestDto.getMemberNo();
            }
        }

        if (authentication != null) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof JwtPrincipalDTO jwtPrincipal) {
                if ("MEMBER".equals(jwtPrincipal.getPrincipalType()) &&
                    jwtPrincipal.getPrincipalNo() != null) {
                    return jwtPrincipal.getPrincipalNo().intValue();
                }
            }

            if (principal instanceof CustomUserDetails cud) {
                return Integer.valueOf(cud.getMemberDto().getMemberNo());
            }
        }

        return null;
    }

}