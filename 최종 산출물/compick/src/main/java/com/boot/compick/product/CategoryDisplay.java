package com.boot.compick.product;

import java.util.List;

/**
 * 8개 부품 카테고리의 표시 순서와 한글 라벨.
 * 부품 쇼핑(/products)과 견적구매(/quotes/new) 화면이 공유한다.
 */
public final class CategoryDisplay {

	public record CategoryTab(String name, String label) {
	}

	public static final List<CategoryTab> CATEGORY_TABS = List.of(
		new CategoryTab("CPU", "CPU"),
		new CategoryTab("CPU_COOLER", "CPU 쿨러"),
		new CategoryTab("MAINBOARD", "메인보드"),
		new CategoryTab("RAM", "RAM"),
		new CategoryTab("GPU", "그래픽카드"),
		new CategoryTab("STORAGE", "저장장치"),
		new CategoryTab("POWER_SUPPLY", "파워"),
		new CategoryTab("CASE", "케이스")
	);

	public static String labelOf(String categoryName) {
		return CATEGORY_TABS.stream()
			.filter(tab -> tab.name().equals(categoryName))
			.map(CategoryTab::label)
			.findFirst()
			.orElse(categoryName);
	}

	private CategoryDisplay() {
	}
}
