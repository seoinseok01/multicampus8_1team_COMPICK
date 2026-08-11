package com.boot.compick.quote.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;
import com.boot.compick.quote.entity.AiRecommendationEntity;
import com.boot.compick.quote.repository.AiRecommendationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class AiKeywordCleanupRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AiKeywordCleanupRunner.class);
	private static final List<String> PART_TERMS = List.of(
		"cpu", "cpu 쿨러", "쿨러", "메인보드", "mainboard", "ram", "메모리",
		"gpu", "그래픽카드", "그래픽 카드", "저장장치", "ssd", "hdd",
		"파워서플라이", "파워 서플라이", "파워", "power supply", "케이스"
	);
	private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

	private final AiRecommendationRepository recommendations;
	private final ProductRepository products;
	private final ObjectMapper objectMapper;

	public AiKeywordCleanupRunner(
		AiRecommendationRepository recommendations,
		ProductRepository products,
		ObjectMapper objectMapper
	) {
		this.recommendations = recommendations;
		this.products = products;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		Set<String> forbiddenTerms = productTerms(products.findAll());
		PART_TERMS.stream().map(AiKeywordCleanupRunner::normalize).forEach(forbiddenTerms::add);
		int changedCount = 0;
		for (AiRecommendationEntity recommendation : recommendations.findAll()) {
			if (cleanKeywords(recommendation, forbiddenTerms)) changedCount++;
		}
		if (changedCount > 0) log.info("기존 AI 추천 {}건의 quote_keywords를 정리했습니다.", changedCount);
	}

	private boolean cleanKeywords(AiRecommendationEntity recommendation, Set<String> forbiddenTerms) {
		try {
			JsonNode parsed = objectMapper.readTree(recommendation.getAiAnswerJson());
			if (!(parsed instanceof ObjectNode root) || !(root.get("quote_keywords") instanceof ArrayNode keywords)) {
				return false;
			}

			ArrayNode cleaned = objectMapper.createArrayNode();
			for (JsonNode keywordNode : keywords) {
				String keyword = keywordNode.asText("").trim();
				if (!keyword.isBlank() && !containsForbiddenTerm(keyword, forbiddenTerms)) cleaned.add(keyword);
			}
			if (cleaned.size() == keywords.size()) return false;

			root.set("quote_keywords", cleaned);
			recommendation.updateAiAnswerJson(objectMapper.writeValueAsString(root));
			return true;
		} catch (Exception exception) {
			log.warn("AI 추천 {}의 JSON을 정리하지 못했습니다.", recommendation.getAiRecommendationId());
			return false;
		}
	}

	private Set<String> productTerms(List<ProductEntity> allProducts) {
		Set<String> terms = new LinkedHashSet<>();
		for (ProductEntity product : allProducts) {
			addTerm(terms, product.getProductName());
			addTerm(terms, product.getModelName());
			addTerm(terms, product.getBrand());
			addSpecTerms(terms, product.getSpecJson());
		}
		return terms;
	}

	private void addSpecTerms(Set<String> terms, String specJson) {
		if (specJson == null || specJson.isBlank()) return;
		try {
			addJsonValues(terms, objectMapper.readTree(specJson));
		} catch (Exception ignored) {
			// 잘못된 상품 스펙 JSON은 키워드 정리 대상에서만 제외한다.
		}
	}

	private void addJsonValues(Set<String> terms, JsonNode node) {
		if (node == null || node.isNull()) return;
		if (node.isValueNode()) {
			addTerm(terms, node.asText());
			return;
		}
		node.elements().forEachRemaining(child -> addJsonValues(terms, child));
	}

	private void addTerm(Set<String> terms, String value) {
		String normalized = normalize(value);
		if (normalized.length() >= 2) terms.add(normalized);
	}

	private boolean containsForbiddenTerm(String keyword, Set<String> forbiddenTerms) {
		String normalizedKeyword = normalize(keyword);
		return forbiddenTerms.stream()
			.anyMatch(term -> containsTerm(normalizedKeyword, term)
				|| normalizedKeyword.length() >= 4 && containsTerm(term, normalizedKeyword));
	}

	private boolean containsTerm(String keyword, String term) {
		if (term.matches("[a-z0-9]+")) {
			return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(term) + "(?![a-z0-9])")
				.matcher(keyword).find();
		}
		return keyword.contains(term);
	}

	private static String normalize(String value) {
		return value == null ? "" : MULTIPLE_SPACES.matcher(value.toLowerCase(Locale.ROOT).trim()).replaceAll(" ");
	}
}
