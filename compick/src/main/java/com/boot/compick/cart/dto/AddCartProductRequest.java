package com.boot.compick.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartProductRequest(
	@NotNull Long productId,
	@Min(1) int quantity
) {
}
