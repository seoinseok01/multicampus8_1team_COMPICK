package com.boot.compick.member.service;

import com.boot.compick.member.entity.Member;
import com.boot.compick.member.entity.MemberStatus;
import com.boot.compick.member.entity.SocialAccount;
import com.boot.compick.member.entity.SocialProvider;
import com.boot.compick.member.repository.MemberRepository;
import com.boot.compick.member.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialAccountService {
    private final SocialAccountRepository socialAccountRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member loginGoogle(String providerUserId, String rawEmail, String name) {
        return loginGoogleWithResult(providerUserId, rawEmail, name).member();
    }

    @Transactional
    public GoogleLoginResult loginGoogleWithResult(String providerUserId, String rawEmail, String name) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        return socialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, providerUserId)
                .map(account -> {
                    if (account.getMember().getStatus() != MemberStatus.ACTIVE)
                        throw new IllegalArgumentException("사용할 수 없는 회원 계정입니다.");
                    account.updateEmail(email);
                    return new GoogleLoginResult(account.getMember(),
                            account.getMember().getLoginId().startsWith("google_"));
                })
                .orElseGet(() -> createOrLink(providerUserId, email, name));
    }

    private GoogleLoginResult createOrLink(String providerUserId, String email, String name) {
        Member member = memberRepository.findByEmail(email).orElse(null);
        boolean credentialSetupRequired = member == null;
        if (member == null) {
            member = memberRepository.save(new Member(
                    uniqueLoginId(providerUserId),
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    name == null || name.isBlank() ? "Google 사용자" : name,
                    email,
                    "미등록"));
        }
        if (member.getStatus() != MemberStatus.ACTIVE)
            throw new IllegalArgumentException("사용할 수 없는 회원 계정입니다.");
        socialAccountRepository.save(new SocialAccount(member, SocialProvider.GOOGLE, providerUserId, email));
        return new GoogleLoginResult(member, credentialSetupRequired);
    }

    @Transactional
    public void setLoginCredentials(String currentLoginId, String loginId, String rawPassword) {
        Member member = memberRepository.findByLoginId(currentLoginId)
                .orElseThrow(() -> new IllegalArgumentException("회원 계정을 찾을 수 없습니다."));
        memberRepository.findByLoginId(loginId)
                .filter(found -> !found.getId().equals(member.getId()))
                .ifPresent(found -> { throw new IllegalArgumentException("이미 사용 중인 아이디입니다."); });
        member.setLoginCredentials(loginId, passwordEncoder.encode(rawPassword));
    }

    private String uniqueLoginId(String providerUserId) {
        String base = "google_" + providerUserId.replaceAll("[^A-Za-z0-9]", "");
        if (base.length() > 50) base = base.substring(0, 50);
        String candidate = base;
        while (memberRepository.existsByLoginId(candidate)) {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            candidate = base.substring(0, Math.min(base.length(), 43)) + "_" + suffix;
        }
        return candidate;
    }

    public record GoogleLoginResult(Member member, boolean credentialSetupRequired) {}
}
