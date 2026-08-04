package com.boot.compick.member.service;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.boot.compick.member.security.CompickOidcUser;

@Service
public class CompickOidcUserService
	implements OAuth2UserService<OidcUserRequest, OidcUser> {

	private final SocialAccountService socialAccountService;
	private final OidcUserService delegate = new OidcUserService();

	public CompickOidcUserService(
		SocialAccountService socialAccountService
	) {
		this.socialAccountService = socialAccountService;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest request)
		throws AuthenticationException {
		OidcUser googleUser = delegate.loadUser(request);

		if (!Boolean.TRUE.equals(googleUser.getEmailVerified())
			|| googleUser.getEmail() == null) {
			throw new OAuth2AuthenticationException(
				"Google에서 인증된 이메일을 확인할 수 없습니다."
			);
		}

		SocialAccountService.GoogleLoginResult result =
			socialAccountService.loginGoogle(
				googleUser.getSubject(),
				googleUser.getEmail(),
				googleUser.getFullName()
			);

		return new CompickOidcUser(
			googleUser,
			result.member().getLoginId(),
			result.credentialSetupRequired()
		);
	}
}
