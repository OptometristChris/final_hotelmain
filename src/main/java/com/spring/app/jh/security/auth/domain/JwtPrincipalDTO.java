package com.spring.app.jh.security.auth.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* ===== (#JWT-AUTH-01) ===== */
/*
    JWT 안의 claim 으로부터 복원한 공통 principal 객체이다.

    [왜 필요한가?]
    - 현재 프로젝트는 로그인 주체가 MEMBER / ADMIN / GUEST 로 나뉜다.
    - 기존 로그인 시점에는
      CustomUserDetails / CustomAdminDetails / Session_GuestDTO 등을 각각 사용할 수 있지만,
      JWT 인증 필터에서는 "토큰 안의 정보"만 보고 공통 방식으로 Authentication 을 복원해야 한다.
    - 따라서 JWT 인증 복원 전용 principal 을 따로 둔다.

    [주의]
    - 이 DTO 는 "JWT 인증 복원용" 이다.
    - 로그인 검증 자체는 여전히 MemberUserDetailsService, AdminUserDetailsService 등을 사용할 수 있다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtPrincipalDTO {

    private String principalType;  // MEMBER / ADMIN / GUEST
    private Long principalNo;      // member_no / admin_no / guest shadow account 의 member_no
    private String loginId;        // memberid / adminid / guest 식별용 문자열
    private String name;           // 화면 표시용 이름
    private List<String> roles;    // ROLE_USER, ROLE_ADMIN_HQ, ROLE_ADMIN_BRANCH, ROLE_GUEST ...
    private String adminType;      // HQ / BRANCH / null
    private Long hotelId;          // 지점관리자일 경우 hotel_id / 아니면 null
}