package com.boot.compick.member.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ADDRESS")
public class Address {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "address_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "address_name", nullable = false, length = 50)
	private String addressName;

	@Column(name = "recipient_name", nullable = false, length = 50)
	private String recipientName;

	@Column(name = "recipient_phone", nullable = false, length = 20)
	private String recipientPhone;

	@Column(name = "zip_code", nullable = false, length = 10)
	private String zipCode;

	@Column(name = "basic_address", nullable = false, length = 255)
	private String basicAddress;

	@Column(name = "detail_address", length = 255)
	private String detailAddress;

	@Column(
		name = "is_default",
		nullable = false,
		length = 1,
		columnDefinition = "CHAR(1)"
	)
	@JdbcTypeCode(SqlTypes.CHAR)
	private String defaultYn = "N";

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Address() {
	}

	public Address(
		Member member,
		String addressName,
		String recipientName,
		String recipientPhone,
		String zipCode,
		String basicAddress,
		String detailAddress,
		boolean defaultAddress
	) {
		this.member = member;
		update(
			addressName,
			recipientName,
			recipientPhone,
			zipCode,
			basicAddress,
			detailAddress,
			defaultAddress
		);
	}

	public void update(
		String addressName,
		String recipientName,
		String recipientPhone,
		String zipCode,
		String basicAddress,
		String detailAddress,
		boolean defaultAddress
	) {
		this.addressName = addressName;
		this.recipientName = recipientName;
		this.recipientPhone = recipientPhone;
		this.zipCode = zipCode;
		this.basicAddress = basicAddress;
		this.detailAddress = detailAddress;
		this.defaultYn = defaultAddress ? "Y" : "N";
	}

	public void makeDefault() {
		this.defaultYn = "Y";
	}

	public void clearDefault() {
		this.defaultYn = "N";
	}

	public boolean isDefault() {
		return "Y".equals(defaultYn);
	}

	public Long getId() {
		return id;
	}

	public Member getMember() {
		return member;
	}

	public String getAddressName() {
		return addressName;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public String getRecipientPhone() {
		return recipientPhone;
	}

	public String getZipCode() {
		return zipCode;
	}

	public String getBasicAddress() {
		return basicAddress;
	}

	public String getDetailAddress() {
		return detailAddress;
	}
}
