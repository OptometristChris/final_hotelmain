package com.spring.app.jh.ops.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.app.jh.ops.admin.model.AdminBranchOpsDAO;
import com.spring.app.jh.security.domain.AdminDTO;

import lombok.RequiredArgsConstructor;

/* ===== OPS (ADMIN-BRANCH) ===== */
@Service
@RequiredArgsConstructor
public class AdminBranchOpsService_imple implements AdminBranchOpsService {

	private final AdminBranchOpsDAO adminBranchOpsDao;

	@Override
	public AdminDTO getMyAdminDetail(int adminNo) {
		return adminBranchOpsDao.selectAdminDetail(adminNo);
	}

	@Override
	@Transactional
	public int updateMyAdminProfile(AdminDTO adminDto) {
		return adminBranchOpsDao.updateAdminProfile(adminDto);
	}

}