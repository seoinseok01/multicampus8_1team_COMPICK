package com.boot.compick.member.repository;

import com.boot.compick.member.entity.EmailVerification;
import com.boot.compick.member.entity.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, VerificationPurpose purpose);
}
