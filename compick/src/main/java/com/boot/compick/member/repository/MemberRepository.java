package com.boot.compick.member.repository;

import com.boot.compick.member.entity.Member;
import com.boot.compick.member.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByNameAndEmailAndStatus(String name, String email, MemberStatus status);
    Optional<Member> findByLoginIdAndEmailAndStatus(String loginId, String email, MemberStatus status);
}
