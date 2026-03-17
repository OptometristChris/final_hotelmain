package com.spring.app.jh.ops.admin.model;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.spring.app.jh.security.domain.AdminDTO;

@Mapper
public interface AdminBranchOpsDAO {

	AdminDTO selectAdminDetail(@Param("adminNo") int adminNo);

	int updateAdminProfile(AdminDTO adminDto);

}