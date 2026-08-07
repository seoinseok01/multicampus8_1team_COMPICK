package com.boot.compick.payment.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PAYMENT")
public class PaymentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "payment_id")
	private Long paymentId;

	@Column(name = "order_id", nullable = false, unique = true)
	private Long orderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 30)
	private PaymentMethod paymentMethod;

	@Column(name = "payment_amount", nullable = false)
	private long paymentAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false, length = 20)
	private PaymentStatus paymentStatus;

	@Column(name = "external_transaction_id", unique = true, length = 100)
	private String externalTransactionId;

	@Column(name = "approval_number", length = 100)
	private String approvalNumber;

	@CreationTimestamp
	@Column(name = "requested_at", nullable = false, updatable = false)
	private LocalDateTime requestedAt;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;

	protected PaymentEntity() {
	}

	private PaymentEntity(
		Long orderId,
		PaymentMethod paymentMethod,
		long paymentAmount,
		String externalTransactionId
	) {
		this.orderId = orderId;
		this.paymentMethod = paymentMethod;
		this.paymentAmount = paymentAmount;
		this.paymentStatus = PaymentStatus.READY;
		this.externalTransactionId = externalTransactionId;
	}

	public static PaymentEntity createReady(
		Long orderId,
		PaymentMethod paymentMethod,
		long paymentAmount,
		String externalTransactionId
	) {
		return new PaymentEntity(orderId, paymentMethod, paymentAmount, externalTransactionId);
	}

	public void approve(String approvalNumber) {
		this.paymentStatus = PaymentStatus.APPROVED;
		this.approvalNumber = approvalNumber;
		this.approvedAt = LocalDateTime.now();
	}

	public void fail() {
		this.paymentStatus = PaymentStatus.FAILED;
	}

	public void cancel() {
		this.paymentStatus = PaymentStatus.CANCELLED;
		this.cancelledAt = LocalDateTime.now();
	}

	public void changeExternalTransactionId(String externalTransactionId) {
		this.externalTransactionId = externalTransactionId;
	}

	public Long getPaymentId() {
		return paymentId;
	}

	public Long getOrderId() {
		return orderId;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public long getPaymentAmount() {
		return paymentAmount;
	}

	public PaymentStatus getPaymentStatus() {
		return paymentStatus;
	}

	public String getExternalTransactionId() {
		return externalTransactionId;
	}

	public String getApprovalNumber() {
		return approvalNumber;
	}

	public LocalDateTime getApprovedAt() {
		return approvedAt;
	}

}
