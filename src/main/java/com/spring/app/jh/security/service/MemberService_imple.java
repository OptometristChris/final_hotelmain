package com.spring.app.jh.security.service;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder; // ★ 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spring.app.common.AES256;
import com.spring.app.common.EmailService;
import com.spring.app.common.Sha256;
import com.spring.app.common.util.LookupKeyUtil;
import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.model.MemberDAO;
import com.spring.app.js.promotion.domain.PromotionDTO;

import lombok.RequiredArgsConstructor;

/* ===== (#스프링시큐리티09) ===== */
@Service
@RequiredArgsConstructor
public class MemberService_imple implements MemberService {
	
	private final MemberDAO memberDao;
	private final AES256 aES256;
	private final EmailService emailService;
	private final PasswordEncoder passwordEncoder;  

	@Override
	public int member_id_check(String memberid) {
		int n = memberDao.member_id_check(memberid);
		return n;
	}

	@Override
	public int emailDuplicateCheck(String email) {
		int n = 0;
		// 암호화된 이메일 넣기
		try {
			n = memberDao.emailDuplicateCheck(aES256.encrypt(email));
		}  catch (UnsupportedEncodingException | GeneralSecurityException e) {
			e.printStackTrace();
		}
		
		return n;
	}

	

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int insert_member(MemberDTO memberdto) throws Exception {

	    // ★ 비밀번호 해시화(단방향 암호화)는 Service에서 처리한다.
	    String hashedUserPwd = passwordEncoder.encode(memberdto.getPasswd());
	    memberdto.setPasswd(hashedUserPwd);

	    // ================================
	    // (추가) lookup_key 세팅 (중요)
	    // - MEMBER/GUEST 중복 방지(lookup_key UNIQUE)
	    // - 비회원 로그인 시 "회원 존재" 자동 판별/유도
	    // ================================
	    String nName  = LookupKeyUtil.normalizeName(memberdto.getName());

	    // 휴대폰 평문(정규화) 확보: hp1/hp2/hp3 기준(암호화 전에!)
	    String plainPhone = "";

	    if(memberdto.getHp1() != null && memberdto.getHp1().trim().length() > 0 &&
	       memberdto.getHp2() != null && memberdto.getHp2().trim().length() > 0 &&
	       memberdto.getHp3() != null && memberdto.getHp3().trim().length() > 0 ) {

	        plainPhone = memberdto.getHp1() + memberdto.getHp2() + memberdto.getHp3();
	    }

	    String nPhone = LookupKeyUtil.normalizePhone(plainPhone);

	    // lookup_key는 phone|name 기반 해시
	    String lookupKey = LookupKeyUtil.buildLookupKey(nName, nPhone);
	    memberdto.setLookupKey(lookupKey);
	    memberdto.setMemberType("MEMBER"); // (선택) DTO에 세팅해두면 추후 재사용 편함

	    // ================================
	    // 양방향 암호화 필요 : email, 휴대폰번호
	    // ================================

	    // email
	    memberdto.setEmail(aES256.encrypt(memberdto.getEmail()));

	    // 휴대폰번호 (AES 암호문 저장)
	    if(nPhone != null && nPhone.trim().length() > 0) {
	        memberdto.setMobile(aES256.encrypt(nPhone));
	    }

	    int result = 0;

	    // 2개의 테이블에 insert 진행 필요
	    // 회원정보 저장하기(insert)
	    int n1 = memberDao.insert_member(memberdto); // selectKey로 memberNo 채워짐

	    if (n1 == 1) {
	        Integer memberNo = memberdto.getMemberNo();
	        result = memberDao.insert_member_authority_by_member_no(memberNo);

	        if (result != 1) {
	            throw new RuntimeException("회원 권한(ROLE_USER) 부여에 실패했습니다. memberNo=" + memberNo);
	        }
	    }

	    return result;
	}

	@Transactional
	@Override
	public int passwdChange(Map<String, String> paraMap) {
		
		// ★ 비밀번호 해시화(단방향 암호화)는 Service에서 처리한다.
		String hashedUserPwd = passwordEncoder.encode(paraMap.get("passwd"));
		paraMap.put("passwd", hashedUserPwd);
		
	    int n = memberDao.passwdChange(paraMap);

	    if(n == 1) {
	        memberDao.passwdModifyDate(paraMap.get("memberid"));
	    }
	    return n;
	}
	
