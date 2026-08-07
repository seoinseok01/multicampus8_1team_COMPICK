package com.boot.compick.quote.dto;

import java.util.List;

public record PresetDetailResponse(
	Long quoteId,
	String quoteName,
	String purposeTag,
	String summaryDescription,
	String imageUrl,
	List<QuoteItemView> items,
	long totalPrice,
	Integer estimatedPowerWatt
) {
}
