package com.spring.app.jh.ops.admin.service;

import java.util.List;
import java.util.Map;

import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.MemberDTO;

public interface AdminHqOpsService {

	AdminDTO getAdminDetail(int adminNo);

	List<String> getAdminAuthorities(int adminNo);

	int updateAdminProfile(AdminDTO adminDto);

	List<AdminDTO> getBranchAdminList(Map<String, String> paraMap);

	int getBranchAdminTotalCount(Map<String, String> paraMap);

	int insertBranchAdmin(AdminDTO adminDto) throws Exception;

	int updateAdminByHq(AdminDTO adminDto);

	int updateAdminEnabled(Map<String, Object> paraMap);

	List<MemberDTO> getAllMember();

}