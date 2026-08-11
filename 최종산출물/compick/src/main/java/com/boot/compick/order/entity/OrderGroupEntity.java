package com.boot.compick.order.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "ORDER_GROUP")
public class OrderGroupEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_group_id")
	private Long orderGroupId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private OrderEntity order;

	@Column(name = "source_quote_id")
	private Long sourceQuoteId;

	@Enumerated(EnumType.STRING)
	@Column(name = "group_type", nullable = false, length = 20)
	private OrderGroupType groupType;

	@Column(name = "group_name", nullable = false, length = 150)
	private String groupName;

	@Column(name = "group_quantity", nullable = false)
	private int groupQuantity;

	@Column(name = "assembly_type", nullable = false, length = 20)
	private String assemblyType;

	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItemEntity> items = new ArrayList<>();

	protected OrderGroupEntity() {
	}

	private OrderGroupEntity(
		OrderEntity order,
		Long sourceQuoteId,
		OrderGroupType groupType,
		String groupName,
		int groupQuantity,
		String assemblyType
	) {
		this.order = order;
		this.sourceQuoteId = sourceQuoteId;
		this.groupType = groupType;
		this.groupName = groupName;
		this.groupQuantity = groupQuantity;
		this.assemblyType = assemblyType;
	}

	static OrderGroupEntity createProductGroup(OrderEntity order, String groupName, int groupQuantity) {
		return new OrderGroupEntity(order, null, OrderGroupType.PRODUCT, groupName, groupQuantity, "SELF");
	}

	static OrderGroupEntity createQuoteGroup(
		OrderEntity order,
		Long sourceQuoteId,
		String groupName,
		int groupQuantity,
		String assemblyType
	) {
		return new OrderGroupEntity(order, sourceQuoteId, OrderGroupType.QUOTE, groupName, groupQuantity, assemblyType);
	}

	public void addItem(Long productId, String productName, long price, int quantity) {
		items.add(OrderItemEntity.create(this, productId, productName, price, quantity));
	}

	public Long getOrderGroupId() {
		return orderGroupId;
	}

	OrderEntity getOrder() {
		return order;
	}

	public Long getSourceQuoteId() {
		return sourceQuoteId;
	}

	public OrderGroupType getGroupType() {
		return groupType;
	}

	public String getGroupName() {
		return groupName;
	}

	public int getGroupQuantity() {
		return groupQuantity;
	}

	public String getAssemblyType() {
		return assemblyType;
	}

	public List<OrderItemEntity> getItems() {
		return items;
	}
}
