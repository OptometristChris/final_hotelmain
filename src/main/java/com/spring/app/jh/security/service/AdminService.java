package com.spring.app.jh.security.service;

import java.util.List;
import java.util.Map;

import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.MemberDTO;

public interface AdminService {

	// ===== AdminUserDetailsService 에서 사용하는 메서드들 ==== //
	// !!!! 로그인 처리를 위해 사용되어지는 것임. !!!! //
	AdminDTO findByAdminid(String username);
	// ===== AdminUserDetailsService 에서 사용하는 메서드들 ==== //

	
	// ===== AdminController 에서 사용하는 메서드들 ==== //
	// 관리자 상세(내정보/상세보기)
	AdminDTO getAdminDetail(int admin_no);

	// 관리자 권한 목록
	List<String> getAdminAuthorities(int admin_no);

	// 내 프로필 수정(HQ/BRANCH 공통)
	int updateAdminProfile(AdminDTO adminDto);
	// ===== AdminController 에서 사용하는 메서드들 ==== //


}
