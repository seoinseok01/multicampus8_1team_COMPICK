package com.boot.compick.home.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
	public String products(
		@RequestParam(required = false) String keyword,
		Model model
	) {
		model.addAttribute("categoryTabs", CategoryDisplay.CATEGORY_TABS);
		model.addAttribute("initialKeyword", keyword == null ? "" : keyword);
		model.addAttribute(
			"initialProducts",
			productService.listProducts(
				"CPU",
				null,
				null,
				null,
				keyword,
				null,
				null,
				PageRequest.of(0, INITIAL_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "ratingCount"))
			)
		);
		return "shopping/products";
	}
}
