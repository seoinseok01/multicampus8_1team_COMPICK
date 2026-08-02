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
	name = "CART_QUOTE_ITEM",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_cart_quote_item",
		columnNames = {"cart_id", "quote_id"}
	)
)
public class CartQuoteItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cart_quote_item_id")
	private Long cartQuoteItemId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id", nullable = false)
	private CartEntity cart;

	@Column(name = "quote_id", nullable = false)
	private Long quoteId;

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

	protected CartQuoteItemEntity() {
	}

	private CartQuoteItemEntity(CartEntity cart, Long quoteId, int quantity) {
		this.cart = cart;
		this.quoteId = quoteId;
		this.quantity = quantity;
		this.selected = "Y";
	}

	public static CartQuoteItemEntity create(CartEntity cart, Long quoteId, int quantity) {
		return new CartQuoteItemEntity(cart, quoteId, quantity);
	}

	public void increaseQuantity(int amount) {
		quantity += amount;
	}

	public void changeSelected(boolean isSelected) {
		selected = isSelected ? "Y" : "N";
	}

	public Long getCartQuoteItemId() {
		return cartQuoteItemId;
	}

	public Long getCartId() {
		return cart.getCartId();
	}

	public Long getQuoteId() {
		return quoteId;
	}

	public int getQuantity() {
		return quantity;
	}

	public boolean isSelected() {
		return "Y".equals(selected);
	}
}
