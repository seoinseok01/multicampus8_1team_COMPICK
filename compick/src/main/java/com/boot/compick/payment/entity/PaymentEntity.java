package com.boot.compick.payment.entity;

import java.time.LocalDateTime;
import com.boot.compick.order.entity.OrderEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "PAYMENT")
@lombok.Getter
public class PaymentEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "payment_id") private Long id;
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false, unique = true) private OrderEntity order;
	@Column(name = "payment_method", nullable = false, length = 30) private String method;
	@Column(name = "payment_amount", nullable = false) private long amount;
	@Column(name = "payment_status", nullable = false, length = 20) private String status;
	@Column(name = "external_transaction_id", length = 100) private String transactionId;
	@Column(name = "approval_number", length = 100) private String approvalNumber;
	@Column(name = "requested_at", nullable = false) private LocalDateTime requestedAt;
	@Column(name = "approved_at") private LocalDateTime approvedAt;
	@Column(name = "cancelled_at") private LocalDateTime cancelledAt;
	@Column(name = "refunded_amount") private Long refundedAmount;
	@Column(name = "cancel_reason", length = 200) private String cancelReason;

	protected PaymentEntity() {}
	public static PaymentEntity approved(OrderEntity order, String method, String paymentKey, String approvalNumber) {
		PaymentEntity payment = new PaymentEntity();
		payment.order = order; payment.method = method == null ? "UNKNOWN" : method;
		payment.amount = order.getFinalAmount(); payment.status = "APPROVED";
		payment.transactionId = paymentKey; payment.approvalNumber = approvalNumber;
		payment.requestedAt = LocalDateTime.now(); payment.approvedAt = payment.requestedAt;
		payment.refundedAmount = 0L;
		return payment;
	}
	public void cancel(long refundedAmount, String reason) {
		this.status = "CANCELLED";
		this.refundedAmount = refundedAmount;
		this.cancelReason = reason;
		this.cancelledAt = LocalDateTime.now();
	}
}
