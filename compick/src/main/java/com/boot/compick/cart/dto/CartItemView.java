package com.boot.compick.cart.dto;

public record CartItemView(
	Long cartItemId,
	Long productId,
	String productName,
	String brand,
	String modelName,
	long price,
	int quantity,
	String imageUrl,
	boolean selected
) {
	public long lineAmount() {
		return price * quantity;
	}
}
