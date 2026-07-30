package com.boot.compick.cart.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.boot.compick.cart.dto.AddCartProductRequest;
import com.boot.compick.cart.dto.AddCartProductResponse;
import com.boot.compick.cart.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart/items")
public class CartApiController {

	private final CartService cartService;

	public CartApiController(CartService cartService) {
		this.cartService = cartService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AddCartProductResponse addProduct(
		@Valid @RequestBody AddCartProductRequest request,
		Principal principal
	) {
		return cartService.addProduct(principal.getName(), request);
	}
}
