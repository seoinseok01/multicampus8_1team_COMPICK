package com.boot.compick.cart.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSelectionRequest(
	@NotNull Boolean selected
) {
}
