package com.boot.compick.quote.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.boot.compick.product.CategoryDisplay;
import com.boot.compick.product.SpecJsonSupport;
import com.boot.compick.product.entity.ProductEntity;

/**
 * 사용자 견적(QuoteService)과 추천 견적 관리(PresetAdminService)가 공유하는
 * "8개 카테고리 + RAM 다중 선택" 검증 로직.
 */
final class QuoteSelectionValidator {

	private static final String RAM_CATEGORY = "RAM";
	private static final String MAINBOARD_CATEGORY = "MAINBOARD";
	private static final int RAM_MODULES_PER_PRODUCT = 2;

	private QuoteSelectionValidator() {
	}

	static void validate(List<ProductEntity> products, Map<Long, Integer> quantityByProductId) {
		Map<String, List<ProductEntity>> byCategory = products.stream()
			.collect(Collectors.groupingBy(product -> product.getCategory().getCategoryName()));

		for (CategoryDisplay.CategoryTab tab : CategoryDisplay.CATEGORY_TABS) {
			List<ProductEntity> itemsInCategory = byCategory.getOrDefault(tab.name(), List.of());
			if (itemsInCategory.isEmpty()) {
				throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"8개 카테고리를 각각 하나 이상 선택해야 합니다."
				);
			}
			if (!RAM_CATEGORY.equals(tab.name()) && itemsInCategory.size() > 1) {
				throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					tab.label() + "는 하나만 선택할 수 있습니다."
				);
			}
		}

		ProductEntity mainboard = byCategory.get(MAINBOARD_CATEGORY).get(0);
		Integer maxRamSlots = SpecJsonSupport.readInt(mainboard.getSpecJson(), "Memory Slots");
		if (maxRamSlots != null) {
			int ramProductQuantity = byCategory.get(RAM_CATEGORY).stream()
				.mapToInt(product -> quantityByProductId.getOrDefault(product.getProductId(), 1))
				.sum();
			int usedRamSlots = ramProductQuantity * RAM_MODULES_PER_PRODUCT;
			if (usedRamSlots > maxRamSlots) {
				throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"이 메인보드는 RAM 상품을 최대 " + (maxRamSlots / RAM_MODULES_PER_PRODUCT)
						+ "개까지 선택할 수 있습니다. (상품 1개당 RAM 2개 구성)"
				);
			}
		}
	}
}
