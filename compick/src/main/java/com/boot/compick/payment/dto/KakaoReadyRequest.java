package com.boot.compick.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoReadyRequest(
	@NotBlank String orderNumber
) {
}
