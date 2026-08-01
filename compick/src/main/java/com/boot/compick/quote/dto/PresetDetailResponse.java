package com.boot.compick.quote.dto;

import java.util.List;

public record PresetDetailResponse(
	Long quoteId,
	String quoteName,
	String purposeTag,
	String summaryDescription,
	List<QuoteItemView> items,
	long totalPrice,
	Integer estimatedPowerWatt
) {
}
