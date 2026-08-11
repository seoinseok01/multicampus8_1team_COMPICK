package com.boot.compick.cart.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartPageController {

	@GetMapping("/cart")
	public String cart() {
		return "cart/index";
	}
}
