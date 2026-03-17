package com.spring.app.jh.security.model;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spring.app.jh.security.domain.MemberDTO;

@Mapper
public interface MemberDAO {

	// ===== MemberUserDetailsService 에서 사용하는 메서드 ===== //
	MemberDTO findByMemberid(@Param("memberid") String username);
	MemberDTO findBySocialProvider(@Param("socialProvider") String socialProvider, @Param("providerUserId") String providerUserId);
	MemberDTO findByMemberNo(@Param("memberNo") Integer memberNo);
	// ===== MemberUserDetailsService 에서 사용하는 메서드 ===== //
	

	// ===== MemberController 에서 사용하는 메서드 ===== //
	int member_id_check(@Param("memberid") String memberid);

	int emailDuplicateCheck(@Param("email") String email);

	int insert_member(MemberDTO memberdto);
	int insert_social_member(MemberDTO memberdto);

	int passwdChange(Map<String, String> paraMap);
	
	int passwdModifyDate(@Param("memberid") String memberid);

	int lastPasswdChangeMonth(@Param("memberid") String memberid);

	void update_last_login(@Param("memberid") String memberid);
	void update_last_login_by_member_no(@Param("memberNo") Integer memberNo);

	void insertLoginhistory(@Param("memberNo") Integer memberNo,
            				@Param("clientip") String clientip);
	
	int insert_member_authority_by_member_no(@Param("memberNo") Integer memberNo);
	
	List<String> authorityListByMemberNo(@Param("memberNo") Integer memberNo);
	
	int update_member_profile(MemberDTO memberdto);
	
	String findMemberId(Map<String, String> paraMap);

	int verifyMemberForPwReset(Map<String, String> paraMap);

	int updatePasswordForTemp(Map<String, String> paraMap);
	
	String findEmailByMemberid(String memberid);
	
	int disable_member(@Param("memberNo") Integer memberNo);
	// ===== MemberController 에서 사용하는 메서드 ===== //


	
	// ===== Guest 로그인/생성(lookup_key) ===== //
	MemberDTO findByLookupKey(@Param("lookupKey") String lookupKey);

	int insert_guest(MemberDTO memberdto); 
	// memberdto: name, mobile(암호문), memberType='GUEST', lookupKey

	Integer findMemberNoByLookupKey(@Param("lookupKey") String lookupKey);




	


	

}
