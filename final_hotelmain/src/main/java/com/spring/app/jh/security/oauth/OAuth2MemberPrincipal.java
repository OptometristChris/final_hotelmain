package com.spring.app.jh.security.oauth;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.spring.app.jh.security.domain.MemberDTO;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OAuth2MemberPrincipal implements OAuth2User {

	private final MemberDTO memberDto;
	private final Map<String, Object> attributes;
	private final String nameAttributeKey;

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return memberDto.getAuthorities()
				.stream()
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());
	}

	@Override
	public String getName() {
		Object value = attributes.get(nameAttributeKey);
		return value == null ? String.valueOf(memberDto.getMemberNo()) : String.valueOf(value);
	}
}
