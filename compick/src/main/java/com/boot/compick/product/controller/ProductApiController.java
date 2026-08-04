package com.boot.compick.product.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.compick.product.dto.ProductFacetResponse;
import com.boot.compick.product.dto.ProductListItemResponse;
import com.boot.compick.product.service.ProductService;

@RestController
public class ProductApiController {

	private static final String SPEC_PARAM_PREFIX = "spec_";
	private static final int DEFAULT_PAGE_SIZE = 20;

	private final ProductService productService;

	public ProductApiController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping("/api/products")
	public Page<ProductListItemResponse> list(
		@RequestParam String category,
		@RequestParam(required = false) List<String> brands,
		@RequestParam(required = false) Long minPrice,
		@RequestParam(required = false) Long maxPrice,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) String storageType,
		@RequestParam(defaultValue = "popular") String sort,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
		@RequestParam Map<String, String> allParams
	) {
		Pageable pageable = PageRequest.of(page, size, sortFor(sort));
		return productService.listProducts(
			category,
			brands,
			minPrice,
			maxPrice,
			keyword,
			storageType,
			specFiltersFrom(allParams),
			pageable
		);
	}

	@GetMapping("/api/products/{category}/facets")
	public ProductFacetResponse facets(@PathVariable String category) {
		return productService.getFacets(category);
	}

	private Sort sortFor(String sort) {
		return switch (sort) {
			case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price");
			case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
			default -> Sort.by(Sort.Direction.DESC, "ratingCount")
				.and(Sort.by(Sort.Direction.DESC, "createdAt"));
		};
	}

	private Map<String, String> specFiltersFrom(Map<String, String> allParams) {
		Map<String, String> specFilters = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : allParams.entrySet()) {
			if (entry.getKey().startsWith(SPEC_PARAM_PREFIX) && !entry.getValue().isBlank()) {
				String specKey = entry.getKey()
					.substring(SPEC_PARAM_PREFIX.length())
					.replace('_', ' ');
				specFilters.put(specKey, entry.getValue());
			}
		}
		return specFilters;
	}
}
