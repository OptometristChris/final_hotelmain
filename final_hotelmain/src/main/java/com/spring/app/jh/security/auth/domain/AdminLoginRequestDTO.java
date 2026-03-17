package com.spring.app.jh.security.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* ===== (#JWT-AUTH-03) ===== */
/*
    관리자 로그인 요청 DTO 이다.

    [현재 로그인 방식]
    - 기존 /admin/loginEnd formLogin 처리 대신
      /api/auth/admin/login 으로 JSON 요청을 받아 직접 인증 처리할 것이므로
      관리자 로그인 폼 입력값을 이 DTO 로 받는다.

    [필드 의미]
    - adminid : 관리자 아이디
    - passwd  : 관리자 비밀번호
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginRequestDTO {

    private String adminid; // 관리자 아이디
    private String passwd;  // 관리자 비밀번호
}