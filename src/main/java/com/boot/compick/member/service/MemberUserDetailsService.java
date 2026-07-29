package com.boot.compick.member.service;

import com.boot.compick.member.entity.Member;
import com.boot.compick.member.entity.MemberStatus;
import com.boot.compick.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다."));
        return User.withUsername(member.getLoginId())
                .password(member.getPasswordHash())
                .roles(member.getRole().name())
                .disabled(member.getStatus() != MemberStatus.ACTIVE)
                .build();
    }
}
