package com.boot.compick.cart.controller;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.boot.compick.order.service.CheckoutService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CartPageController {
	private final CheckoutService checkoutService;

	@GetMapping("/cart")
	public String cart(Principal principal, Model model) {
		model.addAttribute("cart", checkoutService.getCart(principal.getName()));
		return "cart/index";
	}
}
