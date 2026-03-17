package com.spring.app.jh.security.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* ===== (#JWT-AUTH-02) ===== */
/*
    회원 로그인 요청 DTO 이다.

    [현재 로그인 방식]
    - 기존 formLogin() 을 제거하고
      /api/auth/member/login 으로 JSON 요청을 받아 직접 인증 처리할 것이므로
      회원 로그인 폼에서 입력받은 값을 이 DTO 로 받는다.

    [필드 의미]
    - memberid : 회원 아이디
    - passwd   : 회원 비밀번호
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberLoginRequestDTO {

    private String memberid; // 회원 아이디
    private String passwd;   // 회원 비밀번호
}