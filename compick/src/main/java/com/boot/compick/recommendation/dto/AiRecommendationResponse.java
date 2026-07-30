package com.boot.compick.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiRecommendationResponse(
        @JsonProperty("product_ids") List<String> productIds,
        @JsonProperty("quote_keywords") List<String> keywords,
        @JsonProperty("quote_explain") String explanation,
        @JsonProperty("quote_warn") String warning) {
}
