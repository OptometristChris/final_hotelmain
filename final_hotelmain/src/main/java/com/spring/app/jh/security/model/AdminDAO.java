package com.spring.app.jh.security.model;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spring.app.jh.security.domain.AdminDTO;

@Mapper
public interface AdminDAO {

	// ===== AdminUserDetailsService(인증) =====
	AdminDTO findByAdminid(@Param("adminid") String adminid);

	// ===== 권한(인가) =====
	List<String> authorityListByAdminNo(@Param("admin_no") Integer admin_no);

	// ===== 관리자 "내 계정" 조회/수정 =====
	AdminDTO findByAdminNo(@Param("admin_no") int admin_no);

	int updateAdminProfile(AdminDTO adminDto);

}