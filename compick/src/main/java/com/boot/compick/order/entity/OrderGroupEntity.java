package com.boot.compick.order.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "ORDER_GROUP")
@Getter
public class OrderGroupEntity {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_group_id") private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false) private OrderEntity order;
	@Column(name = "source_quote_id") private Long sourceQuoteId;
	@Column(name = "group_type", nullable = false, length = 20) private String groupType;
	@Column(name = "group_name", nullable = false, length = 150) private String groupName;
	@Column(name = "group_quantity", nullable = false) private int groupQuantity;
	@Column(name = "assembly_type", nullable = false, length = 20) private String assemblyType;

	protected OrderGroupEntity() {}
	public static OrderGroupEntity create(OrderEntity order) {
		OrderGroupEntity group = new OrderGroupEntity();
		group.order = order; group.groupType = "PRODUCT"; group.groupName = "장바구니 상품";
		group.groupQuantity = 1; group.assemblyType = "SELF";
		return group;
	}

	public static OrderGroupEntity createQuote(OrderEntity order, Long quoteId, String name, int quantity) {
		OrderGroupEntity group = new OrderGroupEntity();
		group.order = order;
		group.sourceQuoteId = quoteId;
		group.groupType = "QUOTE";
		group.groupName = name;
		group.groupQuantity = quantity;
		group.assemblyType = "SELF";
		return group;
	}
	public Long getId() { return id; }
}
