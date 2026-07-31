package com.boot.compick.member.repository;

import com.boot.compick.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByEmailAndStatus(String email, com.boot.compick.member.entity.MemberStatus status);
    Optional<Member> findByLoginIdAndEmailAndStatus(String loginId, String email, com.boot.compick.member.entity.MemberStatus status);
}
