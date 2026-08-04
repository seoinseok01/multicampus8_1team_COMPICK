package com.boot.compick.quote.dto;

public record CartQuoteItemResponse(
	Long cartId,
	Long quoteId,
	int quantity,
	String message
) {
}
