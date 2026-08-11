package com.boot.compick.home.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.boot.compick.product.service.ProductService;
import com.boot.compick.quote.service.QuoteService;

@Controller
public class HomeController {

	private static final int AI_HIGHLIGHT_LIMIT = 8;

	private final ProductService productService;
	private final QuoteService quoteService;

	public HomeController(ProductService productService, QuoteService quoteService) {
		this.productService = productService;
		this.quoteService = quoteService;
	}

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("popularCategories", productService.findPopularProductsByCategory());
		model.addAttribute("aiHighlights", quoteService.findRecentAiHighlights(AI_HIGHLIGHT_LIMIT));
		return "home/index";
	}
}