	// id 찾기
	@Override
    public String findMemberId(Map<String, String> paraMap) {
        // paraMap: name/email/mobile 중 뭐를 쓰든 DAO/mapper에서 처리
		
		try {
			String mobile = aES256.encrypt(paraMap.get("mobile"));
			
			paraMap.put("mobile", mobile);
		} catch (UnsupportedEncodingException | GeneralSecurityException e) {
			e.printStackTrace();
		}
		
        return memberDao.findMemberId(paraMap);
    }
	
	// 비밀번호 찾기 시 본인 검증하기
	@Override
    public boolean verifyMemberForPwReset(Map<String, String> paraMap) {
		try {
			String mobile = aES256.encrypt(paraMap.get("mobile"));
			
			paraMap.put("mobile", mobile);
		} catch (UnsupportedEncodingException | GeneralSecurityException e) {
			e.printStackTrace();
		}
        int n = memberDao.verifyMemberForPwReset(paraMap);
        return n == 1;
    }
	
	// 이메일 발송 없는 버전, 바로 화면에 띄움
	// 비밀번호 찾기 시 임시비밀번호 발급과 DB 업데이트(성공 시 임시 비번 리턴, 실패 시 null 리턴)
	@Override
    public String issueTempPasswordAndUpdate(Map<String, String> paraMap) {

        // 1) 먼저 검증(안전장치)
        int n = memberDao.verifyMemberForPwReset(paraMap);
        if (n != 1) {
            return null;
        }

        // 2) 임시비밀번호 생성(원문)
        String tempPw = generateTempPassword(10);

        // 3) 해시로 변환(BCrypt)
        String hashedPw = passwordEncoder.encode(tempPw);

        // 4) 업데이트 파라미터 구성
        // paraMap에 memberid가 들어있다고 가정(폼에서 memberid 필수)
        // passwd 키는 mapper에서 #{passwd}로 받게 맞추는게 직관적
        paraMap.put("passwd", hashedPw);

        // 5) DB 업데이트
        int updated = memberDao.updatePasswordForTemp(paraMap);

        return (updated == 1) ? tempPw : null;
    }
	
	
	// 비밀번호 찾기 시 임시비밀번호 이메일 발송 및 DB 업데이트(성공 시 임시 비번 리턴, 실패 시 null 리턴)
	@Override
    public boolean issueTempPasswordAndSendEmail(Map<String, String> paraMap) {

        // 1) 임시비번 발급 + DB 업데이트 (재사용)
        String tempPw = issueTempPasswordAndUpdate(paraMap);
        if (tempPw == null) {
            return false;
        }

        // 2) DB에 저장된 이메일로 발송(폼에서 입력받은 email을 그대로 쓰지 않는게 안전)
        String memberid = paraMap.get("memberid");
        String emailCipher = memberDao.findEmailByMemberid(memberid);
        String toEmail;
        
		try {
			toEmail = aES256.decrypt(emailCipher);
			System.out.println(toEmail);
		} catch (UnsupportedEncodingException | GeneralSecurityException e) {
			e.printStackTrace();
            return false;
		}

        if (toEmail == null || toEmail.trim().isEmpty()) {
            return false;
        }

        try {
            emailService.sendTempPassword(toEmail, memberid, tempPw);
            return true;
        }
        catch (Exception e) {
            // 운영에서는 로그만 남기고 사용자에게 상세오류 노출 금지
            e.printStackTrace();
            return false;
        }
    }
	
	
	
	
	

    // ------------------------------------------------------------
    // 임시 비밀번호 생성기
    // - 운영에서는 정책에 맞게(특수문자 포함/길이) 조정 가능
    // ------------------------------------------------------------
    private String generateTempPassword(int length) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
	
	


	@Override
	public int lastPasswdChangeMonth(String memberid) {
		int n = memberDao.lastPasswdChangeMonth(memberid);
		return n;
	}

	@Override
	public void update_last_login(String memberid) {
		memberDao.update_last_login(memberid);
	}

	@Override
	public void updateLastLoginByMemberNo(Integer memberNo) {
		memberDao.update_last_login_by_member_no(memberNo);
	}

