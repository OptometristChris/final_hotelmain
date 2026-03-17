package com.spring.app.jh.security.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.spring.app.jh.security.domain.AdminDTO;
import com.spring.app.jh.security.domain.CustomAdminDetails;
import com.spring.app.jh.security.model.AdminDAO;

import lombok.RequiredArgsConstructor;

/* ===== (#스프링시큐리티13) ===== */

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

	/*
		UserDetailsService 는 Spring Security 에서 사용자 인증을 담당하는 인터페이스 이다.
	    그래서 Spring Security 는 기본적으로 UserDetailsService 를 구현한 클래스(여기서는 AdminUserDetailsService)를 만들어서 사용자 인증을 처리해준다.

	    이 클래스의 역할은 DB에서 username(아이디 또는 이메일 등)으로 사용자를 조회하여
	    사용자가 있으면 "username(아이디 또는 이메일 등) + 비밀번호 + 권한" 을 담은 UserDetails 객체를 만들어서 반환하는 역할을 한다.
	    사용자 없으면 UsernameNotFoundException 발생시킨다.

	!!! UserDetailsService 는 오로지 loadUserByUsername(String username) 이라는 메소드 1개만 가지고 있다. !!!

      ※ loadUserByUsername(String username) 메소드 ※
        -> 데이터베이스 또는 다른 저장소에서 사용자 정보를 로드하는 역할을 한다.
        -> 리턴타입은 UserDetails 객체를 반환하며, 존재하지 않는 사용자라면 UsernameNotFoundException을 발생시킨다.
        -> 리턴타입인 UserDetails 객체는 DB에서 읽어온 인증된 사용자(로그인 성공한 사용자)의 정보인 아이디, 비밀번호, 권한(Role), 계정 상태 등을 담는 객체이다.

      ★ 현재 프로젝트에서는 "관리자" 인증 전용 서비스로 사용한다.
        (회원 인증은 MemberUserDetailsService 에서 별도로 처리)
	*/

	private final AdminService adminService;

	// !!!! 관리자 로그인 처리 메서드 임 !!!! //
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		// 우리는 UserDetails 인터페이스를 구현한 com.spring.app.security.domain.CustomAdminDetails 클래스를 만들어 두었다.

		// username = 로그인 폼에서 입력한 아이디(관리자 adminid) 를 의미한다고 보면 된다.
		AdminDTO adminDto = adminService.findByAdminid(username);
		// !!! 중요함 adminService.findByUsername(username); 을 잘 봐야 함 !!!
		// -> 여기서 DB 조회 시, 비밀번호/enabled 뿐 아니라 권한(ROLE_*)도 함께 채워져 있어야 한다.
		// -> 또한 admin_type(HQ/BRANCH), BRANCH 인 경우 fk_hotel_id 같은 운영정보도 adminDto에 들어있으면 이후 인가/페이지 제한에 활용 가능.

		System.out.println("~~ [ADMIN] 확인용 adminDto : " + adminDto);

		if (adminDto == null) {
			throw new UsernameNotFoundException("해당하는 관리자 " + username + " 을(를) 찾을 수 없습니다.");
		}

		// !!! 중요 !!! //
		// CustomAdminDetails 에서 getUsername()/getPassword()/getAuthorities() 등을 통해
		// 스프링 시큐리티가 로그인 검증 및 권한 체크를 수행한다.
		return new CustomAdminDetails(adminDto);
	}

}