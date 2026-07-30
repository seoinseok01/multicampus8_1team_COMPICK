package com.boot.compick.cart.entity;

import org.hibernate.annotations.JdbcTypeCode;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "CART_PRODUCT_ITEM",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_cart_product_item",
		columnNames = {"cart_id", "product_id"}
	)
)
public class CartProductItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cart_product_item_id")
	private Long cartProductItemId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id", nullable = false)
	private CartEntity cart;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	@Column(
		name = "is_selected",
		nullable = false,
		length = 1,
		columnDefinition = "CHAR(1)"
	)
	@JdbcTypeCode(SqlTypes.CHAR)
	private String selected;

	protected CartProductItemEntity() {
	}

	private CartProductItemEntity(CartEntity cart, Long productId, int quantity) {
		this.cart = cart;
		this.productId = productId;
		this.quantity = quantity;
		this.selected = "Y";
	}

	public static CartProductItemEntity create(
		CartEntity cart,
		Long productId,
		int quantity
	) {
		return new CartProductItemEntity(cart, productId, quantity);
	}

	public void increaseQuantity(int amount) {
		quantity += amount;
	}

	public int getQuantity() {
		return quantity;
	}
}
