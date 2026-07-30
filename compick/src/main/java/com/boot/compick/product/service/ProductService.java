package com.boot.compick.product.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.compick.product.dto.PopularProductResponse;
import com.boot.compick.product.entity.ProductEntity;
import com.boot.compick.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class ProductService {

	private static final String ON_SALE = "ON_SALE";
	private static final int POPULAR_PRODUCT_LIMIT = 4;

	private final ProductRepository productRepository;
	private final ObjectMapper objectMapper;

	public ProductService(
		ProductRepository productRepository,
		ObjectMapper objectMapper
	) {
		this.productRepository = productRepository;
		this.objectMapper = objectMapper;
	}

	public List<PopularProductResponse> findPopularProducts() {
		return productRepository
<<<<<<< HEAD
			.findTop4BySalesStatusAndStockQuantityGreaterThanOrderByRatingCountDesc(
=======
			.findTop4BySalesStatusAndStockQuantityGreaterThanOrderByRatingCountDescCreatedAtDesc(
>>>>>>> 48ad55d3c2f8342386c89a8e9f5dff696b5a09ad
				ON_SALE,
				0
			)
			.stream()
			.limit(POPULAR_PRODUCT_LIMIT)
			.map(this::toPopularProductResponse)
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
				"코어 / 스레드", jsonValue(specs, "coreThread"),
				"기본 소비전력", watt(product.getPowerConsumption()),
				"내장 그래픽", jsonValue(specs, "integratedGraphics")
			};
			case "GPU" -> new String[] {
				"GPU 칩셋", jsonValue(specs, "chipset"),
				"메모리", jsonValue(specs, "memory"),
				"권장 파워", watt(product.getRecommendedPower()),
				"인터페이스", jsonValue(specs, "interface")
			};
			case "RAM" -> new String[] {
				"메모리 규격", value(product.getMemoryType()),
				"용량", jsonValue(specs, "capacity"),
				"구성", jsonValue(specs, "configuration"),
				"동작 속도", jsonValue(specs, "speed")
			};
			case "STORAGE" -> new String[] {
				"저장장치 유형", jsonValue(specs, "storageType"),
				"용량", jsonValue(specs, "capacity"),
				"인터페이스", jsonValue(specs, "interface"),
				"폼팩터", value(product.getFormFactor())
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
