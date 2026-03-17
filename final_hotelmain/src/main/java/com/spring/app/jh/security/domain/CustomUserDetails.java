package com.spring.app.jh.security.domain;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

/* ===== (#스프링시큐리티14) ===== */

@Getter
public class CustomUserDetails implements UserDetails {
	// Spring Security 에서 인증된 사용자의 정보는 UserDetails 인터페이스를 구현하여 관리한다.
	// UserDetails 인터페이스를 구현한 클래스는 로그인한 사용자의 정보 및 사용자의 권한(Role), 인증 토큰(JWT) 등을 포함할 수 있는 객체가 되어진다.

	private static final long serialVersionUID = 1L;
	private MemberDTO memberDto;

	public CustomUserDetails(MemberDTO memberDto) {
		this.memberDto = memberDto;
	}


	// 권한종류
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {

		// 로그인 한 사용자의 권한목록을 가져오는 것이다.
		// 권한목록은 DB의 tbl_member_authorities 테이블에서 가져온다.
		// (DAO에서 memberDto 안에 authorities(List<String>) 가 채워져 있어야 한다.)

		return memberDto.getAuthorities()
				        .stream()
				        .map(role -> new SimpleGrantedAuthority(role))
				        .collect(Collectors.toList());
	}


	// 아이디
	@Override
	public String getUsername() {
		// 로그인한 사용자의 아이디를 리턴해주는 것이다.
		// (너의 DTO에서는 memberid 이다.)
		return memberDto.getMemberid();
	}


	// 비밀번호
	@Override
	public String getPassword() {
		// 로그인한 사용자의 비밀번호를 리턴해주는 것이다.
		// (너의 DTO에서는 passwd 이다.)
		return memberDto.getPasswd();
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
		return "1".equals(memberDto.getEnabled());
	}

}