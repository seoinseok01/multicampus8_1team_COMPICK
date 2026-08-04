package com.boot.compick.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.compick.member.entity.EmailVerification;
import com.boot.compick.member.entity.VerificationPurpose;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

	Optional<EmailVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(
		String email,
		VerificationPurpose purpose
	);
}
