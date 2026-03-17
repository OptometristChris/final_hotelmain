package com.spring.app.jh.security.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.app.common.AES256;
import com.spring.app.common.util.LookupKeyUtil;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.model.MemberDAO;

import lombok.RequiredArgsConstructor;

/* ===== (GUEST-LOGIN) ===== */
@Service
@RequiredArgsConstructor
public class GuestAuthService_imple implements GuestAuthService {

	private final MemberDAO memberDao;
	private final AES256 aes256;

	/*
	   비회원 로그인(이름+전화번호) 성공 시 즉시 DB에 GUEST 계정 생성/재사용

	   [규칙]
	   1) lookup_key = SHA-256(정규화 phone + '|' + 정규화 name)
	   2) lookup_key UNIQUE 이므로 MEMBER/GUEST 중복 생성 불가
	   3) 존재하면(MEMBER든 GUEST든) 그대로 재사용
	   4) 없으면 GUEST row insert (mobile AES 암호문 저장)
	*/
	@Override
	@Transactional
	public MemberDTO loginOrCreate(String name, String phone) throws Exception {

		String nName  = LookupKeyUtil.normalizeName(name);
		String nPhone = LookupKeyUtil.normalizePhone(phone);
		
		if(nPhone == null || nPhone.length() == 0) {
		    throw new RuntimeException("휴대폰번호가 누락되었습니다.");
		}

		// buildLookupKey는 내부에서 normalize를 또 할 수도 있지만,
		// 여기서는 이미 정규화된 값을 넣는 방식으로 통일
		String lookupKey = LookupKeyUtil.buildLookupKey(nName, nPhone);

		// 1) 기존 MEMBER/GUEST 존재하면 재사용
		MemberDTO existing = memberDao.findByLookupKey(lookupKey);
		if (existing != null) {
			return existing;
		}

		// 2) 없으면 GUEST 생성
		MemberDTO guest = new MemberDTO();
		guest.setName(nName);
		guest.setLookupKey(lookupKey);
		guest.setMemberType("GUEST");
		guest.setMobile(aes256.encrypt(nPhone));  // AES 암호문 저장

		try {
			memberDao.insert_guest(guest); // selectKey로 memberNo 채워짐
			return guest;
		}
		catch (DuplicateKeyException e) {
			// 동시에 같은 name+phone으로 로그인 시 UNIQUE(lookup_key) 충돌 가능
			MemberDTO again = memberDao.findByLookupKey(lookupKey);
			if (again != null) return again;
			throw e;
		}
	}

	/*
	   컨트롤러에서 "이미 MEMBER가 존재하는지" 분기하기 위해 사용
	   - lookup_key 로 기존 계정(MEMBER/GUEST)을 조회해 반환한다.
	*/
	@Override
	public MemberDTO findByLookupKey(String lookupKey) {
		return memberDao.findByLookupKey(lookupKey);
	}
}