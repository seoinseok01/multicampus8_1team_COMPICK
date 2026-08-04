package com.boot.compick.cart.controller;

import java.security.Principal;
import com.boot.compick.quote.dto.CartQuoteItemResponse;
import com.boot.compick.quote.dto.QuoteBuildRequest;
import com.boot.compick.quote.service.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart/quotes")
public class CartQuoteBuildApiController {
    private final QuoteService quoteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartQuoteItemResponse build(@Valid @RequestBody QuoteBuildRequest request, Principal principal) {
        return quoteService.buildAndAddToCart(principal.getName(), request);
    }

    @PostMapping("/{quoteId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CartQuoteItemResponse addPreset(@PathVariable Long quoteId, Principal principal) {
        return quoteService.addExistingQuoteToCart(principal.getName(), quoteId);
    }
}
