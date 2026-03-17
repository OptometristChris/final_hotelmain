package com.spring.app.jh.security.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.spring.app.jh.security.auth.domain.JwtPrincipalDTO;
import com.spring.app.jh.security.auth.domain.MemberLoginRequestDTO;
import com.spring.app.jh.security.auth.domain.TokenRequestDTO;
import com.spring.app.jh.security.auth.service.JwtAuthService;
import com.spring.app.jh.security.domain.CustomUserDetails;
import com.spring.app.jh.security.domain.Session_MemberDTO;
import com.spring.app.jh.security.jwt.JwtToken;
import com.spring.app.jh.security.oauth.OAuth2MemberPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/* ===== (#JWT-CONTROLLER-01) ===== */

@RestController
@RequiredArgsConstructor
public class MemberAuthApiController {

    private final JwtAuthService jwtAuthService;


    // =====================================================================
    // 1) 회원 로그인
    // =====================================================================
    @PostMapping("/api/auth/member/login")
    public JwtToken loginMember(@RequestBody MemberLoginRequestDTO loginDto,
                                HttpServletRequest request,
                                HttpServletResponse response) {

        return jwtAuthService.loginMember(loginDto, request, response);
    }


    // =====================================================================
    // 2) 회원 토큰 재발급
    // =====================================================================
    @PostMapping("/api/auth/member/refresh")
    public JwtToken refreshMember(@RequestBody TokenRequestDTO tokenRequestDto) {

        return jwtAuthService.refresh(tokenRequestDto);
    }


 // =====================================================================
 // 3) 회원 로그아웃
 // =====================================================================
 @PostMapping("/api/auth/member/logout")
 public void logoutMember(Authentication authentication,
                          HttpServletRequest request,
                          HttpServletResponse response) {

     Long memberNo = null;

     // =========================================================================
     // 인증객체(principal) 에서 회원번호를 꺼내본다.
     // 일반 로그인 / JWT 로그인 / 소셜 로그인(네이버, 카카오) 을 모두 처리하기 위함이다.
     // =========================================================================
     if (authentication != null && authentication.getPrincipal() != null) {

         Object principal = authentication.getPrincipal();

         // 1. JWT principal 인 경우
         if (principal instanceof JwtPrincipalDTO jwtPrincipal) {

             if ("MEMBER".equals(jwtPrincipal.getPrincipalType())) {
                 memberNo = jwtPrincipal.getPrincipalNo();
             }
         }

         // 2. 기존 Spring Security CustomUserDetails 인 경우
         else if (principal instanceof CustomUserDetails userDetails) {
             memberNo = Long.valueOf(userDetails.getMemberDto().getMemberNo());
         }

         // 3. 소셜 로그인 principal 인 경우(네이버/카카오 공통)
         else if (principal instanceof OAuth2MemberPrincipal oauth2Principal) {
             memberNo = Long.valueOf(oauth2Principal.getMemberDto().getMemberNo());
         }
     }

     // =========================================================================
     // principal 에서 회원번호를 못 찾은 경우에는 세션에서 한번 더 찾는다.
     // JWT + 세션 하이브리드 구조에서 로그아웃 안정성을 높이기 위함이다.
     // =========================================================================
     if (memberNo == null) {

         HttpSession session = request.getSession(false);

         if (session != null) {

             Object sessionObj = session.getAttribute("sessionMemberDTO");

             if (sessionObj instanceof Session_MemberDTO sessionMemberDTO) {
                 memberNo = Long.valueOf(sessionMemberDTO.getMemberNo());
             }
         }
     }

     // =========================================================================
     // memberNo 를 찾았으면 refresh token 정리까지 수행하고,
     // 못 찾았더라도 세션무효화 / 쿠키삭제 / SecurityContext 정리는 진행되도록 한다.
     // logout() 내부에서 memberNo == null 인 경우도 안전하게 처리해주면 가장 좋다.
     // =========================================================================
     jwtAuthService.logout("MEMBER", memberNo, request, response);
 }
}