package com.boot.compick.member.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "MEMBER")
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Long id;

	@Column(name = "login_id", nullable = false, unique = true, length = 50)
	private String loginId;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "member_name", nullable = false, length = 50)
	private String name;

	@Column(name = "email", nullable = false, unique = true, length = 100)
	private String email;

	@Column(name = "nickname", nullable = false, length = 20)
	private String nickname;

	@Column(name = "phone", nullable = false, length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(name = "member_role", nullable = false, length = 20)
	private MemberRole role = MemberRole.USER;

	@Enumerated(EnumType.STRING)
	@Column(name = "member_status", nullable = false, length = 20)
	private MemberStatus status = MemberStatus.ACTIVE;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Member() {
	}

	public Member(
		String loginId,
		String passwordHash,
		String name,
		String email,
		String nickname,
		String phone
	) {
		this.loginId = loginId;
		this.passwordHash = passwordHash;
		this.name = name;
		this.email = email;
		this.nickname = nickname;
		this.phone = phone;
	}

	public void updateProfile(String email, String nickname, String phone) {
		this.email = email;
		this.nickname = nickname;
		this.phone = phone;
	}

	public void changePassword(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public void withdraw() {
		this.status = MemberStatus.WITHDRAWN;
	}

	public Long getId() {
		return id;
	}

	public String getLoginId() {
		return loginId;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getNickname() {
		return nickname;
	}

	public String getPhone() {
		return phone;
	}

	public MemberRole getRole() {
		return role;
	}

	public MemberStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
