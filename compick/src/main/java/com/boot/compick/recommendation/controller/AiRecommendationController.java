package com.boot.compick.recommendation.controller;

import com.boot.compick.recommendation.dto.AiRecommendationRequest;
import com.boot.compick.recommendation.dto.AiQuoteItem;
import com.boot.compick.recommendation.service.GeminiRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AiRecommendationController {

    private final GeminiRecommendationService recommendationService;

    @GetMapping("/ai-quotes")
    public String form(Model model) {
        model.addAttribute("aiRecommendationRequest", new AiRecommendationRequest());
        return "recommendation/ai-quote";
    }

    @PostMapping("/ai-quotes")
    public String recommend(
            @Valid @ModelAttribute AiRecommendationRequest aiRecommendationRequest,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "recommendation/ai-quote";
        }

        try {
            var recommendation = recommendationService.recommend(aiRecommendationRequest.getRequirements());
            var quoteItems = recommendationService.resolveProducts(recommendation.productIds());
            model.addAttribute("recommendation", recommendation);
            model.addAttribute("quoteItems", quoteItems);
            model.addAttribute("totalPrice", quoteItems.stream().mapToLong(AiQuoteItem::price).sum());
            model.addAttribute("originalRequirements", aiRecommendationRequest.getRequirements());
        } catch (IllegalStateException e) {
            model.addAttribute("recommendationError", e.getMessage());
        }
        return "recommendation/ai-quote";
    }
}
