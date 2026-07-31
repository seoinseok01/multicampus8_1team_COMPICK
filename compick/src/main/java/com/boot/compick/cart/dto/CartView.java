package com.boot.compick.cart.dto;

import java.util.List;

public record CartView(
	List<CartItemView> items,
	long productAmount,
	long shippingFee,
	long totalAmount
) {
	public static CartView empty() {
		return new CartView(List.of(), 0, 0, 0);
	}
}
