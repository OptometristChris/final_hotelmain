package com.spring.app.jh.security.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.spring.app.jh.security.domain.MemberDTO;
import com.spring.app.jh.security.oauth.OAuth2MemberPrincipal;
import com.spring.app.jh.security.oauth.OAuth2UserInfo;
import com.spring.app.jh.security.oauth.OAuth2UserInfoFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final MemberService memberService;
	private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

	    OAuth2User oauth2User = delegate.loadUser(userRequest);

	    String registrationId = userRequest.getClientRegistration().getRegistrationId();
	    OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of(registrationId, oauth2User.getAttributes());

	    System.out.println("======================================");
	    System.out.println("[SOCIAL LOGIN DEBUG]");
	    System.out.println("registrationId = " + registrationId);
	    System.out.println("attributes = " + oauth2User.getAttributes());
	    System.out.println("providerUserId = " + userInfo.getProviderUserId());
	    System.out.println("email = " + userInfo.getEmail());
	    System.out.println("name = " + userInfo.getName());
	    System.out.println("======================================");

	    MemberDTO memberDto = memberService.findOrCreateSocialMember(
	            userInfo.getSocialProvider(),
	            userInfo.getProviderUserId(),
	            userInfo.getEmail(),
	            userInfo.getName());

	    String userNameAttributeName = userRequest.getClientRegistration()
	            .getProviderDetails()
	            .getUserInfoEndpoint()
	            .getUserNameAttributeName();

	    return new OAuth2MemberPrincipal(memberDto, oauth2User.getAttributes(), userNameAttributeName);
	}
}
