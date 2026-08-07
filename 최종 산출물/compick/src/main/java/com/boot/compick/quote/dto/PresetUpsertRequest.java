package com.boot.compick.quote.dto;

import java.util.List;

import com.boot.compick.quote.entity.PurposeTag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 추천 견적(PRESET) 생성/수정 요청 형태.
 * 향후 관리자 페이지 컨트롤러가 그대로 받아 PresetAdminService에 넘기면 된다.
 */
public record PresetUpsertRequest(
	@NotBlank String quoteName,
	PurposeTag purposeTag,
	String summaryDescription,
	String imageUrl,
	@NotEmpty @Valid List<PresetItem> items
) {

	public record PresetItem(
		@NotNull Long productId,
		@Min(1) int quantity
	) {
	}
}
