package com.boot.compick.product.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.boot.compick.product.entity.CategoryEntity;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.CategoryRepository;
import com.boot.compick.product.repository.ProductRepository;
import com.boot.compick.shopping.recommendation.service.AiQuoteParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(name = "product.catalog-initialize", havingValue = "true", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ProductCatalogInitializer implements ApplicationRunner {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AiQuoteParser csvParser;

    @Value("${product.catalog-resource:classpath:shopping/ai/compick-products.csv}")
    private Resource catalogResource;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (productRepository.count() > 0) {
            log.info("상품 데이터가 이미 존재하여 CSV 초기화를 건너뜁니다.");
            return;
        }

        String csv;
        try (InputStream input = catalogResource.getInputStream()) {
            csv = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<List<String>> rows = csvParser.parseRows(csv);
        if (rows.size() < 2) throw new IllegalStateException("상품 CSV 데이터가 비어 있습니다.");

        Map<String, CategoryEntity> categories = new LinkedHashMap<>();
        int inserted = 0;
        for (int index = 1; index < rows.size(); index++) {
            List<String> row = rows.get(index);
            if (row.size() < 7 || row.get(0).isBlank() || row.get(2).isBlank()) continue;
            String categoryName = normalizeCategory(row.get(0));
            CategoryEntity category = categories.computeIfAbsent(categoryName, name ->
                    categoryRepository.findByCategoryName(name)
                            .orElseGet(() -> categoryRepository.save(new CategoryEntity(name))));
            productRepository.save(ProductEntity.createFromCatalog(category, row.get(1).trim(), row.get(2).trim(),
                    parseLong(row.get(3)), parseInt(row.get(4)), row.get(5).trim(), row.get(6).trim()));
            inserted++;
        }
        productRepository.flush();
        log.info("CSV 상품 데이터 {}개를 등록했습니다.", inserted);
    }

    private String normalizeCategory(String value) {
        return switch (value.trim().toUpperCase()) {
            case "COOLER" -> "CPU_COOLER";
            case "MOTHERBOARD" -> "MAINBOARD";
            case "MEMORY" -> "RAM";
            case "VIDEO_CARD" -> "GPU";
            case "SSD", "HDD" -> "STORAGE";
            case "PSU" -> "POWER_SUPPLY";
            default -> value.trim().toUpperCase();
        };
    }

    private long parseLong(String value) {
        try { return Long.parseLong(value.trim()); } catch (NumberFormatException ignored) { return 0; }
    }

    private int parseInt(String value) {
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException ignored) { return 0; }
    }
}
