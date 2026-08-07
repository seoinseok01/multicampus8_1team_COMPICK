package com.boot.compick.order.dto;

import java.util.List;

public record OrderDetailResponse(
	String orderNumber,
	String orderedAt,
	String status,
	String statusLabel,
	List<OrderGroupResponse> groups,
	long productAmount,
	long shippingFee,
	long finalAmount,
	String recipientName,
	String maskedPhone,
	String fullAddress,
	String deliveryRequest,
	String paymentMethodLabel,
	Long paymentAmount,
	boolean cancellable,
	boolean returnRequested
) {
}
