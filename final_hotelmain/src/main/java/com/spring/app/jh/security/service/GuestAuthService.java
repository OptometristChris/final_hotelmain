package com.spring.app.jh.security.service;

import com.spring.app.jh.security.domain.MemberDTO;

/* ===== (GUEST-LOGIN) ===== */
public interface GuestAuthService {

	// 비회원 로그인(이름+전화번호) 시 DB에 GUEST 계정을 생성/재사용 후 MemberDTO 반환
	MemberDTO loginOrCreate(String name, String phone) throws Exception;

	// lookup_key 로 기존 계정 조회(MEMBER/GUEST 공용) - 컨트롤러 분기용
	MemberDTO findByLookupKey(String lookupKey);
}