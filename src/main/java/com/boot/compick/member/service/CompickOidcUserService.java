package com.boot.compick.member.service;

import com.boot.compick.member.security.CompickOidcUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompickOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final SocialAccountService socialAccountService;
    private final OidcUserService delegate = new OidcUserService();

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws AuthenticationException {
        OidcUser googleUser = delegate.loadUser(request);
        Boolean emailVerified = googleUser.getEmailVerified();
        if (!Boolean.TRUE.equals(emailVerified) || googleUser.getEmail() == null)
            throw new OAuth2AuthenticationException("Google에서 인증된 이메일을 확인할 수 없습니다.");
        SocialAccountService.GoogleLoginResult result = socialAccountService.loginGoogleWithResult(
                googleUser.getSubject(), googleUser.getEmail(), googleUser.getFullName());
        return new CompickOidcUser(googleUser, result.member().getLoginId(), result.credentialSetupRequired());
    }
}
