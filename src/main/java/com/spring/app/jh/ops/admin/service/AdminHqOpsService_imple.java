package com.spring.app.jh.ops.admin.service;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.app.common.AES256;
import com.spring.app.jh.ops.admin.model.AdminHqOpsDAO;
import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.MemberDTO;

import lombok.RequiredArgsConstructor;

/* ===== OPS (ADMIN-HQ) ===== */
@Service
@RequiredArgsConstructor
public class AdminHqOpsService_imple implements AdminHqOpsService {

	private final AdminHqOpsDAO adminHqOpsDao;
	private final AES256 aES256;
	private final PasswordEncoder passwordEncoder;

	@Override
	public AdminDTO getAdminDetail(int adminNo) {

		AdminDTO adminDto = adminHqOpsDao.findByAdminNo(adminNo);

		if (adminDto != null) {
			applyDecrypt(adminDto);
			applyAuthorities(adminDto);
		}

		return adminDto;
	}

	@Override
	public List<String> getAdminAuthorities(int adminNo) {
		return adminHqOpsDao.authorityListByAdminNo(adminNo);
	}

	@Override
	public int updateAdminProfile(AdminDTO adminDto) {

		// 내정보 수정: DB 암호문 저장 전제 -> update 전에 encrypt
		applyEncrypt(adminDto);

		return adminHqOpsDao.updateAdminProfile(adminDto);
	}

	@Override
	public List<AdminDTO> getBranchAdminList(Map<String, String> paraMap) {

		List<AdminDTO> list = adminHqOpsDao.getBranchAdminList(paraMap);

		// 목록에서도 email/mobile 필요하면 복호화
		if (list != null) {
			for (AdminDTO dto : list) {
				applyDecrypt(dto);
			}
		}

		return list;
	}

	@Override
	public int getBranchAdminTotalCount(Map<String, String> paraMap) {
		return adminHqOpsDao.getBranchAdminTotalCount(paraMap);
	}

	@Transactional
	@Override
	public int insertBranchAdmin(AdminDTO adminDto) throws Exception {

		// 1) 규칙 강제
		adminDto.setAdmin_type("BRANCH");

		// 2) 비밀번호 해시화
		String hashed = passwordEncoder.encode(adminDto.getPasswd());
		adminDto.setPasswd(hashed);

		// 3) (DB 암호문 저장 전제) email/mobile 암호화
		adminDto.setEmail(aES256.encrypt(adminDto.getEmail()));
		if (adminDto.getMobile() != null && adminDto.getMobile().trim().length() > 0) {
			adminDto.setMobile(aES256.encrypt(adminDto.getMobile()));
		}

		// 4) 관리자 insert (selectKey로 admin_no 채워져야 함)
		int n1 = adminHqOpsDao.insertBranchAdmin(adminDto);

		if (n1 == 1) {
			Integer adminNo = adminDto.getAdmin_no();

			// 5) 기본 권한 부여(ROLE_ADMIN_BRANCH)
			int n2 = adminHqOpsDao.insertBranchAdminAuthority(adminNo, "ROLE_ADMIN_BRANCH");

			if (n2 != 1) {
				throw new RuntimeException("관리자 권한(ROLE_ADMIN_BRANCH) 부여 실패. adminNo=" + adminNo);
			}

			return 1;
		}

		return 0;
	}

	@Override
	public int updateAdminByHq(AdminDTO adminDto) {

		applyEncrypt(adminDto);

		return adminHqOpsDao.updateAdminByHq(adminDto);
	}

	@Override
	public int updateAdminEnabled(Map<String, Object> paraMap) {
		return adminHqOpsDao.updateAdminEnabled(paraMap);
	}

	@Override
	public List<MemberDTO> getAllMember() {
		return adminHqOpsDao.getAllMember();
	}


	// =========================================================
	// private 공통 로직 (암/복호화 + 권한 세팅)
	// =========================================================

	private void applyDecrypt(AdminDTO adminDto) {
		try {
			if (adminDto.getEmail() != null) {
				adminDto.setEmail(aES256.decrypt(adminDto.getEmail()));
			}
			if (adminDto.getMobile() != null) {
				adminDto.setMobile(aES256.decrypt(adminDto.getMobile()));
			}
		} catch (UnsupportedEncodingException | GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	private void applyEncrypt(AdminDTO adminDto) {
		try {
			if (adminDto.getEmail() != null && !adminDto.getEmail().isEmpty()) {
				adminDto.setEmail(aES256.encrypt(adminDto.getEmail()));
			}
			if (adminDto.getMobile() != null && !adminDto.getMobile().isEmpty()) {
				adminDto.setMobile(aES256.encrypt(adminDto.getMobile()));
			}
		} catch (UnsupportedEncodingException | GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	private void applyAuthorities(AdminDTO adminDto) {
		if (adminDto.getAdmin_no() == null) return;

		List<String> authorityList = adminHqOpsDao.authorityListByAdminNo(adminDto.getAdmin_no());

		if (authorityList == null) {
			authorityList = new ArrayList<>();
		}

		adminDto.setAuthorities(authorityList);
	}

}