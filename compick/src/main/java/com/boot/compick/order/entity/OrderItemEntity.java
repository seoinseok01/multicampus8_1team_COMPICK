package com.boot.compick.order.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "ORDER_ITEM")
@Getter
public class OrderItemEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_item_id") private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false) private OrderEntity order;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_group_id", nullable = false) private OrderGroupEntity group;
	@Column(name = "product_id", nullable = false) private Long productId;
	@Column(name = "product_name", nullable = false, length = 255) private String productName;
	@Column(name = "order_price", nullable = false) private long orderPrice;
	@Column(name = "quantity", nullable = false) private int quantity;
	@Column(name = "line_amount", nullable = false) private long lineAmount;

	protected OrderItemEntity() {}
	public static OrderItemEntity create(OrderEntity order, OrderGroupEntity group,
		Long productId, String name, long price, int quantity) {
		OrderItemEntity item = new OrderItemEntity();
		item.order = order; item.group = group; item.productId = productId;
		item.productName = name; item.orderPrice = price; item.quantity = quantity;
		item.lineAmount = price * quantity;
		return item;
	}
}
