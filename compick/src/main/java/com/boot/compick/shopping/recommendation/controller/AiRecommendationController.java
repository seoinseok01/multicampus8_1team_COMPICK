package com.boot.compick.shopping.recommendation.controller;

import com.boot.compick.shopping.recommendation.dto.AiRecommendationRequest;
import com.boot.compick.shopping.recommendation.dto.AiQuoteItem;
import com.boot.compick.shopping.recommendation.service.GeminiRecommendationService;
import com.boot.compick.quote.service.AiQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AiRecommendationController {

    private final GeminiRecommendationService recommendationService;
    private final AiQuoteService aiQuoteService;

    @GetMapping("/ai-quotes")
    public String form(Model model) {
        model.addAttribute("aiRecommendationRequest", new AiRecommendationRequest());
        return "shopping/recommendation/ai-quote";
    }

    @PostMapping("/ai-quotes")
    public String recommend(
            @Valid @ModelAttribute AiRecommendationRequest aiRecommendationRequest,
            BindingResult bindingResult,
            Authentication authentication,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "shopping/recommendation/ai-quote";
        }

        try {
            var recommendation = recommendationService.recommend(aiRecommendationRequest.getRequirements());
            var quoteItems = recommendationService.resolveProducts(recommendation.productIds());
            model.addAttribute("recommendation", recommendation);
            model.addAttribute("quoteItems", quoteItems);
            model.addAttribute("totalPrice", quoteItems.stream().mapToLong(AiQuoteItem::price).sum());
            model.addAttribute("originalRequirements", aiRecommendationRequest.getRequirements());
            if (isLoggedIn(authentication)) {
                try {
                    Long quoteId = aiQuoteService.save(authentication.getName(),
                            aiRecommendationRequest.getRequirements(), recommendation, quoteItems).getId();
                    model.addAttribute("quoteId", quoteId);
                } catch (IllegalArgumentException e) {
                    model.addAttribute("cartIntegrationError", e.getMessage());
                }
            }
        } catch (IllegalStateException e) {
            model.addAttribute("recommendationError", e.getMessage());
        }
        return "shopping/recommendation/ai-quote";
    }

    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
