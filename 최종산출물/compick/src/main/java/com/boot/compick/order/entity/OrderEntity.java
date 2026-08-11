package com.boot.compick.order.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "ORDERS")
public class OrderEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_id")
	private Long orderId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "order_number", nullable = false, unique = true, length = 50)
	private String orderNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "order_status", nullable = false, length = 30)
	private OrderStatus orderStatus;

	@Column(name = "product_amount", nullable = false)
	private long productAmount;

	@Column(name = "shipping_fee", nullable = false)
	private long shippingFee;

	@Column(name = "final_amount", nullable = false)
	private long finalAmount;

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

	@Column(name = "delivery_request", length = 500)
	private String deliveryRequest;

	@Column(name = "return_requested_at")
	private LocalDateTime returnRequestedAt;

	@Column(name = "stock_deducted_at")
	private LocalDateTime stockDeductedAt;

	@Column(name = "stock_restored_at")
	private LocalDateTime stockRestoredAt;

	@CreationTimestamp
	@Column(name = "ordered_at", nullable = false, updatable = false)
	private LocalDateTime orderedAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderGroupEntity> groups = new ArrayList<>();

	protected OrderEntity() {
	}

	private OrderEntity(
		Long memberId,
		String orderNumber,
		long productAmount,
		long shippingFee,
		String recipientName,
		String recipientPhone,
		String zipCode,
		String basicAddress,
		String detailAddress,
		String deliveryRequest
	) {
		this.memberId = memberId;
		this.orderNumber = orderNumber;
		this.orderStatus = OrderStatus.PAYMENT_PENDING;
		this.productAmount = productAmount;
		this.shippingFee = shippingFee;
		this.finalAmount = productAmount + shippingFee;
		this.recipientName = recipientName;
		this.recipientPhone = recipientPhone;
		this.zipCode = zipCode;
		this.basicAddress = basicAddress;
		this.detailAddress = detailAddress;
		this.deliveryRequest = deliveryRequest;
	}

	public static OrderEntity create(
		Long memberId,
		String orderNumber,
		long productAmount,
		long shippingFee,
		String recipientName,
		String recipientPhone,
		String zipCode,
		String basicAddress,
		String detailAddress,
		String deliveryRequest
	) {
		return new OrderEntity(
			memberId,
			orderNumber,
			productAmount,
			shippingFee,
			recipientName,
			recipientPhone,
			zipCode,
			basicAddress,
			detailAddress,
			deliveryRequest
		);
	}

	public OrderGroupEntity addProductGroup(
		Long productId,
		String productName,
		long price,
		int quantity
	) {
		OrderGroupEntity group = OrderGroupEntity.createProductGroup(this, productName, quantity);
		group.addItem(productId, productName, price, quantity);
		groups.add(group);
		return group;
	}

	public OrderGroupEntity addQuoteGroup(
		Long sourceQuoteId,
		String quoteName,
		int quantity,
		String assemblyType
	) {
		OrderGroupEntity group = OrderGroupEntity.createQuoteGroup(this, sourceQuoteId, quoteName, quantity, assemblyType);
		groups.add(group);
		return group;
	}

	public void markPaid() {
		this.orderStatus = OrderStatus.PAID;
	}

	public void cancel() {
		this.orderStatus = OrderStatus.CANCELLED;
	}

	public void requestReturn() {
		this.returnRequestedAt = LocalDateTime.now();
	}

	public void changeStatus(OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}

	public boolean isCancellable() {
		return orderStatus == OrderStatus.PAYMENT_PENDING
			|| orderStatus == OrderStatus.PAID
			|| orderStatus == OrderStatus.PREPARING;
	}

	public Long getOrderId() {
		return orderId;
	}

	public Long getMemberId() {
		return memberId;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public LocalDateTime getReturnRequestedAt() {
		return returnRequestedAt;
	}

	public boolean isReturnRequested() {
		return returnRequestedAt != null;
	}

	public boolean isStockDeducted() {
		return stockDeductedAt != null;
	}

	public boolean isStockRestored() {
		return stockRestoredAt != null;
	}

	public void markStockDeducted() {
		stockDeductedAt = LocalDateTime.now();
	}

	public void markStockRestored() {
		stockRestoredAt = LocalDateTime.now();
	}

	public long getProductAmount() {
		return productAmount;
	}

	public long getShippingFee() {
		return shippingFee;
	}

	public long getFinalAmount() {
		return finalAmount;
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

	public String getDeliveryRequest() {
		return deliveryRequest;
	}

	public LocalDateTime getOrderedAt() {
		return orderedAt;
	}

	public List<OrderGroupEntity> getGroups() {
		return groups;
	}
}
