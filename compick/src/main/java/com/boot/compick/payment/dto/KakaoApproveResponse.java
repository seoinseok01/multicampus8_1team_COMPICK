package com.boot.compick.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoApproveResponse(
	String aid,
	String tid,
	@JsonProperty("partner_order_id") String partnerOrderId,
	@JsonProperty("payment_method_type") String paymentMethodType,
	Amount amount
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Amount(
		long total
	) {
	}
}
