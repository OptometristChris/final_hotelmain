package com.spring.app.jh.security.service;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.spring.app.common.AES256;
import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.model.AdminDAO;

import lombok.RequiredArgsConstructor;

/* ===== (#스프링시큐리티09-ADMIN) ===== */
@Service
@RequiredArgsConstructor
public class AdminService_imple implements AdminService {

	private final AdminDAO adminDao;
	private final AES256 aES256;

	/*
	   =========================================================
	   1) 인증/인가(로그인) 용도
	   =========================================================
	*/
	@Override
	public AdminDTO findByAdminid(String adminid) {

		AdminDTO adminDto = adminDao.findByAdminid(adminid);

		if (adminDto != null) {
			applyDecrypt(adminDto);
			applyAuthorities(adminDto); // ROLE_* 채워서 UserDetails로 넘겨야 함(필수)
		}

		return adminDto;
	}

	/*
	   =========================================================
	   2) 관리자 "내 계정" 관리 용도
	      - (내 정보 조회/수정)
	   =========================================================
	*/
	@Override
	public AdminDTO getAdminDetail(int admin_no) {

		AdminDTO adminDto = adminDao.findByAdminNo(admin_no);

		if (adminDto != null) {
			applyDecrypt(adminDto);
			applyAuthorities(adminDto);
		}

		return adminDto;
	}

	@Override
	public List<String> getAdminAuthorities(int admin_no) {
		return adminDao.authorityListByAdminNo(admin_no);
	}

	@Override
	public int updateAdminProfile(AdminDTO adminDto) {
		// DB에 email/mobile 암호문 저장 전제 -> update 전에 encrypt
		applyEncrypt(adminDto);

		return adminDao.updateAdminProfile(adminDto);
	}


	// =========================================================
	// private 공통 로직
	// =========================================================

	// === 이메일/휴대폰 복호화 ===
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

	// === 이메일/휴대폰 암호화 ===
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

	// === 권한(ROLE_*) 조회 및 세팅 ===
	private void applyAuthorities(AdminDTO adminDto) {
		if (adminDto.getAdmin_no() == null) return;

		List<String> authorityList = adminDao.authorityListByAdminNo(adminDto.getAdmin_no());

		if (authorityList == null) {
			authorityList = new ArrayList<>();
		}

		adminDto.setAuthorities(authorityList);
	}

}