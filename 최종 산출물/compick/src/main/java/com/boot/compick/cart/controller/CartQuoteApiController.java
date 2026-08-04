package com.boot.compick.cart.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.boot.compick.cart.dto.UpdateSelectionRequest;
import com.boot.compick.quote.dto.CartQuoteItemResponse;
import com.boot.compick.quote.dto.QuoteBuildRequest;
import com.boot.compick.quote.service.QuoteService;
import com.boot.compick.cart.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart/quotes")
public class CartQuoteApiController {

	private final QuoteService quoteService;
	private final CartService cartService;

	public CartQuoteApiController(QuoteService quoteService, CartService cartService) {
		this.quoteService = quoteService;
		this.cartService = cartService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CartQuoteItemResponse buildAndAddToCart(
		@Valid @RequestBody QuoteBuildRequest request,
		Principal principal
	) {
		return quoteService.buildAndAddToCart(principal.getName(), request);
	}

	@PostMapping("/{quoteId}")
	@ResponseStatus(HttpStatus.CREATED)
	public CartQuoteItemResponse addExistingQuoteToCart(
		@PathVariable Long quoteId,
		Principal principal
	) {
		return quoteService.addExistingQuoteToCart(principal.getName(), quoteId);
	}

	@PatchMapping("/{quoteId}/selection")
	public void changeQuoteSelection(
		@PathVariable Long quoteId,
		@Valid @RequestBody UpdateSelectionRequest request,
		Principal principal
	) {
		cartService.changeQuoteSelection(principal.getName(), quoteId, request.selected());
	}

	@DeleteMapping("/{quoteId}")
	public void deleteQuote(@PathVariable Long quoteId, Principal principal) {
		cartService.deleteQuoteItem(principal.getName(), quoteId);
	}
}
