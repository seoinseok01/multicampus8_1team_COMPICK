package com.boot.compick.quote.service;

import java.util.*;
import java.util.stream.Collectors;
import com.boot.compick.member.entity.Member;
import com.boot.compick.member.repository.MemberRepository;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;
import com.boot.compick.quote.entity.*;
import com.boot.compick.quote.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class PresetDataInitializer implements ApplicationRunner {
    private static final String SYSTEM_LOGIN_ID = "system_preset";
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final QuoteRepository quoteRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!quoteRepository.findByQuoteTypeOrderByQuoteIdAsc(QuoteType.PRESET).isEmpty()) return;
        Map<String, List<ProductEntity>> products = productRepository.findAll().stream()
                .filter(product -> "ON_SALE".equals(product.getSalesStatus()) && product.getStockQuantity() > 0)
                .sorted(Comparator.comparingLong(ProductEntity::getPrice))
                .collect(Collectors.groupingBy(product -> product.getCategory().getCategoryName()));
        if (!products.keySet().containsAll(List.of("CPU", "CPU_COOLER", "MAINBOARD", "RAM", "GPU", "STORAGE", "POWER_SUPPLY", "CASE"))) return;

        Member owner = memberRepository.findByLoginId(SYSTEM_LOGIN_ID).orElseGet(() -> memberRepository.save(
                new Member(SYSTEM_LOGIN_ID, "$2a$10$5g3XNImbFFhEXfLrTctgcOplVDd80g5Q3e0CtxqmeUhuBW.os/F/m",
                        "COMPICK 시스템", "system-preset@compick.internal", "000-0000-0000")));
        create(owner.getId(), products, 0, "입문용 기본 PC", PurposeTag.BEGINNER, "처음 조립 PC를 구매하는 사용자를 위한 기본 구성입니다.");
        create(owner.getId(), products, 1, "사무용 추천 PC", PurposeTag.OFFICE, "문서 작업과 인터넷 사용에 적합한 사무용 구성입니다.");
        create(owner.getId(), products, 2, "가성비 게이밍 PC", PurposeTag.GAMING, "FHD 게임을 위한 가격 대비 성능 중심 구성입니다.");
        create(owner.getId(), products, 3, "영상 편집 추천 PC", PurposeTag.VIDEO_EDIT, "영상 편집과 멀티태스킹을 고려한 구성입니다.");
    }

    private void create(Long memberId, Map<String, List<ProductEntity>> products, int level,
            String name, PurposeTag purpose, String description) {
        QuoteEntity quote = QuoteEntity.createPreset(memberId, name, purpose, description);
        List.of("CPU", "CPU_COOLER", "MAINBOARD", "RAM", "GPU", "STORAGE", "POWER_SUPPLY", "CASE")
                .forEach(category -> {
                    List<ProductEntity> options = products.get(category);
                    quote.addItem(options.get(Math.min(level, options.size() - 1)).getProductId(), 1);
                });
        quoteRepository.save(quote);
    }
}
