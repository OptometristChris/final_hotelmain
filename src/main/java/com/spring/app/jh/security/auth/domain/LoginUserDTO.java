package com.spring.app.jh.security.auth.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* ===== (#JWT-AUTH-06) ===== */
/*
    로그인 성공 후 "현재 로그인한 사용자 요약 정보"를 내려줄 DTO 이다.

    [왜 필요한가?]
    - 클라이언트가 로그인 성공 직후
      "누가 로그인 되었는지" 화면에 반영할 때 사용할 수 있다.
    - JWT 자체만 내려주면 accessToken / refreshToken 정보만 있고
      화면 표시용 사용자 정보가 부족할 수 있으므로 별도 DTO 를 둔다.

    [필드 의미]
    - principalType : MEMBER / ADMIN / GUEST
    - principalNo   : member_no / admin_no / guest shadow account 의 member_no
    - loginId       : memberid / adminid / guest 식별값
    - name          : 표시 이름
    - roles         : 권한 문자열 목록
    - adminType     : HQ / BRANCH / null
    - hotelId       : 지점관리자일 경우 hotel_id / 아니면 null
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUserDTO {

    private String principalType;  // MEMBER / ADMIN / GUEST
    private Long principalNo;      // member_no / admin_no
    private String loginId;        // memberid / adminid / guest 식별값
    private String name;           // 화면 표시용 이름
    private List<String> roles;    // ROLE_USER, ROLE_ADMIN_HQ ...
    private String adminType;      // HQ / BRANCH / null
    private Long hotelId;          // 지점관리자일 경우 hotel_id / 아니면 null
}