package com.boot.compick.order.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "ORDERS")
@Getter
public class OrderEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_id") private Long id;
	@Column(name = "member_id", nullable = false) private Long memberId;
	@Column(name = "order_number", nullable = false, unique = true, length = 50) private String orderNumber;
	@Enumerated(EnumType.STRING) @Column(name = "order_status", nullable = false, length = 30) private OrderStatus status;
	@Column(name = "product_amount", nullable = false) private long productAmount;
	@Column(name = "shipping_fee", nullable = false) private long shippingFee;
	@Column(name = "final_amount", nullable = false) private long finalAmount;
	@Column(name = "recipient_name", nullable = false, length = 50) private String recipientName;
	@Column(name = "recipient_phone", nullable = false, length = 20) private String recipientPhone;
	@Column(name = "zip_code", nullable = false, length = 10) private String zipCode;
	@Column(name = "basic_address", nullable = false, length = 255) private String basicAddress;
	@Column(name = "detail_address", length = 255) private String detailAddress;
	@Column(name = "delivery_request", length = 500) private String deliveryRequest;
	@Column(name = "ordered_at", nullable = false) private LocalDateTime orderedAt;
	@Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

	protected OrderEntity() {}

	public static OrderEntity create(Long memberId, String orderNumber, long amount,
		String recipientName, String recipientPhone, String zipCode,
		String basicAddress, String detailAddress, String deliveryRequest) {
		OrderEntity order = new OrderEntity();
		order.memberId = memberId;
		order.orderNumber = orderNumber;
		order.status = OrderStatus.PAYMENT_PENDING;
		order.productAmount = amount;
		order.shippingFee = 0;
		order.finalAmount = amount;
		order.recipientName = recipientName;
		order.recipientPhone = recipientPhone;
		order.zipCode = zipCode;
		order.basicAddress = basicAddress;
		order.detailAddress = detailAddress;
		order.deliveryRequest = deliveryRequest;
		order.orderedAt = LocalDateTime.now();
		order.updatedAt = order.orderedAt;
		return order;
	}

	public void markPaid() { status = OrderStatus.PAID; updatedAt = LocalDateTime.now(); }
}
