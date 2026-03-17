package com.spring.app.jh.security.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.spring.app.jh.security.auth.domain.AdminLoginRequestDTO;
import com.spring.app.jh.security.auth.domain.JwtPrincipalDTO;
import com.spring.app.jh.security.auth.domain.TokenRequestDTO;
import com.spring.app.jh.security.auth.service.JwtAuthService;
import com.spring.app.jh.security.domain.CustomAdminDetails;
import com.spring.app.jh.security.jwt.JwtToken;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/* ===== (#JWT-CONTROLLER-02) ===== */

@RestController
@RequiredArgsConstructor
public class AdminAuthApiController {

    private final JwtAuthService jwtAuthService;


    // =====================================================================
    // 1) 관리자 로그인
    // =====================================================================
    @PostMapping("/api/auth/admin/login")
    public JwtToken loginAdmin(@RequestBody AdminLoginRequestDTO loginDto,
                               HttpServletRequest request,
                               HttpServletResponse response) {

        return jwtAuthService.loginAdmin(loginDto, request, response);
    }


    // =====================================================================
    // 2) 관리자 토큰 재발급
    // =====================================================================
    @PostMapping("/api/auth/admin/refresh")
    public JwtToken refreshAdmin(@RequestBody TokenRequestDTO tokenRequestDto) {

        return jwtAuthService.refresh(tokenRequestDto);
    }


    // =====================================================================
    // 3) 관리자 로그아웃
    // =====================================================================
    @PostMapping("/api/auth/admin/logout")
    public void logoutAdmin(Authentication authentication,
                            HttpServletRequest request,
                            HttpServletResponse response) {

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("로그인한 관리자 정보가 없습니다.");
        }

        Object principal = authentication.getPrincipal();

        Long adminNo = null;

        // 1. JWT principal 인 경우
        if (principal instanceof JwtPrincipalDTO jwtPrincipal) {
            if (!"ADMIN".equals(jwtPrincipal.getPrincipalType())) {
                throw new RuntimeException("관리자 로그아웃 대상이 아닙니다.");
            }
            adminNo = jwtPrincipal.getPrincipalNo();
        }

        // 2. 기존 Spring Security CustomAdminDetails 인 경우
        else if (principal instanceof CustomAdminDetails adminDetails) {
            adminNo = Long.valueOf(adminDetails.getAdminDto().getAdmin_no());
        }

        else {
            throw new RuntimeException("관리자 principal 타입을 처리할 수 없습니다.");
        }

        jwtAuthService.logout("ADMIN", adminNo, request, response);
    }
}