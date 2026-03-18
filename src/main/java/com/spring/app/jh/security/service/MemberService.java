package com.spring.app.jh.security.service;

import java.util.List;
import java.util.Map;

import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.js.promotion.domain.PromotionDTO;

public interface MemberService {
	
	// ===== MemberController 에서 사용하는 메서드들 ==== //
	// id 중복 검사
	int member_id_check(String memberid);

	// 이메일 중복 검사
	int emailDuplicateCheck(String email);

	// 회원가입
	int insert_member(MemberDTO memberdto) throws Exception;

	// 비밀번호 변경
	int passwdChange(Map<String, String> paraMap);
	
	// 회원정보 수정 저장
	int update_member_profile(MemberDTO memberdto);
	
	// id 찾기
	String findMemberId(Map<String, String> paraMap);

	// 비밀번호 찾기 시 본인 검증하기
	boolean verifyMemberForPwReset(Map<String, String> paraMap);

	// 비밀번호 찾기 시 임시비밀번호 발급과 DB 업데이트(성공 시 임시 비번 리턴, 실패 시 null 리턴)
	String issueTempPasswordAndUpdate(Map<String, String> paraMap);
	
	// 비밀번호 찾기 시 임시비밀번호 이메일 발송 및 DB 업데이트(성공 시 임시 비번 리턴, 실패 시 null 리턴)
	boolean issueTempPasswordAndSendEmail(Map<String, String> paraMap);
	
	// 회원탈퇴(soft delete)
	int disable_member(Integer memberNo);
	
	// 마이페이지 추천 프로모션 3건 조회
    List<PromotionDTO> selectMypagePromoList();
	// ===== MemberController 에서 사용하는 메서드들 ==== //
	
	
	// ===== MemberUserDetailsService 에서 사용하는 메서드들 ==== //
	// !!!! 로그인 처리를 위해 사용되어지는 것임. !!!! //
	MemberDTO findByMemberid(String memberid);
	MemberDTO findByMemberNo(Integer memberNo);
	MemberDTO findOrCreateSocialMember(String socialProvider, String providerUserId, String email, String name);
	// ===== MemberUserDetailsService 에서 사용하는 메서드들 ==== //
	
	
	// ===== MemberAuthenticationSuccessHandler 에서 사용하는 메서드들 ==== //
	// 가장 최근 비밀번호 변경일자 조회
	int lastPasswdChangeMonth(String memberid);

	// 가장 최근 로그인 한 일자를 지금으로 변경
	void update_last_login(String memberid);
	void updateLastLoginByMemberNo(Integer memberNo);

	// 로그인 기록 테이블에 최근 로그인 기록 저장
	void insertLoginhistory(Integer memberNo, String clientip);
	// ===== MemberAuthenticationSuccessHandler 에서 사용하는 메서드들 ==== //


	
	
	
	
	

	
	

	
	
	
	
}
