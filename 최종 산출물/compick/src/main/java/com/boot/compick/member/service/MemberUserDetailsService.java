package com.boot.compick.member.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.boot.compick.member.entity.Member;
import com.boot.compick.member.entity.MemberStatus;
import com.boot.compick.member.repository.MemberRepository;

@Service
public class MemberUserDetailsService implements UserDetailsService {

	private final MemberRepository memberRepository;

	public MemberUserDetailsService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String loginId) {
		Member member = memberRepository.findByLoginId(loginId)
			.orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다."));

		return User.withUsername(member.getLoginId())
			.password(member.getPasswordHash())
			.roles(member.getRole().name())
			.disabled(member.getStatus() != MemberStatus.ACTIVE)
			.build();
	}
}
