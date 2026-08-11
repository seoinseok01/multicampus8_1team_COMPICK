package com.boot.compick.cart.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.boot.compick.cart.dto.AddCartProductRequest;
import com.boot.compick.cart.dto.AddCartProductResponse;
import com.boot.compick.cart.dto.CartViewResponse;
import com.boot.compick.cart.dto.UpdateQuantityRequest;
import com.boot.compick.cart.dto.UpdateSelectionRequest;
import com.boot.compick.cart.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {

	private final CartService cartService;

	public CartApiController(CartService cartService) {
		this.cartService = cartService;
	}

	@GetMapping
	public CartViewResponse getCart(Principal principal) {
		return cartService.getCartView(principal.getName());
	}

	@PostMapping("/items")
	@ResponseStatus(HttpStatus.CREATED)
	public AddCartProductResponse addProduct(
		@Valid @RequestBody AddCartProductRequest request,
		Principal principal
	) {
		return cartService.addProduct(principal.getName(), request);
	}

	@PatchMapping("/items/{productId}/quantity")
	public void changeProductQuantity(
		@PathVariable Long productId,
		@Valid @RequestBody UpdateQuantityRequest request,
		Principal principal
	) {
		cartService.changeProductQuantity(principal.getName(), productId, request.quantity());
	}

	@PatchMapping("/items/{productId}/selection")
	public void changeProductSelection(
		@PathVariable Long productId,
		@Valid @RequestBody UpdateSelectionRequest request,
		Principal principal
	) {
		cartService.changeProductSelection(principal.getName(), productId, request.selected());
	}

	@DeleteMapping("/items/{productId}")
	public void deleteProduct(@PathVariable Long productId, Principal principal) {
		cartService.deleteProductItem(principal.getName(), productId);
	}
}
