package com.spring.app.jh.ops.admin.model;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.MemberDTO;

@Mapper
public interface AdminHqOpsDAO {

	// 권한 리스트
	List<String> authorityListByAdminNo(@Param("admin_no") Integer admin_no);

	// 관리자 PK로 상세 조회
	AdminDTO findByAdminNo(@Param("admin_no") int admin_no);

	// 내 프로필 수정
	int updateAdminProfile(AdminDTO adminDto);

	// BRANCH 관리자 목록/카운트
	List<AdminDTO> getBranchAdminList(Map<String, String> paraMap);

	int getBranchAdminTotalCount(Map<String, String> paraMap);

	// BRANCH 발급 + 권한부여
	int insertBranchAdmin(AdminDTO adminDto);

	int insertBranchAdminAuthority(@Param("admin_no") Integer adminNo,
								   @Param("string") String string);

	// HQ가 BRANCH 수정/활성화
	int updateAdminByHq(AdminDTO adminDto);

	int updateAdminEnabled(Map<String, Object> paraMap);

	// 회원 전체 조회
	List<MemberDTO> getAllMember();

}