package com.boot.compick.shopping.recommendation.dto;

import java.util.Map;

public record AiProductCatalog(
        byte[] uploadData,
        Map<String, AiQuoteItem> productsById) {
}
