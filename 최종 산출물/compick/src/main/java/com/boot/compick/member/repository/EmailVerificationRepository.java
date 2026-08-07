package com.boot.compick.member.repository;
import com.boot.compick.member.entity.*; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface EmailVerificationRepository extends JpaRepository<EmailVerification,Long>{Optional<EmailVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email,VerificationPurpose purpose);}

