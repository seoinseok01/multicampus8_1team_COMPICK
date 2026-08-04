package com.boot.compick.cart.dto;

public record CartProductLineResponse(
	Long productId,
	String category,
	String brand,
	String name,
	String imageUrl,
	long price,
	int quantity,
	long lineTotal,
	boolean selected,
	boolean purchasable
) {
}
