package com.spring.app.jh.security.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* ===== (#JWT-AUTH-05) ===== */
/*
    토큰 재발급 요청 DTO 이다.

    [언제 사용하는가?]
    - access token 이 만료되었을 때
      /api/auth/member/refresh
      /api/auth/admin/refresh
      /api/auth/guest/refresh
      같은 재발급 API 에서 사용한다.

    [필드 의미]
    - accessToken  : 만료되었거나 만료 직전의 access token
    - refreshToken : DB 저장값과 비교할 refresh token
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequestDTO {

    private String accessToken;   // 기존 access token
    private String refreshToken;  // 재발급 검증용 refresh token
}