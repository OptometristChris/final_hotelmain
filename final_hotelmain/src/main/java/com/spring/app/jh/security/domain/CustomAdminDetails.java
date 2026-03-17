package com.spring.app.jh.security.domain;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

/* ===== (#스프링시큐리티14) ===== */

@Getter
public class CustomAdminDetails implements UserDetails {
	// Spring Security 에서 인증된 사용자의 정보는 UserDetails 인터페이스를 구현하여 관리한다.
    // UserDetails 인터페이스를 구현한 클래스는 로그인한 사용자의 정보 및 사용자의 권한(Role), 인증 토큰(JWT) 등을 포함할 수 있는 객체가 되어진다.
    //
    // ※ 관리자도 회원과 동일하게 UserDetails 로 관리한다.
    //    단, DB가 tbl_admin_security / tbl_admin_authorities 로 분리되어 있을 뿐이다.

	private static final long serialVersionUID = 1L;
	private AdminDTO adminDto;

	public CustomAdminDetails(AdminDTO adminDto) {
		this.adminDto = adminDto;
	}


	// 권한종류
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {

		// 로그인 한 관리자의 권한목록을 가져오는 것이다.
		// 권한목록은 DB의 tbl_admin_authorities 테이블에서 가져온다.
		// (DAO에서 adminDto 안에 authorities(List<String>) 가 채워져 있어야 한다.)

		return adminDto.getAuthorities()
				       .stream()
				       .map(role -> new SimpleGrantedAuthority(role))
				       .collect(Collectors.toList());
	}


	// 아이디
	@Override
	public String getUsername() {
		// 로그인한 관리자의 아이디를 리턴해주는 것이다.
		// (너의 DTO에서는 adminid 이다.)
		return adminDto.getAdminid();
	}


	// 비밀번호
	@Override
	public String getPassword() {
		// 로그인한 관리자의 비밀번호를 리턴해주는 것이다.
		// (너의 DTO에서는 passwd 이다.)
		return adminDto.getPasswd();
	}


	// 계정이 만료 되었는지 여부 (true: 만료안됨)
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}


	// 계정이 잠겼는지 여부 (true: 잠기지 않음)
	@Override
	public boolean isAccountNonLocked() {
		return true;
	}


	// 비밀번호가 만료 되었는지 여부 (true: 만료안됨)
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}


	// 계정이 활성화 되었는지 여부 (true: 활성화)
	@Override
	public boolean isEnabled() {
		// enabled 컬럼값이 1 이면 true, 0 이면 false 처럼 사용하면 된다.
		// (너의 DTO에서는 enabled 이고, '1'/'0' 형태이다.)
		return "1".equals(adminDto.getEnabled());
	}

}