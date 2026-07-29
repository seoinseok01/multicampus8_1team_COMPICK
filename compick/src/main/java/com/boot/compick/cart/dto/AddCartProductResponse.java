package com.boot.compick.cart.dto;

public record AddCartProductResponse(
	Long cartId,
	Long productId,
	int quantity,
	long cartItemCount,
	String message
) {
}
