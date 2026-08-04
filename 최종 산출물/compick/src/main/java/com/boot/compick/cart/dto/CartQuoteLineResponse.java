package com.boot.compick.cart.dto;

import java.util.List;

public record CartQuoteLineResponse(
	Long quoteId,
	String quoteName,
	int itemCount,
	boolean compatible,
	int compatibilityIssueCount,
	List<CartQuoteItemPreview> items,
	long totalPrice,
	boolean selected
) {
}
