package com.boot.compick.cart.dto;

import java.util.List;

public record CartViewResponse(
	List<CartProductLineResponse> products,
	List<CartQuoteLineResponse> quotes
) {
}
