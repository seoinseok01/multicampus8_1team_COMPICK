package com.boot.compick.member.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "EMAIL_VERIFICATION")
public class EmailVerification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "verification_id")
	private Long id;

	@Column(name = "email", nullable = false, length = 100)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(name = "purpose", nullable = false, length = 30)
	private VerificationPurpose purpose;

	@Column(name = "verification_code_hash", nullable = false, length = 255)
	private String codeHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	@Column(name = "used_at")
	private LocalDateTime usedAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	protected EmailVerification() {
	}

	public EmailVerification(
		String email,
		VerificationPurpose purpose,
		String codeHash,
		LocalDateTime expiresAt,
		LocalDateTime createdAt
	) {
		this.email = email;
		this.purpose = purpose;
		this.codeHash = codeHash;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
	}

	public void increaseAttempt() {
		this.attemptCount++;
	}

	public void verify(LocalDateTime now) {
		this.verifiedAt = now;
	}

	public void use(LocalDateTime now) {
		this.usedAt = now;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public VerificationPurpose getPurpose() {
		return purpose;
	}

	public String getCodeHash() {
		return codeHash;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public LocalDateTime getVerifiedAt() {
		return verifiedAt;
	}

	public LocalDateTime getUsedAt() {
		return usedAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
