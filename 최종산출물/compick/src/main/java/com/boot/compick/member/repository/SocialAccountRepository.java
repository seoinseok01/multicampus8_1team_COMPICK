package com.boot.compick.member.repository;

import com.boot.compick.member.entity.SocialAccount;
import com.boot.compick.member.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
	@EntityGraph(attributePaths = "member")
    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
    Optional<SocialAccount> findByMemberIdAndProvider(Long memberId, SocialProvider provider);
    long deleteByMemberId(Long memberId);
}
