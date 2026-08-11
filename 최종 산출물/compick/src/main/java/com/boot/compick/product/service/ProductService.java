package com.boot.compick.product.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.boot.compick.product.dto.PopularProductResponse;
import com.boot.compick.product.dto.PopularCategoryResponse;
import com.boot.compick.product.CategoryDisplay;
import com.boot.compick.product.dto.ProductDetailResponse;
import com.boot.compick.product.dto.ProductFacetResponse;
import com.boot.compick.product.dto.ProductListItemResponse;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;
import com.boot.compick.product.repository.ProductSpecFacetRepository;
import com.boot.compick.product.repository.ProductSpecifications;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class ProductService {

	private static final String ON_SALE = "ON_SALE";
	private static final int POPULAR_PRODUCT_LIMIT = 5;

	private static final Map<String, List<String>> CATEGORY_SPEC_FACETS = Map.of(
		"CPU", List.of("Socket", "Core Count"),
		"MAINBOARD", List.of("Socket", "Memory Type", "Form Factor"),
		"GPU", List.of("Chipset", "Memory"),
		"RAM", List.of("Speed"),
		"STORAGE", List.of("Form Factor", "Type"),
		"CASE", List.of("Form Factor"),
		"POWER_SUPPLY", List.of("Wattage", "Efficiency"),
		"CPU_COOLER", List.of("Type")
	);

	private final ProductRepository productRepository;
	private final ProductSpecFacetRepository specFacetRepository;
	private final ObjectMapper objectMapper;

	public ProductService(
		ProductRepository productRepository,
		ProductSpecFacetRepository specFacetRepository,
		ObjectMapper objectMapper
	) {
		this.productRepository = productRepository;
		this.specFacetRepository = specFacetRepository;
		this.objectMapper = objectMapper;
	}

	public Page<ProductListItemResponse> listProducts(
		String category,
		List<String> brands,
		Long minPrice,
		Long maxPrice,
		String storageType,
		Map<String, String> specFilters,
		Pageable pageable
	) {
		Specification<ProductEntity> spec = Specification
			.where(ProductSpecifications.onSale())
			.and(ProductSpecifications.categoryIs(category));

		if (brands != null && !brands.isEmpty()) {
			spec = spec.and(ProductSpecifications.brandIn(brands));
		}
		if (minPrice != null) {
			spec = spec.and(ProductSpecifications.priceGreaterThanOrEqual(minPrice));
		}
		if (maxPrice != null) {
			spec = spec.and(ProductSpecifications.priceLessThanOrEqual(maxPrice));
		}
		if (storageType != null && !storageType.isBlank()) {
			spec = spec.and(ProductSpecifications.storageTypeIs(storageType));
		}
		if (specFilters != null && !specFilters.isEmpty()) {
			spec = spec.and(ProductSpecifications.specFiltersFrom(category, specFilters));
		}

		return productRepository.findAll(spec, pageable)
			.map(ProductListItemResponse::from);
	}

	public ProductFacetResponse getFacets(String category) {
		List<String> brands = productRepository.findDistinctBrandsByCategory(category);

		Map<String, List<String>> specOptions = new LinkedHashMap<>();
		for (String key : CATEGORY_SPEC_FACETS.getOrDefault(category, List.of())) {
			List<String> values = specFacetRepository.findDistinctSpecValues(category, "$.\"" + key + "\"");
			specOptions.put(key, values);
		}

		Long minPrice = productRepository.findMinPriceByCategory(category);
		Long maxPrice = productRepository.findMaxPriceByCategory(category);

		return new ProductFacetResponse(
			brands,
			specOptions,
			minPrice == null ? 0 : minPrice,
			maxPrice == null ? 0 : maxPrice
		);
	}

	public ProductDetailResponse getDetail(Long productId) {
		ProductEntity product = productRepository.findById(productId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."));
		return ProductDetailResponse.from(product);
	}

	public List<PopularCategoryResponse> findPopularProductsByCategory() {
		Pageable popularProducts = PageRequest.of(
			0,
			POPULAR_PRODUCT_LIMIT,
			Sort.by(Sort.Direction.DESC, "ratingCount")
				.and(Sort.by(Sort.Direction.DESC, "createdAt"))
		);
		return CategoryDisplay.CATEGORY_TABS.stream()
			.map(tab -> new PopularCategoryResponse(
				tab.name(),
				tab.label(),
				productRepository.findByCategoryCategoryNameAndSalesStatusAndStockQuantityGreaterThan(
					tab.name(), ON_SALE, 0, popularProducts
				).stream().map(this::toPopularProductResponse).toList()
			))
			.toList();
	}

	private PopularProductResponse toPopularProductResponse(ProductEntity product) {
		String category = product.getCategory().getCategoryName();
		JsonNode specs = readSpecs(product.getSpecJson());
		String[] specification = specificationFor(category, product, specs);
		String description = product.getProductDescription();

		if (description == null || description.isBlank()) {
			description = product.getModelName();
		}

		return new PopularProductResponse(
			product.getProductId(),
			category,
			product.getBrand(),
			product.getProductName(),
			product.getPrice(),
			product.getStockQuantity(),
			description,
			product.getImageUrl(),
			specification[0],
			specification[1],
			specification[2],
			specification[3],
			specification[4],
			specification[5],
			specification[6],
			specification[7]
		);
	}

	private JsonNode readSpecs(String specJson) {
		if (specJson == null || specJson.isBlank()) {
			return objectMapper.createObjectNode();
		}

		try {
			return objectMapper.readTree(specJson);
		} catch (Exception ignored) {
			return objectMapper.createObjectNode();
		}
	}

	private String[] specificationFor(
		String category,
		ProductEntity product,
		JsonNode specs
	) {
		return switch (category) {
			case "CPU" -> new String[] {
				"소켓", value(product.getSocketType()),
				"코어 수", jsonValue(specs, "Core Count"),
				"기본 소비전력", watt(product.getPowerConsumption()),
				"마이크로아키텍처", jsonValue(specs, "Microarchitecture")
			};
			case "GPU" -> new String[] {
				"GPU 칩셋", jsonValue(specs, "Chipset"),
				"메모리", jsonValue(specs, "Memory"),
				"권장 파워", watt(product.getRecommendedPower()),
				"코어 클럭", jsonValue(specs, "Core Clock")
			};
			case "RAM" -> new String[] {
				"메모리 규격", value(product.getMemoryType()),
				"구성", jsonValue(specs, "Modules"),
				"동작 속도", jsonValue(specs, "Speed"),
				"색상", jsonValue(specs, "Color")
			};
			case "STORAGE" -> new String[] {
				"저장장치 유형", jsonValue(specs, "Type"),
				"용량", jsonValue(specs, "Capacity"),
				"폼팩터", value(product.getFormFactor()),
				"제조사", value(product.getBrand())
			};
			default -> new String[] {
				"카테고리", value(category),
				"제조사", value(product.getBrand()),
				"모델명", value(product.getModelName()),
				"판매 상태", value(product.getSalesStatus())
			};
		};
	}

	private String jsonValue(JsonNode specs, String fieldName) {
		JsonNode value = specs.get(fieldName);
		return value == null || value.isNull() ? "-" : value.asText("-");
	}

	private String watt(Integer value) {
		return value == null ? "-" : value + " W";
	}

	private String value(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}
}
