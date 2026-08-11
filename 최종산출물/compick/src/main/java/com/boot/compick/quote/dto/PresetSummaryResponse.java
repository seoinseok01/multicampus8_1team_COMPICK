package com.boot.compick.quote.dto;

import java.util.List;

public record PresetSummaryResponse(
	Long quoteId,
	String quoteName,
	String purposeTag,
	String summaryDescription,
	String imageUrl,
	long totalPrice,
	List<String> highlightSpecs
) {
}
