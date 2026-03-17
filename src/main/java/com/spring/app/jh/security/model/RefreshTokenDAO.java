package com.spring.app.jh.security.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spring.app.jh.security.domain.RefreshTokenDTO;

/* ===== (#JWT-DB-02) ===== */
/*
    refresh token 저장/조회/수정/삭제를 담당하는 DAO 인터페이스이다.

    [현재 프로젝트 방식]
    - jh.security 패키지는 @Mapper 인터페이스 + xml 매퍼(namespace=DAO 인터페이스 FQCN) 방식으로 간다.
    - 따라서 별도의 RefreshTokenDAO_imple 는 만들지 않는다.

    [현재 운용 정책]
    - principalType + principalNo 조합당 refresh token 1개
    - 로그인 성공 시
      → 기존 토큰이 있으면 update, 없으면 insert
    - refresh 시
      → DB 저장 refresh token 과 요청 refresh token 을 비교
    - logout 시
      → revoke 또는 delete 처리
 */
@Mapper
public interface RefreshTokenDAO {

    // =====================================================================
    // 1) refresh token 신규 저장
    // =====================================================================
    int insertRefreshToken(RefreshTokenDTO dto);


    // =====================================================================
    // 2) 사용자 기준 refresh token 조회
    // =====================================================================
    /*
        principalType + principalNo 조합으로 1건 조회한다.
        예)
        - MEMBER + 15
        - ADMIN  + 3
        - GUEST  + 101
     */
    RefreshTokenDTO selectRefreshTokenByPrincipal(@Param("principalType") String principalType,
                                                  @Param("principalNo") Long principalNo);


    // =====================================================================
    // 3) token 문자열 기준 refresh token 조회
    // =====================================================================
    /*
        refresh 요청 시, 전달받은 refresh token 원문으로 직접 조회할 때 사용 가능하다.
     */
    RefreshTokenDTO selectRefreshTokenByTokenValue(@Param("tokenValue") String tokenValue);


    // =====================================================================
    // 4) refresh token 갱신
    // =====================================================================
    /*
        기존 사용자 refresh token 이 있을 경우
        tokenValue, expiresAt, revokedYn, updatedAt 등을 갱신한다.
     */
    int updateRefreshToken(RefreshTokenDTO dto);


    // =====================================================================
    // 5) refresh token revoke 처리
    // =====================================================================
    /*
        실제 삭제 대신 revoked_yn='Y' 로 무효화할 때 사용한다.
     */
    int revokeRefreshToken(@Param("principalType") String principalType,
                           @Param("principalNo") Long principalNo);


    // =====================================================================
    // 6) refresh token 실제 삭제
    // =====================================================================
    int deleteRefreshToken(@Param("principalType") String principalType,
                           @Param("principalNo") Long principalNo);
}