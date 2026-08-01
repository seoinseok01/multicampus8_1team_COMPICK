package com.boot.compick.home.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.boot.compick.product.CategoryDisplay;
import com.boot.compick.product.service.ProductService;

@Controller
public class ShoppingPageController {

	private static final int INITIAL_PAGE_SIZE = 12;

	private final ProductService productService;

	public ShoppingPageController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping("/products")
	public String products(Model model) {
		model.addAttribute("categoryTabs", CategoryDisplay.CATEGORY_TABS);
		model.addAttribute(
			"initialProducts",
			productService.listProducts(
				"CPU",
				null,
				null,
				null,
				null,
				null,
				null,
				PageRequest.of(0, INITIAL_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "ratingCount"))
			)
		);
		return "shopping/products";
	}

	@GetMapping("/ai-quotes")
	public String aiQuotes() {
		return "shopping/ai-quotes";
	}
}
