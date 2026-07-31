package com.boot.compick.member.security;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class CompickOidcUser implements OidcUser {

	private final OidcUser delegate;
	private final String loginId;
	private final boolean credentialSetupRequired;

	public CompickOidcUser(
		OidcUser delegate,
		String loginId,
		boolean credentialSetupRequired
	) {
		this.delegate = delegate;
		this.loginId = loginId;
		this.credentialSetupRequired = credentialSetupRequired;
	}

	public boolean isCredentialSetupRequired() {
		return credentialSetupRequired;
	}

	@Override
	public Map<String, Object> getClaims() {
		return delegate.getClaims();
	}

	@Override
	public OidcUserInfo getUserInfo() {
		return delegate.getUserInfo();
	}

	@Override
	public OidcIdToken getIdToken() {
		return delegate.getIdToken();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return delegate.getAuthorities();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return delegate.getAttributes();
	}

	@Override
	public String getName() {
		return loginId;
	}
}
