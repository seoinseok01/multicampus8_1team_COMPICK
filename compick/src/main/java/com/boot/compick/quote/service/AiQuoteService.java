package com.boot.compick.quote.service;

import java.util.List;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.service.MemberService;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;
import com.boot.compick.quote.entity.*;
import com.boot.compick.quote.repository.*;
import com.boot.compick.shopping.recommendation.dto.AiQuoteItem;
import com.boot.compick.shopping.recommendation.dto.AiRecommendationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiQuoteService {
    private final MemberService memberService;
    private final ProductRepository productRepository;
    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final AiRecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public QuoteEntity save(String loginId, String requirements,
            AiRecommendationResponse response, List<AiQuoteItem> items) {
        Member member = memberService.findActiveByLoginId(loginId);
        List<ProductEntity> products = items.stream().map(this::findProduct).toList();
        if (products.isEmpty()) {
            throw new IllegalArgumentException("저장할 AI 견적 상품이 없습니다.");
        }

        QuoteEntity quote = quoteRepository.save(QuoteEntity.ai(member, quoteName(requirements)));
        quoteItemRepository.saveAll(products.stream()
                .map(product -> QuoteItemEntity.create(quote, product))
                .toList());
        recommendationRepository.save(AiRecommendationEntity.create(
                quote, requirements, toJson(response)));
        return quote;
    }

    public QuoteEntity findOwned(String loginId, Long quoteId) {
        Long memberId = memberService.findActiveByLoginId(loginId).getId();
        return quoteRepository.findByIdAndMemberId(quoteId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("견적서를 찾을 수 없습니다."));
    }

    private ProductEntity findProduct(AiQuoteItem item) {
        return productRepository.findFirstByProductName(item.name())
                .orElseThrow(() -> new IllegalArgumentException(
                        "상품 테이블에서 견적 부품을 찾을 수 없습니다: " + item.name()));
    }

    private String quoteName(String requirements) {
        String value = requirements == null ? "AI 추천 견적" : requirements.strip();
        return "AI 견적 - " + value.substring(0, Math.min(value.length(), 135));
    }

    private String toJson(AiRecommendationResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI 견적 응답을 저장하지 못했습니다.", e);
        }
    }
}
