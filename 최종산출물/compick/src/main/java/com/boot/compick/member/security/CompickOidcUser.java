package com.boot.compick.member.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

import com.boot.compick.member.entity.MemberRole;

public class CompickOidcUser implements OidcUser {
    private final OidcUser delegate;
    private final String loginId;
    private final boolean credentialSetupRequired;
    private final Set<GrantedAuthority> authorities;

    public CompickOidcUser(OidcUser delegate, String loginId, MemberRole role, boolean credentialSetupRequired) {
        this.delegate = delegate;
        this.loginId = loginId;
        this.credentialSetupRequired = credentialSetupRequired;
		this.authorities = new LinkedHashSet<>(delegate.getAuthorities());
		this.authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
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
		return Set.copyOf(authorities);
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
