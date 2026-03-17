package com.spring.app.jh.security.oauth;

import java.util.Map;

public class OAuth2UserInfoFactory {

	@SuppressWarnings("unchecked")
	public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {

		if ("kakao".equals(registrationId)) {
			String providerUserId = String.valueOf(attributes.get("id"));
			Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
			Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

			String email = kakaoAccount == null ? null : (String) kakaoAccount.get("email");
			String name = null;
			if (properties != null) {
				name = (String) properties.get("nickname");
			}
			if ((name == null || name.isBlank()) && kakaoAccount != null) {
				Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
				if (profile != null) name = (String) profile.get("nickname");
			}
			return new OAuth2UserInfo("kakao", providerUserId, email, name);
		}

		if ("naver".equals(registrationId)) {
			Map<String, Object> response = (Map<String, Object>) attributes.get("response");
			if (response == null) throw new IllegalArgumentException("네이버 응답(response)이 없습니다.");
			String providerUserId = String.valueOf(response.get("id"));
			String email = (String) response.get("email");
			String name = (String) response.get("name");
			if (name == null || name.isBlank()) name = (String) response.get("nickname");
			return new OAuth2UserInfo("naver", providerUserId, email, name);
		}

		throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다. registrationId=" + registrationId);
	}
}
