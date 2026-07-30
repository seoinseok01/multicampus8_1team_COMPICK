package com.boot.compick.recommendation;

import com.boot.compick.recommendation.service.AiQuoteParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AiQuoteParserTests {

    @Test
    void createsTemporaryIdsForEveryCatalogProduct() throws Exception {
        String csv;
        try (var input = getClass().getResourceAsStream("/ai/compick-products.csv")) {
            assertThat(input).isNotNull();
            csv = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        var catalog = new AiQuoteParser().createCatalog(csv);

        assertThat(catalog.productsById()).hasSize(543);
        assertThat(catalog.productsById()).containsKeys("AI-000001", "AI-000543");
        assertThat(new String(catalog.uploadData(), StandardCharsets.UTF_8))
                .startsWith("ai_product_id,category,brand,name,price");
    }
}
