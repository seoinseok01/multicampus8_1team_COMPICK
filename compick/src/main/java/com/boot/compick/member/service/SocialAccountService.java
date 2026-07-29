package com.boot.compick.member.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.compick.member.entity.Member;
import com.boot.compick.member.entity.MemberStatus;
import com.boot.compick.member.entity.SocialAccount;
import com.boot.compick.member.entity.SocialProvider;
import com.boot.compick.member.repository.MemberRepository;
import com.boot.compick.member.repository.SocialAccountRepository;

@Service
public class SocialAccountService {

	private final SocialAccountRepository socialAccountRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	public SocialAccountService(
		SocialAccountRepository socialAccountRepository,
		MemberRepository memberRepository,
		PasswordEncoder passwordEncoder
	) {
		this.socialAccountRepository = socialAccountRepository;
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public GoogleLoginResult loginGoogle(
		String providerUserId,
		String rawEmail,
		String name
	) {
		String email = rawEmail.trim().toLowerCase(Locale.ROOT);

		return socialAccountRepository
			.findByProviderAndProviderUserId(
				SocialProvider.GOOGLE,
				providerUserId
			)
			.map(account -> {
				Member member = account.getMember();
				ensureActive(member);
				account.updateEmail(email);
				return new GoogleLoginResult(
					member,
					!account.isSetupCompleted()
				);
			})
			.orElseGet(() -> createOrLink(providerUserId, email, name));
	}

	@Transactional
	public void setSocialPassword(String currentLoginId, String rawPassword) {
		Member member = memberRepository.findByLoginId(currentLoginId)
			.orElseThrow(() ->
				new IllegalArgumentException("회원 계정을 찾을 수 없습니다.")
			);
		SocialAccount account = socialAccountRepository
			.findByMemberIdAndProvider(member.getId(), SocialProvider.GOOGLE)
			.orElseThrow(() ->
				new IllegalArgumentException("Google 연결 정보를 찾을 수 없습니다.")
			);

		member.changePassword(passwordEncoder.encode(rawPassword));
		account.completeSetup();
	}

	private GoogleLoginResult createOrLink(
		String providerUserId,
		String email,
		String name
	) {
		Member member = memberRepository.findByEmail(email).orElse(null);
		boolean credentialSetupRequired = member == null;

		if (member == null) {
			String displayName = normalizedName(name);
			member = memberRepository.save(new Member(
				uniqueLoginId(providerUserId),
				passwordEncoder.encode(UUID.randomUUID().toString()),
				displayName,
				email,
				normalizedNickname(displayName),
				"미등록"
			));
		}

		ensureActive(member);
		socialAccountRepository.save(new SocialAccount(
			member,
			SocialProvider.GOOGLE,
			providerUserId,
			email,
			!credentialSetupRequired
		));
		return new GoogleLoginResult(member, credentialSetupRequired);
	}

	private void ensureActive(Member member) {
		if (member.getStatus() != MemberStatus.ACTIVE) {
			throw new IllegalArgumentException("사용할 수 없는 회원 계정입니다.");
		}
	}

	private String uniqueLoginId(String providerUserId) {
		String sanitized = providerUserId.replaceAll("[^A-Za-z0-9]", "");
		String base = "google_" + sanitized;
		if (base.length() > 50) {
			base = base.substring(0, 50);
		}

		String candidate = base;
		while (memberRepository.existsByLoginId(candidate)) {
			String suffix = UUID.randomUUID().toString().substring(0, 6);
			candidate =
				base.substring(0, Math.min(base.length(), 43)) + "_" + suffix;
		}
		return candidate;
	}

	private String normalizedName(String name) {
		if (name == null || name.isBlank()) {
			return "Google 사용자";
		}
		String trimmed = name.trim();
		return trimmed.length() > 50 ? trimmed.substring(0, 50) : trimmed;
	}

	private String normalizedNickname(String name) {
		return name.length() > 20 ? name.substring(0, 20) : name;
	}

	public record GoogleLoginResult(
		Member member,
		boolean credentialSetupRequired
	) {
	}
}
