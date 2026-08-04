package com.boot.compick.quote.entity;

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
	name = "QUOTE_ITEM",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_quote_item_product",
		columnNames = {"quote_id", "product_id"}
	)
)
public class QuoteItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "quote_item_id")
	private Long quoteItemId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "quote_id", nullable = false)
	private QuoteEntity quote;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "quantity", nullable = false)
	private int quantity = 1;

	protected QuoteItemEntity() {
	}

	private QuoteItemEntity(QuoteEntity quote, Long productId, int quantity) {
		this.quote = quote;
		this.productId = productId;
		this.quantity = quantity;
	}

	public static QuoteItemEntity create(QuoteEntity quote, Long productId, int quantity) {
		return new QuoteItemEntity(quote, productId, quantity);
	}

	public Long getProductId() {
		return productId;
	}

	public Long getId() { return quoteItemId; }
	public QuoteEntity getQuote() { return quote; }

	public int getQuantity() {
		return quantity;
	}
}
