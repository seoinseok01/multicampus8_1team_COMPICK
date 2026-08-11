package com.boot.compick.quote.dto;

import java.util.List;

/** 메인페이지 AI 추천 배너용. 회원 신원은 담지 않고 요청/답변 내용만 노출한다. */
public record AiHighlightResponse(
	String requirement,
	List<String> keywords,
	String explanation,
	List<QuoteItemView> items,
	long totalPrice
) {
}
