package com.spring.app.jh.security.domain;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* ===== (#JWT-DB-01) ===== */
/*
    tbl_refreshtoken 테이블 1행과 매핑되는 DTO 이다.

    [현재 프로젝트 기준]
    - principalType : MEMBER / ADMIN / GUEST
    - principalNo   : member_no / admin_no / guest shadow account 의 member_no
    - loginId       : memberid / adminid / guest 식별용 문자열
    - tokenValue    : refresh token 원문
    - expiresAt     : refresh token 만료시각
    - revokedYn     : 서버에서 무효화했는지 여부

    ※ 초기 단계에서는 tokenValue 원문 저장으로 간다.
       이후 보안 강화를 원하면 hash 저장 방식으로 바꿀 수 있다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenDTO {

    private Long refreshTokenId;   // PK
    private String principalType;  // MEMBER / ADMIN / GUEST
    private Long principalNo;      // member_no / admin_no
    private String loginId;        // memberid / adminid / guest 식별값
    private String tokenValue;     // refresh token 원문
    private Date expiresAt;        // refresh token 만료시각
    private String revokedYn;      // Y / N
    private Date createdAt;        // 생성일시
    private Date updatedAt;        // 수정일시
}