package com.boot.compick.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ADDRESS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    @Column(name = "basic_address", nullable = false)
    private String basicAddress;
    @Column(name = "detail_address")
    private String detailAddress;
    @Column(name = "is_default", nullable = false, length = 1)
    private String defaultYn = "N";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Address(Member member, String addressName, String recipientName, String recipientPhone,
                   String zipCode, String basicAddress, String detailAddress, boolean isDefault) {
        this.member = member;
        update(addressName, recipientName, recipientPhone, zipCode, basicAddress, detailAddress, isDefault);
    }

    public void update(String addressName, String recipientName, String recipientPhone,
                       String zipCode, String basicAddress, String detailAddress, boolean isDefault) {
        this.addressName = addressName;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipCode = zipCode;
        this.basicAddress = basicAddress;
        this.detailAddress = detailAddress;
        this.defaultYn = isDefault ? "Y" : "N";
    }

    public boolean isDefault() { return "Y".equals(defaultYn); }
    public void clearDefault() { this.defaultYn = "N"; }
    public void makeDefault() { this.defaultYn = "Y"; }
}
