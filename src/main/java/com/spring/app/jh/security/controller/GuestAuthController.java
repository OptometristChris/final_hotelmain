package com.spring.app.jh.security.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.spring.app.common.util.LookupKeyUtil;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.domain.Session_GuestDTO;
import com.spring.app.jh.security.service.GuestAuthService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/* ===== (GUEST-LOGIN) ===== */
@Controller
@RequiredArgsConstructor
public class GuestAuthController {

	private final GuestAuthService guestAuthService;

	

	@PostMapping("/guest/loginEnd")
    public String loginEnd(@RequestParam("name") String name,
                           @RequestParam("phone") String phone,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) throws Exception {

        String lookupKey = LookupKeyUtil.buildLookupKey(name, phone);

        // 1) 이미 MEMBER가 존재하면: 아이디찾기로 유도
        MemberDTO existing = guestAuthService.findByLookupKey(lookupKey);
        if (existing != null && "MEMBER".equals(existing.getMemberType())) {
            redirectAttributes.addFlashAttribute("msg",
                "입력하신 정보로 가입된 회원이 존재합니다. 아이디 찾기 화면으로 이동합니다.");
            return "redirect:/security/member_id_find";
        }

        // 2) 그 외: GUEST 로그인(or 생성)
        MemberDTO account = guestAuthService.loginOrCreate(name, phone);

        Session_GuestDTO guestSession = new Session_GuestDTO();
        guestSession.setMemberNo(account.getMemberNo());
        guestSession.setGuestName(name);
        guestSession.setGuestPhone(phone);
        guestSession.setLookupKey(account.getLookupKey());

        session.setAttribute("guestSession", guestSession);
        session.removeAttribute("sessionMemberDTO");

        return "redirect:/index";
    }

	// 게스트 로그아웃
    @GetMapping("/guest/logout")
    public String guestLogout(HttpSession session) {
        session.removeAttribute("guestSession");
        return "redirect:/index";
    }
}