	@Override
	public MemberDTO findByMemberid(String memberid) {

		MemberDTO memberDto = memberDao.findByMemberid(memberid);

		if (memberDto != null) {
			applyDecrypt(memberDto);
			applyAuthorities(memberDto);
		}

		return memberDto;
	}

	@Override
	public MemberDTO findByMemberNo(Integer memberNo) {

		MemberDTO memberDto = memberDao.findByMemberNo(memberNo);

		if (memberDto != null) {
			applyDecrypt(memberDto);
			applyAuthorities(memberDto);
		}

		return memberDto;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public MemberDTO findOrCreateSocialMember(String socialProvider, String providerUserId, String email, String name) {

		MemberDTO memberDto = memberDao.findBySocialProvider(socialProvider, providerUserId);
		if (memberDto != null) {
			applyDecrypt(memberDto);
			applyAuthorities(memberDto);
			memberDto.setSocialJoinJustCreated(false); // 기존 회원
			return memberDto;
		}

		MemberDTO newMember = new MemberDTO();
		newMember.setName(name != null && !name.trim().isEmpty() ? name : socialProvider + "회원");
		newMember.setEnabled("1");
		newMember.setMemberType("MEMBER");
		newMember.setSocialProvider(socialProvider);
		newMember.setProviderUserId(providerUserId);
		newMember.setGrade_code("CLASSIC");

		String syntheticId = socialProvider + "_" + Sha256.encrypt(providerUserId).substring(0, 20);
		newMember.setMemberid(syntheticId);
		newMember.setPasswd(passwordEncoder.encode(Sha256.encrypt(socialProvider + "|" + providerUserId + "|SOCIAL")));

		if (email != null && !email.trim().isEmpty()) {
			try {
				newMember.setEmail(aES256.encrypt(email.trim()));
			}
			catch (UnsupportedEncodingException | GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
		}

		memberDao.insert_social_member(newMember);
		memberDao.insert_member_authority_by_member_no(newMember.getMemberNo());

		MemberDTO savedMember = memberDao.findByMemberNo(newMember.getMemberNo());
		applyDecrypt(savedMember);
		applyAuthorities(savedMember);
		savedMember.setSocialJoinJustCreated(true); // 신규 가입 회원

		return savedMember;
	}

	@Override
	public void insertLoginhistory(Integer memberNo, String clientip) {
		memberDao.insertLoginhistory(memberNo, clientip);
	}

	@Override
	public int update_member_profile(MemberDTO memberdto, boolean canEditEmail) {

	    if (canEditEmail) {
	        String email = memberdto.getEmail();

	        if (email != null && !email.trim().isEmpty()) {
	            try {
	                memberdto.setEmail(aES256.encrypt(email.trim()));
	            }
	            catch (UnsupportedEncodingException | GeneralSecurityException e) {
	                throw new RuntimeException(e);
	            }
	        }
	        else {
	            memberdto.setEmail(null);
	        }
	    }
	    else {
	        memberdto.setEmail(null);
	    }

	    int n = memberDao.update_member_profile(memberdto, canEditEmail);
	    return n;
	}
	
	@Transactional
	@Override
	public int disable_member(Integer memberNo) {
	    return memberDao.disable_member(memberNo);
	}
	
	@Override
    public List<PromotionDTO> selectMypagePromoList() {
        return memberDao.selectMypagePromoList();
    }
	
	
	// =========================================================
	// private 공통 로직
	// =========================================================

	// === 이메일/휴대폰 복호화 ===
	// DB에는 암호문 저장을 전제로 하므로, 화면에 보여주거나 세션에 담기 전에 복호화한다.
	private void applyDecrypt(MemberDTO memberDto) {
		try {
			if (memberDto.getEmail() != null) {
				memberDto.setEmail(aES256.decrypt(memberDto.getEmail()));
			}
			if (memberDto.getMobile() != null) {
				memberDto.setMobile(aES256.decrypt(memberDto.getMobile()));
			}
		} catch (UnsupportedEncodingException | GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	// === 권한(ROLE_*) 조회 및 세팅 ===
	// 우리 프로젝트는 권한 테이블이 memberid 기반이 아니라 member_no 기반이다.
	private void applyAuthorities(MemberDTO memberDto) {
		if (memberDto.getMemberNo() == null) return;

		List<String> authorityList = memberDao.authorityListByMemberNo(memberDto.getMemberNo());
		memberDto.setAuthorities(authorityList);
	}

	
	
}