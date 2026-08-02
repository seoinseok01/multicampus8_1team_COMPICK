package com.boot.compick.order.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
	@NotNull Long addressId,
	String deliveryRequest
) {
}
