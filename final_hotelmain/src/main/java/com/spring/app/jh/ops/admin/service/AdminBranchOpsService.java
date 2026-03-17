package com.spring.app.jh.ops.admin.service;

import com.spring.app.jh.security.domain.AdminDTO;

public interface AdminBranchOpsService {

	AdminDTO getMyAdminDetail(int adminNo);

	int updateMyAdminProfile(AdminDTO adminDto);

}