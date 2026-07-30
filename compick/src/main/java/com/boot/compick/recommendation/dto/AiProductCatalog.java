package com.boot.compick.recommendation.dto;

import java.util.Map;

public record AiProductCatalog(
        byte[] uploadData,
        Map<String, AiQuoteItem> productsById) {
}
