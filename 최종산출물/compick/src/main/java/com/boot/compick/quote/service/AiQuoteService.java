package com.boot.compick.quote.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.compick.member.service.MemberService;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;
import com.boot.compick.quote.entity.AiRecommendationEntity;
import com.boot.compick.quote.entity.QuoteEntity;
import com.boot.compick.quote.repository.AiRecommendationRepository;
import com.boot.compick.quote.repository.QuoteRepository;
import com.boot.compick.shopping.recommendation.dto.AiQuoteItem;
import com.boot.compick.shopping.recommendation.dto.AiRecommendationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AI 추천 결과를 로그인한 회원의 견적(QUOTE, quote_type=AI)으로 저장한다.
 * 저장된 견적은 "/quotes/new?basedOn="에서 수정하거나 장바구니에 바로 담을 수 있다.
 */
@Service
@Transactional(readOnly = true)
public class AiQuoteService {

	private static final int QUOTE_NAME_MAX_LENGTH = 150;
	private static final String QUOTE_NAME_PREFIX = "AI 견적 - ";

	private final MemberService memberService;
	private final ProductRepository productRepository;
	private final QuoteRepository quoteRepository;
	private final AiRecommendationRepository recommendationRepository;
	private final ObjectMapper objectMapper;

	public AiQuoteService(
		MemberService memberService,
		ProductRepository productRepository,
		QuoteRepository quoteRepository,
		AiRecommendationRepository recommendationRepository,
		ObjectMapper objectMapper
	) {
		this.memberService = memberService;
		this.productRepository = productRepository;
		this.quoteRepository = quoteRepository;
		this.recommendationRepository = recommendationRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public QuoteEntity save(
		String loginId,
		String requirements,
		AiRecommendationResponse response,
		List<AiQuoteItem> items
	) {
		List<String> productNames = items.stream().map(AiQuoteItem::name).distinct().toList();
		Map<String, ProductEntity> productsByName = productRepository.findByProductNameIn(productNames).stream()
			.collect(Collectors.toMap(
				ProductEntity::getProductName,
				Function.identity(),
				(first, ignored) -> first,
				LinkedHashMap::new
			));
		List<ProductEntity> products = items.stream()
			.map(item -> findProduct(item, productsByName))
			.toList();
		if (products.isEmpty()) {
			throw new IllegalArgumentException("저장할 AI 견적 상품이 없습니다.");
		}

		Long memberId = memberService.findActiveByLoginId(loginId).getId();
		QuoteEntity quote = QuoteEntity.createAi(memberId, quoteName(requirements));
		products.forEach(product -> quote.addItem(product.getProductId(), 1));
		QuoteEntity savedQuote = quoteRepository.save(quote);

		recommendationRepository.save(
			AiRecommendationEntity.create(savedQuote, requirements, toJson(response))
		);
		return savedQuote;
	}

	private ProductEntity findProduct(AiQuoteItem item, Map<String, ProductEntity> productsByName) {
		ProductEntity product = productsByName.get(item.name());
		if (product == null) {
			throw new IllegalArgumentException("상품 목록에서 추천 부품을 찾을 수 없습니다: " + item.name());
		}
		return product;
	}

	private String quoteName(String requirements) {
		String value = requirements == null ? "AI 추천 견적" : requirements.strip();
		int limit = QUOTE_NAME_MAX_LENGTH - QUOTE_NAME_PREFIX.length();
		return QUOTE_NAME_PREFIX + value.substring(0, Math.min(value.length(), limit));
	}

	private String toJson(AiRecommendationResponse response) {
		try {
			return objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("AI 견적 응답을 저장하지 못했습니다.", exception);
		}
	}
}
