package com.boot.compick.order.entity;

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
@Table(name = "ORDER_ITEM")
public class OrderItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_item_id")
	private Long orderItemId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private OrderEntity order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_group_id", nullable = false)
	private OrderGroupEntity group;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "product_name", nullable = false, length = 255)
	private String productName;

	@Column(name = "order_price", nullable = false)
	private long orderPrice;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	@Column(name = "line_amount", nullable = false)
	private long lineAmount;

	protected OrderItemEntity() {
	}

	private OrderItemEntity(
		OrderGroupEntity group,
		Long productId,
		String productName,
		long orderPrice,
		int quantity
	) {
		this.order = group.getOrder();
		this.group = group;
		this.productId = productId;
		this.productName = productName;
		this.orderPrice = orderPrice;
		this.quantity = quantity;
		this.lineAmount = orderPrice * quantity;
	}

	static OrderItemEntity create(
		OrderGroupEntity group,
		Long productId,
		String productName,
		long orderPrice,
		int quantity
	) {
		return new OrderItemEntity(group, productId, productName, orderPrice, quantity);
	}

	public Long getProductId() {
		return productId;
	}

	public String getProductName() {
		return productName;
	}

	public long getOrderPrice() {
		return orderPrice;
	}

	public int getQuantity() {
		return quantity;
	}

	public long getLineAmount() {
		return lineAmount;
	}
}
