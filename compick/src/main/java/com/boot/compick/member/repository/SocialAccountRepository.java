package com.boot.compick.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.member.entity.SocialAccount;
import com.boot.compick.member.entity.SocialProvider;

public interface SocialAccountRepository
	extends JpaRepository<SocialAccount, Long> {

	Optional<SocialAccount> findByProviderAndProviderUserId(
		SocialProvider provider,
		String providerUserId
	);

	Optional<SocialAccount> findByMemberIdAndProvider(
		Long memberId,
		SocialProvider provider
	);

	long deleteByMemberId(Long memberId);
}
