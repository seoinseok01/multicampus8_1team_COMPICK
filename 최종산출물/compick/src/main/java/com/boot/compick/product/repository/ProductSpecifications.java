package com.boot.compick.product.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;

import com.boot.compick.product.entity.ProductEntity;

import jakarta.persistence.criteria.Predicate;

public final class ProductSpecifications {

	private ProductSpecifications() {
	}

	public static Specification<ProductEntity> onSale() {
		return (root, query, cb) -> cb.equal(root.get("salesStatus"), "ON_SALE");
	}

	public static Specification<ProductEntity> categoryIs(String categoryName) {
		return (root, query, cb) ->
			cb.equal(root.get("category").get("categoryName"), categoryName);
	}

	public static Specification<ProductEntity> brandIn(List<String> brands) {
		return (root, query, cb) -> root.get("brand").in(brands);
	}

	public static Specification<ProductEntity> priceGreaterThanOrEqual(long minPrice) {
		return (root, query, cb) -> cb.ge(root.get("price"), minPrice);
	}

	public static Specification<ProductEntity> priceLessThanOrEqual(long maxPrice) {
		return (root, query, cb) -> cb.le(root.get("price"), maxPrice);
	}

	public static Specification<ProductEntity> specEquals(String jsonPath, String value) {
		return (root, query, cb) -> {
			Predicate specValue = cb.equal(
				cb.function("json_value", String.class, root.get("specJson"), cb.literal(jsonPath)),
				value
			);
			return specValue;
		};
	}

	/**
	 * 저장장치 상단 SSD/HDD 토글용 필터.
	 * STORAGE 카테고리의 spec_json "Type" 값은 "SSD" 또는 회전수("7200 RPM"/"5400 RPM")로 저장돼 있어
	 * HDD는 두 회전수 값을 OR로 묶어야 한다.
	 */
	public static Specification<ProductEntity> storageTypeIs(String storageType) {
		if ("SSD".equalsIgnoreCase(storageType)) {
			return specEquals("$.\"Type\"", "SSD");
		}
		if ("HDD".equalsIgnoreCase(storageType)) {
			return Specification
				.where(specEquals("$.\"Type\"", "7200 RPM"))
				.or(specEquals("$.\"Type\"", "5400 RPM"))
				.or(specEquals("$.\"Type\"", "HDD"));
		}
		return Specification.where(null);
	}

	public static Specification<ProductEntity> specFiltersFrom(
		String categoryName,
		Map<String, String> specFilters
	) {
		Specification<ProductEntity> combined = Specification.where(null);
		for (Map.Entry<String, String> entry : specFilters.entrySet()) {
			String jsonPath = "$.\"" + entry.getKey() + "\"";
			combined = combined.and(specEquals(jsonPath, entry.getValue()));
		}
		return combined;
	}
}
