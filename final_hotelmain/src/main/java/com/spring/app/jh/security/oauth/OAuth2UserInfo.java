package com.spring.app.jh.security.oauth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OAuth2UserInfo {
	private String socialProvider;
	private String providerUserId;
	private String email;
	private String name;
}
