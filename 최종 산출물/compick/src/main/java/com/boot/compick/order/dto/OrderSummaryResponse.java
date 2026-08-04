package com.boot.compick.order.dto;

public record OrderSummaryResponse(
	String orderNumber,
	String orderedAt,
	String status,
	String statusLabel,
	String title,
	long finalAmount
) {
}
