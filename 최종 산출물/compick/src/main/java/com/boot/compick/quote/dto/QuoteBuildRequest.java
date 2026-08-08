package com.boot.compick.quote.dto;

import java.util.List;

import com.boot.compick.quote.entity.AssemblyType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record QuoteBuildRequest(
	@NotEmpty @Valid List<QuoteLineItem> items,
	@NotNull AssemblyType assemblyType
) {

	/**
	 * RAM과 저장장치(SSD/HDD)는 동일 상품을 여러 개 선택할 수 있어 productId당 quantity를 함께 받는다.
	 * 그 외 카테고리는 항상 quantity=1로 보낸다.
	 */
	public record QuoteLineItem(
		@NotNull Long productId,
		@Min(1) int quantity
	) {
	}
}
