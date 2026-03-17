package com.spring.app.jh.ops.user.controller;

import java.util.Enumeration;

import org.springframework.security.core.context.SecurityContext;
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
                          Model model) {
    	

        Integer memberNo = getSessionMemberNo(session);
        if (memberNo == null) return "redirect:/security/login";

        ShuttleReservePageDTO page = shuttleService.getReservePage(reservationId, memberNo);
        model.addAttribute("page", page);

        return "shuttle/reserve";
    }
    
    

    
    @PostMapping("/confirm")
    public String confirm(@RequestParam long reservationId,
                          @RequestParam(required = false) java.util.List<Long> toTimetableIds,
                          @RequestParam(required = false) java.util.List<Integer> toQtys,
                          @RequestParam(required = false) java.util.List<Long> fromTimetableIds,
                          @RequestParam(required = false) java.util.List<Integer> fromQtys,
                          HttpSession session) {

        Integer memberNo = getSessionMemberNo(session);
        if (memberNo == null) return "redirect:/security/login";

        shuttleService.confirm(reservationId, memberNo,
                toTimetableIds, toQtys,
                fromTimetableIds, fromQtys);

        return "redirect:/shuttle/confirm/view";
    }
    
    @GetMapping("/confirm/view")
    public String confirmView(HttpSession session, Model model) {

        Integer memberNo = getSessionMemberNo(session);
        if (memberNo == null) return "redirect:/security/login";

        ShuttleConfirmPageDTO page = shuttleService.getConfirmPage(memberNo);

        model.addAttribute("memberName", page.getMemberName());
        model.addAttribute("memberNo", memberNo);
        model.addAttribute("validCount", page.getValidCount());
        model.addAttribute("cards", page.getCards());

        return "shuttle/confirm_view";
    }

    private Integer getSessionMemberNo(HttpSession session) {

        if (session == null) return null;

        Object obj = session.getAttribute("sessionMemberDTO");
        if (obj instanceof Session_MemberDTO dto) {
            return dto.getMemberNo();
        }

        SecurityContext context =
                (SecurityContext)
                        session.getAttribute("SPRING_SECURITY_CONTEXT");

        if (context != null && context.getAuthentication() != null) {
            Object principal = context.getAuthentication().getPrincipal();

            if (principal instanceof JwtPrincipalDTO jwtPrincipal) {
                if ("MEMBER".equals(jwtPrincipal.getPrincipalType())) {
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