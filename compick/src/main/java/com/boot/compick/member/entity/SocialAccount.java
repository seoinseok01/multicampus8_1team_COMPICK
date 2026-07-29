package com.boot.compick.member.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "SOCIAL_ACCOUNT",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_social_provider_user",
		columnNames = {"provider", "provider_user_id"}
	)
)
public class SocialAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "social_account_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, length = 20)
	private SocialProvider provider;

	@Column(name = "provider_user_id", nullable = false, length = 255)
	private String providerUserId;

	@Column(name = "provider_email", nullable = false, length = 100)
	private String providerEmail;

	@Column(
		name = "setup_completed",
		nullable = false,
		length = 1,
		columnDefinition = "CHAR(1)"
	)
	@JdbcTypeCode(SqlTypes.CHAR)
	private String setupCompletedYn = "N";

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected SocialAccount() {
	}

	public SocialAccount(
		Member member,
		SocialProvider provider,
		String providerUserId,
		String providerEmail,
		boolean setupCompleted
	) {
		this.member = member;
		this.provider = provider;
		this.providerUserId = providerUserId;
		this.providerEmail = providerEmail;
		this.setupCompletedYn = setupCompleted ? "Y" : "N";
	}

	public void updateEmail(String email) {
		this.providerEmail = email;
	}

	public Member getMember() {
		return member;
	}

	public boolean isSetupCompleted() {
		return "Y".equals(setupCompletedYn);
	}

	public void completeSetup() {
		this.setupCompletedYn = "Y";
	}
}
