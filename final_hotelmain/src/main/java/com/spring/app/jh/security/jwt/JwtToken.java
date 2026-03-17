package com.spring.app.jh.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* ===== (#JWT-01) ===== */
/*
    로그인 성공 후 클라이언트에게 내려줄 JWT 정보를 담는 DTO 이다.

    - grantType              : 일반적으로 "Bearer"
    - accessToken            : 실제 인증에 사용하는 토큰
    - refreshToken           : access token 재발급에 사용하는 토큰
    - accessTokenExpireTime  : access token 만료 시각(epoch milli)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtToken {

    private String grantType;            // 예: Bearer
    private String accessToken;          // 인증용 access token
    private String refreshToken;         // 재발급용 refresh token
    private Long accessTokenExpireTime;  // access token 만료시각(epoch milli)
    
    private String principalType;
    private String adminType;
    private Long hotelId;
}