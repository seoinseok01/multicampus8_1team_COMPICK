package com.boot.compick.cart.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.boot.compick.cart.service.CartViewService;

@Controller
public class CartPageController {

	private final CartViewService cartViewService;

	public CartPageController(CartViewService cartViewService) {
		this.cartViewService = cartViewService;
	}

	@GetMapping("/cart")
	public String cart(Principal principal, Model model) {
		model.addAttribute("cart", cartViewService.findByLoginId(principal.getName()));
		return "cart/index";
	}
}
