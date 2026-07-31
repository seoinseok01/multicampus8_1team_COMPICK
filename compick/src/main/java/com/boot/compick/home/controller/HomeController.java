package com.boot.compick.home.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.boot.compick.member.service.MemberService;
import com.boot.compick.product.service.ProductService;

@Controller
public class HomeController {

	private final ProductService productService;
	private final MemberService memberService;

	public HomeController(
		ProductService productService,
		MemberService memberService
	) {
		this.productService = productService;
		this.memberService = memberService;
	}

	@GetMapping("/")
	public String home(Authentication authentication, Model model) {
		model.addAttribute("popularProducts", productService.findPopularProducts());
		if (isLoggedIn(authentication)) {
			model.addAttribute(
				"phoneMissing",
				memberService.isPhoneMissing(authentication.getName())
			);
		}
		return "home/index";
	}

	private boolean isLoggedIn(Authentication authentication) {
		return authentication != null
			&& authentication.isAuthenticated()
			&& !(authentication instanceof AnonymousAuthenticationToken);
	}
}
