package com.spring.app.jh.security.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.spring.app.jh.security.auth.domain.AdminLoginRequestDTO;
import com.spring.app.jh.security.auth.domain.MemberLoginRequestDTO;
import com.spring.app.jh.security.auth.domain.TokenRequestDTO;
import com.spring.app.jh.security.jwt.JwtToken;

/* ===== (#JWT-SERVICE-01) ===== */
/*
    JWT 로그인/재발급/로그아웃을 담당하는 Service 인터페이스이다.

    [현재 프로젝트 방향]
    - 회원 / 관리자 / 게스트 모두 JWT 발급 대상이다.
    - 다만 비게시판 영역은 JWT 로그인 후 세션도 같이 유지하는 하이브리드 구조로 간다.
    - 따라서 로그인 성공 시
      (1) JWT 발급
      (2) refresh token DB 저장
      (3) 세션(SecurityContext) 저장
      을 함께 처리할 수 있어야 한다.
 */
public interface JwtAuthService {

    // =====================================================================
    // 1) 회원 로그인
    // =====================================================================
    JwtToken loginMember(MemberLoginRequestDTO loginDto,
                         HttpServletRequest request,
                         HttpServletResponse response);


    // =====================================================================
    // 2) 관리자 로그인
    // =====================================================================
    JwtToken loginAdmin(AdminLoginRequestDTO loginDto,
                        HttpServletRequest request,
                        HttpServletResponse response);




    // =====================================================================
    // 4) 토큰 재발급
    // =====================================================================
    JwtToken refresh(TokenRequestDTO tokenRequestDto);


    // =====================================================================
    // 5) 로그아웃
    // =====================================================================
    void logout(String principalType,
                Long principalNo,
                HttpServletRequest request,
                HttpServletResponse response);
}